package edu.mayo.mprc.daemon;

import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ResourceFactory;
import edu.mayo.mprc.config.ui.FactoryDescriptor;

/**
 * Creates a fully configured worker.
 * This means that the instance of this class needs to have configuration pre-set.
 * <p/>
 * This is used as a parameter to {@link SimpleRunner}.
 */
public interface WorkerFactory<C extends ResourceConfig, R extends Worker> extends ResourceFactory<C, R>, FactoryDescriptor {
}
