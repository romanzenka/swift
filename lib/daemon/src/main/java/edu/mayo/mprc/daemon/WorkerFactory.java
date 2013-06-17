package edu.mayo.mprc.daemon;

import edu.mayo.mprc.config.FactoryDescriptor;

/**
 * Creates a fully configured worker.
 * This means that the instace of this class needs to have configuration pre-set.
 * <p/>
 * This is used as a parameter to {@link SimpleRunner}.
 */
public interface WorkerFactory extends FactoryDescriptor {
	/**
	 * @return Fully configured worker.
	 */
	Worker createWorker();

	/**
	 * Ensure that the worker to be created will work (all of its attributes are correctly set).
	 * Utilize the {@link edu.mayo.mprc.daemon.Worker#check} method.
	 */
	void checkWorker();
}
