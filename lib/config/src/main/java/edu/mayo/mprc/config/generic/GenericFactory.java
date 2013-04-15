package edu.mayo.mprc.config.generic;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;

/**
 * A special factory that does not create actual config classes. Instead it will produce instances
 * of the same, generic configuration class, except for Daemon, Service and Runner.
 * <p/>
 * This is useful when parsing a configuration file and not having access to all Swift modules.
 *
 * @author Roman Zenka
 */
public final class GenericFactory extends FactoryBase<ResourceConfig, Object> implements ReaderFactory {
	public GenericFactory() {
	}

	@Override
	public Class<? extends ResourceConfig> getConfigClass(final String type) {
		if (ServiceConfig.TYPE.equals(type)) {
			return ServiceConfig.class;
		} else if (DaemonConfig.TYPE.equals(type)) {
			return DaemonConfig.class;
		} else if (type.endsWith("Runner")) {
			return GenericRunner.class;
		} else {
			return GenericResource.class;
		}
	}

	@Override
	public Object create(final ResourceConfig config, final DependencyResolver dependencies) {
		throw new MprcException("The objects are not actually being created, just the configs are read");
	}

}
