package edu.mayo.mprc.config;

import edu.mayo.mprc.MprcException;

/**
 * The service manager allows the application to send a service a work packet. The service does the work
 * and responds.
 * <p/>
 * A service configuration on its own does not define how is the service provided. It is just an identified endpoint.
 * How the service is run is defined by a runner, which contains a link to the worker itself.
 */
public final class ServiceConfig implements ResourceConfig, NamedResource, HierarchicalResource {
	public static final String RUNNER = "runner";
	public static final String NAME = "name";
	public static final String TYPE = "service";

	private String name;

	private DaemonConfig parent;
	private RunnerConfig runner;

	public ServiceConfig() {
	}

	public ServiceConfig(final String name, final RunnerConfig runner) {
		setName(name);
		setRunner(runner);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(final String name) {
		this.name = name;
	}

	public RunnerConfig getRunner() {
		return runner;
	}

	public void setRunner(final RunnerConfig runner) {
		this.runner = runner;
		this.runner.setParentConfig(this);
	}

	@Override
	public DaemonConfig getParentConfig() {
		return parent;
	}

	@Override
	public void setParentConfig(final ResourceConfig parent) {
		if (parent instanceof DaemonConfig) {
			this.parent = (DaemonConfig) parent;
		} else {
			throw new MprcException(String.format("Service %s: the parent can only be a deamon, was given %s", getName(), parent.getClass().getName()));
		}
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
