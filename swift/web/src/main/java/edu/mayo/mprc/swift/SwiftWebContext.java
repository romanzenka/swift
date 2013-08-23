package edu.mayo.mprc.swift;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.MessageBroker;
import edu.mayo.mprc.dbcurator.server.CurationWebContext;
import edu.mayo.mprc.swift.commands.SwiftEnvironmentImpl;
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
	private static WebUiHolder webUiHolder;
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
					webUiHolder = MainFactoryContext.getWebUiHolder();
					final MultiFactory factoryTable = MainFactoryContext.getResourceTable();

					final ApplicationConfig swiftConfig = ApplicationConfig.load(installPropertyFile, factoryTable);

					//Daemon selection logic. If only one daemon is defined, start daemon. If more that one daemon is defined, start selected daemon
					//if id matches. Throw exceptions if in any other case.
					DaemonConfig daemonConfig = null;

					if (swiftConfig.getDaemons().isEmpty()) {
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
					final MessageBroker.Config messageBroker = SwiftEnvironmentImpl.getMessageBroker(daemonConfig);
					MainFactoryContext.getServiceFactory().initialize(messageBroker.getBrokerUrl(), daemonConfig.getName());

					final Daemon daemon = (Daemon) factoryTable.create(daemonConfig, new DependencyResolver(factoryTable));

					for (final Object obj : daemon.getResources()) {
						if (obj instanceof WebUi) {
							webUiHolder.setWebUi((WebUi) obj);
							break;
						}
					}
					if (webUiHolder.getWebUi() == null) {
						throw new MprcException("The daemon " + daemonId + " does not define any web interface module.");
					}
					webUiHolder.getWebUi().setMainDaemon(daemon);

					SwiftConfig.setupFileTokenFactory(swiftConfig, daemonConfig, webUiHolder.getWebUi().getFileTokenFactory(), MainFactoryContext.getConnectionPool());

					// Initialize DB curator
					final CurationWebContext curationWebContext = (CurationWebContext) MainFactoryContext.getContext().getBean("curationWebContext");
					curationWebContext.initialize(
							webUiHolder.getWebUi().getFastaFolder(),
							webUiHolder.getWebUi().getFastaUploadFolder(),
							webUiHolder.getWebUi().getFastaArchiveFolder(),
							// TODO: Fix this - the curator will keep creating temp folders and never deleting them
							// TODO: Also, the user should be able to specify where the temp files should go
							FileUtilities.createTempFolder());

					daemon.start();
					webUiHolder.getWebUi().getSwiftMonitor().initialize(swiftConfig);
					webUiHolder.getWebUi().getSwiftMonitor().start();

					initializedDaemon = daemonId;
				} catch (Exception t) {
					LOGGER.fatal("Swift web application should be terminated", t);
					if (webUiHolder != null) {
						webUiHolder.stopSwiftMonitor();
					}
					MainFactoryContext.getConnectionPool().close();
					System.exit(1);
					throw new MprcException(t);
				}
			}
		}
	}

	public static void destroy() {
		if (webUiHolder.getWebUi() != null) {
			if (webUiHolder.getWebUi().getMainDaemon() != null) {
				webUiHolder.getWebUi().getMainDaemon().stop();
			}
			if (webUiHolder.getWebUi().getSwiftMonitor() != null) {
				webUiHolder.getWebUi().getSwiftMonitor().stop();
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
			return webUiHolder.getWebUi();
		}
	}

	public static WebUiHolder getWebUiHolder() {
		synchronized (SwiftWebContext.class) {
			return webUiHolder;
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
			final MapConfigReader reader = new MapConfigReader(dependencies, map);
			config.load(reader);
			webUiHolder = MainFactoryContext.getWebUiHolder();
			webUiHolder.setWebUi((WebUi) MainFactoryContext.getResourceTable().createSingleton(config, dependencies));
		}
	}
}
