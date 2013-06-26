package edu.mayo.mprc.config;

/**
 * A runner is code that waits for work packets and then passes them to a worker.
 */
public abstract class RunnerConfig implements ResourceConfig {
	public static final String LOG_OUTPUT_FOLDER = "logOutputFolder";
	private ResourceConfig workerConfiguration;
	private String logOutputFolder = "var/log";

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

	public String getLogOutputFolder() {
		return logOutputFolder;
	}

	public void setLogOutputFolder(final String logOutputFolder) {
		this.logOutputFolder = logOutputFolder;
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
		writer.put(LOG_OUTPUT_FOLDER, getLogOutputFolder(), "Where to write logs");
	}

	@Override
	public void load(ConfigReader reader) {
		setWorkerConfiguration(reader.getObject("worker"));
		logOutputFolder = reader.get(LOG_OUTPUT_FOLDER);
	}

	@Override
	public int getPriority() {
		return 0;
	}
}
