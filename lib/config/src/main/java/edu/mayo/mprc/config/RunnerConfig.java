package edu.mayo.mprc.config;

/**
 * A runner is code that waits for work packets and then passes them to a worker.
 */
public abstract class RunnerConfig implements ResourceConfig {
	public static final String LOG_OUTPUT_FOLDER = "logOutputFolder";
	private ResourceConfig workerConfiguration;

	public static final String WORKER = "worker";

	public RunnerConfig() {
	}

	public RunnerConfig(final ResourceConfig workerConfiguration) {
		this.workerConfiguration = workerConfiguration;
	}

	public ResourceConfig getWorkerConfiguration() {
		return workerConfiguration;
	}

	public void setWorkerConfiguration(final ResourceConfig workerConfiguration) {
		this.workerConfiguration = workerConfiguration;
	}

	@Override
	public String toString() {
		return "RunnerConfig{" +
				"workerConfiguration=" + workerConfiguration +
				'}';
	}

	@Override
	public void save(ConfigWriter writer) {
		writer.put(WORKER, getWorkerConfiguration());
	}

	@Override
	public void load(ConfigReader reader) {
		setWorkerConfiguration(reader.getObject("worker"));
	}

	@Override
	public int getPriority() {
		return 0;
	}
}
