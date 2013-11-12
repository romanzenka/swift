package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.enginedeployment.DeploymentResult;
import edu.mayo.mprc.sequest.SequestDeploymentResult;
import edu.mayo.mprc.sequest.SequestDeploymentService;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.List;

final class DatabaseDeployment extends AsyncTaskBase implements DatabaseDeploymentResult {
	private static final Logger LOGGER = Logger.getLogger(DatabaseDeployment.class);

	private final String engineCode;
	private final String engineFriendlyName;
	private final Curation dbToDeploy;
	private final File paramsFile;

	private File hdrFile;
	private File fastaFile;
	private List<File> generatedFiles;
	private DeploymentResult deploymentResult;

	DatabaseDeployment(final WorkflowEngine engine, final String engineCode, final String engineFriendlyName, final DaemonConnection deploymentDaemon, final File paramsFile, final Curation dbToDeploy, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(engine, deploymentDaemon, fileTokenFactory, fromScratch);
		this.engineCode = engineCode;
		this.engineFriendlyName = engineFriendlyName;
		this.paramsFile = paramsFile;
		this.dbToDeploy = dbToDeploy;
		setName("DbDeploy " + engineFriendlyName);
		if (paramsFile != null) {
			setDescription(engineFriendlyName +
					" db deployment of " + this.dbToDeploy.getShortName() +
					", Params file: " +
					fileTokenFactory.fileToTaggedDatabaseToken(paramsFile));
		} else {
			setDescription(engineFriendlyName + " db deployment of " + this.dbToDeploy.getShortName());
		}
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 *         to send a work packet.
	 */
	@Override
	public WorkPacket createWorkPacket() {
		final DeploymentRequest workPacket = new DeploymentRequest(getFullId(), dbToDeploy.getFastaFile());
		if ("SEQUEST".equalsIgnoreCase(engineCode)) {
			// Sequest needs the param file path as a parameter
			workPacket.addProperty(
					SequestDeploymentService.SEQUEST_PARAMS_FILE,
					paramsFile);
		}
		return workPacket;
	}

	@Override
	public synchronized void onSuccess() {
	}

	@Override
	public synchronized void onProgress(final ProgressInfo progressInfo) {
		// The deployer sends deployment result as progress message
		if (progressInfo instanceof DeploymentResult) {
			deploymentResult = (DeploymentResult) progressInfo;
			fastaFile = deploymentResult.getDeployedFile();
			generatedFiles = deploymentResult.getGeneratedFiles();
			LOGGER.debug("Deployment received. Deployed fasta file: " + fastaFile + " for " + engineFriendlyName);
		}
		if (progressInfo instanceof SequestDeploymentResult) {
			final SequestDeploymentResult result = (SequestDeploymentResult) progressInfo;
			fastaFile = result.getDeployedFile();
			hdrFile = result.getFileToSearchAgainst();
			generatedFiles = result.getGeneratedFiles();
			deploymentResult = result;
		}
	}

	@Override
	public String getShortDbName() {
		return dbToDeploy.getShortName();
	}

	//this may not be an hdrFile but actually a fasta file

	@Override
	public synchronized File getSequestHdrFile() {
		return hdrFile;
	}

	/**
	 * @return Deployed fasta file.
	 */
	@Override
	public synchronized File getFastaFile() {
		return fastaFile;
	}

	@Override
	public synchronized List<File> getGeneratedFiles() {
		return generatedFiles;
	}

	@Override
	public DeploymentResult getDeploymentResult() {
		return deploymentResult;
	}
}
