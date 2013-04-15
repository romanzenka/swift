package edu.mayo.mprc.config;

import edu.mayo.mprc.MprcException;

/**
 * A special factory that does not create actual config classes. Instead it will produce instances
 * of the same, generic configuration class, except for Daemon, Service and Runner.
 * <p/>
 * This is useful when parsing a configuration file and not having access to all Swift modules.
 *
 * @author Roman Zenka
 */
public final class IndependentReaderFactory extends FactoryBase<ResourceConfig, Object> implements ReaderFactory {
	public IndependentReaderFactory() {
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
