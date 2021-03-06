package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.worker.log.LogWriterAppender;
import edu.mayo.mprc.daemon.worker.log.NewLogFiles;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.MonitorUtilities;
import edu.mayo.mprc.utilities.log.ChildLog;
import edu.mayo.mprc.utilities.log.ParentLog;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * @author Roman Zenka
 */
public final class DaemonLoggerFactory {
	/**
	 * UUID for a root object
	 */
	public static final UUID ROOT = UUID.fromString("00000000-0000-0000-0000-000000000000");

	private static final Logger LOGGER = Logger.getLogger(DaemonLoggerFactory.class);

	private File logOutputFolder;

	public DaemonLoggerFactory(final File logOutputFolder) {
		this.logOutputFolder = logOutputFolder;
	}

	/**
	 * Creates a root parent log that matches the sender of the request.
	 * <p/>
	 * This log will notify the requester about each child log created
	 *
	 * @param id       ID of the parent log. Use {@link #ROOT} for root logger (if needed)
	 * @param reporter A means of reporting progress (in this case the fact we created child logs)
	 * @return A parent log corresponding to the request sender.
	 */
	public ParentLog createLog(final UUID id, final UserProgressReporter reporter) {
		return new RequestParentLog(id, reporter);
	}

	public File getLogFolder() {
		return logOutputFolder;
	}

	private final class RequestParentLog implements ParentLog {
		private static final long serialVersionUID = 6391153716801269497L;
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

		@Override
		public UUID getLogId() {
			return id;
		}
	}

	private final class RequestChildLog implements ChildLog {
		private static final long serialVersionUID = -4428747725005957176L;
		// Logging directly within this process
		private LogWriterAppender outLogWriterAppender;
		private LogWriterAppender errorLogWriterAppender;

		/**
		 * Logging using {@link #getOutputLogger()}
		 */
		private Logger outLogger;

		/**
		 * Logging using {@link #getErrorLogger()}
		 */
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
			logFileId = UUID.randomUUID();
			initialize(new File(outputLogFilePath), new File(errorLogFilePath), reporter, parentLogId);
		}

		private void initialize(final File outputLogFile, final File errorLogFile, final UserProgressReporter reporter, final UUID parentLogId) {
			this.reporter = reporter;

			standardOutFile = outputLogFile;
			standardErrorFile = errorLogFile;

			// Let the caller know that we have new logs
			final NewLogFiles data = new NewLogFiles(parentLogId, logFileId,
					getStandardOutFile(), getStandardErrorFile());

			LOGGER.info(String.format("Child log with id %s", logFileId.toString()));
			reporter.reportProgress(data);
		}

		/**
		 * Set up Log4j to append to log files
		 * Make sure to call {@link #stopLogging()} when done, preferably in a finally section.
		 */
		public void startLogging() {
			mdcKey = logFileId.toString();
			MDC.put(mdcKey, mdcKey);

			outLogWriterAppender = newOutWriterAppender(true);
			outLogWriterAppender.setAllowedMDCKey(mdcKey, mdcKey);
			final Logger root = Logger.getLogger("edu.mayo.mprc");
			root.addAppender(outLogWriterAppender);

			errorLogWriterAppender = newErrorWriterAppender(true);
			errorLogWriterAppender.addAllowedLevel(Level.ERROR);
			errorLogWriterAppender.setAllowedMDCKey(mdcKey, mdcKey);
			root.addAppender(errorLogWriterAppender);

			LOGGER.info("Logging from " + MonitorUtilities.getHostInformation());
		}

		private LogWriterAppender newOutWriterAppender(final boolean fullFormat) {
			try {
				final LogWriterAppender logWriterAppender = new LogWriterAppender(new FileWriter(standardOutFile.getAbsoluteFile()));
				logWriterAppender.setName("out");
				setFormat(logWriterAppender, fullFormat);
				return logWriterAppender;
			} catch (final IOException e) {
				throw new MprcException("Could not start logging", e);
			}
		}

		private LogWriterAppender newErrorWriterAppender(final boolean fullFormat) {
			try {
				final LogWriterAppender logWriterAppender = new LogWriterAppender(new FileWriter(standardErrorFile.getAbsoluteFile()));
				logWriterAppender.setName("err");
				setFormat(logWriterAppender, fullFormat);
				return logWriterAppender;
			} catch (final IOException e) {
				throw new MprcException("Could not start logging", e);
			}
		}

		private void setFormat(LogWriterAppender logWriterAppender, final boolean fullFormat) {
			if (fullFormat) {
				logWriterAppender.setLayout(new PatternLayout("%d{ISO8601} - %-5p %c{2} %M:%L %x - %m\n"));
			} else {
				logWriterAppender.setLayout(new PatternLayout("%d{ISO8601} %m\n"));
			}
		}

		/**
		 * Reverts Log4j back to normal.
		 */
		public void stopLogging() {
			MDC.remove(mdcKey);
			final Logger root = Logger.getLogger("edu.mayo.mprc");

			if (outLogWriterAppender != null) {
				root.removeAppender(outLogWriterAppender);
				FileUtilities.closeObjectQuietly(outLogWriterAppender);
			}

			if (errorLogWriterAppender != null) {
				root.removeAppender(errorLogWriterAppender);
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
				outLogger.setLevel(Level.INFO);
				outLogger.setAdditivity(false);
				outLogger.addAppender(newOutWriterAppender(false));
			}
			return outLogger;
		}

		@Override
		public Logger getErrorLogger() {
			if (errorLogger == null) {
				errorLogger = Logger.getLogger(STD_ERR_FILE_PREFIX + logFileId);
				errorLogger.setLevel(Level.ERROR);
				errorLogger.setAdditivity(false);
				errorLogger.addAppender(newErrorWriterAppender(false));
			}
			return errorLogger;
		}

		@Override
		public void close() {
			if (outLogger != null) {
				final Appender out = outLogger.getAppender("out");
				out.close();
				outLogger = null;
			}

			if (errorLogger != null) {
				final Appender err = errorLogger.getAppender("err");
				err.close();
				errorLogger = null;
			}
		}

		@Override
		public ChildLog createChildLog() {
			return new RequestChildLog(logOutputFolder, reporter, logFileId);
		}

		@Override
		public ChildLog createChildLog(final String outputLogFilePath, final String errorLogFilePath) {
			return new RequestChildLog(outputLogFilePath, errorLogFilePath, reporter, logFileId);
		}

		@Override
		public UUID getLogId() {
			return logFileId;
		}
	}

}
