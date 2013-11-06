package edu.mayo.mprc.daemon;

import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.util.ArrayList;
import java.util.List;

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
		for (final ProgressReporter reporter : reporters) {
			reporter.reportProgress(progressInfo);
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
}
