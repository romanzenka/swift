package edu.mayo.mprc.sge;

import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * This class manages a folder with failed jobs. For a given serialized grid packet whose execution failed,
 * it can create a copy in the failed job folder and return path to it. When the folder gets too full, it
 * gets cleaned up.
 *
 * @author Roman Zenka
 */
public final class FailedJobManager {
	private static final Logger LOGGER = Logger.getLogger(FailedJobManager.class);
	public static final int CLEANUP_FREQUENCY = 10;
	public static final String STORED_EXTENSION = ".failed_job";

	private static final FilenameFilter STORED_FILES = new StoredFilesFilter();
	public static final long MAX_AGE_MILLIS = TimeUnit.DAYS.toMillis(1);
	/**
	 * When true, we store files. When false, no files get stored.
	 */
	private boolean storeFiles;

	/**
	 * Where to store the files.
	 */
	private File storedFileDirectory;

	/**
	 * Do not store any files.
	 */
	public FailedJobManager() {
	}

	/**
	 * Store files using the given settings.
	 *
	 * @param storedFileDirectory Where to store files.
	 */
	public FailedJobManager(final File storedFileDirectory) {
		this.storedFileDirectory = storedFileDirectory;
		storeFiles = true;
		FileUtilities.ensureFolderExists(storedFileDirectory);
	}

	/**
	 * @param file File to store
	 * @return Where did the file get stored, or null if we are not set to store files
	 */
	public File storeFile(final File file) {
		if (storeFiles) {
			final UUID newFileName = UUID.randomUUID();

			final File newFile = new File(storedFileDirectory, newFileName + FileUtilities.getExtension(file.getName()) + STORED_EXTENSION);

			FileUtilities.copyFile(file, newFile, true);

			final File[] files = storedFileDirectory.listFiles(STORED_FILES);
			// On every tenth file, we run a cleanup
			if (files.length % CLEANUP_FREQUENCY == 0) {
				cleanup(files, new Date().getTime() - MAX_AGE_MILLIS);
			}

			return newFile;
		}
		return null;
	}

	private void cleanup(final File[] files, final long newerThan) {
		LOGGER.debug(String.format("Cleaning up failed job packages from %s", storedFileDirectory));
		int cleaned = 0;
		for (final File file : files) {
			if (file.lastModified() < newerThan) {
				FileUtilities.quietDelete(file);
				cleaned++;
			}
		}
		LOGGER.debug(String.format("Cleaned up %d files", cleaned));
	}

	private static class StoredFilesFilter implements FilenameFilter {
		@Override
		public boolean accept(final File dir, final String name) {
			return name.endsWith(STORED_EXTENSION);
		}
	}
}
