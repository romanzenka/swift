package edu.mayo.mprc.daemon;

import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;

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
	private final long parentLogId;
	private final long logId;
	private final File errorLogFile;
	private final File outputLogFile;

	public NewLogFiles(final long parentLogId, final long logId, final File outputLogFile, final File errorLogFile) {
		this.parentLogId = parentLogId;
		this.logId = logId;
		this.outputLogFile = outputLogFile;
		this.errorLogFile = errorLogFile;
	}

	public long getLogId() {
		return logId;
	}

	public long getParentLogId() {
		return parentLogId;
	}

	public File getErrorLogFile() {
		return errorLogFile;
	}

	public File getOutputLogFile() {
		return outputLogFile;
	}

}
