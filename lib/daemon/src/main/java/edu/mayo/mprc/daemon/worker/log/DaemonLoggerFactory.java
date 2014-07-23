package edu.mayo.mprc.daemon.worker.log;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.NewLogFile;
import edu.mayo.mprc.messaging.Request;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.log.ChildLog;
import edu.mayo.mprc.utilities.log.ParentLog;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Roman Zenka
 */
public final class DaemonLoggerFactory {
	private File logOutputFolder;
	private static final AtomicLong UNIQUE_LOG_FILE_ID = new AtomicLong(System.currentTimeMillis());

	public DaemonLoggerFactory(final File logOutputFolder) {
		this.logOutputFolder = logOutputFolder;
	}

	/**
	 * Creates a root parent log that matches the sender of the request.
	 * <p/>
	 * This log will notify the requester about each child log created
	 *
	 * @param request Request that arrived to this daemon
	 * @return A parent log corresponding to the request sender.
	 */
	public ParentLog createLog(final Request request) {
		return new RequestParentLog(request);
	}

	private final class RequestParentLog implements ParentLog {
		private Request request;

		private RequestParentLog(final Request request) {
			this.request = request;
		}

		@Override
		public ChildLog createChildLog() {
			return new RequestChildLog(logOutputFolder, request, 0);
		}

		@Override
		public ChildLog createChildLog(final String outputLogFilePath, final String errorLogFilePath) {
			return new RequestChildLog(outputLogFilePath, errorLogFilePath, request, 0);
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
		private Request request;
		private long logFileId;

		private static final String STD_ERR_FILE_PREFIX = "e";
		private static final String STD_OUT_FILE_PREFIX = "o";
		private static final String LOG_FILE_EXTENSION = ".log";

		/**
		 * Create standard output and error log files
		 */
		public RequestChildLog(final File logOutputFolder, final Request request, final long parentLogId) {
			final Date date = new Date();
			logFileId = UNIQUE_LOG_FILE_ID.incrementAndGet();

			final File logSubFolder = FileUtilities.getDateBasedDirectory(logOutputFolder, date);
			initialize(
					new File(logSubFolder, STD_OUT_FILE_PREFIX + logFileId + LOG_FILE_EXTENSION),
					new File(logSubFolder, STD_ERR_FILE_PREFIX + logFileId + LOG_FILE_EXTENSION),
					request, parentLogId);
		}

		public RequestChildLog(final String outputLogFilePath, final String errorLogFilePath, final Request request, final long parentLogId) {
			initialize(new File(outputLogFilePath), new File(errorLogFilePath), request, parentLogId);
		}

		private void initialize(final File outputLogFile, final File errorLogFile, final Request request, final long parentLogId) {
			this.request = request;

			standardOutFile = outputLogFile;
			standardErrorFile = errorLogFile;

			// Let the caller know that we have new logs
			request.sendResponse(new NewLogFile(parentLogId, logFileId, getStandardOutFile(), getStandardErrorFile()), false);
		}

		/**
		 * Set up Log4j to append to log files
		 * Make sure to call {@link #stopLogging()} when done, preferably in a finally section.
		 */
		public void startLogging() {
			mdcKey = Long.toString(logFileId);
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
			return new RequestChildLog(logOutputFolder, request, logFileId);
		}

		@Override
		public ChildLog createChildLog(final String outputLogFilePath, final String errorLogFilePath) {
			return new RequestChildLog(outputLogFilePath, errorLogFilePath, request, logFileId);
		}
	}

}
