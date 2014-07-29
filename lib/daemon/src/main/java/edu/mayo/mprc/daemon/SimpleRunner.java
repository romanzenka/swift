package edu.mayo.mprc.daemon;

import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.FactoryDescriptor;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerFactory;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.utilities.log.ChildLog;
import edu.mayo.mprc.utilities.log.ParentLog;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Map;
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
	/**
	 * Configuration for a worker
	 */
	private ResourceConfig config;
	private DependencyResolver dependencies;

	public SimpleRunner() {
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
		} catch (final Exception e) {
			request.sendResponse(e, true);
			return;
		}
		executorService.execute(new RequestProcessor(worker, request));
	}

	@Override
	public String check() {
		LOGGER.info("Checking worker for " + factory.getUserName());
		return factory.create(config, dependencies).check();
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

	public void setConfig(final ResourceConfig config) {
		this.config = config;
	}

	public ResourceConfig getConfig() {
		return config;
	}

	public void setDependencies(final DependencyResolver dependencies) {
		this.dependencies = dependencies;
	}

	public DependencyResolver getDependencies() {
		return dependencies;
	}

	@Override
	public void install(final Map<String, String> params) {
		LOGGER.info("Installing runner for " + getDaemonConnection().getConnectionName());
		// First check if the factory we hold needs installing
		if (factory instanceof Installable) {
			((Installable) factory).install(params);
		}

		// Check whether the created worker itself needs installing
		final Worker worker = factory.create(config, dependencies);
		if (worker instanceof Installable) {
			((Installable) worker).install(params);
		}
	}

	@Override
	public void provideConfiguration(final Map<String, String> currentConfiguration) {
		if (config instanceof UiConfigurationProvider) {
			((UiConfigurationProvider) config).provideConfiguration(currentConfiguration);
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
			runner.setDaemonLoggerFactory(new DaemonLoggerFactory(new File(config.getLogOutputFolder())));

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
			// As we run, all that we log via Log4j will be reported within the child log files
			// We pass the child logger onto the worker, so it can spawn its own children with their own logs
			final RunnerProgressReporter progressReporter = new RunnerProgressReporter(SimpleRunner.this, request);
			// Root logger - we have to start from scratch as we currently do not pass the parent log over the wire
			final ParentLog log = getDaemonLoggerFactory().createLog(request.getWorkPacket().getTaskId(), progressReporter);

			// Here we send a progress message that new log file was established
			final ChildLog childLog = log.createChildLog();

			childLog.startLogging();
			try {
				if (worker instanceof Lifecycle) {
					((Lifecycle) worker).start();
				}

				// Set the log in the progress reporter to the newly spawned child
				progressReporter.setParentLog(childLog);
				worker.processRequest(request.getWorkPacket(), progressReporter);
				if (worker instanceof Lifecycle) {
					((Lifecycle) worker).stop();
				}
			} catch (final Exception t) {
				// SWALLOWED
				LOGGER.error("Exception was thrown when processing a request - that means a communication error, or badly implemented worker.\nWorkers must never thrown an exception - they must report a failure.", t);
			} finally {
				childLog.stopLogging();
			}
		}

	}
}
