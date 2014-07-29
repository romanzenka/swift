package edu.mayo.mprc.qstat;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

/**
 * Work packet for the qstat call. Defines the job id to be queried.
 */
public final class QstatWorkPacket extends WorkPacketBase {
	private static final long serialVersionUID = 20080130;
	private int jobId;

	public QstatWorkPacket(final int jobId) {
		super(true);
		this.jobId = jobId;
	}

	public int getJobId() {
		return jobId;
	}
}
