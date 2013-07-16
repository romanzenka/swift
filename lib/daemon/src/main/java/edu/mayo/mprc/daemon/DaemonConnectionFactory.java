package edu.mayo.mprc.daemon;

import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.messaging.Service;
import edu.mayo.mprc.messaging.ServiceFactory;

/**
 * Knows all about communication between daemons.
 * Knows which daemon it is a part of.
 * Capable of creating either the receiving or the sending end for a service of a given id.
 */
public final class DaemonConnectionFactory extends FactoryBase<ServiceConfig, DaemonConnection> implements FactoryDescriptor {
	private FileTokenFactory fileTokenFactory;
	private ServiceFactory serviceFactory;

	public FileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
		this.fileTokenFactory = fileTokenFactory;
	}

	public ServiceFactory getServiceFactory() {
		return serviceFactory;
	}

	public void setServiceFactory(final ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public DaemonConnection create(final ServiceConfig config, final DependencyResolver dependencies) {
		// The creation of the daemon connection depends on a message broker being present
		final ServiceFactory factory = getServiceFactory();
		final Service service = factory.createService(config.getName());
		return new DirectDaemonConnection(service, fileTokenFactory);
	}

	@Override
	public String getType() {
		return ServiceConfig.TYPE;
	}

	@Override
	public String getUserName() {
		return "Service";
	}

	@Override
	public String getDescription() {
		return "A service is basically a queue that accepts work requests and executes them";
	}

	@Override
	public Class<? extends ResourceConfig> getConfigClass() {
		return ServiceConfig.class;
	}

	@Override
	public ServiceUiFactory getServiceUiFactory() {
		return null;
	}
}
