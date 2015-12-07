package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.swift.db.SwiftDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Roman Zenka
 */
@Component("upgrade-db")
public final class UpgradeDatabase implements SwiftCommand {
	private static final Logger LOGGER = Logger.getLogger(UpgradeDatabase.class);

	@Resource(name = "swiftDao")
	private SwiftDao swiftDao;

	@Resource(name = "searchDbDao")
	private SearchDbDao searchDbDao;

	@Override
	public String getDescription() {
		return "Usage: upgrade-db - ensure that the database is filled with latest data";
	}

	@Override
	public ExitCode run(SwiftEnvironment environment) {
		swiftDao.begin();
		try {
			final int databaseVersion = swiftDao.getDatabaseVersion();

			LOGGER.info("Filling in the Raw file sizes");
			searchDbDao.fillTandemMassSpectrometrySampleSizes();

			swiftDao.commit();
			return ExitCode.Ok;
		} catch (Exception e) {
			swiftDao.rollback();
			environment.logCommandError(MprcException.getDetailedMessage(e));
			return ExitCode.Error;
		}
	}
}
