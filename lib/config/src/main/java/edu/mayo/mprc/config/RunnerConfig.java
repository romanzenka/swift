package edu.mayo.mprc.config;

import com.google.common.base.Strings;
import edu.mayo.mprc.MprcException;

/**
 * A runner is code that waits for work packets and then passes them to a worker.
 */
public abstract class RunnerConfig implements ResourceConfig, HierarchicalResource {
	public static final String LOG_OUTPUT_FOLDER = "logOutputFolder";
	private ResourceConfig workerConfiguration;
	private String logOutputFolder;
	private ServiceConfig parent;

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
		if (!Strings.isNullOrEmpty(logOutputFolder)) {
			return logOutputFolder;
		}
		// We do not set a log folder ourselves. Use the daemon's log
		return parent.getParentConfig().getLogOutputFolder();
	}

	public void setLogOutputFolder(final String logOutputFolder) {
		if (logOutputFolder != null) {
			this.logOutputFolder = logOutputFolder.trim();
		} else {
			this.logOutputFolder = logOutputFolder;
		}
	}

	@Override
	public String toString() {
		return "RunnerConfig{" +
				"workerConfiguration=" + workerConfiguration +
				'}';
	}

	@Override
	public void save(final ConfigWriter writer) {
		writer.put(WORKER, getWorkerConfiguration());
		writer.put(LOG_OUTPUT_FOLDER, logOutputFolder);
	}

	@Override
	public void load(final ConfigReader reader) {
		setWorkerConfiguration(reader.getObject(WORKER));
		setLogOutputFolder(reader.get(LOG_OUTPUT_FOLDER));
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public ServiceConfig getParentConfig() {
		return parent;
	}

	@Override
	public void setParentConfig(final ResourceConfig parent) {
		if (parent instanceof ServiceConfig) {
			this.parent = (ServiceConfig) parent;
		} else {
			throw new MprcException(String.format("Parent of a runner config can only be a service, was %s", parent.getClass().getName()));
		}
	}
}
