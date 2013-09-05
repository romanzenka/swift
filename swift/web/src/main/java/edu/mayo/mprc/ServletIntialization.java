package edu.mayo.mprc;

import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.swift.MainFactoryContext;
import edu.mayo.mprc.swift.Swift;
import edu.mayo.mprc.swift.commands.InitializeWebCommand;
import edu.mayo.mprc.swift.commands.SwiftCommandLine;
import edu.mayo.mprc.swift.commands.SwiftEnvironment;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

public final class ServletIntialization {
	private static final Logger LOGGER = Logger.getLogger(ServletIntialization.class);

	private static volatile boolean wasInitialized = false;
	private static final String SWIFT_INSTALL = "SWIFT_INSTALL";
	private static final String SWIFT_HOME = "SWIFT_HOME";
	private static final String SWIFT_CONF_RELATIVE_PATH = Swift.CONFIG_FILE_NAME;

	private ServletIntialization() {
	}

	public static void initServletConfiguration(final ServletConfig config) throws ServletException {
		try {
			if (wasInitialized) {
				return;
			}

			setupLogging();

			final String action = getAction(config);
			final File confFile = getConfigFile(config);
			final String swiftDaemon = getSwiftDaemon(config);

			final SwiftEnvironment swiftEnvironment = MainFactoryContext.getSwiftEnvironment();
			// Simulate command line being passed to Swift
			final SwiftCommandLine commandLine = new SwiftCommandLine(InitializeWebCommand.INITIALIZE_WEB, Arrays.asList("action", action), confFile, swiftDaemon, null, null);
			if (swiftEnvironment.runSwiftCommand(commandLine) != ExitCode.Ok) {
				throw new MprcException("Initialization failed, shut down Swift");
			}

			wasInitialized = true;
		} catch (Exception e) {
			throw new ServletException("Could not initialize Swift web", e);
		}
	}

	private static String getSwiftDaemon(final ServletConfig config) {
		String swiftDaemon;
		swiftDaemon = config.getServletContext().getInitParameter("SWIFT_DAEMON");
		if (swiftDaemon == null) {
			// Hack - we allow SWIFT_DAEMON to be defined as system property to make debugging in GWT easier
			swiftDaemon = System.getProperty("SWIFT_DAEMON");
		}
		return swiftDaemon;
	}

	/**
	 * Determine the location of the Swift config file.
	 *
	 * @param config Servlet configuration (can define properties that point to Swift config file).
	 * @return The Swift config file.
	 */
	private static File getConfigFile(final ServletConfig config) {
		File confFile = null;
		if (getSwiftHome(config) == null) {
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
			confFile = getSwiftHome(config);
		}
		return confFile;
	}

	/**
	 * @return Location of Swift home directory as the user specified. If the location was not specified, returns null.
	 */
	private static File getSwiftHome(final ServletConfig config) {
		final String swiftInstall = config.getServletContext().getInitParameter(SWIFT_INSTALL);
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

	private static String getAction(final ServletConfig config) {
		String action = config.getServletContext().getInitParameter("SWIFT_ACTION");
		if (action == null) {
			action = System.getenv("SWIFT_ACTION");
		}
		if (action == null) {
			action = System.getProperty("SWIFT_ACTION");
		}
		return action;
	}

	public static boolean redirectToConfig(final ServletConfig config, final HttpServletResponse response) throws IOException {
		if ("config".equals(getAction(config))) {
			response.sendRedirect("/configuration");
			return true;
		}
		return false;
	}
}
