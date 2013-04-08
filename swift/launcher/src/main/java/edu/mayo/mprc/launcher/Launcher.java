package edu.mayo.mprc.launcher;

import edu.mayo.mprc.MprcException;
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
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.Arrays;
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
	private final Object stopMonitor = new Object();
	private volatile boolean restartRequested = false;

	private static final int EXIT_CODE_OK = 0;
	private static final int EXIT_CODE_ERROR = 1;
	private static final int EXIT_CODE_RESTART = 2;
	public static final int POLLING_INTERVAL = 10 * 1000;

	// Enlarge the header buffer size as we use large cookies to store the currently opened directories
	public static final int HEADER_BUFFER_SIZE = 65536;

	public static void main(final String[] args) {
		final Launcher launcher = new Launcher();
		launcher.runLauncher(args);
	}

	private void runLauncher(final String[] args) {
		final OptionParser parser = new OptionParser();
		parser.accepts("config", "Reconfigure Swift. Will run a web server with the configuration screen on a given --port");
		parser.accepts("install", "Installation config file. If not specified, Swift will run in configuration mode and produce this file. Default is " + CONFIG_FILE_NAME + ".").withRequiredArg().ofType(File.class);
		parser.accepts("daemon", "Daemon to be run. Will run the daemon defined by the given name in the config file. If there is only one daemon defined in the config file, the daemon will be run and this flag is disregarded.").withRequiredArg().describedAs("Daemon name").ofType(String.class);
		parser.accepts("port", "Port to run the configuration web server on. Default is " + DEFAULT_PORT + ".").withRequiredArg().ofType(Integer.class);
		parser.accepts("war", "Path to swift.war file. This is needed only when running config or the web part of Swift. Default is swift.war in the local directory.").withRequiredArg().ofType(File.class);
		parser.acceptsAll(Arrays.asList("help", "?"), "Show this help screen");
		OptionSet options = null;

		try {
			options = parser.parse(args);
		} catch (Exception t) {
			FileUtilities.err(t.getMessage());
			displayHelpMessage(parser);
			System.exit(EXIT_CODE_ERROR);
		}
		if (options == null || options.has("?")) {
			displayHelpMessage(parser);
			System.exit(EXIT_CODE_OK);
		}

		if (options.has("config")) {
			File configFile = null;
			if (options.has("install")) {
				configFile = CommandLine.findFile(options, "install", "installation config file", CONFIG_FILE_NAME);
			} else {
				configFile = new File(CONFIG_FILE_NAME);
			}
			// We are running configured Swift with web server enabled
			runWebServer(options, configFile, "config");
		} else {
			final File installPropertyFile = CommandLine.findFile(options, "install", "installation config file", CONFIG_FILE_NAME);

			final FileMonitor monitor = new FileMonitor(POLLING_INTERVAL);
			monitor.addFile(installPropertyFile);
			monitor.addListener(this);

			// This method will exit once the web server is up and running
			final Server webServer = runWebServer(options, installPropertyFile, "production");

			System.exit(shutdownWhenRestartRequested(webServer) ? EXIT_CODE_RESTART : EXIT_CODE_OK);
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
			final String s = "Sending the web server a stop signal";
			FileUtilities.out(s);
			webServer.stop();
			FileUtilities.out("Waiting for web server to terminate");
			webServer.join();
			FileUtilities.out("Web server terminated, exiting");
		} catch (Exception t) {
			// SWALLOWED - just exit
			FileUtilities.err("Problems stopping the web server:\n\t" + MprcException.getDetailedMessage(t));
		}
		return restart;
	}

	private Server runWebServer(final OptionSet options, final File configFile, final String action) {
		final File warFile = CommandLine.findFile(options, "war", "swift web interface file", "swift.war");

		final String daemonId = getDaemonId(options);
		final int portNumber = getPortNumber(options, configFile, daemonId);
		final File tempFolder = getTempFolder(configFile, daemonId);

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
				public Object call() throws Exception {
					server.start();
					return 1;
				}
			});
		} catch (Exception t) {
			FileUtilities.err("Swift web server could not be launched. Please run this script with --help for more information.");
			FileUtilities.stackTrace(t);
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

	private String getDaemonId(final OptionSet options) {
		String daemonId = null;
		if (options.has("daemon")) {
			daemonId = options.valueOf("daemon").toString();
		}
		return daemonId;
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
						FileUtilities.out("Please point your web client to http://" + localMachine.getCanonicalHostName() + ":" + portNumber);
						break;
					}
				} catch (Exception t) {
					FileUtilities.err("Swift web server could not be launched. Please run this script with --help for more information.");
					FileUtilities.stackTrace(t);
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

	private File getTempFolder(final File configFile, final String daemonId) {
		File tempFolder = null;
		try {
			tempFolder = new File(getDaemonConfigValue(configFile, daemonId, "/tempFolderPath"));
		} catch (Exception ignore) {
			tempFolder = FileUtilities.getDefaultTempDirectory();
			LOGGER.warn("Could not parse the config file " + configFile.getPath() + " to temporary daemon folder. Using default " + tempFolder);
		}
		return tempFolder;
	}

	private int getPortNumber(final OptionSet options, final File configFile, final String daemonId) {
		int portNumber = DEFAULT_PORT;
		if (options.has("port")) {
			portNumber = (Integer) options.valueOf("port");
		} else {
			try {
				portNumber = Integer.parseInt(getDaemonConfigValue(configFile, daemonId, "/resources/webUi/port"));
			} catch (Exception ignore) {
				LOGGER.warn("Could not parse the config file " + configFile.getPath() + " to obtain web port number. Using default " + DEFAULT_PORT);
				portNumber = DEFAULT_PORT;
			}
		}
		return portNumber;
	}

	private String getDaemonConfigValue(final File configFile, final String daemonId, final String configValuePath) throws XPathExpressionException, FileNotFoundException {
		final XPathFactory xPathFactory = XPathFactory.newInstance();
		final XPath xPath = xPathFactory.newXPath();
		final XPathExpression expression;
		if (daemonId != null) {
			expression = xPath.compile("/application/daemons/daemon[@name='" + daemonId + "']/" + configValuePath);
		} else {
			expression = xPath.compile("/application/daemons/daemon/" + configValuePath);
		}
		final FileInputStream stream = new FileInputStream(configFile);
		try {
			final InputSource inputSource = new InputSource(stream);
			return expression.evaluate(inputSource);
		} finally {
			FileUtilities.closeQuietly(stream);
		}
	}

	private void displayHelpMessage(final OptionParser parser) {
		try {
			FileUtilities.out("This command starts Swift web interface with the provided configuration parameters.");
			FileUtilities.out("If Swift is not yet configured, it will run a web server that allows you to configure it first.");
			FileUtilities.out("");
			FileUtilities.out("To run Swift daemons (no UI), please use java -jar swift.jar");
			FileUtilities.out("");
			FileUtilities.out("Usage:");
			parser.printHelpOn(System.out);
		} catch (Exception t) {
			LOGGER.fatal("Could not display help message.", t);
			System.exit(1);
		}
	}

	@Override
	public void fileChanged(final File file) {
		synchronized (stopMonitor) {
			FileUtilities.out("The configuration file " + file.getPath() + " is modified. Restarting.");
			restartRequested = true;
			stopMonitor.notifyAll();
		}
	}
}
