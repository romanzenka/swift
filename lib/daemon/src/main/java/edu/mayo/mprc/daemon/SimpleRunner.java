package edu.mayo.mprc.daemon;

import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;

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
	private WorkerFactory<ResourceConfig, Worker> factory;
	private ExecutorService executorService;
	private DaemonConnection daemonConnection = null;
	private ResourceConfig config;
	private DependencyResolver dependencies;

	public SimpleRunner() {
		super();
	}

	@Override
	public String toString() {
		return "Daemon Runner for " + getFactory().getUserName();
	}

	@Override
	protected void processRequest(final DaemonRequest request) {
		final Worker worker;
		try {
			worker = getFactory().create(config, dependencies);
		} catch (Exception e) {
			request.sendResponse(e, true);
			return;
		}
		executorService.execute(new RequestProcessor(worker, request));
	}

	@Override
	public void check() {
		factory.create(config, dependencies).check();
	}

	@Override
	public void stop() {
		super.stop();
		executorService.shutdownNow();
	}

	public WorkerFactory<ResourceConfig, Worker> getFactory() {
		return factory;
	}

	public void setFactory(final WorkerFactory<ResourceConfig, Worker> factory) {
		this.factory = factory;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void setExecutorService(final ExecutorService executorService) {
		this.executorService = executorService;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isOperational() {
		return operational;
	}

	@Override
	public void setOperational(final boolean operational) {
		this.operational = operational;
	}

	@Override
	public DaemonConnection getDaemonConnection() {
		return daemonConnection;
	}

	@Override
	public void setDaemonConnection(final DaemonConnection daemonConnection) {
		this.daemonConnection = daemonConnection;
	}

	public void setConfig(ResourceConfig config) {
		this.config = config;
	}

	public ResourceConfig getConfig() {
		return config;
	}

	public void setDependencies(DependencyResolver dependencies) {
		this.dependencies = dependencies;
	}

	public DependencyResolver getDependencies() {
		return dependencies;
	}

	@Override
	public void install() {
		// First check if the factory we hold needs installing
		if (factory instanceof Installable) {
			((Installable) factory).install();
		}

		// Check whether the created worker itself needs installing
		final Worker worker = factory.create(config, dependencies);
		if (worker instanceof Installable) {
			((Installable) worker).install();
		}
	}

	private final class MyProgressReporter implements ProgressReporter {
		private final DaemonRequest request;
		private final LoggingSetup loggingSetup;

		private MyProgressReporter(final DaemonRequest request, final LoggingSetup loggingSetup) {
			this.request = request;
			this.loggingSetup = loggingSetup;
		}

		@Override
		public void reportStart(final String hostString) {
			sendResponse(request, new DaemonProgressMessage(hostString), false);
			reportLogFiles();
		}

		@Override
		public void reportProgress(final ProgressInfo progressInfo) {
			sendResponse(request, new DaemonProgressMessage(DaemonProgress.UserSpecificProgressInfo, progressInfo), false);
		}

		@Override
		public void reportSuccess() {
			reportLogFiles();
			sendResponse(request, new DaemonProgressMessage(DaemonProgress.RequestCompleted), true);
		}

		@Override
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

		@Override
		public void save(final ConfigWriter writer) {
			writer.put("numThreads", getNumThreads(), DEFAULT_NUM_THREADS, "Number of threads");
			super.save(writer);
		}

		@Override
		public void load(final ConfigReader reader) {
			numThreads = reader.getInteger("numThreads", DEFAULT_NUM_THREADS);
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

			runner.setFactory(getWorkerFactory(getTable(), workerFactoryConfig));
			runner.setConfig(workerFactoryConfig);
			runner.setDependencies(dependencies);
			final int numThreads = config.getNumThreads();
			runner.setExecutorService(new SimpleThreadPoolExecutor(numThreads, runner.getFactory().getUserName(), true));

			return runner;
		}

		public void setTable(final MultiFactory table) {
			this.table = table;
		}

		public MultiFactory getTable() {
			return table;
		}

		private static WorkerFactoryBase getWorkerFactory(final MultiFactory table, final ResourceConfig config) {
			final WorkerFactoryBase factory =
					((WorkerFactoryBase) table.getFactory(config.getClass()));
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

		RequestProcessor(final Worker worker, final DaemonRequest request) {
			this.worker = worker;
			this.request = request;
		}

		@Override
		public void run() {
			final LoggingSetup logging;
			if (worker instanceof NoLoggingWorker) {
				logging = null;
			} else {
				logging = new LoggingSetup(getLogDirectory());
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
