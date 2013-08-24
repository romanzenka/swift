package edu.mayo.mprc.enginedeployment;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.monitor.MonitorUtilities;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * This is a base class for DeploymentServices that will take a DeploymentRequest and return a DeploymentResult.
 */
public abstract class DeploymentService<T extends DeploymentResult> implements Worker {
	private static final Logger LOGGER = Logger.getLogger(DeploymentService.class);

	private String engineVersion;
	private File engineRootFolder;
	private File deployableDbFolder;

	/**
	 * Performs the work of the Daemon and calls performDeployment to do this.  Communicates the result back to the caller.
	 * This will give a shortname equal to the filename less extension if a shortname is not provided but if there is a shortname
	 * provided it will use that shortname.
	 * <p/>
	 * This method takes special care of requests that are already being processed. These are bundled with the current
	 * execution, so when the deployment finishes, all waiting processes are terminated at once.
	 *
	 * @param workPacket       - deployment request to process
	 * @param progressReporter - to report progress
	 */
	@Override
	public void processRequest(final WorkPacket workPacket, final ProgressReporter progressReporter) {
		progressReporter.reportStart(MonitorUtilities.getHostInformation());
		DeploymentRequest request = null;
		try {
			if (!(workPacket instanceof DeploymentRequest)) {
				throw new DaemonException("Unknown request: " + workPacket.toString());
			}
			request = (DeploymentRequest) workPacket;

			if (request.getCurationFile() == null) {
				throw new DaemonException("The request must contain a Curation, the curation does not need to be in the database though.");
			}

			synchronized (getCoDeployments()) {
				final List<ProgressReporter> list = getProgressReporters(request);
				if (list != null) {
					// We are already working on the particular request. Add the reporter to the list.
					LOGGER.debug("We are already working on " + (request.isUndeployment() ? "undeployment" : "deployment") + " of " + request.getShortName());
					list.add(progressReporter);
					// When we finish work, our reporter will be notified as well as the other ones
					return;
				} else {
					// First time we do particular request. Initialize a list, but and add ourselves
					addProgressReporter(request, progressReporter);
				}
			}

			T result = null;

			if (request.isUndeployment()) {
				result = performUndeployment(request);
			} else {
				result = performDeployment(request);

				if (result.getCompositeException() != null) {
					LOGGER.info("Deployment failed");
					throw new MprcException(result.getCompositeException());
				}

				result.addMessage("Deployment of " + request.getShortName() + " complete.");
				LOGGER.debug("Deployment of " + request.getShortName() + " complete.");

				//Synchronize file tokens between source and destination system.
				workPacket.synchronizeFileTokensOnReceiver();
			}

			// Report success for all other requests for same deployment
			reportCoDeploymentSuccess(request, result);

		} catch (Exception t) {
			// SWALLOWED: We never throw an exception, instead we report it
			if (request != null) {
				reportCoDeploymentFailure(request, t);
			}
		}
	}

	/**
	 * Each running request is identified by a given key, depending on whether it is undeploy or deploy request.
	 *
	 * @param request
	 * @return co-deployment key for table of co-deployments.
	 */
	private String getUniqueCoDeploymentKey(final DeploymentRequest request) {
		return request.isUndeployment() ? "Undeployment " + request.getShortName() : "Deployment " + request.getShortName();
	}

	/**
	 * @return Map of co-deployments. This map is static and separate for each deployer. Always return the same map.
	 */
	public abstract Map<String, List<ProgressReporter>> getCoDeployments();

	/**
	 * Return all reporters waiting for a given request to be finished.
	 */
	private List<ProgressReporter> getProgressReporters(final DeploymentRequest request) {
		return getCoDeployments().get(getUniqueCoDeploymentKey(request));
	}

	/**
	 * Add a new request with a reporter.
	 *
	 * @param request  Request
	 * @param reporter Reporter to set up of the request.
	 */
	private void addProgressReporter(final DeploymentRequest request, final ProgressReporter reporter) {
		final List<ProgressReporter> list = new ArrayList<ProgressReporter>();
		list.add(reporter);
		getCoDeployments().put(getUniqueCoDeploymentKey(request), list);
	}

	private void removeProgressReporters(final DeploymentRequest request) {
		LOGGER.debug("Removing deployment from queue: " + request.getShortName());
		getCoDeployments().remove(getUniqueCoDeploymentKey(request));
	}

	private void reportCoDeploymentFailure(final DeploymentRequest request, final Throwable t) {
		synchronized (getCoDeployments()) {
			try {
				final List<ProgressReporter> reporters = getProgressReporters(request);
				if (reporters == null) {
					LOGGER.warn("No reporter list associated with " + request.getShortName() + ", could not report failure", t);
					return;
				}
				for (final ProgressReporter r : reporters) {
					r.reportFailure(t);
				}
			} finally {
				removeProgressReporters(request);
			}
		}
	}

	private void reportCoDeploymentSuccess(final DeploymentRequest request, final T result) {
		synchronized (getCoDeployments()) {
			try {
				final List<ProgressReporter> reporters = getProgressReporters(request);
				if (reporters == null) {
					LOGGER.warn("No reporters associated with " + request.getShortName());
					return;
				}
				for (final ProgressReporter r : reporters) {
					r.reportProgress(result);
					r.reportSuccess();
				}
			} finally {
				removeProgressReporters(request);
			}
		}
	}

	/**
	 * Gets the file that we will deploy to the search engine.  This may require a copy of the file depending on settings since
	 * we may allow a file to be deployed from its original place.
	 *
	 * @param request the request that has the information we need for deployment including curation file path
	 * @return the File that should be deployed.
	 */
	protected File getCurationFile(final DeploymentRequest request) {
		if (request.getCurationFile() == null) {
			throw new MprcException("Could not find a curation with this shortname or uniquename that has been run.");
		}

		final File file = request.getCurationFile();
		if (file == null) {
			throw new MprcException("The curation specified hasn't been run for some reason.");//shouldn't happen
		}

		if (!file.exists()) {
			throw new MprcException("The file did not exist where we were looking for it.\n" + file.getAbsolutePath());
		}

		return file;
	}

	/**
	 * gets the folder that we should copy the file to.
	 *
	 * @return the File object represening the folder where we should deploy files from.
	 */
	protected File getConfigDeploymentFolder() {
		final File engineDeploymentFolder = deployableDbFolder;

		//if the folders are the same meaning they were explicity pointed to the same location
		if (engineDeploymentFolder.exists()) {
			return engineDeploymentFolder;
		} else {
			throw new MprcException("The deployment folder " + engineDeploymentFolder.getAbsolutePath() + " does not exist");
		}
	}

	/**
	 * Cleans up deployment files that are related to the give deployed file.
	 *
	 * @param deployedFile
	 * @param reportResult
	 */
	protected void cleanUpDeployedFiles(final File deployedFile, final DeploymentResult reportResult) {
		if (deployedFile.exists()) {
			LOGGER.info("Deleting deployment files related to file: " + deployedFile.getAbsolutePath());
			deleteDeployedFiles(deployedFile, reportResult);
		} else {
			LOGGER.info("Failed to delete deployment files related to file: " + deployedFile.getAbsolutePath());
			LOGGER.info("File: " + deployedFile.getAbsolutePath() + " does not exist.");
		}
	}

	/**
	 * Deletes given deployed fasta file and all related files.
	 *
	 * @param deployedFastaFile
	 * @param reportResult
	 */
	protected void deleteDeployedFiles(final File deployedFastaFile, final DeploymentResult reportResult) {
		if (deployedFastaFile.exists()) {
			final File deploymentFolder = deployedFastaFile.getParentFile();

			final List<File> notDeletedFiles = new LinkedList<File>();
			final List<File> deletedFiles = new LinkedList<File>();

			validateAndDeleteDeploymentRelatedFiles(deployedFastaFile, deploymentFolder, deletedFiles, notDeletedFiles);

			validateUndeploymentResults(reportResult, deletedFiles, notDeletedFiles, deploymentFolder);
		}
	}

	/**
	 * Validates deployment related files and deletes related files and adds deleted files to given deletedFiles list.
	 * Not related files are not deleted and added to notDeletedFiles list.
	 *
	 * @param deployedFastaFile
	 * @param deploymentFolder
	 * @param deletedFiles
	 * @param notDeletedFiles
	 */
	protected void validateAndDeleteDeploymentRelatedFiles(final File deployedFastaFile, final File deploymentFolder, final List<File> deletedFiles, final List<File> notDeletedFiles) {
		FileUtilities.deleteNow(deployedFastaFile);
		deletedFiles.add(deployedFastaFile);
	}

	/**
	 * Validates results given all paramaters and add report messages to report result object.
	 *
	 * @param reportResult
	 * @param deletedFiles
	 * @param notDeletedFiles
	 */
	protected void validateUndeploymentResults(final DeploymentResult reportResult, final List<File> deletedFiles, final List<File> notDeletedFiles, final File deploymentFolder) {

		final StringBuilder notDeleted = new StringBuilder();
		final StringBuilder deleted = new StringBuilder("Deleted files:\n");

		for (final File file : deletedFiles) {
			deleted.append("\t").append((file.isDirectory() ? "Directory: " : "File: "))
					.append(file.getAbsolutePath())
					.append("\n");
		}

		for (final File file : notDeletedFiles) {
			notDeleted.append("\t").append((file.isDirectory() ? "Directory: " : "File: "))
					.append(file.getAbsolutePath())
					.append("\n");
		}

		if (!notDeletedFiles.isEmpty()) {
			reportResult.addMessage(deleted.toString());

			reportResult.setUndeployedFiles(deletedFiles);
			reportResult.addMessage("There were more files than expected in deployment folder, or some files failed to be deleted. The following files were not removed:\n"
					+ notDeleted.toString());
		} else {
			FileUtilities.deleteNow(deploymentFolder);
			reportResult.setUndeployedFiles(Arrays.asList(deploymentFolder));
		}
	}

	public String getEngineVersion() {
		return engineVersion;
	}

	public void setEngineVersion(final String engineVersion) {
		this.engineVersion = engineVersion;
	}

	public File getEngineRootFolder() {
		return engineRootFolder;
	}

	public void setEngineRootFolder(final File engineRootFolder) {
		this.engineRootFolder = engineRootFolder;
	}

	public File getDeployableDbFolder() {
		return deployableDbFolder;
	}

	public void setDeployableDbFolder(final File deployableDbFolder) {
		this.deployableDbFolder = deployableDbFolder;
	}

	/**
	 * Does a number of steps that will deploy the database.
	 *
	 * @param request the deployment request that we want to perform
	 */
	public abstract T performDeployment(DeploymentRequest request);

	/**
	 * Undeploys the database from search engine.
	 *
	 * @param request the undeployment request that we want to perform
	 */
	public abstract T performUndeployment(DeploymentRequest request);
}
