package edu.mayo.mprc.swift;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ApplicationConfig;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ServiceConfig;
import edu.mayo.mprc.daemon.MessageBroker;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.database.FileType;
import edu.mayo.mprc.messaging.ActiveMQConnectionPool;
import edu.mayo.mprc.swift.db.FileTokenFactoryWrapper;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Methods related to Swift configuration.
 *
 * @author Roman Zenka
 */
public final class SwiftConfig {
	private SwiftConfig() {
	}

	/**
	 * Validates the config, returns a list of discovered errors.
	 *
	 * @param swift Swift configuration to validate.
	 */
	public static List<String> validateSwiftConfig(final ApplicationConfig swift) {
		final List<String> errors = new ArrayList<String>();
		// Make sure we have the essential modules
		if (swift.getModulesOfConfigType(SwiftSearcher.Config.class).isEmpty()) {
			errors.add("Without " + SwiftSearcher.NAME + " module you will not be able to run any Swift searches.");
		}
		if (swift.getModulesOfConfigType(WebUi.Config.class).isEmpty()) {
			errors.add("Without " + WebUi.NAME + " modules you will not be able to interact with Swift.");
		}
		final int numDbs = swift.getModulesOfConfigType(Database.Config.class).size();
		if (numDbs == 0) {
			errors.add("Without " + Database.Factory.NAME + " module, Swift will not be able to function.");
		}
		if (numDbs > 1) {
			errors.add("Swift cannot currently be configured with more than one " + Database.Factory.NAME + " module.");
		}
		if (swift.getModulesOfConfigType(MessageBroker.Config.class).size() != 1) {
			errors.add("Without a single " + MessageBroker.NAME + " module, other Swift modules will not be able to communicate with each other.");
		}

		// Make sure that modules that have to be within one daemon are within one daemon
		// Currently the web ui has to be at the same place as searcher it links to
		// A searcher has to be at the same place as the database it links to
		for (final ResourceConfig config : swift.getModulesOfConfigType(SwiftSearcher.Config.class)) {
			final SwiftSearcher.Config searcher = (SwiftSearcher.Config) config;
			final ResourceConfig database = searcher.getDatabase();
			if (database == null) {
				errors.add("Each " + SwiftSearcher.NAME + " has to reference a " + Database.Factory.NAME + " module that is within the same daemon.");
			} else {
				if (isDifferentDaemon(swift, database, searcher)) {
					errors.add("Each " + SwiftSearcher.NAME + " must be located in the same daemon as the " + Database.Factory.NAME + " it refers to.");
					break;
				}
			}
		}

		for (final ResourceConfig config : swift.getModulesOfConfigType(WebUi.Config.class)) {
			final WebUi.Config ui = (WebUi.Config) config;
			final ServiceConfig searcher = ui.getSearcher();
			if (searcher == null) {
				errors.add("Each " + WebUi.NAME + " has to reference a " + SwiftSearcher.NAME + " module that is within the same daemon.");
			} else {
				if (isDifferentDaemon(swift, searcher, ui)) {
					errors.add("Each " + WebUi.NAME + " must be located in the same daemon as the " + SwiftSearcher.NAME + " it refers to.");
					break;
				}
			}
		}

		return errors;
	}

	private static boolean isDifferentDaemon(final ApplicationConfig swift, final ResourceConfig config1, final ResourceConfig config2) {
		if (config1 == null || config2 == null) {
			return false;
		}
		final DaemonConfig daemon1 = swift.getDaemonForResource(config1);
		if (daemon1 == null) {
			return false;
		}
		final DaemonConfig daemon2 = swift.getDaemonForResource(config2);
		return !daemon1.equals(daemon2);
	}

	/**
	 * Returns the daemon the user asked us to run. If the user did not specify anything, and there is just one
	 * daemon defined, we return that one. Otherwise we look at the hostname of the machine we run, and attempt
	 * to find a daemon that matches this hostname. If all fails, we throw an exception.
	 *
	 * @param daemonId    ID of the daemon to run.
	 * @param swiftConfig Current swift config.
	 * @return Configuration of the daemon the user wants to run.
	 */
	public static DaemonConfig getUserSpecifiedDaemonConfig(final String daemonId, final ApplicationConfig swiftConfig) {
		final String daemonIdToLoad;
		if (daemonId == null) {
			// The user did not specify daemon name. If there is only one daemon defined, that is fine - we
			// will run that one. Otherwise complain.
			if (swiftConfig.getDaemons().size() > 1) {
				daemonIdToLoad = daemonIdMatchingHostname(swiftConfig.getDaemons());
				if (daemonIdToLoad == null) {
					throw new MprcException("There is more than one daemon specified in this configuration, none matches the hostname.\n"
							+ "Run Swift with --daemon set to one of: " + joinDaemonNames(swiftConfig));
				}
			} else {
				daemonIdToLoad = swiftConfig.getDaemons().get(0).getName();
			}
		} else {
			daemonIdToLoad = daemonId;
		}
		return swiftConfig.getDaemonConfig(daemonIdToLoad);
	}

	/**
	 * Return id of the daemon that matches the current hostname.
	 *
	 * @param daemons List of daemons to check.
	 * @return Daemon id matching hostname, null if none found.
	 */
	private static String daemonIdMatchingHostname(final Iterable<DaemonConfig> daemons) {
		final String hostname = FileUtilities.getHostname();
		for (final DaemonConfig config : daemons) {
			if (hostname.equalsIgnoreCase(config.getHostName())) {
				return config.getName();
			}
		}

		final String shortHostname = FileUtilities.getShortHostname();
		for (final DaemonConfig config : daemons) {
			if (shortHostname.equalsIgnoreCase(FileUtilities.extractShortHostname(config.getHostName()))) {
				return config.getName();
			}
		}

		return null;
	}

	/**
	 * Join names of all daemons defined in this config.
	 */
	private static String joinDaemonNames(ApplicationConfig swiftConfig) {
		final StringBuilder builder = new StringBuilder(100);
		for (final DaemonConfig daemonConfig : swiftConfig.getDaemons()) {
			builder.append("'");
			builder.append(daemonConfig.getName());
			builder.append("', ");
		}
		builder.setLength(builder.length() - 2);
		return builder.toString();
	}

	/**
	 * Sets up the file token factory. File token factory needs to know which daemon we are running in,
	 * and where is the database module. The database module is located within the config.
	 *
	 * @param swiftConfig  Complete Swift config.
	 * @param daemonConfig Config for the active daemon.
	 */
	public static void setupFileTokenFactory(final ApplicationConfig swiftConfig, final DaemonConfig daemonConfig, final FileTokenFactory fileTokenFactory, final ActiveMQConnectionPool connectionPool) {
		// Setup the actual daemon
		fileTokenFactory.setDaemonConfigInfo(daemonConfig.createDaemonConfigInfo());
		if (daemonConfig.getTempFolderPath() == null) {
			throw new MprcException("The temporary folder is not configured for this daemon. Swift cannot run.");
		}
		fileTokenFactory.setTempFolderRepository(new File(daemonConfig.getTempFolderPath()));

		final DaemonConfig databaseDaemonConfig = getDatabaseDaemonConfig(swiftConfig);

		fileTokenFactory.setDatabaseDaemonConfigInfo(databaseDaemonConfig.createDaemonConfigInfo());

		FileType.initialize(new FileTokenFactoryWrapper(fileTokenFactory));
	}

	/**
	 * Returns a config for a daemon that contains the database. There must be exactly one such daemon.
	 *
	 * @param swiftConfig Swift configuration.
	 * @return Daemon that contains the database module.
	 */
	static DaemonConfig getDatabaseDaemonConfig(final ApplicationConfig swiftConfig) {
		final ResourceConfig databaseResource = getDatabaseResource(swiftConfig);
		return swiftConfig.getDaemonForResource(databaseResource);
	}

	static ResourceConfig getDatabaseResource(final ApplicationConfig swiftConfig) {
		final List<ResourceConfig> configs = swiftConfig.getModulesOfConfigType(Database.Config.class);
		if (configs.size() > 1) {
			throw new MprcException("Swift has more than one database defined.");
		}
		if (configs.isEmpty()) {
			throw new MprcException("Swift does not define a database.");
		}
		return configs.get(0);
	}
}
