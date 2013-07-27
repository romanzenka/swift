package edu.mayo.mprc.daemon;

import edu.mayo.mprc.daemon.monitor.MonitorUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.Serializable;

/**
 * Message about daemon progress.
 */
public final class DaemonProgressMessage implements Serializable {
	private static final long serialVersionUID = 20071220L;

	private DaemonProgress progress;
	private ProgressInfo progressData;
	private String host;

	public DaemonProgressMessage(final DaemonProgress progress) {
		this(progress, null);
	}

	/**
	 * Processing started on a given host.
	 * TODO: This is confugins, improve
	 *
	 * @param host Host the processing started on.
	 */
	public DaemonProgressMessage(final String host) {
		this(DaemonProgress.RequestProcessingStarted, null);
		this.host = host;
	}

	public DaemonProgressMessage(final DaemonProgress progress, final ProgressInfo progressData) {
		if (DaemonProgress.UserSpecificProgressInfo != progress && null != progressData) {
			throw new IllegalArgumentException("You cannot specify user progress data on a request that is not marked as user-specific.");
		}
		this.progress = progress;
		this.progressData = progressData;
		host = MonitorUtilities.getHostInformation();
	}

	/**
	 * @return How far we progressed.
	 */
	public DaemonProgress getProgress() {
		return progress;
	}

	/**
	 * @return User data describing the progress in higher detail.
	 */
	public ProgressInfo getProgressData() {
		return progressData;
	}

	/**
	 * Return a string describing the host/user/JVM that provided this progress update.
	 */
	public String getHostString() {
		return host;
	}
}
