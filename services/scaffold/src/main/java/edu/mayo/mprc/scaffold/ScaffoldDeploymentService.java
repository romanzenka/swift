package edu.mayo.mprc.scaffold;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.WorkerConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.enginedeployment.DeploymentResult;
import edu.mayo.mprc.enginedeployment.DeploymentService;
import edu.mayo.mprc.fasta.DatabaseAnnotation;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
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
public final class ScaffoldDeploymentService extends DeploymentService<DeploymentResult> {
	private static final Logger LOGGER = Logger.getLogger(ScaffoldDeploymentService.class);
	public static final String TYPE = "scaffoldDeployer";
	public static final String NAME = "Scaffold DB Deployer";
	public static final String DESC = "Indexes the FASTA database to be used by Scaffold 2. Scaffold 2 has its own indexing process.";

	private String memoryLimit = "256M";
	private String scaffoldJavaVmPath;

	private static final String SCAFFOLD_JAVA_VM_PATH = "scaffoldJavaVmPath";
	private static final String DEPLOYABLE_DB_FOLDER = "deployableDbFolder";
	private static final String INSTALL_DIR = "installDir";

	public ScaffoldDeploymentService() {
	}

	public DeploymentResult performDeployment(final DeploymentRequest request) {
		final DeploymentResult reportInto = new DeploymentResult();

		if (wasPreviouslyDeployed(request, reportInto)) {
			return reportInto;
		}

		LOGGER.debug("Asked to deploy curation " + request.getShortName());
		final File toDeploy = this.getFileToDeploy(request.getShortName());
		LOGGER.debug("Deploying file " + toDeploy.getAbsolutePath());
		if (!toDeploy.exists()) {
			LOGGER.debug("Creating file to deploy at " + toDeploy.getAbsolutePath());
			FileUtilities.copyFile(getCurationFile(request), toDeploy, /*overwrite*/false);
		}

		produceIndexFile(toDeploy, request.getAnnotation());

		LOGGER.debug("Setting deployed file to " + toDeploy.getAbsolutePath());

		reportInto.setDeployedFile(toDeploy);

		final List<File> generatedFiles = new LinkedList<File>();
		generatedFiles.add(getExistingIndex(toDeploy));
		reportInto.setGeneratedFiles(generatedFiles);

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

		final Pattern pattern = Pattern.compile(deployedFastaFile.getName() + "\\.\\d+\\.index");

		for (final File deploymentFile : deploymentFiles) {
			if (!deploymentFile.isDirectory()
					&& (pattern.matcher(deploymentFile.getName()).matches()
					|| deploymentFile.getName().equals(deployedFastaFile.getName()))) {
				if (FileUtilities.deleteNow(deploymentFile)) {
					deletedFiles.add(deploymentFile);
					continue;
				}
			}

			notDeletedFiles.add(deploymentFile);
		}
	}

	private void produceIndexFile(final File toDeploy, final DatabaseAnnotation annotation) {
		final ScaffoldArgsBuilder execution = new ScaffoldArgsBuilder(getEngineRootFolder());

		// Returns work folder for scaffold. Depending on the version, it is either the folder where the output is produced,
		// or the Scaffold install folder itself.
		final File workFolder = execution.getWorkFolder(toDeploy.getParentFile());

		final List<String> args = execution.buildScaffoldArgs(memoryLimit, execution.getScaffoldIndexerClassName());

		// Make sure the work folder is there.
		FileUtilities.ensureFolderExists(workFolder);

		final List<String> thisargs = new ArrayList<String>(args.size() + 4);
		thisargs.add(scaffoldJavaVmPath);
		for (final String arg : args) {
			thisargs.add(arg);
		}
		thisargs.add(toDeploy.getAbsolutePath());
		thisargs.add(annotation.getAccessionRegex());
		thisargs.add(annotation.getDescriptionRegex());

		final ProcessBuilder processBuilder = new ProcessBuilder(thisargs)
				.directory(workFolder);
		final ProcessCaller caller = new ProcessCaller(processBuilder);
		caller.runAndCheck("Scaffold");

		if (null == getExistingIndex(toDeploy)) {
			throw new MprcException("Index creation failed.  Could not find any created index with the fasta file: " + toDeploy.getAbsolutePath());
		}
	}

	protected File getExistingIndex(final File sisterFasta) {
		final Pattern indexPattern = Pattern.compile(Pattern.quote(sisterFasta.getName()) + "\\.[^.]+\\.index");
		final File[] deployedIndices = sisterFasta.getParentFile().listFiles(new FilenameFilter() {
			public boolean accept(final File dir, final String name) {
				return indexPattern.matcher(name).matches();
			}
		});

		if (deployedIndices.length == 0) {
			return null;
		} else {
			return deployedIndices[0];
		}
	}

	/**
	 * Finds out the path to the file that we will want to deploy to.  This does not perform the copy action but only finds
	 * out where we want to place the FASTA file.
	 */
	protected File getFileToDeploy(final String uniqueName) {
		final File deploymentFolder = getCurrentDeploymentFolder(uniqueName);
		FileUtilities.ensureFolderExists(deploymentFolder);
		return new File(deploymentFolder, uniqueName + ".fasta");
	}

	private File getCurrentDeploymentFolder(final String uniqueName) {
		return new File(getConfigDeploymentFolder(), uniqueName);
	}

	public boolean wasPreviouslyDeployed(final DeploymentRequest request, final DeploymentResult reportInto) {
		final File toCheckForPreviousDeployment = this.getFileToDeploy(request.getShortName());

		if (!toCheckForPreviousDeployment.exists()) {
			return false;
		}
		final File deployedIndex = getExistingIndex(toCheckForPreviousDeployment);

		if (null != deployedIndex) {
			reportInto.setDeployedFile(toCheckForPreviousDeployment);
			final List<File> generatedFiles = new LinkedList();
			generatedFiles.add(deployedIndex);
			reportInto.setGeneratedFiles(generatedFiles);
			LOGGER.debug("File already deployed: " + deployedIndex.getAbsolutePath());
			return true;
		}
		return false;
	}

	public String getScaffoldJavaVmPath() {
		return scaffoldJavaVmPath;
	}

	public void setScaffoldJavaVmPath(final String scaffoldJavaVmPath) {
		this.scaffoldJavaVmPath = scaffoldJavaVmPath;
	}

	public String getMemoryLimit() {
		return memoryLimit;
	}

	public void setMemoryLimit(final String memoryLimit) {
		this.memoryLimit = memoryLimit;
	}

	private static final Map<String, List<ProgressReporter>> CO_DEPLOYMENTS = new HashMap<String, List<ProgressReporter>>();

	public Map<String, List<ProgressReporter>> getCoDeployments() {
		return CO_DEPLOYMENTS;
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config extends WorkerConfig {
		private String scaffoldJavaVmPath;
		private String deployableDbFolder;
		private String installDir;

		public Config() {
		}

		public Config(final String scaffoldJavaVmPath, final String deployableDbFolder, final String installDir) {
			this.scaffoldJavaVmPath = scaffoldJavaVmPath;
			this.deployableDbFolder = deployableDbFolder;
			this.installDir = installDir;
		}

		public String getScaffoldJavaVmPath() {
			return scaffoldJavaVmPath;
		}

		public void setScaffoldJavaVmPath(final String scaffoldJavaVmPath) {
			this.scaffoldJavaVmPath = scaffoldJavaVmPath;
		}

		public String getDeployableDbFolder() {
			return deployableDbFolder;
		}

		public void setDeployableDbFolder(final String deployableDbFolder) {
			this.deployableDbFolder = deployableDbFolder;
		}

		public String getInstallDir() {
			return installDir;
		}

		public void setInstallDir(final String installDir) {
			this.installDir = installDir;
		}

		public Map<String, String> save(final DependencyResolver resolver) {
			final Map<String, String> map = new TreeMap<String, String>();
			map.put(SCAFFOLD_JAVA_VM_PATH, scaffoldJavaVmPath);
			map.put(DEPLOYABLE_DB_FOLDER, deployableDbFolder);
			map.put(INSTALL_DIR, installDir);
			return map;
		}

		public void load(final Map<String, String> values, final DependencyResolver resolver) {
			scaffoldJavaVmPath = values.get(SCAFFOLD_JAVA_VM_PATH);
			deployableDbFolder = values.get(DEPLOYABLE_DB_FOLDER);
			installDir = values.get(INSTALL_DIR);
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("scaffoldDeploymentServiceFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			ScaffoldDeploymentService worker = null;
			try {
				worker = new ScaffoldDeploymentService();
				worker.setDeployableDbFolder(new File(config.getDeployableDbFolder()).getAbsoluteFile());
				worker.setScaffoldJavaVmPath(config.getScaffoldJavaVmPath());
				worker.setEngineRootFolder(new File(config.getInstallDir()).getAbsoluteFile());

			} catch (Exception e) {
				throw new MprcException("Scaffold deployment service worker could not be created.", e);
			}
			return worker;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(INSTALL_DIR, "Installation Folder", "Scaffold installation folder").existingDirectory().required()
					.property(DEPLOYABLE_DB_FOLDER, "Database Folder", "Folder where deployer copies database files to")
					.required()
					.existingDirectory()
					.defaultValue("var/scaffold_index")

					.property(SCAFFOLD_JAVA_VM_PATH, "Java VM Path", "<tt>java</tt> executable to run Scaffold with")
					.required()
					.executable(Arrays.asList("-version"))
					.defaultValue("java");
		}
	}
}
