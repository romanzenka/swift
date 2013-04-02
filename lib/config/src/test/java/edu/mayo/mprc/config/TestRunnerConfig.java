package edu.mayo.mprc.config;

/**
 * @author Roman Zenka
 */
public final class TestRunnerConfig extends RunnerConfig {
	public TestRunnerConfig(ResourceConfig workerConfiguration) {
		super(workerConfiguration);
	}

	@Override
	public void save(final ConfigWriter writer) {
		writer.put(WORKER, getWorkerConfiguration());
	}

	@Override
	public void load(final ConfigReader reader) {
		setWorkerConfiguration(reader.getObject(WORKER));
	}

	@Override
	public int getPriority() {
		return 0;
	}
}
