package edu.mayo.mprc.swift;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.common.server.WebApplicationStopper;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.MultiFactory;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.messaging.ServiceFactory;
import edu.mayo.mprc.swift.commands.ExitCode;
import edu.mayo.mprc.swift.commands.SwiftCommand;
import edu.mayo.mprc.swift.commands.SwiftCommandLine;
import edu.mayo.mprc.swift.commands.SwiftEnvironment;
import edu.mayo.mprc.swift.configuration.client.model.ConfigurationService;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.resources.SwiftMonitor;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import edu.mayo.mprc.swift.search.DefaultSwiftSearcherCaller;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.MonitorUtilities;
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
public final class ServletInitialization implements SwiftCommand, ServletContextAware, InitializingBean, WebApplicationStopper {
	private static final Logger LOGGER = Logger.getLogger(ServletInitialization.class);

	private static final String SWIFT_INSTALL = "SWIFT_INSTALL";
	private static final String SWIFT_HOME = "SWIFT_HOME";
	private static final String SWIFT_CONF_RELATIVE_PATH = Swift.CONFIG_FILE_NAME;

	private WebUiHolder webUiHolder;
	private ConfigurationService configurationService;
	private MultiFactory factoryTable;
	private ServiceFactory serviceFactory;
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

			final File home = getSwiftHome(servletContext);
			webUiHolder.setSwiftHome(home);

			final SwiftEnvironment swiftEnvironment = MainFactoryContext.getSwiftEnvironment();
			// Simulate command line being passed to Swift

			swiftEnvironment.registerCommand("servlet-initialization", this);

			final SwiftCommandLine commandLine = new SwiftCommandLine("servlet-initialization", Arrays.asList("action", action), confFile, swiftDaemon, null, null);
			if (swiftEnvironment.runSwiftCommand(commandLine) != ExitCode.Ok) {
				stopWebApplication();
			}
		} catch (Exception e) {
			stopWebApplication();
			throw new RuntimeException("Could not initialize Swift web", e);
		}
	}

	@Override
	public String getDescription() {
		return "Internal Swift command that initializes the web";
	}

	public ExitCode run(SwiftEnvironment environment) {
		Daemon daemon = null;
		try {
			File configFile = environment.getConfigFile();
			System.setProperty("SWIFT_INSTALL", configFile.getAbsolutePath());

			// Run the daemon only if we have a config file
			// If no config is available, we need to switch to the config mode.
			if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
				final DaemonConfig daemonConfig = environment.getDaemonConfig();

				// WebUi needs reference to the actual daemon
				daemon = environment.createDaemon(daemonConfig);

				final WebUi webUi = webUiHolder.getWebUi();

				if (webUi == null) {
					throw new MprcException("The daemon " + daemonConfig.getName() + " does not define any web interface module.");
				}
				webUi.setMainDaemon(daemon);
				getSwiftSearcherCaller().setSwiftSearcherConnection(webUi.getSwiftSearcherDaemonConnection());
				getSwiftSearcherCaller().setBrowseRoot(webUi.getBrowseRoot());

				cleanupDatabaseAfterStartup(webUi.getSwiftDao());

				// Start all the services
				daemon.start();

				// Start the ping monitor
				getSwiftMonitor().start();
			}

		} catch (Exception t) {
			LOGGER.fatal("Swift web application is terminating", t);
			if (webUiHolder != null) {
				webUiHolder.stopSwiftMonitor();
			}
			if (daemon != null) {
				daemon.stop();
			}
			getSwiftMonitor().stop();
			return ExitCode.Error;
		}
		return ExitCode.Ok;
	}

	/**
	 * See {@link edu.mayo.mprc.swift.db.SwiftDao#cleanupAfterStartup} for explanation.
	 */
	private void cleanupDatabaseAfterStartup(SwiftDao swiftDao) {
		swiftDao.begin();
		try {
			swiftDao.cleanupAfterStartup();
			swiftDao.commit();
		} catch (final Exception e) {
			swiftDao.rollback();
			throw new MprcException("Database cleanup after startup failed", e);
		}
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

	public ConfigurationService getConfigurationService() {
		return configurationService;
	}

	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
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
	public static File getConfigFile(final ServletContext context) {
		return new File(getSwiftHome(context), SWIFT_CONF_RELATIVE_PATH);
	}

	/**
	 * @return Location of Swift home directory as the user specified. If the location was not specified, returns null.
	 */
	public static File getSwiftHome(final ServletContext context) {
		String swiftInstall = context.getInitParameter(SWIFT_INSTALL);
		if (swiftInstall == null) {
			swiftInstall = System.getenv(SWIFT_HOME);
			if (swiftInstall == null) {
				swiftInstall = System.getProperty(SWIFT_HOME);
			}
			if (swiftInstall == null) {
				swiftInstall = new File(".").getAbsolutePath();
			}
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

	public void stopWebApplication() {

		int port = getStopPort(servletContext);
		if (port > 0) {
			LOGGER.fatal("Stopping Swift");
			MonitorUtilities.sendStopSignal(port);
		} else {
			LOGGER.fatal("Cannot stop Swift, jetty stop port was not configured. Please terminate Swift manually.");
			throw new MprcException("Jetty stop port was not configured");
		}
	}

	private static int getStopPort(final ServletContext context) {
		String stopPort = context.getInitParameter("SWIFT_STOP_PORT");
		if (stopPort == null) {
			stopPort = System.getenv("SWIFT_STOP_PORT");
		}
		if (stopPort == null) {
			stopPort = System.getProperty("SWIFT_STOP_PORT");
		}

		return stopPort == null ? -1 : Integer.valueOf(stopPort);
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
