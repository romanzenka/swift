package edu.mayo.mprc.swift.configuration.server;

import edu.mayo.mprc.GWTServiceExceptionFactory;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.common.client.GWTServiceException;
import edu.mayo.mprc.common.server.SpringGwtServlet;
import edu.mayo.mprc.common.server.WebApplicationStopper;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.SimpleRunner;
import edu.mayo.mprc.sge.GridRunner;
import edu.mayo.mprc.swift.MainFactoryContext;
import edu.mayo.mprc.swift.commands.SwiftCommand;
import edu.mayo.mprc.swift.commands.SwiftEnvironment;
import edu.mayo.mprc.swift.configuration.client.model.ApplicationModel;
import edu.mayo.mprc.swift.configuration.client.model.ConfigurationService;
import edu.mayo.mprc.swift.configuration.client.model.ResourceModel;
import edu.mayo.mprc.swift.configuration.client.model.UiChangesReplayer;
import edu.mayo.mprc.swift.configuration.server.session.ServletStorage;
import edu.mayo.mprc.swift.configuration.server.session.SessionStorage;
import edu.mayo.mprc.swift.resources.ResourceTable;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;

import java.io.File;

/**
 * Does all the server-side work needed for configuration process to function.
 * <p/>
 * Note: The implementation must never throw a runtime exception. Runtime exceptions are not transfered properly by GWT.
 */
public final class ConfigurationServiceImpl extends SpringGwtServlet implements ConfigurationService {
	private static final long serialVersionUID = 20101221L;

	private transient SessionStorage storage;
	private transient ResourceTable resourceTable;
	private transient SwiftEnvironment swiftEnvironment;
	private transient SwiftCommand installCommand;
	private WebApplicationStopper stopper;

	public ConfigurationServiceImpl() {
		setStorage(new ServletStorage(this));
	}

	/**
	 * We need to take the client-side model of the configuration and convert it to {@link edu.mayo.mprc.config.ApplicationConfig}.
	 * <p/>
	 * As the configuration gets saved, we produce startup scripts as well.
	 */
	@Override
	public UiChangesReplayer saveConfiguration() throws GWTServiceException {
		try {
			return getData().saveConfig(null);
		} catch (Exception t) {
			throw GWTServiceExceptionFactory.createException(MprcException.getDetailedMessage(t), t);
		}
	}

	@Override
	public ApplicationModel loadConfiguration() throws GWTServiceException {
		final File swiftConfig = swiftEnvironment.getConfigFile();
		return loadFromFile(swiftConfig);
	}

	ApplicationModel loadFromFile(final File swiftConfig) throws GWTServiceException {
		MainFactoryContext.initialize();
		final File configFile = swiftConfig.getAbsoluteFile();
		try {
			if (configFile.exists()) {
				ApplicationConfig config = new ApplicationConfig(new DependencyResolver(resourceTable));
				ApplicationConfig.load(config, configFile, getResourceTable());
				getData().setConfig(config);
			} else {
				getData().loadDefaultConfig();
			}
			return getData().getModel();
		} catch (Exception t) {
			throw GWTServiceExceptionFactory.createException("Cannot load configuration from " + configFile.getPath(), t);
		}
	}

	@Override
	public ResourceModel createChild(final String parentId, final String type) throws GWTServiceException {
		return getData().createChild(parentId, type);
	}

	@Override
	public void removeChild(final String childId) throws GWTServiceException {
		getData().removeChild(childId);
	}

	@Override
	public void changeRunner(final String serviceId, final String newRunnerType) throws GWTServiceException {
		final ServiceConfig serviceConfig = (ServiceConfig) getData().getResourceConfig(serviceId);
		RunnerConfig runner = null;
		if ("localRunner".equals(newRunnerType)) {
			runner = new SimpleRunner.Config(serviceConfig.getRunner().getWorkerConfiguration());
		} else {
			runner = new GridRunner.Config(serviceConfig.getRunner().getWorkerConfiguration());
		}
		getData().changeRunner(serviceConfig, runner);
	}

	@Override
	public UiChangesReplayer propertyChanged(final String modelId, final String propertyName, final String newValue, final boolean onDemand) throws GWTServiceException {
		final ResourceConfig resourceConfig = getData().getResourceConfig(modelId);
		return getData().setProperty(resourceConfig, propertyName, newValue, onDemand);
	}

	@Override
	public void fix(final String moduleId, final String propertyName, final String action) throws GWTServiceException {
		final ResourceConfig resourceConfig = getData().getResourceConfig(moduleId);
		getData().fix(resourceConfig, propertyName, action);
	}

	@Override
	public void terminateProgram() {
		if (stopper != null) {
			stopper.stopWebApplication();
		}
	}

	private ConfigurationData getData() {
		final Object obj = getStorage().get("configurationData");
		if (obj == null) {
			final ConfigurationData configurationData = new ConfigurationData(getResourceTable(), getSwiftEnvironment(), getInstallCommand());
			getStorage().put("configurationData", configurationData);
			return configurationData;
		}
		if (obj instanceof ConfigurationData) {
			return (ConfigurationData) obj;
		} else {
			ExceptionUtilities.throwCastException(obj, ConfigurationData.class);
			return null;
		}
	}

	public SessionStorage getStorage() {
		return storage;
	}

	public void setStorage(SessionStorage storage) {
		this.storage = storage;
	}

	public ResourceTable getResourceTable() {
		return resourceTable;
	}

	public void setResourceTable(final ResourceTable resourceTable) {
		this.resourceTable = resourceTable;
	}

	public SwiftEnvironment getSwiftEnvironment() {
		return swiftEnvironment;
	}

	public void setSwiftEnvironment(SwiftEnvironment swiftEnvironment) {
		this.swiftEnvironment = swiftEnvironment;
	}

	public SwiftCommand getInstallCommand() {
		return installCommand;
	}

	public void setInstallCommand(SwiftCommand installCommand) {
		this.installCommand = installCommand;
	}

	public WebApplicationStopper getStopper() {
		return stopper;
	}

	public void setStopper(WebApplicationStopper stopper) {
		this.stopper = stopper;
	}
}