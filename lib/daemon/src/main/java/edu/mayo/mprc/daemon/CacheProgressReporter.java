package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.log.NewLogFiles;
import edu.mayo.mprc.utilities.log.ParentLog;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReport;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Holds progress reporters for a particular work in progress.
 * Every new notification is distributed to all current reporters.
 * The newcomers get immediately notified of the past events (abridged form).
 */
final class CacheProgressReporter implements ProgressReporter {
	private boolean startReported;
	private String startedOnHost;
	private ProgressInfo lastProgressInfo;
	private boolean successReported;
	private Throwable failureReported;
	private List<ProgressReporter> reporters = new ArrayList<ProgressReporter>(1);

	CacheProgressReporter() {
	}

	public synchronized void addProgressReporter(final ProgressReporter reporter) {
		reporters.add(reporter);
		if (startReported) {
			reporter.reportStart(startedOnHost);
		}
		if (lastProgressInfo != null) {
			reporter.reportProgress(lastProgressInfo);
		}
		if (successReported) {
			reporter.reportSuccess();
		}
		if (failureReported != null) {
			reporter.reportFailure(failureReported);
		}
	}

	@Override
	public synchronized void reportStart(final String hostString) {
		startReported = true;
		startedOnHost = hostString;
		for (final ProgressReporter reporter : reporters) {
			reporter.reportStart(hostString);
		}
	}

	@Override
	public synchronized void reportProgress(final ProgressInfo progressInfo) {
		lastProgressInfo = progressInfo;
		if (progressInfo instanceof NewLogFiles) {
			// We only pass the log info on our first reporter.
			// Others should get info that another task is doing their work
			reporters.get(0).reportProgress(progressInfo);
		} else {
			for (final ProgressReporter reporter : reporters) {
				reporter.reportProgress(progressInfo);
			}
		}
	}

	@Override
	public synchronized void reportSuccess() {
		successReported = true;
		for (final ProgressReporter reporter : reporters) {
			reporter.reportSuccess();
		}
	}

	@Override
	public synchronized void reportFailure(final Throwable t) {
		failureReported = t;
		for (final ProgressReporter reporter : reporters) {
			reporter.reportFailure(failureReported);
		}
	}

	@Override
	/**
	 * The parent log is the parent log of the very first requestor.
	 * Every newcomer should only reference the same parent log.
	 */
	public synchronized ParentLog getLog() {
		throw new MprcException("The cache progress reporter does not define parent log");
	}
}
