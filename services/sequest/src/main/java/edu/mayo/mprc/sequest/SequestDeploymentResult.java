package edu.mayo.mprc.sequest;

import edu.mayo.mprc.enginedeployment.DeploymentResult;

import java.io.File;

public final class SequestDeploymentResult extends DeploymentResult {
	private static final long serialVersionUID = 20080327L; //previously: 20071220L;
	private File fileToSearchAgainst;

	public File getFileToSearchAgainst() {
		return fileToSearchAgainst;
	}

	public void setFileToSearchAgainst(final File toSearchAgainst) {
		fileToSearchAgainst = toSearchAgainst;
	}
}
