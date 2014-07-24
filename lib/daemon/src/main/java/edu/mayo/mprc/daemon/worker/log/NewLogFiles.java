package edu.mayo.mprc.daemon.worker.log;

import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;
import java.util.UUID;

/**
 * Notify the caller that we have a new pair of log files.
 * <p/>
 * The very first time this response is sent, the parent log file ID will be set to zero (we are creating
 * the root log).
 * <p/>
 * As child logs are created, they will refer to their parent using the parent's id.
 * <p/>
 * The goal is that each log has an unique id within the persistence context, which is typically
 * a single task.
 *
 * @author Roman Zenka
 */
public final class NewLogFiles extends FileHolder implements ProgressInfo {
	private static final long serialVersionUID = 20140722;
	private UUID parentLogId;
	private UUID logId;
	private File errorLogFile;
	private File outputLogFile;

	public NewLogFiles() {
	}

	public NewLogFiles(final UUID parentLogId, final UUID logId, final File outputLogFile, final File errorLogFile) {
		this.parentLogId = parentLogId;
		this.logId = logId;
		this.outputLogFile = outputLogFile;
		this.errorLogFile = errorLogFile;
	}

	public UUID getLogId() {
		return logId;
	}

	public UUID getParentLogId() {
		return parentLogId;
	}

	public File getErrorLogFile() {
		return errorLogFile;
	}

	public File getOutputLogFile() {
		return outputLogFile;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof NewLogFiles)) return false;

		NewLogFiles that = (NewLogFiles) o;

		if (logId != null ? !logId.equals(that.logId) : that.logId != null) return false;
		if (parentLogId != null ? !parentLogId.equals(that.parentLogId) : that.parentLogId != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = parentLogId != null ? parentLogId.hashCode() : 0;
		result = 31 * result + (logId != null ? logId.hashCode() : 0);
		return result;
	}
}
