package edu.mayo.mprc.scaffold3;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.enginedeployment.DeploymentResult;
import edu.mayo.mprc.enginedeployment.DeploymentService;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Deploys a Scaffold database - a two step process:
 * <ul>
 * <li>Deploy the .fasta file</li>
 * <li>Index the .fasta file</li>
 * </ul>
 * Indexing means running Scaffold's
 * <code>com.proteomesoftware.scaffold.DatabaseIndexer <fasta file> [<db accession regex>] [<db description regex>]</code>
 * The db accession and db description regex's are the same as the ones we use in the .scafml file.
 */
public final class Scaffold3DeploymentService extends DeploymentService<DeploymentResult> {
	private static final Logger LOGGER = Logger.getLogger(Scaffold3DeploymentService.class);
	public static final String TYPE = "scaffold3Deployer";
	public static final String NAME = "Scaffold 3 DB Deployer";

	private static final String DEPLOYABLE_DB_FOLDER = "deployableDbFolder";
	public static final String DESC = "Copies the FASTA database to a separate folder. Scaffold 3 has its own indexing process that creates the index next to the FASTA file.";

	public Scaffold3DeploymentService() {
	}

	public DeploymentResult performDeployment(final DeploymentRequest request) {
		final DeploymentResult reportInto = new DeploymentResult();

		if (wasPreviouslyDeployed(request, reportInto)) {
			return reportInto;
		}

		LOGGER.debug("Asked to deploy curation " + request.getShortName());
		final File deployFrom = getCurationFile(request);
		final File deployTo = this.getFileToDeploy(request.getShortName());

		LOGGER.debug("Deploying file " + deployTo.getAbsolutePath());

		FileUtilities.ensureFolderExists(deployTo.getParentFile());
		FileUtilities.copyFile(deployFrom, deployTo, /*overwrite*/true);

		LOGGER.debug("Setting deployed file to " + deployTo.getAbsolutePath());

		reportInto.setDeployedFile(deployTo);

		return reportInto;
	}

	@Override
	public DeploymentResult performUndeployment(final DeploymentRequest request) {
		final DeploymentResult reportResult = new DeploymentResult();
		final File deployedFile = this.getFileToDeploy(request.getShortName());

		this.cleanUpDeployedFiles(deployedFile, reportResult);

		return reportResult;
	}

	@Override
	protected void validateAndDeleteDeploymentRelatedFiles(final File deployedFastaFile, final File deploymentFolder, final List<File> deletedFiles, final List<File> notDeletedFiles) {
		final File[] deploymentFiles = deploymentFolder.listFiles();

		final Pattern pattern = Pattern.compile(deployedFastaFile.getName() + "\\.\\[0-9.]+\\.index");

		for (final File deploymentFile : deploymentFiles) {
			if (isDeploymentRelatedFile(deployedFastaFile, pattern, deploymentFile) && FileUtilities.deleteNow(deploymentFile)) {
				deletedFiles.add(deploymentFile);
				continue;
			}

			notDeletedFiles.add(deploymentFile);
		}
	}

	private boolean isDeploymentRelatedFile(final File deployedFastaFile, final Pattern pattern, final File deploymentFile) {
		return !deploymentFile.isDirectory()
				&& (pattern.matcher(deploymentFile.getName()).matches()
				|| deploymentFile.getName().equals(deployedFastaFile.getName()));
	}

	/**
	 * Finds out the path to the file that we will want to deploy to.  This does not perform the copy action but only finds
	 * out where we want to place the FASTA file.
	 */
	protected File getFileToDeploy(final String uniqueName) {
		return new File(getCurrentDeploymentFolder(uniqueName), uniqueName + ".fasta");
	}

	private File getCurrentDeploymentFolder(final String uniqueName) {
		return new File(getConfigDeploymentFolder(), uniqueName);
	}

	public boolean wasPreviouslyDeployed(final DeploymentRequest request, final DeploymentResult reportInto) {
		final File deployFrom = getCurationFile(request);
		final File deployTo = getFileToDeploy(request.getShortName());

		// The target either does not exist or it is older than the source
		if (!deployTo.exists() || deployTo.lastModified() < deployFrom.lastModified()) {
			return false;
		}

		reportInto.setDeployedFile(deployTo);
		LOGGER.debug("File already deployed: " + deployTo.getAbsolutePath());
		return true;
	}

	private static final Map<String, List<ProgressReporter>> CO_DEPLOYMENTS = new HashMap<String, List<ProgressReporter>>();

	public Map<String, List<ProgressReporter>> getCoDeployments() {
		return CO_DEPLOYMENTS;
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private String deployableDbFolder;

		public Config() {
		}

		public Config(final String deployableDbFolder) {
			this.deployableDbFolder = deployableDbFolder;
		}

		public String getDeployableDbFolder() {
			return deployableDbFolder;
		}

		public void setDeployableDbFolder(final String deployableDbFolder) {
			this.deployableDbFolder = deployableDbFolder;
		}

		public Map<String, String> save(final DependencyResolver resolver) {
			final Map<String, String> map = new TreeMap<String, String>();
			map.put(DEPLOYABLE_DB_FOLDER, deployableDbFolder);
			return map;
		}

		public void load(final Map<String, String> values, final DependencyResolver resolver) {
			deployableDbFolder = values.get(DEPLOYABLE_DB_FOLDER);
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("scaffold3DeploymentServiceFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			Scaffold3DeploymentService worker = null;
			try {
				worker = new Scaffold3DeploymentService();
				worker.setDeployableDbFolder(new File(config.getDeployableDbFolder()).getAbsoluteFile());

			} catch (Exception e) {
				throw new MprcException("Scaffold deployment service worker could not be created.", e);
			}
			return worker;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(DEPLOYABLE_DB_FOLDER, "Database Folder", "Folder where deployer copies database files to")
					.required()
					.existingDirectory()
					.defaultValue("var/scaffold3_index");
		}
	}
}
