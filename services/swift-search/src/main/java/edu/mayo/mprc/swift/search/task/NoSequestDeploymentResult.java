package edu.mayo.mprc.swift.search.task;


import edu.mayo.mprc.enginedeployment.DeploymentResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class NoSequestDeploymentResult implements DatabaseDeploymentResult {
	private File fastaFile;

	NoSequestDeploymentResult(final File fastaFile) {
		this.fastaFile = fastaFile;
	}

	@Override
	public String getShortDbName() {
		assert false : "Short database name is not used by Sequest";
		return null;
	}

	/**
	 * @return We did not do deployment, so null is returned.
	 */
	@Override
	public File getSequestHdrFile() {
		return null;
	}

	/**
	 * @return The path to the taxonomy.xml file.
	 */
	public File getTaxonXml() {
		assert false : "Sequest deployment does not define Tandem's taxonomy";
		return null;
	}

	@Override
	public File getFastaFile() {
		return fastaFile;
	}

	@Override
	public List<File> getGeneratedFiles() {
		return new ArrayList<File>(0);
	}

	@Override
	public DeploymentResult getDeploymentResult() {
		return null;
	}
}
