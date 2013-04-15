package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.messaging.Service;
import edu.mayo.mprc.messaging.ServiceFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Knows all about communication between daemons.
 * Knows which daemon it is a part of.
 * Capable of creating either the receiving or the sending end for a service of a given id.
 */
public final class DaemonConnectionFactory extends FactoryBase<ServiceConfig, DaemonConnection> implements FactoryDescriptor {
	private FileTokenFactory fileTokenFactory;
	private ServiceFactory serviceFactory;
	private String brokerUrl;

	public FileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	public String getBrokerUrl() {
		return brokerUrl;
	}

	public void setBrokerUrl(String brokerUrl) {
		this.brokerUrl = brokerUrl;
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
		final URI serviceUri = getServiceUri(config.getName());
		final ServiceFactory factory = getServiceFactory();
		final Service service = factory.createService(serviceUri);
		return new DirectDaemonConnection(service, fileTokenFactory);
	}

	private URI getServiceUri(final String name) {
		if (brokerUrl == null) {
			throw new MprcException("The broker URL has to be initialized before we can start creating connections to daemons");
		}
		try {
			return new URI(brokerUrl + (brokerUrl.contains("?") ? "&" : "?") + "simplequeue=" + name);
		} catch (URISyntaxException e) {
			throw new MprcException("Wrong service URI for broker [" + brokerUrl + "] and queue name [" + name + "]", e);
		}
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
