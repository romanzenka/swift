package edu.mayo.mprc.daemon;

import edu.mayo.mprc.utilities.progress.ProgressInfo;

/**
 * When a warning is detected, the warning message gets sent. This should get picked by the workflow and mark
 * the tasks appropriately, and stored in the database. (The db can store one warning).
 *
 * @author Roman Zenka
 */
public final class TaskWarning implements ProgressInfo {
	private static final long serialVersionUID = -4108376314237047875L;

	private String warningMessage;

	public TaskWarning() {
	}

	public TaskWarning(final String warningMessage) {
		this.warningMessage = warningMessage;
	}

	public String getWarningMessage() {
		return warningMessage;
	}

	public void setWarningMessage(final String warningMessage) {
		this.warningMessage = warningMessage;
	}
}
