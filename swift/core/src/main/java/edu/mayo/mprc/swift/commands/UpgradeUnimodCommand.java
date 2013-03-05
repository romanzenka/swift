package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.unimod.UnimodDao;
import edu.mayo.mprc.unimod.UnimodUpgrade;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

/**
 * @author Roman Zenka
 */
public final class UpgradeUnimodCommand implements SwiftCommand {
	private static final Logger LOGGER = Logger.getLogger(UpgradeUnimodCommand.class);
	private UnimodDao unimodDao;

	public UnimodDao getUnimodDao() {
		return unimodDao;
	}

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
		final UnimodUpgrade upgrade = getUnimodDao().upgrade(getUnimodDao().getDefaultUnimod(), new Change("Upgrading unimod", new DateTime()));
		LOGGER.info(upgrade.toString());
		return ExitCode.Ok;
	}
}
