package edu.mayo.mprc.daemon;

import edu.mayo.mprc.utilities.log.ParentLog;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

/**
 * A progress reporter for runners.
 *
 * @author Roman Zenka
 */
public final class RunnerProgressReporter implements ProgressReporter {
	private AbstractRunner runner;
	private final DaemonRequest request;
	private ParentLog parentLog;

	public RunnerProgressReporter(final AbstractRunner runner, final DaemonRequest request) {
		this.runner = runner;
		this.request = request;
	}

	@Override
	public void reportStart(final String hostString) {
		runner.sendResponse(request, new DaemonProgressMessage(hostString), false);
	}

	@Override
	public void reportProgress(final ProgressInfo progressInfo) {
		runner.sendResponse(request, new DaemonProgressMessage(DaemonProgress.UserSpecificProgressInfo, progressInfo), false);
	}

	@Override
	public void reportSuccess() {
		runner.sendResponse(request, new DaemonProgressMessage(DaemonProgress.RequestCompleted), true);
	}

	@Override
	public void reportFailure(final Throwable t) {
		runner.sendResponse(request, t, true);
	}

	public void setParentLog(final ParentLog parentLog) {
		this.parentLog = parentLog;
	}

	@Override
	public ParentLog getParentLog() {
		return parentLog;
	}
}
