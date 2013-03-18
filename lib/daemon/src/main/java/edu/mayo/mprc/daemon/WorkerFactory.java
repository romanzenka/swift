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
}
