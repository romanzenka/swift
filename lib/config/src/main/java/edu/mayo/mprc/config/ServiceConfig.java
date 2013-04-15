package edu.mayo.mprc.config;

/**
 * The service manager allows the application to send a service a work packet. The service does the work
 * and responds.
 * <p/>
 * A service configuration on its own does not define how is the service provided. It is just an identified endpoint.
 * How the service is run is defined by a runner, which contains a link to the worker itself.
 */
public final class ServiceConfig implements ResourceConfig, NamedResource {
	public static final String RUNNER = "runner";
	public static final String NAME = "name";
	public static final String TYPE = "service";

	private String name;

	private RunnerConfig runner;

	public ServiceConfig() {
	}

	public ServiceConfig(final String name, final RunnerConfig runner) {
		this.name = name;
		this.runner = runner;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public RunnerConfig getRunner() {
		return runner;
	}

	public void setRunner(final RunnerConfig runner) {
		this.runner = runner;
	}

	@Override
	public void save(final ConfigWriter writer) {
		writer.put(NAME, getName());
		writer.put(RUNNER, writer.save(runner));
	}

	@Override
	public void load(final ConfigReader reader) {
		setName(reader.get(NAME));
		setRunner((RunnerConfig) reader.getObject(RUNNER));
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public String toString() {
		return "ServiceConfig{" +
				"name='" + name + '\'' +
				", runner=" + runner +
				'}';
	}
}
