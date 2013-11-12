package edu.mayo.mprc.daemon;

import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerFactory;

/**
 * Fake factory - single worker in a single thread has the same worker object recycled all the time
 */
final class TestWorkerFactory implements WorkerFactory<ResourceConfig, Worker> {
	private final Worker finalWorker;

	TestWorkerFactory(final Worker finalWorker) {
		this.finalWorker = finalWorker;
	}

	@Override
	public Worker create(ResourceConfig config, DependencyResolver dependencies) {
		return finalWorker;
	}

	@Override
	public Worker createSingleton(ResourceConfig config, DependencyResolver dependencies) {
		return finalWorker;
	}

	@Override
	public String getType() {
		return "test";
	}

	@Override
	public String getUserName() {
		return finalWorker.toString();
	}

	@Override
	public String getDescription() {
		return "Test worker";
	}

	@Override
	public Class<? extends ResourceConfig> getConfigClass() {
		return null;
	}

	@Override
	public ServiceUiFactory getServiceUiFactory() {
		return null;
	}
}
