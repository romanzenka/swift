package edu.mayo.mprc.omssa;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ExecutableSwitching;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.enginedeployment.DeploymentResult;
import edu.mayo.mprc.enginedeployment.DeploymentService;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.*;

/**
 * This class calls formatdb (BlastTools) on a fasta file.  It is given a curation with a fasta file.  Creates or uses
 * a folder named the same as the file in the same folder, links/copies the fasta file into this folder, and finally
 * calls formatdb to create the *.fasta.p?? files associated with the index.
 * <p/>
 * It checks to make sure that a deployment has not already been performed by looking at the filename.
 */
public final class OmssaDeploymentService extends DeploymentService<DeploymentResult> {
	private static final Logger LOGGER = Logger.getLogger(OmssaDeploymentService.class);
	public static final String TYPE = "omssaDeployer";
	public static final String NAME = "Omssa DB Deployer";
	public static final String DESC = "Indexes FASTA databases for OMSSA. OMSSA requires the databases to be indexed before it can use them.";
	private File formatDbExe;

	private final List<String> indexExtensions = Arrays.asList(".phr", ".pin", ".psd", ".psi", ".psq");
	public static final String FORMATDBEXEC_LOG_FILE_NAME = "formatdb.log";
	// timeout after 5 minutes
	private static final int DEPLOYMENT_LOCK_TIMEOUT = 5 * 60 * 1000;

	public static final String FORMAT_DB_EXE = "formatDbExe";
	public static final String DEPLOYABLE_DB_FOLDER = "deployableDbFolder";

	public OmssaDeploymentService(final File formatDbExe) {
		this.formatDbExe = formatDbExe;
	}

	public File getFormatDbExe() {
		return formatDbExe;
	}

	public void setFormatDbExe(final File formatDbExe) {
		this.formatDbExe = formatDbExe;
	}

	/**
	 * gets a file that can be used for deployment.  This will make sure the file exists.  This does not perform indexing
	 * but only gets a fasta file that can be used to perform the indexing or to see if the indexing has already been performed.
	 * If on *nix this will create a link to the give curationFile from within a folder along side the curation file with the same name.
	 * If on Windows then a copy is made of the curationFile.
	 *
	 * @param curationFile the file we want to make a deployable file from.
	 * @return the file that can be used for indexing or may have already been indexed.
	 */
	private File getDeployableFile(final File curationFile) {
		final File toDeploy = getDeployedFastaFile(curationFile);

		FileUtilities.ensureFolderExists(toDeploy.getParentFile());

		if (!toDeploy.exists()) {
			FileUtilities.copyFile(curationFile, toDeploy, /*overwrite*/true);
		}

		return toDeploy;
	}

	/**
	 * Provide deployed database name for given input database.
	 *
	 * @param curationFile Input .fasta file that was/is to be deployed.
	 * @return Where should the fasta be deployed to
	 */
	private File getDeployedFastaFile(final File curationFile) {
		final File deploymentFolder = new File(
				getConfigDeploymentFolder(),
				FileUtilities.stripExtension(curationFile.getName()));

		return new File(deploymentFolder, curationFile.getName().replace("FASTA", "fasta"));
	}

	/**
	 * Determines if a given file has already been indexed by looking for .phr, .pin, .psd, .psi, and .psq files along side the deployableFile
	 * and prefixed with the fasta filename.  If all files are found then true is returned.  If none are found false is returned.  If only
	 * some are found then the index is incomplete and then all of the index files are deleted and false is returned.
	 * <p/>
	 * This will
	 *
	 * @param deployableFile
	 * @return a LockFile that must be deleted once deployment is complete.  If it was already deployed then null is returned.
	 */
	private File checkForPreviousDeployment(final File deployableFile) {

		//if we see a lock file already in place then we will wait for up to 5 minutes for an apparent already
		//running deployment to complete before checking.
		final File lockFile = new File(deployableFile.getAbsolutePath() + ".omssaDepLock");
		try {
			FileUtilities.waitOnLockFile(lockFile, DEPLOYMENT_LOCK_TIMEOUT);
		} catch (InterruptedException e) {
			LOGGER.warn(e.getMessage() + " However, assuming it is a previous error and continuing anyway.");
		}

		try {
			final File[] blastIndexFiles = deployableFile.getParentFile().listFiles(new FilenameFilter() {
				public boolean accept(final File dir, final String name) {
					if (!name.startsWith(deployableFile.getName())) {
						return false;
					}
					for (final String s : OmssaDeploymentService.this.indexExtensions) {
						if (name.endsWith(s)) {
							return true;
						}
					}
					return false;
				}
			});

			if (blastIndexFiles.length == indexExtensions.size()) {
				LOGGER.debug("OMSSA database " + deployableFile + " is already deployed.");
				return null;
			} else {
				LOGGER.debug("OMSSA database " + deployableFile + " needs to be redeployed (" + blastIndexFiles.length +
						" of " + indexExtensions.size() + " files are present).");
				//delete all if any were missing since hte index is not valid
				for (final File file : blastIndexFiles) {
					FileUtilities.quietDelete(file);
				}

				lockFile.createNewFile(); //this will create if not existent or not create quietly if it does exist.
				return lockFile;
			}

		} catch (Exception e) {
			throw new MprcException("Error determining if a deployment has already been performed.", e);
		}
	}

	public DeploymentResult performDeployment(final DeploymentRequest request) {
		LOGGER.info("Deploying OMSSA database " + request.getShortName());
		final DeploymentResult reportInto = new DeploymentResult();
		File curationFile = null;
		File lockFile = null;
		try {
			curationFile = request.getCurationFile();

			if (curationFile == null) {
				throw new IllegalArgumentException("There was not file associated with the curation " + request.getShortName());
			} else if (!curationFile.exists()) {
				throw new FileNotFoundException("Could not find a fasta file at " + curationFile.getAbsolutePath());
			}

			final File toDeploy = getDeployableFile(curationFile);

			reportInto.setDeployedFile(toDeploy);

			lockFile = checkForPreviousDeployment(toDeploy);

			if (lockFile == null) {
				reportInto.addMessage("No deployment is required.");
				reportInto.setGeneratedFiles(FileUtilities.getFilesFromFolder(toDeploy.getParentFile()));

				return reportInto;
			}

			final ProcessCaller caller = getFormatDBCaller(toDeploy);

			caller.runAndCheck("formatDB"); //this will block until complete but that is OK since this daemon is a thread.
			LOGGER.info("OMSSA database " + request.getShortName() + " is deployed");

			reportInto.setGeneratedFiles(FileUtilities.getFilesFromFolder(toDeploy.getParentFile()));

			return reportInto;

		} catch (Exception t) {
			LOGGER.error("Failure deploying from Curation:" + request.getShortName(), t);
			throw new MprcException(t);
		} finally {
			if (lockFile != null) {
				FileUtilities.quietDelete(lockFile);
			}
		}
	}

	@Override
	public DeploymentResult performUndeployment(final DeploymentRequest request) {
		final DeploymentResult reportResult = new DeploymentResult();
		final File deployedFile = getDeployedFastaFile(request.getCurationFile());

		this.cleanUpDeployedFiles(deployedFile, reportResult);

		return reportResult;
	}

	protected void validateAndDeleteDeploymentRelatedFiles(final File deployedFastaFile, final File deploymentFolder, final List<File> deletedFiles, final List<File> notDeletedFiles) {
		final File[] deploymentFiles = deploymentFolder.listFiles();

		for (final File deploymentFile : deploymentFiles) {
			if ((!deploymentFile.isDirectory()
					&& FileUtilities.getFileNameWithoutExtension(deploymentFile).equals(deployedFastaFile.getName())
					&& indexExtensions.contains("." + FileUtilities.getExtension(deploymentFile.getName())))
					|| deploymentFile.getName().equals(deployedFastaFile.getName())
					|| deploymentFile.getName().equals(FORMATDBEXEC_LOG_FILE_NAME)) {

				if (FileUtilities.deleteNow(deploymentFile)) {
					deletedFiles.add(deploymentFile);
					continue;
				}
			}

			notDeletedFiles.add(deploymentFile);
		}
	}

	protected ProcessCaller getFormatDBCaller(final File fastaFile) {
		final List<String> commandStrings = new ArrayList<String>(4);
		commandStrings.add(formatDbExe.getAbsolutePath());
		commandStrings.add("-i");
		commandStrings.add(fastaFile.getAbsolutePath());
		commandStrings.add("-o");

		return new ProcessCaller(new ProcessBuilder(commandStrings).directory(fastaFile.getParentFile()));
	}

	private static final Map<String, List<ProgressReporter>> CO_DEPLOYMENTS = new HashMap<String, List<ProgressReporter>>();

	public Map<String, List<ProgressReporter>> getCoDeployments() {
		return CO_DEPLOYMENTS;
	}

	@Override
	public void check() {
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {

		private String formatDbExe;
		private String deployableDbFolder;

		public Config() {
		}

		public Config(final String formatDbExe, final String deployableDbFolder) {
			this.formatDbExe = formatDbExe;
			this.deployableDbFolder = deployableDbFolder;
		}

		public String getDeployableDbFolder() {
			return deployableDbFolder;
		}

		public void setDeployableDbFolder(final String deployableDbFolder) {
			this.deployableDbFolder = deployableDbFolder;
		}

		public String getFormatDbExe() {
			return formatDbExe;
		}

		public void setFormatDbExe(final String formatDbExe) {
			this.formatDbExe = formatDbExe;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(FORMAT_DB_EXE, getFormatDbExe());
			writer.put(DEPLOYABLE_DB_FOLDER, getDeployableDbFolder());
		}

		@Override
		public void load(final ConfigReader reader) {
			setFormatDbExe(reader.get(FORMAT_DB_EXE));
			setDeployableDbFolder(reader.get(DEPLOYABLE_DB_FOLDER));
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("omssaDeploymentServiceFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			OmssaDeploymentService worker = null;
			try {
				worker = new OmssaDeploymentService(new File(config.getFormatDbExe()));
			} catch (Exception e) {
				throw new MprcException("Omssa deployment service worker could not be created.", e);
			}

			worker.setDeployableDbFolder(new File(config.getDeployableDbFolder()).getAbsoluteFile());

			return worker;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String WINDOWS = "bin/formatdb/windows/formatdb.exe";
		private static final String LINUX = "bin/formatdb/linux/formatdb";

		private static final String DEFAULT_DEPLOYMENT_FOLDER = "var/omssa_index";

		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(FORMAT_DB_EXE, "Database Format Executable", "Omssa deployer database format executable." +
					"<p>Swift install contains following executables for your convenience:</p>"
					+ "<table>"
					+ "<tr><td><tt>" + WINDOWS + "</tt></td><td>Windows</td></tr>"
					+ "<tr><td><tt>" + LINUX + "</tt></td><td>Linux</td></tr>"
					+ "</table>"
					+ "<br/>Executable can be downloaded from <a href=\"http://pubchem.ncbi.nlm.nih.gov/omssa/download.htm\"/>http://pubchem.ncbi.nlm.nih.gov/omssa/download.htm</a>")
					.executable(Arrays.asList("-v"))
					.required()
					.property(DEPLOYABLE_DB_FOLDER, "Database Index Folder",
							"The deployer links the .fasta file to this folder, so Omssa can create its indices around it.")
					.existingDirectory()
					.defaultValue(DEFAULT_DEPLOYMENT_FOLDER)
					.required()
					.addChangeListener(new ExecutableSwitching(resource, FORMAT_DB_EXE, WINDOWS, LINUX));
		}
	}
}
