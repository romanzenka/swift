package edu.mayo.mprc.daemon;

import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;

/**
 * Progress report sent after task is submitted for execution.
 */
public final class AssignedTaskData extends FileHolder implements ProgressInfo {
	private static final long serialVersionUID = 20071220L;
	private String jobId;

	public AssignedTaskData(final String jobId) {
		this.jobId = jobId;
	}

	public String getAssignedId() {
		return jobId;
	}
}
