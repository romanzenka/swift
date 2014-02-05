package edu.mayo.mprc.swift.search.task;


import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.enginedeployment.DeploymentResult;

import java.io.File;
import java.util.List;

/**
 * Represents a result of deploying a database. Implemented either by database deployer task, or a direct value
 * in case the deployment is skipped and the results are readily available.
 * <p/>
 * TODO: Simplify this/get rid of it. Too confusing.
 */
interface DatabaseDeploymentResult {
	/**
	 * @return The database to be deployed.
	 */
	Curation getDbToDeploy();

	/**
	 * Mascot and Tandem short database name.
	 *
	 * @return Short database name for Mascot and Tandem only - otherwise null.
	 */
	String getShortDbName();

	/**
	 * @return Sequest .hdr file, or (in case there was no Sequest deployment done) a null.
	 */
	File getSequestHdrFile();

	/**
	 * In some cases you may not want to deploy the fasta file but here is a handle to it.
	 */
	File getFastaFile();

	/**
	 * Returns files generated with the database deployment.
	 */
	List<File> getGeneratedFiles();

	/**
	 * @return Result as sent by the database deployer.
	 */
	DeploymentResult getDeploymentResult();
}
