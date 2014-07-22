package edu.mayo.mprc.daemon;

import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.FactoryDescriptor;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.daemon.worker.log.DaemonLoggerFactory;
import edu.mayo.mprc.messaging.ResponseDispatcher;
import edu.mayo.mprc.messaging.Service;
import edu.mayo.mprc.messaging.ServiceFactory;

import java.io.File;

/**
 * Knows all about communication between daemons.
 * Knows which daemon it is a part of.
 * Capable of creating either the receiving or the sending end for a service of a given id.
 */
public final class DaemonConnectionFactory extends FactoryBase<ServiceConfig, DaemonConnection> implements FactoryDescriptor, Lifecycle {
	private FileTokenFactory fileTokenFactory;
	private ServiceFactory serviceFactory;
	// The response dispatcher for the daemon we are currently in process of creating
	private ResponseDispatcher responseDispatcher;
	private boolean running;

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

	@Override
	public DaemonConnection create(final ServiceConfig config, final DependencyResolver dependencies) {
		final ServiceFactory factory = getServiceFactory();
		final Service service = factory.createService(config.getName(), responseDispatcher);
		final File logFolder = new File(factory.getContext().getDaemonConfig().getLogOutputFolder());
		return new DirectDaemonConnection(service, fileTokenFactory, new DaemonLoggerFactory(logFolder));
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

	public ResponseDispatcher getResponseDispatcher() {
		return responseDispatcher;
	}

	public void setResponseDispatcher(ResponseDispatcher responseDispatcher) {
		this.responseDispatcher = responseDispatcher;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void start() {
		if (!isRunning()) {
			running = true;
			if (serviceFactory != null) {
				serviceFactory.start();
			}
			// Getting a daemon connection requires the file token factory to be operational
			if (fileTokenFactory instanceof Lifecycle) {
				((Lifecycle) fileTokenFactory).start();
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			serviceFactory.stop();
			running = false;
		}
	}
}
