package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import edu.mayo.mprc.workflow.engine.AssignedExecutedOnHost;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import java.util.Date;

abstract class TaskProgressListener implements ProgressListener {
	private static final Logger LOGGER = Logger.getLogger(TaskProgressListener.class);

	private final AsyncTaskBase task;

	protected TaskProgressListener(final AsyncTaskBase task) {
		this.task = task;
	}

	@Override
	public void requestEnqueued(final String hostString) {
		task.setTaskEnqueued(new Date());
		task.setExecutedOnHost(hostString);
		task.afterProgressInformationReceived(new AssignedExecutedOnHost(hostString));
	}

	@Override
	public void requestProcessingStarted(final String hostString) {
		task.setTaskProcessingStarted(new Date());
		task.setExecutedOnHost(hostString);
		task.afterProgressInformationReceived(new AssignedExecutedOnHost(hostString));
	}

	@Override
	public void requestTerminated(final Exception e) {
		try {
			NDC.push(task.getFullId());
			LOGGER.debug("Task failed: " + task.getName(), e);
			task.setError(e);
		} finally {
			NDC.pop();
		}
	}

	@Override
	public void userProgressInformation(final ProgressInfo progressInfo) {
		task.afterProgressInformationReceived(progressInfo);
	}

	public AsyncTaskBase getTask() {
		return task;
	}
}
