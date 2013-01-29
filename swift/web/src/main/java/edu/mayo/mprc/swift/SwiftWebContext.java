package edu.mayo.mprc.swift;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ApplicationConfig;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.MultiFactory;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.dbcurator.server.CurationWebContext;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a utility class for centralizing access to the Spring ApplicationContext.  Ideally this
 * class would eventually go away as we wire more and more of Swift through Spring but in reality it will take a large
 * effort (too large?) to decouple Swift enough to make full Spring wiring possible.
 */
public final class SwiftWebContext {
	private static WebUi webUi;
	private static String initializedDaemon;
	private static boolean initializationRan = false;

	private static final Logger LOGGER = Logger.getLogger(SwiftWebContext.class);

	private SwiftWebContext() {
	}

	public static void initialize(final File installPropertyFile, final String daemonId) {
		synchronized (SwiftWebContext.class) {
			if (!initializationRan) {
				initializationRan = true;
				try {
					System.setProperty("SWIFT_INSTALL", installPropertyFile.getAbsolutePath());
					MainFactoryContext.initialize();
					final Daemon.Factory daemonFactory = MainFactoryContext.getDaemonFactory();
					final MultiFactory factoryTable = MainFactoryContext.getResourceTable();

					final ApplicationConfig swiftConfig = ApplicationConfig.load(installPropertyFile, factoryTable);

					//Daemon selection logic. If only one daemon is defined, start daemon. If more that one daemon is defined, start selected daemon
					//if id matches. Throw exceptions if in any other case.
					DaemonConfig daemonConfig = null;

					if (swiftConfig.getDaemons().size() == 0) {
						throw new MprcException("No daemon has been defined in the Swift config file. Define daemon to swift config file and try again.");
					} else if (swiftConfig.getDaemons().size() > 1 && daemonId == null) {
						throw new MprcException("Multiple daemons are defined in the Swift config file, but no daemon was selected to be run. Select daemon and try again.");
					} else if (swiftConfig.getDaemons().size() == 1) {
						daemonConfig = swiftConfig.getDaemons().get(0);

					} else {
						for (final DaemonConfig cfg : swiftConfig.getDaemons()) {
							if (cfg.getName().equals(daemonId)) {
								daemonConfig = cfg;
								break;
							}
						}

						if (daemonConfig == null) {
							throw new MprcException("Daemon '" + daemonId + "' is not defined in the Swift config file." +
									"Defined daemons:" + "\n" + getDaemonNameList(swiftConfig.getDaemons()) + "\n" +
									"Verify daemon name and try again.");
						}
					}

					final Daemon daemon = daemonFactory.createDaemon(daemonConfig);

					for (final Object obj : daemon.getResources()) {
						if (obj instanceof WebUi) {
							webUi = (WebUi) obj;
							break;
						}
					}
					if (webUi == null) {
						throw new MprcException("The daemon " + daemonId + " does not define any web interface module.");
					}

					SwiftConfig.setupFileTokenFactory(swiftConfig, daemonConfig, webUi.getFileTokenFactory());

					// Initialize DB curator
					CurationWebContext.initialize(
							webUi.getCurationDao(),
							webUi.getFastaFolder(),
							webUi.getFastaUploadFolder(),
							webUi.getFastaArchiveFolder(),
							// TODO: Fix this - the curator will keep creating temp folders and never deleting them
							// TODO: Also, the user should be able to specify where the temp files should go
							FileUtilities.createTempFolder());

					daemon.start();
					webUi.getSwiftMonitor().initialize(swiftConfig);
					webUi.getSwiftMonitor().start();

					initializedDaemon = daemonId;
				} catch (Exception t) {
					LOGGER.fatal("Swift web application should be terminated", t);
					webUi.getSwiftMonitor().stop();
					System.exit(1);
					throw new MprcException(t);
				}
			}
		}
	}

	public static boolean isInitialized(final String daemonId) {
		synchronized (SwiftWebContext.class) {
			if (initializedDaemon != null) {
				return initializedDaemon.equals(daemonId);
			}
		}

		return false;
	}

	private static String getDaemonNameList(final List<DaemonConfig> daemonConfigList) {
		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("\n");

		for (final DaemonConfig daemonConfig : daemonConfigList) {
			stringBuilder.append(daemonConfig.getName()).append("\n");
		}

		return stringBuilder.toString();
	}

	/**
	 * @return Centralized configuration for all the servlets.
	 */
	public static WebUi getServletConfig() {
		synchronized (SwiftWebContext.class) {
			return webUi;
		}
	}

	public static String getPathPrefix() {
		final String prefix = getServletConfig().getFileTokenFactory().fileToDatabaseToken(
				getServletConfig().getBrowseRoot());
		if (!prefix.endsWith("/")) {
			return prefix + "/";
		}
		return prefix;
	}

	/**
	 * TODO: SwiftWebContext must not be a singleton to enable proper testing - fix!
	 */
	public static void setupTest() {
		synchronized (SwiftWebContext.class) {
			final WebUi.Config config = new WebUi.Config();
			final DependencyResolver dependencies = new DependencyResolver(null);
			final Map<String, String> map = new HashMap<String, String>(1);
			map.put(WebUi.BROWSE_ROOT, "/");
			config.load(map, dependencies);
			webUi = (WebUi) MainFactoryContext.getResourceTable().createSingleton(config, dependencies);
		}
	}
}
