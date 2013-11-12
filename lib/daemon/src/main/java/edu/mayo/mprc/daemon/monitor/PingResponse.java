package edu.mayo.mprc.daemon.monitor;

import edu.mayo.mprc.utilities.progress.ProgressInfo;

/**
 * @author Roman Zenka
 */
public final class PingResponse implements ProgressInfo {
	private static final long serialVersionUID = -538020049361746255L;

	private DaemonStatus status = new DaemonStatus();

	public DaemonStatus getStatus() {
		return status;
	}
}
