package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.files.SenderTokenTranslator;
import edu.mayo.mprc.daemon.worker.log.LogWriterAppender;
import edu.mayo.mprc.daemon.worker.log.NewLogFiles;
import edu.mayo.mprc.messaging.Request;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.log.ChildLog;
import edu.mayo.mprc.utilities.log.ParentLog;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Roman Zenka
 */
public final class DaemonLoggerFactory {
	/**
	 * UUID for a root object
	 */
	public static final UUID ROOT = UUID.fromString("00000000-0000-0000-0000-000000000000");

	private File logOutputFolder;

	public DaemonLoggerFactory(final File logOutputFolder) {
		this.logOutputFolder = logOutputFolder;
	}

	/**
	 * Creates a root parent log that matches the sender of the request.
	 * <p/>
	 * This log will notify the requester about each child log created
	 *
	 * @param id       ID of the parent log. Use {@link #ROOT} for root logger
	 * @param reporter A means of reporting progress (in this case the fact we created child logs)
	 * @return A parent log corresponding to the request sender.
	 */
	public ParentLog createLog(final UUID id, final UserProgressReporter reporter) {
		return new RequestParentLog(id, reporter);
	}

	public File getLogFolder() {
		return logOutputFolder;
	}

	/**
	 * Does this log id correspond to root logger?
	 */
	public boolean isRoot(final UUID logId) {
		return ROOT.equals(logId);
	}

	private final class RequestParentLog implements ParentLog {
		private UUID id;
		private UserProgressReporter reporter;

		private RequestParentLog(final UUID id, final UserProgressReporter reporter) {
			this.id = id;
			this.reporter = reporter;
		}

		@Override
		public ChildLog createChildLog() {
			return new RequestChildLog(logOutputFolder, reporter, id);
		}

		@Override
		public ChildLog createChildLog(final String outputLogFilePath, final String errorLogFilePath) {
			return new RequestChildLog(outputLogFilePath, errorLogFilePath, reporter, id);
		}
	}

	private final class RequestChildLog implements ChildLog {
		private LogWriterAppender outLogWriterAppender;
		private LogWriterAppender errorLogWriterAppender;
		private Logger outLogger;
		private Logger errorLogger;
		private File standardOutFile;
		private File standardErrorFile;
		private String mdcKey;
		private UserProgressReporter reporter;
		private UUID logFileId;

		private static final String STD_ERR_FILE_PREFIX = "e";
		private static final String STD_OUT_FILE_PREFIX = "o";
		private static final String LOG_FILE_EXTENSION = ".log";

		/**
		 * Create standard output and error log files
		 */
		public RequestChildLog(final File logOutputFolder, final UserProgressReporter reporter, final UUID parentLogId) {
			final Date date = new Date();
			logFileId = UUID.randomUUID();

			final File logSubFolder = FileUtilities.getDateBasedDirectory(logOutputFolder, date);
			initialize(
					new File(logSubFolder, STD_OUT_FILE_PREFIX + logFileId + LOG_FILE_EXTENSION),
					new File(logSubFolder, STD_ERR_FILE_PREFIX + logFileId + LOG_FILE_EXTENSION),
					reporter, parentLogId);
		}

		public RequestChildLog(final String outputLogFilePath, final String errorLogFilePath, final UserProgressReporter reporter, final UUID parentLogId) {
			initialize(new File(outputLogFilePath), new File(errorLogFilePath), reporter, parentLogId);
		}

		private void initialize(final File outputLogFile, final File errorLogFile, final UserProgressReporter reporter, final UUID parentLogId) {
			this.reporter = reporter;

			standardOutFile = outputLogFile;
			standardErrorFile = errorLogFile;

			// Let the caller know that we have new logs
			final NewLogFiles data = new NewLogFiles(parentLogId, logFileId,
					getStandardOutFile(), getStandardErrorFile());

			reporter.reportProgress(data);
		}

		/**
		 * Set up Log4j to append to log files
		 * Make sure to call {@link #stopLogging()} when done, preferably in a finally section.
		 */
		public void startLogging() {
			mdcKey = logFileId.toString();
			MDC.put(mdcKey, mdcKey);

			outLogWriterAppender = newOutWriterAppender();
			outLogWriterAppender.setAllowedMDCKey(mdcKey, mdcKey);
			Logger.getRootLogger().addAppender(outLogWriterAppender);

			errorLogWriterAppender = newErrorWriterAppender();
			errorLogWriterAppender.addAllowedLevel(Level.ERROR);
			errorLogWriterAppender.setAllowedMDCKey(mdcKey, mdcKey);
			Logger.getRootLogger().addAppender(errorLogWriterAppender);
		}

		private LogWriterAppender newOutWriterAppender() {
			try {
				return new LogWriterAppender(new FileWriter(standardOutFile.getAbsoluteFile()));
			} catch (final IOException e) {
				throw new MprcException("Could not start logging", e);
			}
		}

		private LogWriterAppender newErrorWriterAppender() {
			try {
				return new LogWriterAppender(new FileWriter(standardErrorFile.getAbsoluteFile()));
			} catch (final IOException e) {
				throw new MprcException("Could not start logging", e);
			}
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

		@Override
		public Logger getOutputLogger() {
			if (outLogger == null) {
				outLogger = Logger.getLogger(STD_OUT_FILE_PREFIX + logFileId);
				outLogger.addAppender(newOutWriterAppender());
			}
			return outLogger;
		}

		@Override
		public Logger getErrorLogger() {
			if (errorLogger == null) {
				errorLogger = Logger.getLogger(STD_ERR_FILE_PREFIX + logFileId);
				errorLogger.addAppender(newErrorWriterAppender());
			}
			return errorLogger;
		}

		@Override
		public ChildLog createChildLog() {
			return new RequestChildLog(logOutputFolder, reporter, logFileId);
		}

		@Override
		public ChildLog createChildLog(final String outputLogFilePath, final String errorLogFilePath) {
			return new RequestChildLog(outputLogFilePath, errorLogFilePath, reporter, logFileId);
		}
	}

}
