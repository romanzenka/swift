package edu.mayo.mprc.sge;

//import drma_simulator.*;

import org.ggf.drmaa.JobInfo;

/**
 * this packet needs to provide grid engine task run information
 * including
 * - application name
 * - parameter string
 */
public final class GridEngineWorkPacket extends GridWorkPacket {
	private JobInfo jobInfo;

	public GridEngineWorkPacket(final GridWorkPacket packet) {
		super(packet);
	}

	public void setJobInfo(final JobInfo pJobInfo) {
		jobInfo = pJobInfo;
	}

	public JobInfo getJobInfo() {
		return jobInfo;
	}


}
