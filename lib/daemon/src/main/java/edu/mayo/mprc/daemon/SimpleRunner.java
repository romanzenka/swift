package edu.mayo.mprc.daemon;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
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
		return "Daemon Runner for " + factory.getDescription();
	}

	protected void processRequest(final DaemonRequest request) {
		final Worker worker = factory.createWorker();
		if (request.getWorkPacket() instanceof PingPacket) {
			request.sendResponse(new PingResponse(), true);
			return;
		}
		executorService.execute(new RequestProcessor(worker, request));
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

	@XStreamAlias("simpleDaemonRunner")
	public static final class Config extends RunnerConfig {
		private int numThreads = 1;
		private String logOutputFolder = ".";

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
		public Map<String, String> save(final DependencyResolver resolver) {
			final Map<String, String> map = new TreeMap<String, String>();
			map.put("numThreads", String.valueOf(numThreads));
			map.put("logOutputFolder", logOutputFolder);
			return map;
		}

		@Override
		public void load(final Map<String, String> values, final DependencyResolver resolver) {
			final String numThreadsString = values.get("numThreads");
			numThreads = Integer.parseInt(numThreadsString);

			logOutputFolder = values.get("logOutputFolder");
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Factory extends FactoryBase<Config, SimpleRunner> {
		private MultiFactory table;

		@Override
		public SimpleRunner create(final Config config, final DependencyResolver dependencies) {
			final SimpleRunner runner = new SimpleRunner();

			runner.setEnabled(true);

			final ResourceConfig workerFactoryConfig = config.getWorkerConfiguration();

			runner.setFactory(getWorkerFactory(getTable(), workerFactoryConfig, dependencies));
			final int numThreads = config.getNumThreads();
			runner.setExecutorService(new SimpleThreadPoolExecutor(numThreads, runner.getFactory().getDescription(), true));
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
			factory.setDescription(table.getUserName(config));
			factory.setConfig(config);
			factory.setDependencies(dependencies);

			return factory;
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
