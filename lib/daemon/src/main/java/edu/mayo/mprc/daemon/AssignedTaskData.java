package edu.mayo.mprc.daemon;

import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;

/**
 * Progress report sent after task is submitted for execution.
 */
public final class AssignedTaskData extends FileHolder implements ProgressInfo {
	private static final long serialVersionUID = 20071220L;
	private File errorLogFile;
	private File outputLogFile;
	private String jobId;

	public AssignedTaskData() {
	}

	public AssignedTaskData(final File outputLogFile, final File errorLogFile) {
		this(null, outputLogFile, errorLogFile);
	}

	public AssignedTaskData(final String id, final String outputLogFilePath, final String errorLogFilePath) {
		this(id, new File(outputLogFilePath), new File(errorLogFilePath));
	}

	public AssignedTaskData(final String jobId, final File outputLogFile, final File errorLogFile) {
		this.outputLogFile = outputLogFile;
		this.errorLogFile = errorLogFile;
		this.jobId = jobId;
	}

	public String getAssignedId() {
		return jobId;
	}

	public File getErrorLogFile() {
		return errorLogFile;
	}

	public File getOutputLogFile() {
		return outputLogFile;
	}

	public String toString() {
		return "SGE task id: " + (jobId != null ? jobId : "None") + " | Standard Out: " + outputLogFile.toString() + " | Error Out: " + errorLogFile.toString();
	}
}
