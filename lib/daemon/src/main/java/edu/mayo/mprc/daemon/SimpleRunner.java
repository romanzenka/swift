package edu.mayo.mprc.daemon;

import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * Runs {@link Worker} instances using specified {@link ExecutorService} and
 * {@link WorkerFactory}.
 * A new worker is created for each request.
 */
public final class SimpleRunner extends AbstractRunner {
	private static final Logger LOGGER = Logger.getLogger(SimpleRunner.class);
	public static final String TYPE = "localRunner";
	public static final String NAME = "Local Runner";
	private boolean enabled = true;
	private boolean operational;
	private WorkerFactory factory;
	private ExecutorService executorService;
	private File logOutputFolder;
	private DaemonConnection daemonConnection = null;

	public SimpleRunner() {
		super();
	}

	@Override
	public String toString() {
		return "Daemon Runner for " + getFactory().getUserName();
	}

	protected void processRequest(final DaemonRequest request) {
		final Worker worker;
		try {
			worker = getFactory().createWorker();
		} catch (Exception e) {
			request.sendResponse(e, true);
			return;
		}
		executorService.execute(new RequestProcessor(worker, request));
	}

	@Override
	public void check() {
		factory.checkWorker();
	}

	public void stop() {
		super.stop();
		executorService.shutdownNow();
	}

	public WorkerFactory getFactory() {
		return factory;
	}

	public void setFactory(final WorkerFactory factory) {
		this.factory = factory;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void setExecutorService(final ExecutorService executorService) {
		this.executorService = executorService;
	}

	public File getLogOutputFolder() {
		return logOutputFolder;
	}

	public void setLogOutputFolder(final File logOutputFolder) {
		this.logOutputFolder = logOutputFolder;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isOperational() {
		return operational;
	}

	public void setOperational(final boolean operational) {
		this.operational = operational;
	}

	public DaemonConnection getDaemonConnection() {
		return daemonConnection;
	}

	public void setDaemonConnection(final DaemonConnection daemonConnection) {
		this.daemonConnection = daemonConnection;
	}

	private final class MyProgressReporter implements ProgressReporter {
		private final DaemonRequest request;
		private final LoggingSetup loggingSetup;

		private MyProgressReporter(final DaemonRequest request, final LoggingSetup loggingSetup) {
			this.request = request;
			this.loggingSetup = loggingSetup;
		}

		@Override
		public void reportStart() {
			sendResponse(request, new DaemonProgressMessage(DaemonProgress.RequestProcessingStarted), false);
			reportLogFiles();
		}

		public void reportProgress(final ProgressInfo progressInfo) {
			sendResponse(request, new DaemonProgressMessage(DaemonProgress.UserSpecificProgressInfo, progressInfo), false);
		}

		public void reportSuccess() {
			reportLogFiles();
			sendResponse(request, new DaemonProgressMessage(DaemonProgress.RequestCompleted), true);
		}

		public void reportFailure(final Throwable t) {
			reportLogFiles();
			sendResponse(request, t, true);
		}

		private void reportLogFiles() {
			if (loggingSetup != null) {
				sendResponse(request, new DaemonProgressMessage(DaemonProgress.UserSpecificProgressInfo,
						new AssignedTaskData(loggingSetup.getStandardOutFile(), loggingSetup.getStandardErrorFile())), false);
			}
		}
	}

	public static final class Config extends RunnerConfig {
		public static final int DEFAULT_NUM_THREADS = 1;
		private int numThreads = 1;
		private String logOutputFolder = "var/log";

		public Config() {
		}

		public Config(final ResourceConfig workerFactory) {
			super(workerFactory);
		}

		public int getNumThreads() {
			return numThreads;
		}

		public void setNumThreads(final int numThreads) {
			this.numThreads = numThreads;
		}

		public String getLogOutputFolder() {
			return logOutputFolder;
		}

		public void setLogOutputFolder(final String logOutputFolder) {
			this.logOutputFolder = logOutputFolder;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put("numThreads", getNumThreads(), DEFAULT_NUM_THREADS, "Number of threads");
			writer.put("logOutputFolder", getLogOutputFolder(), "Where to write logs");
			super.save(writer);
		}

		@Override
		public void load(final ConfigReader reader) {
			numThreads = reader.getInteger("numThreads", DEFAULT_NUM_THREADS);
			logOutputFolder = reader.get("logOutputFolder");
			super.load(reader);
		}
	}

	public static final class Factory extends FactoryBase<Config, SimpleRunner> implements FactoryDescriptor {
		private MultiFactory table;

		@Override
		public SimpleRunner create(final Config config, final DependencyResolver dependencies) {
			final SimpleRunner runner = new SimpleRunner();

			runner.setEnabled(true);

			final ResourceConfig workerFactoryConfig = config.getWorkerConfiguration();

			runner.setFactory(getWorkerFactory(getTable(), workerFactoryConfig, dependencies));
			final int numThreads = config.getNumThreads();
			runner.setExecutorService(new SimpleThreadPoolExecutor(numThreads, runner.getFactory().getUserName(), true));
			// Important to convert the log file to absolute, otherwise user.dir is not taken into account and
			// the behavior is inconsistent within the IDE while debugging
			runner.setLogOutputFolder(new File(config.getLogOutputFolder()).getAbsoluteFile());

			return runner;
		}

		public void setTable(final MultiFactory table) {
			this.table = table;
		}

		public MultiFactory getTable() {
			return table;
		}

		private static WorkerFactoryBase getWorkerFactory(final MultiFactory table, final ResourceConfig config, final DependencyResolver dependencies) {
			final WorkerFactoryBase factory =
					(WorkerFactoryBase) table.getFactory(config.getClass());
			// Since the factory by default does not even know what is it creating (that would require duplicating
			// the description into the factory), set the description to the user name of created module.
			factory.setConfig(config);
			factory.setDependencies(dependencies);

			return factory;
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

	private final class RequestProcessor implements Runnable {
		private final Worker worker;
		private final DaemonRequest request;

		public RequestProcessor(final Worker worker, final DaemonRequest request) {
			this.worker = worker;
			this.request = request;
		}

		public void run() {
			final LoggingSetup logging;
			if (worker instanceof NoLoggingWorker) {
				logging = null;
			} else {
				logging = new LoggingSetup(logOutputFolder);
				try {
					logging.startLogging();
				} catch (Exception e) {
					sendResponse(request, new DaemonException("Cannot establish logging for request", e), true);
				}

			}
			try {
				worker.processRequest(request.getWorkPacket(), new MyProgressReporter(request, logging));
			} catch (Exception t) {
				// SWALLOWED
				LOGGER.error("Exception was thrown when processing a request - that means a communication error, or badly implemented worker.\nWorkers must never thrown an exception - they must report a failure.", t);
			} finally {
				if (logging != null) {
					logging.stopLogging();
				}
			}
		}

	}
}
