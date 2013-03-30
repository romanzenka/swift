package edu.mayo.mprc.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * A runner is code that waits for work packets and then passes them to a worker.
 */
@XStreamAlias("runner")
public abstract class RunnerConfig implements ResourceConfig {
	private WorkerConfig workerConfiguration;

	public RunnerConfig() {
	}

	public RunnerConfig(final WorkerConfig workerConfiguration) {
		this.workerConfiguration = workerConfiguration;
	}

	public WorkerConfig getWorkerConfiguration() {
		return workerConfiguration;
	}

	public void setWorkerConfiguration(final WorkerConfig workerConfiguration) {
		this.workerConfiguration = workerConfiguration;
	}

	@Override
	public String toString() {
		return "RunnerConfig{" +
				"workerConfiguration=" + workerConfiguration +
				'}';
	}

	/**
	 * Runner config is written inline within its enclosing service, including the worker config.
	 *
	 * @param writer Writer to write the config into.
	 */
	public abstract void writeInline(ConfigWriter writer);
}
