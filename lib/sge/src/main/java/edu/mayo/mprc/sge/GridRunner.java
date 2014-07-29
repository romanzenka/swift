package edu.mayo.mprc.sge;

import com.google.common.base.Strings;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.FactoryDescriptor;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.*;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.messaging.ResponseListener;
import edu.mayo.mprc.messaging.ServiceFactory;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.log.ParentLog;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Daemon Runner that sends {@link SgePacket} objects to the Sun Grid Engine.
 * The {@link SgePacket} is
 * saved as a shared xml file and a {@link File} URI that represents the shared xml file
 * is sent through the Grid.
 */
public final class GridRunner extends AbstractRunner {

	public static final String TYPE = "sgeRunner";
	public static final String NAME = "Sun Grid Engine Runner";

	private static final String WRAPPER_SCRIPT = "wrapperScript";
	private static final String NATIVE_SPECIFICATION = "nativeSpecification";
	private static final String MEMORY_REQUIREMENT = "memoryRequirement";
	private static final String QUEUE_NAME = "queueName";

	private boolean enabled;
	private boolean operational;
	private DaemonConnection daemonConnection;

	//Grid specific variables
	private GridEngineJobManager manager;
	private String nativeSpecification;
	private String queueName;
	private String memoryRequirement;
	private String wrapperScript;

	private ResourceConfig workerFactoryConfig;

	private GridScriptFactory gridScriptFactory;
	private FileTokenFactory fileTokenFactory;
	private ServiceFactory serviceFactory;

	private static AtomicLong uniqueId = new AtomicLong(System.currentTimeMillis());

	private static final Logger LOGGER = Logger.getLogger(GridRunner.class);

	public GridRunner() {
	}

	@Override
	public void start() {
		super.start();
		manager.start();
		serviceFactory.start();
		daemonConnection.start();
	}

	@Override
	public void stop() {
		// Disables message processing
		enabled = false;
		serviceFactory.stop();
		manager.stop();
		super.stop();
		daemonConnection.stop();
	}

	@Override
	public String toString() {
		return "Grid Daemon Runner for " + (daemonConnection == null ? "(null)" : daemonConnection.getConnectionName());
	}

	@Override
	protected void processRequest(final DaemonRequest request) {
		final GridWorkPacket gridWorkPacket = getBaseGridWorkPacket(gridScriptFactory.getApplicationName(wrapperScript));
		final File daemonWorkerAllocatorInputFile = new File(getSharedTempDirectory(), queueName + "_" + uniqueId.incrementAndGet());

		try {
			final SgeMessageListener allocatorListener = new SgeMessageListener(request);
			final SgePacket gridDaemonAllocatorInputObject =
					new SgePacket(
							serviceFactory.serializeRequest(request.getWorkPacket(), getDaemon().getResponseDispatcher(), allocatorListener)
							, workerFactoryConfig
							, fileTokenFactory.getDaemonConfigInfo(),
							getDaemonLoggerFactory().getLogFolder());

			writeWorkerAllocatorInputObject(daemonWorkerAllocatorInputFile, gridDaemonAllocatorInputObject);

			final List<String> parameters = gridScriptFactory.getParameters(wrapperScript, daemonWorkerAllocatorInputFile);
			gridWorkPacket.setParameters(parameters);

			// Set our own listener to the work packet progress. When the packet returns, the execution will be resumed
			final MyWorkPacketStateListener listener = new MyWorkPacketStateListener(request, daemonWorkerAllocatorInputFile, allocatorListener);
			gridWorkPacket.setListener(listener);
			gridWorkPacket.setPriority(request.getWorkPacket().getPriority());
			// Run the job
			final String requestId = manager.passToGridEngine(gridWorkPacket);

			// Report the information about the running task to the caller, making sure they get the task id and the logs
			final AssignedTaskData data = new AssignedTaskData(requestId);

			final RunnerProgressReporter reporter = new RunnerProgressReporter(this, request);

			// Report the assigned ID
			reporter.reportProgress(data);

			final ParentLog log = getDaemonLoggerFactory().createLog(request.getWorkPacket().getTaskId(), reporter);

			// Report that we spawned a child with its own SGE log, we use the SGE-based log paths for this
			log.createChildLog(gridWorkPacket.getOutputLogFilePath(), gridWorkPacket.getErrorLogFilePath());

			// We are not done yet! The grid work packet's progress listener will get called when the state of the task changes,
			// and either mark the task failed or successful.
		} catch (Exception t) {
			FileUtilities.quietDelete(daemonWorkerAllocatorInputFile);
			final DaemonException daemonException = new DaemonException("Failed passing work packet " + gridWorkPacket.toString() + " to grid engine", t);
			LOGGER.error(MprcException.getDetailedMessage(daemonException), daemonException);
			sendResponse(request, daemonException, true);
			throw daemonException;
		}
	}

	@Override
	public String check() {
		// TODO: We will need to submit a 'checking packet' to the grid and collect response
		return null;
	}

	private static void writeWorkerAllocatorInputObject(final File file, final SgePacket object) throws IOException {
		BufferedWriter bufferedWriter = null;

		try {
			final XStream xStream = new XStream(new DomDriver());
			bufferedWriter = new BufferedWriter(new FileWriter(file));
			bufferedWriter.write(xStream.toXML(object));
		} finally {
			FileUtilities.closeQuietly(bufferedWriter);
		}
	}

	public void setServiceFactory(final ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	@Override
	public void install(Map<String, String> params) {
		// TODO: We will need to submit an 'installing packet' to the grid and collect response
	}

	@Override
	public void provideConfiguration(Map<String, String> currentConfiguration) {
		if (workerFactoryConfig instanceof UiConfigurationProvider) {
			((UiConfigurationProvider) workerFactoryConfig).provideConfiguration(currentConfiguration);
		}
	}

	/**
	 * Listens to RMI calls from the process running within SGE. None of the messages is final.
	 */
	private class SgeMessageListener implements ResponseListener {
		private static final long serialVersionUID = 20090324L;
		private DaemonRequest request;
		private Throwable lastThrowable;

		SgeMessageListener(final DaemonRequest request) {
			this.request = request;
		}

		public synchronized Throwable getLastThrowable() {
			return lastThrowable;
		}

		@Override
		public void responseReceived(final Serializable response, final boolean isLast) {
			if (response instanceof Throwable) {
				// We do send an error message now.
				// That is done once SGE detects termination of the process.
				synchronized (this) {
					lastThrowable = (Throwable) response;
				}
			} else {
				// Not final - a progress message
				sendResponse(request, response, false);
			}
		}
	}

	/**
	 * This listener is running within the grid engine monitor thread.
	 */
	private class MyWorkPacketStateListener implements GridWorkPacketStateListener {
		private boolean reported;
		private final DaemonRequest request;
		private final File sgePacketFile;
		private final SgeMessageListener allocatorListener;

		MyWorkPacketStateListener(final DaemonRequest request, final File sgePacketFile, final SgeMessageListener allocatorListener) {
			reported = false;
			this.request = request;
			this.sgePacketFile = sgePacketFile;
			this.allocatorListener = allocatorListener;
		}

		/**
		 * Process message from the grid engine itself.
		 * In case the process failed, we keep the work packet around so the developer can reproduce the error.
		 *
		 * @param w Work packet whose state changed
		 */
		@Override
		public void stateChanged(final GridWorkPacket w) {
			if (w == null) {
				return;
			}
			// We report state change just once.
			if (!reported) {
				try {
					if (w.getPassed()) {
						// This is the last response we will send - request is completed.
						// There might have been an error from RMI, check that
						if (allocatorListener.getLastThrowable() == null) {
							sendResponse(request, new DaemonProgressMessage(DaemonProgress.RequestCompleted), true);
						} else {
							sendResponse(request, new DaemonException(allocatorListener.getLastThrowable()), true);
						}
					} else if (w.getFailed()) {
						// This is the last response we will send - request failed
						if (allocatorListener.getLastThrowable() == null) {
							sendResponse(request, new DaemonException(w.getErrorMessage()), true);
						} else {
							sendResponse(request, new DaemonException(w.getErrorMessage(), allocatorListener.getLastThrowable()), true);
						}
					}
					reported = true;
				} finally {
					if (!w.getFailed()) {
						//Delete workPacket file
						LOGGER.debug("Deleting sge packet file: " + sgePacketFile.getAbsolutePath());
						FileUtilities.quietDelete(sgePacketFile);
					} else {
						LOGGER.warn("Retaining sge packet file: " + sgePacketFile.getAbsolutePath());
					}
				}
			}
		}
	}

	@Override
	public boolean isOperational() {
		return operational || !enabled;
	}

	@Override
	public void setOperational(final boolean operational) {
		this.operational = operational;
	}

	private GridWorkPacket getBaseGridWorkPacket(final String command) {
		final GridWorkPacket gridWorkPacket = new GridWorkPacket(command, null);

		gridWorkPacket.setNativeSpecification(nativeSpecification);
		gridWorkPacket.setQueueName(queueName);
		gridWorkPacket.setMemoryRequirement(memoryRequirement);

		gridWorkPacket.setWorkingFolder(new File(".").getAbsolutePath());
		gridWorkPacket.setLogFolder(FileUtilities.getDateBasedDirectory(getDaemonLoggerFactory().getLogFolder(), new Date()).getAbsolutePath());

		return gridWorkPacket;
	}

	public File getSharedTempDirectory() {
		return getDaemon().getTempFolder();
	}

	public GridEngineJobManager getManager() {
		return manager;
	}

	public void setManager(final GridEngineJobManager manager) {
		this.manager = manager;
	}

	public ResourceConfig getWorkerFactoryConfig() {
		return workerFactoryConfig;
	}

	public void setWorkerFactoryConfig(final ResourceConfig workerFactoryConfig) {
		this.workerFactoryConfig = workerFactoryConfig;
	}

	public String getNativeSpecification() {
		return nativeSpecification;
	}

	public void setNativeSpecification(final String nativeSpecification) {
		this.nativeSpecification = nativeSpecification;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(final String queueName) {
		this.queueName = queueName;
	}

	public String getMemoryRequirement() {
		return memoryRequirement;
	}

	public void setMemoryRequirement(final String memoryRequirement) {
		this.memoryRequirement = memoryRequirement;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public GridScriptFactory getGridScriptFactory() {
		return gridScriptFactory;
	}

	public void setGridScriptFactory(final GridScriptFactory gridScriptFactory) {
		this.gridScriptFactory = gridScriptFactory;
	}

	@Override
	public DaemonConnection getDaemonConnection() {
		return daemonConnection;
	}

	@Override
	public void setDaemonConnection(final DaemonConnection daemonConnection) {
		this.daemonConnection = daemonConnection;
	}

	public String getWrapperScript() {
		return wrapperScript;
	}

	public void setWrapperScript(final String wrapperScript) {
		this.wrapperScript = wrapperScript;
	}

	public FileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
		this.fileTokenFactory = fileTokenFactory;
	}

	public static final class Config extends RunnerConfig {
		private String queueName;
		private String memoryRequirement;
		private String nativeSpecification;
		private String wrapperScript;

		public Config() {
		}

		public Config(final ResourceConfig workerConfig) {
			super(workerConfig);
		}

		public String getQueueName() {
			return queueName;
		}

		public void setQueueName(final String queueName) {
			this.queueName = queueName;
		}

		public String getMemoryRequirement() {
			return memoryRequirement;
		}

		public void setMemoryRequirement(final String memoryRequirement) {
			this.memoryRequirement = memoryRequirement;
		}

		public String getNativeSpecification() {
			return nativeSpecification;
		}

		public void setNativeSpecification(final String nativeSpecification) {
			this.nativeSpecification = nativeSpecification;
		}

		public String getWrapperScript() {
			return wrapperScript;
		}

		public void setWrapperScript(final String wrapperScript) {
			this.wrapperScript = wrapperScript;
		}

		@Override
		public void save(final ConfigWriter writer) {
			super.save(writer);
			writer.put(QUEUE_NAME, getQueueName(), "Name of the SGE queue");
			writer.put(MEMORY_REQUIREMENT, getMemoryRequirement(), "", "Memory requirements for the SGE queue");
			writer.put(NATIVE_SPECIFICATION, getNativeSpecification(), "", "Native specification (additional parameters) for the SGE queue");
			writer.put(WRAPPER_SCRIPT, getWrapperScript(), "A wrapper script that will ensure smooth execution of the Swift component (create/tear down environment). Takes the command to execute as its parameter.");
		}

		@Override
		public void load(final ConfigReader reader) {
			setQueueName(reader.get(QUEUE_NAME));
			setMemoryRequirement(reader.get(MEMORY_REQUIREMENT, ""));
			setNativeSpecification(reader.get(NATIVE_SPECIFICATION, ""));
			setWrapperScript(reader.get(WRAPPER_SCRIPT));
			super.load(reader);
		}
	}

	@Component("gridDaemonRunnerFactory")
	public static final class Factory extends FactoryBase<Config, GridRunner> implements FactoryDescriptor {

		private GridEngineJobManager gridEngineManager;
		private GridScriptFactory gridScriptFactory;
		private FileTokenFactory fileTokenFactory;
		private ServiceFactory serviceFactory;

		@Override
		public GridRunner create(final Config config, final DependencyResolver dependencies) {
			final GridRunner runner = new GridRunner();

			runner.setServiceFactory(getServiceFactory());
			runner.setEnabled(true);

			runner.setQueueName(config.getQueueName());

			if (config.getMemoryRequirement() != null) {
				runner.setMemoryRequirement(config.getMemoryRequirement());
			}

			if (config.getNativeSpecification() != null) {
				runner.setNativeSpecification(config.getNativeSpecification());
			}

			runner.setGridScriptFactory(gridScriptFactory);
			runner.setManager(gridEngineManager);
			runner.setWrapperScript(getAbsoluteExecutablePath(config));
			runner.setWorkerFactoryConfig(config.getWorkerConfiguration());
			runner.setFileTokenFactory(fileTokenFactory);
			runner.setDaemonLoggerFactory(new DaemonLoggerFactory(new File(config.getLogOutputFolder())));

			return runner;
		}

		private static String getAbsoluteExecutablePath(final Config config) {
			if (Strings.isNullOrEmpty(config.getWrapperScript())) {
				return "";
			}
			return FileUtilities.getAbsoluteFileForExecutables(new File(config.getWrapperScript())).getPath();
		}

		public GridEngineJobManager getGridEngineManager() {
			return gridEngineManager;
		}

		@Resource(name = "gridEngineJobManager")
		public void setGridEngineManager(final GridEngineJobManager gridEngineManager) {
			this.gridEngineManager = gridEngineManager;
		}

		public GridScriptFactory getGridScriptFactory() {
			return gridScriptFactory;
		}

		@Resource(name = "gridScriptFactory")
		public void setGridScriptFactory(final GridScriptFactory gridScriptFactory) {
			this.gridScriptFactory = gridScriptFactory;
		}

		public FileTokenFactory getFileTokenFactory() {
			return fileTokenFactory;
		}

		@Resource(name = "fileTokenFactory")
		public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
			this.fileTokenFactory = fileTokenFactory;
		}

		public ServiceFactory getServiceFactory() {
			return serviceFactory;
		}

		@Resource(name = "serviceFactory")
		public void setServiceFactory(final ServiceFactory serviceFactory) {
			this.serviceFactory = serviceFactory;
		}

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public String getUserName() {
			return NAME;
		}

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public Class<? extends ResourceConfig> getConfigClass() {
			return Config.class;
		}

		@Override
		public ServiceUiFactory getServiceUiFactory() {
			return null;
		}
	}
}
