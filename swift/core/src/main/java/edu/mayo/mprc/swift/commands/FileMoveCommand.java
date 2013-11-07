package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;


/**
 * Scenario: The user moves directories in directory A to directory B.
 * <p/>
 * We do not know exactly what moved.
 * <p/>
 * Go through the database and find all references to file that are in A. If those files do not exist,
 * but they do exist in B, update the database reference to point to B.
 *
 * @author Roman Zenka
 */
@Component("file-move-command")
public final class FileMoveCommand implements SwiftCommand {
	private SwiftDao swiftDao;

	@Override
	public String getDescription() {
		return "Usage: file-move <dir A> <dir B> - updates broken database file links that do not exist in A, but do exist in B";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		final SwiftSearcher.Config config = LoadToSearchDb.getSearcher(environment.getDaemonConfig());
		LoadToSearchDb.initializeDatabase(environment, config);

		final File from = new File(environment.getParameters().get(0)).getAbsoluteFile();
		final File to = new File(environment.getParameters().get(1)).getAbsoluteFile();

		checkExistingDirectory(from);
		checkExistingDirectory(to);

		swiftDao.begin();
		try {
			swiftDao.renameAllFileReferences(from, to);
			swiftDao.commit();
		} catch (Exception e) {
			swiftDao.rollback();
			throw new MprcException(e);
		}

		return ExitCode.Ok;
	}

	private void checkExistingDirectory(final File file) {
		if (!file.exists()) {
			throw new MprcException("The file " + file.getAbsolutePath() + " must exist");
		}
		if (!file.isDirectory()) {
			throw new MprcException("The file " + file.getAbsolutePath() + " must be a directory");
		}
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	@Resource(name = "swiftDao")
	public void setSwiftDao(SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}
}
