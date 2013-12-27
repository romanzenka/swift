package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.utilities.FileListener;
import edu.mayo.mprc.utilities.FileMonitor;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.MonitorUtilities;
import org.apache.log4j.Logger;
import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
 */
public final class Launcher implements FileListener {
	private static final Logger LOGGER = Logger.getLogger(Launcher.class);
	private static final int DEFAULT_PORT = 8080;
	private final Object stopMonitor = new Object();
	private volatile boolean restartRequested;
	private SwiftEnvironment swiftEnvironment;
	private JettyStopThread jettyStopThread;

	private static final int EXIT_CODE_ERROR = 1;
	public static final long POLLING_INTERVAL = (long) (10 * 1000);

	// Enlarge the header buffer size as we use large cookies to store the currently opened directories
	public static final int HEADER_BUFFER_SIZE = 65536;

	public Launcher() {
	}

	public ExitCode runLauncher(final boolean configMode, final SwiftEnvironment swiftEnvironment) {
		this.swiftEnvironment = swiftEnvironment;

		if (configMode) {
			// We are running configured Swift with web server enabled
			final Server webServer = runWebServer(swiftEnvironment, true);

			return shutdownWhenRestartRequested(webServer) ? ExitCode.Restart : ExitCode.Ok;
		} else {
			final FileMonitor monitor = new FileMonitor(POLLING_INTERVAL);
			monitor.fileToBeChanged(swiftEnvironment.getConfigFile(), this);

			// This method will exit once the web server is up and running
			final Server webServer = runWebServer(swiftEnvironment, false);

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

	private Server runWebServer(final SwiftEnvironment environment, final boolean configMode) {
		final File warFile = new File("lib/swift-web-3.5-SNAPSHOT.war");

		final String daemonId;
		if (configMode) {
			daemonId = MonitorUtilities.getShortHostname();
		} else {
			daemonId = swiftEnvironment.getDaemonConfig().getName();
		}
		final int portNumber = getPortNumber(environment, configMode);
		final File tempFolder = getTempFolder(environment, configMode);

		final Server server = new Server(portNumber);
		server.addLifeCycle(new AbstractLifeCycle() {
			@Override
			protected void doStop() throws Exception {
				stopSwift();
			}
		});
		jettyStopThread = new JettyStopThread(server);
		jettyStopThread.start();

		for (final Connector connector : server.getConnectors()) {
			connector.setHeaderBufferSize(HEADER_BUFFER_SIZE);
		}

		Future<Object> future = null;
		try {
			WebAppContext webAppContext = makeWebAppContext(environment.getConfigFile(), configMode ? "config" : "production", warFile, daemonId, tempFolder, jettyStopThread.getPort());
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

	private WebAppContext makeWebAppContext(final File configFile, final String action, final File warFile, final String daemonId, final File tempFolder, int stopPort) {
		final WebAppContext webAppContext = new WebAppContext();
		webAppContext.setWar(warFile.getAbsolutePath());
		webAppContext.setContextPath("/");
		// We must set temp directory, otherwise the app goes to /tmp which will get deleted periodically
		webAppContext.setTempDirectory(tempFolder);
		// We try to prevent jetty from extracting the .war file
		webAppContext.setExtractWAR(false);
		final Map<String, String> initParams = new HashMap<String, String>(3);
		if (configFile != null) {
			initParams.put("SWIFT_CONFIG", configFile.getAbsolutePath());
		}
		initParams.put("SWIFT_ACTION", action);
		initParams.put("SWIFT_DAEMON", daemonId);
		initParams.put("SWIFT_STOP_PORT", String.valueOf(stopPort));

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

	static File getTempFolder(final SwiftEnvironment environment, final boolean configMode) {
		File tempFolder;
		try {
			final DaemonConfig daemonConfig = environment.getDaemonConfig();
			tempFolder = new File(daemonConfig.getTempFolderPath());
		} catch (Exception e) {
			tempFolder = FileUtilities.getDefaultTempDirectory();
			if (!configMode) {
				LOGGER.warn("Could not parse the config file " + (environment.getConfigFile() == null ? "<null>" : environment.getConfigFile().getPath()) + " to obtain temporary daemon folder. Using default " + tempFolder, e);
			}
		}
		return tempFolder;
	}

	static int getPortNumber(final SwiftEnvironment environment, final boolean configMode) {
		final List<String> options = environment.getParameters();
		final File configFile = environment.getConfigFile();
		for (int i = 0; i < options.size() - 1; i++) {
			if ("port".equals(options.get(i))) {
				return Integer.valueOf(options.get(i + 1));
			}
		}
		try {
			final WebUi.Config webUi = (WebUi.Config) getResourceConfig(environment, WebUi.Config.class);
			return Integer.parseInt(webUi.getPort());
		} catch (Exception e) {
			if (configMode) {
				// We are configuring Swift, no wonder config file is not present
				LOGGER.info("Default port used for config: " + DEFAULT_PORT);
			} else {
				LOGGER.warn("Could not parse the config file " + (configFile == null ? "<null>" : configFile.getPath()) + " to obtain web port number. Using default " + DEFAULT_PORT, e);
			}
			return DEFAULT_PORT;
		}
	}

	static ResourceConfig getResourceConfig(final SwiftEnvironment environment, final Class<?> clazz) {
		final DaemonConfig daemonConfig = environment.getDaemonConfig();
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

	@Override
	public void fileChanged(final Collection<File> files, final boolean timeout) {
		if (!timeout) {
			LOGGER.info("The configuration file " + files.iterator().next() + " is modified. Restarting.");
			stopSwift();
		}
	}

	private void stopSwift() {
		synchronized (stopMonitor) {
			restartRequested = true;
			stopMonitor.notifyAll();
		}
	}
}
