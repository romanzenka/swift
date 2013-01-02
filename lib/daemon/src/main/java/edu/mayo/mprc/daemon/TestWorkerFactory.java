package edu.mayo.mprc.daemon;

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

	public String getDescription() {
		return finalWorker.toString();
	}
}
