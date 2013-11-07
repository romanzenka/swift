package edu.mayo.mprc;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.MultiFactory;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.MessageBroker;
import edu.mayo.mprc.dbcurator.server.CurationWebContext;
import edu.mayo.mprc.messaging.ServiceFactory;
import edu.mayo.mprc.swift.*;
import edu.mayo.mprc.swift.commands.SwiftCommand;
import edu.mayo.mprc.swift.commands.SwiftCommandLine;
import edu.mayo.mprc.swift.commands.SwiftEnvironment;
import edu.mayo.mprc.swift.commands.SwiftEnvironmentImpl;
import edu.mayo.mprc.swift.search.DefaultSwiftSearcherCaller;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

/**
 * Here we utilize the {@link SwiftCommand} logic to bootstrap the web application.
 * <p/>
 * The main reason is that there is code in {@link SwiftEnvironment} that can deal with parsing
 * Swift config files that we need.
 */
public final class ServletInitialization implements SwiftCommand, ServletContextAware, InitializingBean {
	private static final Logger LOGGER = Logger.getLogger(ServletInitialization.class);

	private static final String SWIFT_INSTALL = "SWIFT_INSTALL";
	private static final String SWIFT_HOME = "SWIFT_HOME";
	private static final String SWIFT_CONF_RELATIVE_PATH = Swift.CONFIG_FILE_NAME;

	private WebUiHolder webUiHolder;
	private MultiFactory factoryTable;
	private ServiceFactory serviceFactory;
	private CurationWebContext curationWebContext;
	private SwiftMonitor swiftMonitor;
	private DefaultSwiftSearcherCaller swiftSearcherCaller;
	private ServletContext servletContext;

	public static boolean redirectToConfig(final ServletContext context, final HttpServletResponse response) throws IOException {
		if ("config".equals(getAction(context))) {
			response.sendRedirect("/configuration");
			return true;
		}
		return false;
	}

	@Override
	public void afterPropertiesSet() {
		try {
			setupLogging();

			final String action = getAction(servletContext);
			final File confFile = getConfigFile(servletContext);
			final String swiftDaemon = getSwiftDaemon(servletContext);

			final SwiftEnvironment swiftEnvironment = MainFactoryContext.getSwiftEnvironment();
			// Simulate command line being passed to Swift

			swiftEnvironment.registerCommand(this);

			final SwiftCommandLine commandLine = new SwiftCommandLine(getName(), Arrays.asList("action", action), confFile, swiftDaemon, null, null);
			if (swiftEnvironment.runSwiftCommand(commandLine) != ExitCode.Ok) {
				throw new MprcException("Initialization failed, shut down Swift");
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not initialize Swift web", e);
		}
	}

	@Override
	public String getName() {
		return "ServletInitialization";
	}

	@Override
	public String getDescription() {
		return "Internal Swift command that initializes the web";
	}

	public ExitCode run(SwiftEnvironment environment) {
		try {
			System.setProperty("SWIFT_INSTALL", environment.getConfigFile().getAbsolutePath());

			final DaemonConfig daemonConfig = environment.getDaemonConfig();

			// The service factory needs to be initialized by message broker config
			final MessageBroker.Config messageBroker = SwiftEnvironmentImpl.getMessageBroker(daemonConfig);
			serviceFactory.initialize(messageBroker.getBrokerUrl(), daemonConfig.getName());

			// WebUi needs reference to the actual daemon
			final Daemon daemon = environment.createDaemon(daemonConfig);

			final WebUi webUi = webUiHolder.getWebUi();

			if (webUi == null) {
				throw new MprcException("The daemon " + daemonConfig.getName() + " does not define any web interface module.");
			}
			webUi.setMainDaemon(daemon);
			getSwiftSearcherCaller().setSwiftSearcherConnection(webUi.getSwiftSearcherDaemonConnection());
			getSwiftSearcherCaller().setBrowseRoot(webUi.getBrowseRoot());

			// Initialize DB curator
			curationWebContext.initialize(
					webUi.getFastaFolder(),
					webUi.getFastaUploadFolder(),
					webUi.getFastaArchiveFolder(),
					// TODO: Fix this - the curator will keep creating temp folders and never deleting them
					// TODO: Also, the user should be able to specify where the temp files should go
					FileUtilities.createTempFolder());

			// Start all the services
			daemon.start();

			// Start the ping monitor
			getSwiftMonitor().initialize(environment.getApplicationConfig());
			getSwiftMonitor().start();

		} catch (Exception t) {
			LOGGER.fatal("Swift web application should be terminated", t);
			if (webUiHolder != null) {
				webUiHolder.stopSwiftMonitor();
			}
			return ExitCode.Error;
		}
		return ExitCode.Ok;
	}

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	public void setWebUiHolder(final WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}

	public MultiFactory getFactoryTable() {
		return factoryTable;
	}

	public void setFactoryTable(final MultiFactory factoryTable) {
		this.factoryTable = factoryTable;
	}

	public ServiceFactory getServiceFactory() {
		return serviceFactory;
	}

	public void setServiceFactory(final ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public CurationWebContext getCurationWebContext() {
		return curationWebContext;
	}

	public void setCurationWebContext(final CurationWebContext curationWebContext) {
		this.curationWebContext = curationWebContext;
	}

	public SwiftMonitor getSwiftMonitor() {
		return swiftMonitor;
	}

	public void setSwiftMonitor(final SwiftMonitor swiftMonitor) {
		this.swiftMonitor = swiftMonitor;
	}

	public DefaultSwiftSearcherCaller getSwiftSearcherCaller() {
		return swiftSearcherCaller;
	}

	public void setSwiftSearcherCaller(final DefaultSwiftSearcherCaller swiftSearcherCaller) {
		this.swiftSearcherCaller = swiftSearcherCaller;
	}

	public ServletInitialization() {
	}

	@Override
	public void setServletContext(final ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	private static String getSwiftDaemon(final ServletContext context) {
		String swiftDaemon;
		swiftDaemon = context.getInitParameter("SWIFT_DAEMON");
		if (swiftDaemon == null) {
			// Hack - we allow SWIFT_DAEMON to be defined as system property to make debugging in GWT easier
			swiftDaemon = System.getProperty("SWIFT_DAEMON");
		}
		return swiftDaemon;
	}

	/**
	 * Determine the location of the Swift config file.
	 *
	 * @param context Servlet context (can define properties that point to Swift config file).
	 * @return The Swift config file.
	 */
	private static File getConfigFile(final ServletContext context) {
		File confFile = null;
		if (getSwiftHome(context) == null) {
			String swiftHome = System.getenv(SWIFT_HOME);
			if (swiftHome == null) {
				swiftHome = System.getProperty(SWIFT_HOME);
			}
			if (swiftHome != null) {
				confFile = new File(swiftHome, SWIFT_CONF_RELATIVE_PATH).getAbsoluteFile();
			}
			if (confFile == null || !confFile.exists()) {
				confFile = new File(SWIFT_CONF_RELATIVE_PATH).getAbsoluteFile();
			}
		} else {
			confFile = getSwiftHome(context);
		}
		return confFile;
	}

	/**
	 * @return Location of Swift home directory as the user specified. If the location was not specified, returns null.
	 */
	private static File getSwiftHome(final ServletContext context) {
		final String swiftInstall = context.getInitParameter(SWIFT_INSTALL);
		if (swiftInstall == null) {
			return null;
		}
		return new File(swiftInstall);
	}

	/**
	 * Set up a configuration that logs on the console.
	 * <p/>
	 * If log4j configuration file is not specified, use default set up in the installation conf directory.
	 */
	private static void setupLogging() {
		final String configString = System.getProperty("log4j.configuration", "file:conf/log4j.properties");
		try {
			final URI configUri = new URI(configString);
			final File configFile = FileUtilities.fileFromUri(configUri);
			PropertyConfigurator.configure(configFile.getAbsolutePath());
		} catch (Exception e) {
			// SWALLOWED - login is not a big deal
			LOGGER.error("Could not initialize logging", e);
		}
	}

	private static String getAction(final ServletContext context) {
		String action = context.getInitParameter("SWIFT_ACTION");
		if (action == null) {
			action = System.getenv("SWIFT_ACTION");
		}
		if (action == null) {
			action = System.getProperty("SWIFT_ACTION");
		}
		return action;
	}
}
