package edu.mayo.mprc.myrimatch;

import edu.mayo.mprc.enginedeployment.DeploymentResult;

public final class MyriMatchDeploymentResult extends DeploymentResult {
	private static final long serialVersionUID = 20110715L;
	private long numForwardEntries;
	private String decoySequencePrefix;

	public MyriMatchDeploymentResult() {
	}

	public long getNumForwardEntries() {
		return numForwardEntries;
	}

	public void setNumForwardEntries(final long numForwardEntries) {
		this.numForwardEntries = numForwardEntries;
	}

	public String getDecoySequencePrefix() {
		return decoySequencePrefix;
	}

	public void setDecoySequencePrefix(final String decoySequencePrefix) {
		this.decoySequencePrefix = decoySequencePrefix;
	}
}
