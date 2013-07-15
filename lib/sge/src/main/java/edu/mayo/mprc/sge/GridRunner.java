package edu.mayo.mprc.sge;


import com.google.common.base.Strings;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.*;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.messaging.rmi.BoundMessenger;
import edu.mayo.mprc.messaging.rmi.MessageListener;
import edu.mayo.mprc.messaging.rmi.MessengerFactory;
import edu.mayo.mprc.messaging.rmi.SimpleOneWayMessenger;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Daemon Runner that sends {@link SgePacket} objects to the Sun Grid Engine.
 * The {@link SgePacket} is
 * saved as a shared xml file and a {@link java.io.File} URI that represents the shared xml file
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

	private MessengerFactory messengerFactory;
	private GridScriptFactory gridScriptFactory;
	private FileTokenFactory fileTokenFactory;

	private static AtomicLong uniqueId = new AtomicLong(System.currentTimeMillis());

	private static final Logger LOGGER = Logger.getLogger(GridRunner.class);

	public GridRunner() {
	}

	public void stop() {
		super.stop();
		// Disables message processing
		enabled = false;
		if (manager != null) {
			manager.close();
		}
	}

	@Override
	public String toString() {
		return "Grid Daemon Runner for " + (daemonConnection == null ? "(null)" : daemonConnection.getConnectionName());
	}

	protected void processRequest(final DaemonRequest request) {
		final GridWorkPacket gridWorkPacket = getBaseGridWorkPacket(gridScriptFactory.getApplicationName(wrapperScript));
		final File daemonWorkerAllocatorInputFile = new File(getSharedTempDirectory(), queueName + "_" + uniqueId.incrementAndGet());

		try {
			final BoundMessenger<SimpleOneWayMessenger> boundMessenger = messengerFactory.createOneWayMessenger();
			final SgeMessageListener allocatorListener = new SgeMessageListener(request);
			boundMessenger.getMessenger().addMessageListener(allocatorListener);

			final SgePacket gridDaemonAllocatorInputObject =
					new SgePacket(request.getWorkPacket()
							, boundMessenger.getMessengerInfo()
							, workerFactoryConfig
							, fileTokenFactory.getDaemonConfigInfo());

			if (getSharedTempDirectory() != null) {
				gridDaemonAllocatorInputObject.setSharedTempDirectory(getSharedTempDirectory().getAbsolutePath());
			}

			writeWorkerAllocatorInputObject(daemonWorkerAllocatorInputFile, gridDaemonAllocatorInputObject);

			final List<String> parameters = gridScriptFactory.getParameters(wrapperScript, daemonWorkerAllocatorInputFile);
			gridWorkPacket.setParameters(parameters);

			// Set our own listener to the work packet progress. When the packet returns, the execution will be resumed
			final MyWorkPacketStateListener listener = new MyWorkPacketStateListener(request, daemonWorkerAllocatorInputFile, boundMessenger, allocatorListener);
			gridWorkPacket.setListener(listener);
			gridWorkPacket.setPriority(request.getWorkPacket().getPriority());
			// Run the job
			final String requestId = manager.passToGridEngine(gridWorkPacket);

			// Report the information about the running task to the caller, making sure they get the task id and the logs
			final AssignedTaskData data = new AssignedTaskData(requestId, gridWorkPacket.getOutputLogFilePath(), gridWorkPacket.getErrorLogFilePath());

			// The listener will report the task information on failure/success so the logs have a chance to get
			// transferred.
			listener.setTaskData(data);

			// Report the assigned ID and log files. Since the logs are not filled in yet, they would be available on the caller only if it shares disk space
			reportTaskData(request, data);

			// We are not done yet! The grid work packet's progress listener will get called when the state of the task changes,
			// and either mark the task failed or successful.
		} catch (Exception t) {
			FileUtilities.quietDelete(daemonWorkerAllocatorInputFile);
			final DaemonException daemonException = new DaemonException("Failed passing work packet " + gridWorkPacket.toString() + " to grid engine", t);
			sendResponse(request, daemonException, true);
			throw daemonException;
		}
	}

	@Override
	public void check() {
		// Not implemented yet
	}

	void reportTaskData(final DaemonRequest request, final AssignedTaskData data) {
		// Clone the data object to make sure the file tokens get updated (files might come into existence that
		// did not exist before).
		final AssignedTaskData clonedTaskData = new AssignedTaskData(data.getAssignedId(), data.getOutputLogFile(), data.getErrorLogFile());
		sendResponse(request, new DaemonProgressMessage(DaemonProgress.UserSpecificProgressInfo,
				clonedTaskData), false);
	}

	private static void writeWorkerAllocatorInputObject(File file, SgePacket object) throws IOException {
		BufferedWriter bufferedWriter = null;

		try {
			final XStream xStream = new XStream(new DomDriver());
			bufferedWriter = new BufferedWriter(new FileWriter(file));
			bufferedWriter.write(xStream.toXML(object));
		} finally {
			FileUtilities.closeQuietly(bufferedWriter);
		}
	}

	/**
	 * Listens to RMI calls from the process running within SGE. None of the messages is final.
	 */
	private class SgeMessageListener implements MessageListener {
		private static final long serialVersionUID = 20090324L;
		private DaemonRequest request;
		private Throwable lastThrowable;

		public SgeMessageListener(final DaemonRequest request) {
			this.request = request;
		}

		public synchronized Throwable getLastThrowable() {
			return lastThrowable;
		}

		public void messageReceived(final Object message) {
			if (message instanceof Serializable) {
				if (message instanceof Throwable) {
					// We do send an error message now.
					// That is done once SGE detects termination of the process.
					synchronized (this) {
						lastThrowable = (Throwable) message;
					}
				} else {
					// Not final - a progress message
					sendResponse(request, (Serializable) message, false);
				}
			} else {
				sendResponse(request, "Progress message from SGE " + message.toString(), false);
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
		private final BoundMessenger boundMessenger;
		private final SgeMessageListener allocatorListener;

		/**
		 * Needs to be synchronized - being set from different thread
		 */
		private AssignedTaskData taskData;

		/**
		 * @param allocatorListener The listener for the RMI messages. We use it so we can send an exception that was cached when SGE terminates.
		 */
		public MyWorkPacketStateListener(final DaemonRequest request, final File sgePacketFile, final BoundMessenger boundMessenger, final SgeMessageListener allocatorListener) {
			reported = false;
			this.request = request;
			this.sgePacketFile = sgePacketFile;
			this.boundMessenger = boundMessenger;
			this.allocatorListener = allocatorListener;
		}

		/**
		 * Process message from the grid engine itself.
		 * In case the process failed, we keep the work packet around so the developer can reproduce the error.
		 *
		 * @param w Work packet whose state changed
		 */
		public void stateChanged(final GridWorkPacket w) {
			if (w == null) {
				return;
			}
			// We report state change just once.
			if (!reported) {
				try {
					// First, re-report the output and error logs.
					// If we are running on a different machine, the logs are now complete
					// and need to be uploaded to the caller.
					final AssignedTaskData toReport = getTaskData();
					if (toReport != null) {
						reportTaskData(request, toReport);
					}
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

					boundMessenger.dispose();
				} catch (IOException e) {
					LOGGER.warn("Error disposing messenger: " + boundMessenger.getMessengerInfo().getMessengerRemoteName(), e);
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

		public synchronized AssignedTaskData getTaskData() {
			return taskData;
		}

		public synchronized void setTaskData(AssignedTaskData taskData) {
			this.taskData = taskData;
		}
	}

	public boolean isOperational() {
		return operational || !enabled;
	}

	public void setOperational(final boolean operational) {
		this.operational = operational;
	}

	private GridWorkPacket getBaseGridWorkPacket(final String command) {
		final GridWorkPacket gridWorkPacket = new GridWorkPacket(command, null);

		gridWorkPacket.setNativeSpecification(nativeSpecification);
		gridWorkPacket.setQueueName(queueName);
		gridWorkPacket.setMemoryRequirement(memoryRequirement);

		gridWorkPacket.setWorkingFolder(new File(".").getAbsolutePath());
		gridWorkPacket.setLogFolder(FileUtilities.getDateBasedDirectory(getLogDirectory(), new Date()).getAbsolutePath());

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

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public MessengerFactory getMessengerFactory() {
		return messengerFactory;
	}

	public void setMessengerFactory(final MessengerFactory messengerFactory) {
		this.messengerFactory = messengerFactory;
	}

	public GridScriptFactory getGridScriptFactory() {
		return gridScriptFactory;
	}

	public void setGridScriptFactory(final GridScriptFactory gridScriptFactory) {
		this.gridScriptFactory = gridScriptFactory;
	}

	public DaemonConnection getDaemonConnection() {
		return daemonConnection;
	}

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

		public void save(final ConfigWriter writer) {
			super.save(writer);
			writer.put(QUEUE_NAME, getQueueName(), "Name of the SGE queue");
			writer.put(MEMORY_REQUIREMENT, getMemoryRequirement(), "", "Memory requirements for the SGE queue");
			writer.put(NATIVE_SPECIFICATION, getNativeSpecification(), "", "Native specification (additional parameters) for the SGE queue");
			writer.put(WRAPPER_SCRIPT, getWrapperScript(), "A wrapper script that will ensure smooth execution of the Swift component (create/tear down environment). Takes the command to execute as its parameter.");
		}

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
		private MessengerFactory messengerFactory;
		private FileTokenFactory fileTokenFactory;

		@Override
		public GridRunner create(final Config config, final DependencyResolver dependencies) {
			// Check that SGE is initialized. If it cannot initialize, we cannot create the runners
			gridEngineManager.initialize();

			final GridRunner runner = new GridRunner();

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
			runner.setMessengerFactory(messengerFactory);
			runner.setWrapperScript(getAbsoluteExecutablePath(config));
			runner.setWorkerFactoryConfig(config.getWorkerConfiguration());
			runner.setFileTokenFactory(fileTokenFactory);

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

		public MessengerFactory getMessengerFactory() {
			return messengerFactory;
		}

		@Resource(name = "messengerFactory")
		public void setMessengerFactory(final MessengerFactory messengerFactory) {
			this.messengerFactory = messengerFactory;
		}

		public FileTokenFactory getFileTokenFactory() {
			return fileTokenFactory;
		}

		@Resource(name = "fileTokenFactory")
		public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
			this.fileTokenFactory = fileTokenFactory;
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
