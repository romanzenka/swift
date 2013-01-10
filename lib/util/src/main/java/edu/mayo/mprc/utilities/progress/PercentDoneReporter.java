package edu.mayo.mprc.utilities.progress;

import org.apache.log4j.Logger;

import java.text.MessageFormat;

/**
 * Helps with the task of reporting how many percent is done.
 * You can specify how much time between reports has to pass (not to flood the system with reports).
 *
 * @author Roman Zenka
 */
public final class PercentDoneReporter {
	private static final Logger LOGGER = Logger.getLogger(PercentDoneReporter.class);

	private static final float PERCENT = 100.0f;

	/**
	 * By default, report each second only.
	 */
	private static final long ONE_SECOND = 1000L;

	/**
	 * How many milliseconds between reports.
	 */
	private long reportEachMillis;

	/**
	 * When did we do the last report.
	 */
	private long lastReportTimestamp;

	/**
	 * Where to report progress.
	 */
	private UserProgressReporter progressReporter;

	/**
	 * Message to prepend the percent report with.
	 */
	private String progressMessage;

	/**
	 * See {@link #PercentDoneReporter(UserProgressReporter, String, long)}. Will report once a second.
	 *
	 * @param progressReporter Reporter to report to.
	 * @param progressMessage  Message to prepend each report with in the log.
	 */
	public PercentDoneReporter(final UserProgressReporter progressReporter, final String progressMessage) {
		this(progressReporter, progressMessage, ONE_SECOND);
	}

	/**
	 * Report percent done.
	 *
	 * @param progressReporter Reporter to report to.
	 * @param progressMessage  Message to prepend each report with in the log.
	 * @param reportEachMillis How far are the reports to be spaced (1000=report once a second).
	 */
	public PercentDoneReporter(final UserProgressReporter progressReporter, final String progressMessage, final long reportEachMillis) {
		// Make sure we report instantly the first time
		lastReportTimestamp = System.currentTimeMillis() - reportEachMillis;

		this.progressReporter = progressReporter;
		this.progressMessage = progressMessage;
		this.reportEachMillis = reportEachMillis;
	}

	public String getProgressMessage() {
		return progressMessage;
	}

	public void setProgressMessage(final String progressMessage) {
		this.progressMessage = progressMessage;
	}

	/**
	 * @param percent How many percent done. 1 == 100%
	 */
	public void reportProgress(final float percent) {
		final long timeNow = System.currentTimeMillis();
		if (reportEachMillis < timeNow - lastReportTimestamp) {
			if (null != progressReporter) {
				progressReporter.reportProgress(new PercentDone(percent * PERCENT));
			}
			LOGGER.info(MessageFormat.format("{0}{1,number,#.##} percent done.", progressMessage, percent * PERCENT));
			lastReportTimestamp = timeNow;
		}
	}
}
