package edu.mayo.mprc.launcher;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.swift.WebUi;
import edu.mayo.mprc.swift.commands.SwiftEnvironment;
import edu.mayo.mprc.utilities.CommandLine;
import edu.mayo.mprc.utilities.FileListener;
import edu.mayo.mprc.utilities.FileMonitor;
import edu.mayo.mprc.utilities.FileUtilities;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Main entry point for Swift.
 * <ul>
 * <li>Starts up all the services as instructed, including web server.</li>
 * <li>If configuration is missing, starts up web server with config page.</li>
 * </ul>
 * <p/>
 * The return code from the main method is important. Following meanings are defined:
 * <ul>
 * <li>0 - smooth shutdown, do not attempt to restart</li>
 * <li>{@link #UPGRADE_EXIT_CODE} - shutting down to be restarted immediately (e.g. just got reconfigured)</li>
 * <li>anything else - error</li>
 * </ul>
 */
public final class Launcher implements FileListener {
	public static final int UPGRADE_EXIT_CODE = 100;
	private static final Logger LOGGER = Logger.getLogger(Launcher.class);
	private static final int DEFAULT_PORT = 8080;
	private static final String CONFIG_FILE_NAME = "conf/swift.conf";
	public static final String CONFIG_OPTION = "config";
	public static final String INSTALL_OPTION = "install";
	public static final String PORT_OPTION = "port";
	public static final String WAR_OPTION = "war";
	private final Object stopMonitor = new Object();
	private volatile boolean restartRequested = false;
	private SwiftEnvironment swiftEnvironment;

	private static final int EXIT_CODE_OK = 0;
	private static final int EXIT_CODE_ERROR = 1;
	private static final int EXIT_CODE_RESTART = 2;
	public static final int POLLING_INTERVAL = 10 * 1000;

	// Enlarge the header buffer size as we use large cookies to store the currently opened directories
	public static final int HEADER_BUFFER_SIZE = 65536;

	public Launcher() {
	}

	public ExitCode runLauncher(final String[] args, final SwiftEnvironment swiftEnvironment) {
		this.swiftEnvironment = swiftEnvironment;
		final OptionParser parser = new OptionParser();
		parser.accepts(CONFIG_OPTION, "Reconfigure Swift. Will run a web server with the configuration screen on a given --port");
		parser.accepts(INSTALL_OPTION, "Installation config file. If not specified, Swift will run in configuration mode and produce this file. Default is " + CONFIG_FILE_NAME + ".").withRequiredArg().ofType(File.class);
		parser.accepts(PORT_OPTION, "Port to run the configuration web server on. Default is " + DEFAULT_PORT + ".").withRequiredArg().ofType(Integer.class);
		parser.accepts(WAR_OPTION, "Path to swift.war file. This is needed only when running config or the web part of Swift. Default is swift.war in the local directory.").withRequiredArg().ofType(File.class);
		OptionSet options = null;

		try {
			options = parser.parse(args);
		} catch (Exception t) {
			FileUtilities.err(t.getMessage());
			displayHelpMessage(parser);
			System.exit(EXIT_CODE_ERROR);
		}
		if (options == null) {
			displayHelpMessage(parser);
			System.exit(EXIT_CODE_OK);
		}

		if (options.has(CONFIG_OPTION)) {
			File configFile = null;
			if (options.has(INSTALL_OPTION)) {
				configFile = CommandLine.findFile(options, INSTALL_OPTION, "installation config file", CONFIG_FILE_NAME);
			} else {
				configFile = new File(CONFIG_FILE_NAME);
			}
			// We are running configured Swift with web server enabled
			final Server webServer = runWebServer(options, configFile, CONFIG_OPTION);

			return shutdownWhenRestartRequested(webServer) ? ExitCode.Restart : ExitCode.Ok;
		} else {
			final File installPropertyFile = CommandLine.findFile(options, INSTALL_OPTION, "installation config file", CONFIG_FILE_NAME);

			final FileMonitor monitor = new FileMonitor(POLLING_INTERVAL);
			if (installPropertyFile != null) {
				monitor.fileToBeChanged(installPropertyFile, this);
			}

			// This method will exit once the web server is up and running
			final Server webServer = runWebServer(options, installPropertyFile, "production");

			return shutdownWhenRestartRequested(webServer) ? ExitCode.Restart : ExitCode.Ok;
		}
	}

	/**
	 * Wait until the web server finishes its execution.
	 *
	 * @param webServer Web server to wait for.
	 * @return True if restart was requested, false if the web server is just shutting down.
	 */
	private boolean shutdownWhenRestartRequested(final Server webServer) {
		boolean restart = false;
		synchronized (stopMonitor) {
			try {
				while (!restartRequested) {
					stopMonitor.wait(POLLING_INTERVAL);
				}
				restart = restartRequested;
			} catch (InterruptedException ignore) {
				FileUtilities.err("Interrupted, exiting");
			}
		}
		try {
			LOGGER.info("Sending the web server a stop signal");
			webServer.stop();
			LOGGER.info("Waiting for web server to terminate");
			webServer.join();
			LOGGER.info("Web server terminated, exiting");
		} catch (Exception t) {
			// SWALLOWED - just exit
			LOGGER.error("Problems stopping the web server", t);
		}
		return restart;
	}

	private Server runWebServer(final OptionSet options, final File configFile, final String action) {
		final File warFile = CommandLine.findFile(options, WAR_OPTION, "swift web interface file", "lib/swift-web-3.5-SNAPSHOT.war");
		if (warFile == null) {
			throw new MprcException("Could not locate the swift's war file");
		}

		final String daemonId = swiftEnvironment.getDaemonConfig().getName();
		final int portNumber = getPortNumber(options, configFile);
		final File tempFolder = getTempFolder(options, configFile);

		final Server server = new Server(portNumber);
		for (final Connector connector : server.getConnectors()) {
			connector.setHeaderBufferSize(HEADER_BUFFER_SIZE);
		}

		WebAppContext webAppContext = null;

		Future<Object> future = null;
		try {
			webAppContext = makeWebAppContext(configFile, action, warFile, daemonId, tempFolder);
			server.addHandler(webAppContext);

			future = Executors.newSingleThreadExecutor().submit(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					server.start();
					return 1;
				}
			});
		} catch (Exception t) {
			LOGGER.error("Swift web server could not be launched. Please run this script with --help for more information.", t);
			System.exit(1);
			return null;
		}

		loopTillWebUp(portNumber, future);

		return server;

	}

	private WebAppContext makeWebAppContext(final File configFile, final String action, final File warFile, final String daemonId, final File tempFolder) {
		final WebAppContext webAppContext;
		webAppContext = new WebAppContext();
		webAppContext.setWar(warFile.getAbsolutePath());
		webAppContext.setContextPath("/");
		// We must set temp directory, otherwise the app goes to /tmp which will get deleted periodically
		webAppContext.setTempDirectory(tempFolder);
		final Map<String, String> initParams = new HashMap<String, String>(3);
		if (configFile != null) {
			initParams.put("SWIFT_CONFIG", configFile.getAbsolutePath());
		}
		initParams.put("SWIFT_ACTION", action);
		initParams.put("SWIFT_DAEMON", daemonId);

		webAppContext.setInitParams(initParams);
		return webAppContext;
	}

	/**
	 * Loop until we either notice the server is running, or the server exits with an exception
	 */
	private void loopTillWebUp(final int portNumber, final Future<Object> future) {
		while (true) {
			if (future.isDone()) {
				// .get will throw an exception if the web server failed.
				try {
					if (1 == (Integer) future.get()) {
						final InetAddress localMachine = InetAddress.getLocalHost();
						LOGGER.info("Please point your web client to http://" + localMachine.getCanonicalHostName() + ":" + portNumber);
						break;
					}
				} catch (Exception t) {
					LOGGER.error("Swift web server could not be launched. Please run this script with --help for more information.", t);
					System.exit(EXIT_CODE_ERROR);
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException ignore) {
				// SWALLOWED - we exit right away when interrupted.
				break;
			}
		}
	}

	File getTempFolder(final OptionSet options, final File configFile) {
		File tempFolder = null;
		try {
			final DaemonConfig daemonConfig = swiftEnvironment.getDaemonConfig();
			tempFolder = new File(daemonConfig.getTempFolderPath());
		} catch (Exception e) {
			tempFolder = FileUtilities.getDefaultTempDirectory();
			if (!configNode(options)) {
				LOGGER.warn("Could not parse the config file " + (configFile == null ? "<null>" : configFile.getPath()) + " to obtain temporary daemon folder. Using default " + tempFolder, e);
			}
		}
		return tempFolder;
	}

	int getPortNumber(final OptionSet options, final File configFile) {
		final int portNumber;
		if (options != null && options.has(PORT_OPTION)) {
			portNumber = (Integer) options.valueOf(PORT_OPTION);
		} else {
			try {
				final WebUi.Config webUi = (WebUi.Config)getResourceConfig(WebUi.Config.class);
				return Integer.parseInt(webUi.getPort());
			} catch (Exception e) {
				if (configNode(options)) {
					// We are configuring Swift, no wonder config file is not present
					LOGGER.info("Default port used for config: " + DEFAULT_PORT);
				} else {
					LOGGER.warn("Could not parse the config file " + (configFile == null ? "<null>" : configFile.getPath()) + " to obtain web port number. Using default " + DEFAULT_PORT, e);
				}
				portNumber = DEFAULT_PORT;
			}
		}
		return portNumber;
	}

	private static boolean configNode(final OptionSet options) {
		return options != null && options.has(CONFIG_OPTION);
	}

	ResourceConfig getResourceConfig(final Class clazz) {
		final DaemonConfig daemonConfig = swiftEnvironment.getDaemonConfig();
		if (daemonConfig == null) {
			return null;
		}
		for (final ResourceConfig resource : daemonConfig.getResources()) {
			if (clazz.isAssignableFrom(resource.getClass())) {
				return resource;
			}
		}
		throw new MprcException("Resource of type " + clazz.getSimpleName() + " not defined in daemon");
	}

	private static void displayHelpMessage(final OptionParser parser) {
		try {
			final String helpString = getHelpString(parser);

			LOGGER.info("This command starts Swift web interface with the provided configuration parameters." + "\n" +
					"If Swift is not yet configured, it will run a web server that allows you to configure it first." + "\n" +
					"\n" +
					"Usage:" + "\n" + helpString);
		} catch (Exception t) {
			LOGGER.fatal("Could not display help message.", t);
			System.exit(1);
		}
	}

	private static String getHelpString(final OptionParser parser) {
		try {
			final ByteArrayOutputStream bos = new ByteArrayOutputStream();
			parser.printHelpOn(bos);
			final String result = bos.toString("UTF-8");
			bos.close();
			return result;
		} catch (Exception e) {
			// SWALLOWED - just say you cannot create the help
			return "<no help available> : " + MprcException.getDetailedMessage(e);
		}
	}

	@Override
	public void fileChanged(final Collection<File> files, final boolean timeout) {
		if (!timeout) {
			synchronized (stopMonitor) {
				LOGGER.info("The configuration file " + files.iterator().next() + " is modified. Restarting.");
				restartRequested = true;
				stopMonitor.notifyAll();
			}
		}
	}
}
