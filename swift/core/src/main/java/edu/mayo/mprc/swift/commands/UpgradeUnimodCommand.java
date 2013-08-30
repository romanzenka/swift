package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.unimod.UnimodDao;
import edu.mayo.mprc.unimod.UnimodUpgrade;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Roman Zenka
 */
@Component("upgradeUnimodCommand")
public final class UpgradeUnimodCommand implements SwiftCommand {
	private static final Logger LOGGER = Logger.getLogger(UpgradeUnimodCommand.class);
	private UnimodDao unimodDao;

	public UnimodDao getUnimodDao() {
		return unimodDao;
	}

	@Resource(name = "unimodDao")
	public void setUnimodDao(UnimodDao unimodDao) {
		this.unimodDao = unimodDao;
	}

	@Override
	public String getName() {
		return "upgrade-unimod";
	}

	@Override
	public String getDescription() {
		return "Upgrade unimod in the database to the latest version";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		initializeDatabase(environment, environment.getSwiftSearcher());
		getUnimodDao().begin();
		try {
			final UnimodUpgrade upgrade = getUnimodDao().upgrade(getUnimodDao().getDefaultUnimod(), new Change("Upgrading unimod", new DateTime()));
			LOGGER.info(upgrade.toString());
			getUnimodDao().commit();
		} catch (Exception e) {
			getUnimodDao().rollback();
			throw new MprcException("Unimod upgrade failed", e);
		}
		return ExitCode.Ok;
	}

	/**
	 * Initialize the database referenced by given Swift searcher.
	 *
	 * @param environment Swift environment.
	 * @param config      The configuration of the Swift searcher.
	 */
	public static void initializeDatabase(SwiftEnvironment environment, SwiftSearcher.Config config) {
		LOGGER.info("Initializing database");
		final Object database = environment.createResource(config.getDatabase());
		LOGGER.info("Database initialized");
	}

}
