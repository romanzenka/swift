package edu.mayo.mprc.daemon;

import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;

/**
 * Fake factory - single worker in a single thread has the same worker object recycled all the time
 */
final class TestWorkerFactory implements WorkerFactory {
	private final Worker finalWorker;

	public TestWorkerFactory(final Worker finalWorker) {
		this.finalWorker = finalWorker;
	}

	public Worker createWorker() {
		return finalWorker;
	}

	@Override
	public String getType() {
		return "test";
	}

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
