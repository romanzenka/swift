package edu.mayo.mprc.daemon;

import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Changes setup of Log4j to funnel everything with given MDC key to a particular stdout/stderr.
 * Creates a date-based sub-folder for the given log folder to cut down on number of side by side log files.
 */
final class LoggingSetup {
	private final File logOutputFolder;
	private LogWriterAppender outLogWriterAppender;
	private LogWriterAppender errorLogWriterAppender;
	private File standardOutFile;
	private File standardErrorFile;
	private String mdcKey;

	private static final String STD_ERR_FILE_PREFIX = "e";
	private static final String STD_OUT_FILE_PREFIX = "o";
	private static final String LOG_FILE_EXTENSION = ".log";

	private static final AtomicLong UNIQUE_LOG_FILE_ID = new AtomicLong(System.currentTimeMillis());

	LoggingSetup(final File logOutputFolder) {
		this.logOutputFolder = logOutputFolder;
	}

	/**
	 * Create standard output and error log files, set up Log4j to append to them.
	 * Make sure to call {@link #stopLogging()} when done, preferably in a finally section.
	 *
	 * @throws IOException
	 */
	public void startLogging() throws IOException {
		final Date date = new Date();
		final File logSubFolder = FileUtilities.getDateBasedDirectory(logOutputFolder, date);

		final long logFileId = UNIQUE_LOG_FILE_ID.incrementAndGet();
		standardOutFile = new File(logSubFolder, STD_OUT_FILE_PREFIX + logFileId + LOG_FILE_EXTENSION);
		standardErrorFile = new File(logSubFolder, STD_ERR_FILE_PREFIX + logFileId + LOG_FILE_EXTENSION);

		mdcKey = Long.toString(logFileId);
		MDC.put(mdcKey, mdcKey);

		outLogWriterAppender = new LogWriterAppender(new FileWriter(standardOutFile.getAbsoluteFile()));
		outLogWriterAppender.setAllowedMDCKey(mdcKey, mdcKey);
		Logger.getRootLogger().addAppender(outLogWriterAppender);

		errorLogWriterAppender = new LogWriterAppender(new FileWriter(standardErrorFile.getAbsoluteFile()));
		errorLogWriterAppender.addAllowedLevel(Level.ERROR);
		errorLogWriterAppender.setAllowedMDCKey(mdcKey, mdcKey);
		Logger.getRootLogger().addAppender(errorLogWriterAppender);
	}

	/**
	 * Reverts Log4j back to normal.
	 */
	public void stopLogging() {
		MDC.remove(mdcKey);

		if (outLogWriterAppender != null) {
			Logger.getRootLogger().removeAppender(outLogWriterAppender);
			FileUtilities.closeObjectQuietly(outLogWriterAppender);
		}

		if (errorLogWriterAppender != null) {
			Logger.getRootLogger().removeAppender(errorLogWriterAppender);
			FileUtilities.closeObjectQuietly(errorLogWriterAppender);
		}
	}

	public File getStandardOutFile() {
		return standardOutFile;
	}

	public File getStandardErrorFile() {
		return standardErrorFile;
	}
}
