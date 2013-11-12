package edu.mayo.mprc.sequest;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ResourceConfigBase;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.enginedeployment.DeploymentService;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.GZipUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;

/**
 * This is the daemon that deploys a sequest indexed database using makedb.exe through wine.
 */
public final class SequestDeploymentService extends DeploymentService<SequestDeploymentResult> {
	private static final Logger LOGGER = Logger.getLogger(SequestDeploymentService.class);

	public static final int MAX_SEQUEST_INDEX_LENGTH = 20;
	public static final String SEQUEST_PARAMS_FILE = "sequest.params_file";

	private static final String DEPLOYABLE_DB_FOLDER = "deployableDbFolder";
	private static final String ENGINE_ROOT_FOLDER = "engineRootFolder";
	private static final String WINE_WRAPPER_SCRIPT = "wineWrapperScript";
	public static final String TYPE = "sequestDeployer";
	public static final String NAME = "Sequest DB Deployer";
	public static final String DESC = "Indexes FASTA databases for Sequest. You need this to run Sequest efficiently - on non-indexed databases the performance suffers.";

	private File makeDBExe;
	private File sortExe;
	private File cmdExe;

	private String wineWrapperScript;

	private SequestMappingFactory sequestMappingFactory;

	private SequestToMakeDBConverter converter;

	public SequestDeploymentService() {
	}

	public String getWineWrapperScript() {
		return wineWrapperScript;
	}

	public void setWineWrapperScript(final String wineWrapperScript) {
		this.wineWrapperScript = wineWrapperScript;
	}

	public boolean isUseWine() {
		return wineWrapperScript != null && !wineWrapperScript.isEmpty();
	}

	public SequestMappingFactory getSequestMappingFactory() {
		return sequestMappingFactory;
	}

	public void setSequestMappingFactory(final SequestMappingFactory sequestMappingFactory) {
		this.sequestMappingFactory = sequestMappingFactory;
	}

	@Override
	public void setEngineRootFolder(final File engineRootFolder) {
		super.setEngineRootFolder(engineRootFolder);
		makeDBExe = new File(getEngineRootFolder(), "makedb4.exe");
		sortExe = new File(getEngineRootFolder(), "sort.exe");
		cmdExe = new File(getEngineRootFolder(), "cmd.exe");
	}

	/**
	 * does a number of steps that will deploy the database to Sequest.  It is assumed that Sequest is installed on the
	 * system where this process is running.
	 * <p/>
	 * Given:
	 * <dl>
	 * <dt>ParamSet name</dt>
	 * <dd>Orbitrap_SprotRev_Latest_CabC_OxM</dd>
	 * <dt>Deployment folder where database indices should reside finally.</dt>
	 * <dd>dbcurator/SprotRev_20071105/</dd>
	 * <dt>Fasta file, located in the deployment folder.</dt>
	 * <dd>dbcurator/SprotRev_20071105/SprotRev_20071105.fasta</dd>
	 * <dt>makedb.params, copied into temporary folder.</dt>
	 * <dd>shared/Test_2007111501/params/makedb.params</dd>
	 * </dl>
	 * <p/>
	 * This method creates a directory tree:
	 * <ul>
	 * <li>dbcurator/SprotRev_20071105/SprotRev_20071105_Orbitrap_SprotRev_Latest_CabC_OxM.makedb.params</li>
	 * <li>temporary directory: tmp&lt;timestamp&gt;</li>
	 * <li>dbcurator/SprotRev_20071105/tmp&lt;timestamp&gt;/makedb.params -> ../SprotRev_20071105_Orbitrap_SprotRev_Latest_CabC_OxM.makedb.params</li>
	 * <li>sort and cmd executables, eg:<br/>
	 * dbcurator/SprotRev_20071105/tmp/sort.exe -> .../</li>
	 * </ul>
	 * <p/>
	 * It invokes makedb.exe like:
	 * <ul>
	 * <li>wine ${TERMO_TOP}/makedb4.exe -D${db}.fasta -O${uniqueName}_${paramsName}.hdr</li>
	 * </ul>
	 * and parses its output.
	 * <p/>
	 * Outputs:
	 * <ul>
	 * <li>HDR, DGT, IDX files (collectively the sequest index); atomically rename to ../&lt;X&gt;.hdr, etc.</li>
	 * <li>dbcurator/SprotRev_20071105/SprotRev_20071105_Orbitrap_SprotRev_Latest_CabC_OxM.hdr, etc.</li>
	 * <li>makedb.params</li>
	 * <li>dbcurator/SprotRev_20071105/SprotRev_20071105_Orbitrap_SprotRev_Latest_CabC_OxM.makedb.params</li>
	 * </ul>
	 *
	 * @param request the deployment request that we want to perform
	 */
	@Override
	public SequestDeploymentResult performDeployment(final DeploymentRequest request) {
		final SequestDeploymentResult reportInto = new SequestDeploymentResult();

		if (isNoDeploymentNecessary(request, reportInto)) {
			reportInto.setDeployedFile(request.getCurationFile());
			return reportInto;
		}

		//key files generated as a result of the deployment.  The caller may be interested in these.
		final List<File> generatedFiles = new ArrayList<File>();
		//a directory for holding temporary files while the deployment is happending this will be cleaned up at the end
		final File tempExecFolder = getExecutionFolder(request);
		//the directory where the deployment will reside at the end
		final File deploymentDir = getCurrentDeploymentFolder(request);
		//the name that should be used to represent the curation and parameter set combination

		File decompressedFile = null;
		try {

			FileUtilities.ensureFolderExists(tempExecFolder);

			//1.  setup the deployment folder
			final File deploymentFolder = getCurrentDeploymentFolder(request);
			FileUtilities.ensureFolderExists(deploymentFolder);
			generatedFiles.add(deploymentFolder);

			//2.  map the fasta file to the deployment folder
			final File fastaFile = super.getCurationFile(request);
			final File movedFasta = getDeployedFastaFile(request);

			// TODO: Look into this, simplify, fix
			FileUtilities.linkOrCopy(fastaFile, movedFasta, true, true);
			File toDeploy = movedFasta;
			reportInto.setDeployedFile(movedFasta);
			if (GZipUtilities.isGZipped(toDeploy)) {
				final File decompDest = new File(toDeploy.getAbsolutePath() + "_decomp");
				GZipUtilities.decompressFile(toDeploy, decompDest);
				toDeploy = decompDest;
				decompressedFile = decompDest;
			}

			generatedFiles.add(movedFasta);

			//3.  create a makedb.params in the same folder using the sequest.params file
			final File mkdbFile = new File(tempExecFolder, tempExecFolder.getName() + ".makedb.params");

			final File sequestParamsFile = (File) request.getProperty(SEQUEST_PARAMS_FILE);
			generateMakeDbFile(sequestParamsFile, toDeploy, mkdbFile);

			if (!mkdbFile.exists()) {
				throw new MprcException("makedb parameter file " + mkdbFile.getAbsolutePath() + " is missing.");
			}

			//4.  link or copy the cmd.exe and sort.exe in the deploymend folder
			LOGGER.debug("Linking the executable files needed to run makedb:\n"
					+ "\t" + cmdExe.getAbsolutePath() + "\n"
					+ "\t" + sortExe.getAbsolutePath() + "\n");
			FileUtilities.linkOrCopy(cmdExe, new File(tempExecFolder, "cmd.exe"), /*allowOverwrite*/false, /*symbolic*/true);
			FileUtilities.linkOrCopy(sortExe, new File(tempExecFolder, "sort.exe"), false, /*symbolic*/true);
			FileUtilities.copyFile(mkdbFile, new File(tempExecFolder, "makedb.params"), true);

			//5.  execute makedb4.exe passing in the fasta file and
			final File hdrFile = new File(tempExecFolder, tempExecFolder.getName() + ".fasta.hdr");
			final List<String> cmd = new ArrayList<String>();

			if (isUseWine()) {
				cmd.add(wineWrapperScript);
			}
			cmd.add(makeDBExe.getAbsolutePath());
			cmd.add("-D" + toDeploy.getAbsolutePath());
			cmd.add("-O" + hdrFile.getAbsolutePath());
			final ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.directory(tempExecFolder);

			final ProcessCaller caller = new ProcessCaller(pb);
			caller.runAndCheck("Sequest database indexing (makedb)");

			//6.  Ensure .fasta.hdr was indeed produced
			if (!hdrFile.exists()) {
				throw new MprcException("HDR file " + hdrFile.getAbsolutePath() + " was not produced.");
			} else if (hdrFile.length() == 0) {
				throw new MprcException("HDR file " + hdrFile.getAbsolutePath() + " has zero length.");
			}
			final File newHdr = new File(deploymentDir, hdrFile.getName());
			FileUtilities.rename(hdrFile, newHdr); //move it to the permanent location
			reportInto.setFileToSearchAgainst(newHdr);
			generatedFiles.add(newHdr);

			//7.  Ensure the .dgt file produced as larger than the original fasta file
			final File dgtFile = new File(tempExecFolder, tempExecFolder.getName() + ".fasta.dgt");
			if (!dgtFile.exists()) {
				throw new MprcException("The digest file " + dgtFile.getAbsolutePath() + " that was to be created by makedb.exe does not exist.");
			}
			if (dgtFile.length() < fastaFile.length()) {
				throw new MprcException("The digest file " + dgtFile.getAbsolutePath() + " is shorter than corresponding fasta file " + fastaFile.getAbsolutePath() + " (" + dgtFile.length() + " < " + fastaFile.length() + ")");
			}

			//8.  Move all of the desired generated files to the directory where they will stay
			final File newDgt = new File(deploymentDir, dgtFile.getName());
			FileUtilities.rename(dgtFile, newDgt);
			generatedFiles.add(newDgt);

			final File logFile = new File(tempExecFolder, "makedb.log");
			final File newLog = new File(deploymentDir, tempExecFolder.getName() + ".makedb.log");
			FileUtilities.rename(logFile, newLog);
			generatedFiles.add(newLog);

			final File idxFile = new File(tempExecFolder, tempExecFolder.getName() + ".fasta.idx");
			final File newIdx = new File(deploymentDir, idxFile.getName());
			FileUtilities.rename(idxFile, newIdx);
			generatedFiles.add(newIdx);

			//move makedb file up one level
			final File mkdbUpOne = new File(deploymentDir, mkdbFile.getName());
			FileUtilities.rename(mkdbFile, mkdbUpOne);
			generatedFiles.add(mkdbUpOne);

			final File infoFile = new File(FileUtilities.stripExtension(FileUtilities.stripExtension(newIdx.getAbsolutePath())) + ".info");
			final StringBuilder info = new StringBuilder(200);

			info.append("Paramset used: ").append(getParamSetName(request)).append("\n").append("\n")
					.append("database name: ").append(request.getShortName())
					.append("database description: ").append(request.getTitle())
					.append("\n");

			FileUtilities.writeStringToFile(infoFile, info.toString(), true);
			generatedFiles.add(infoFile);

			reportInto.setGeneratedFiles(generatedFiles);

		} catch (IOException e) {
			throw new MprcException("Some trouble with all of the file transfers into the deployment folder.", e);
		} finally {
			if (decompressedFile != null) {
				FileUtilities.quietDelete((File) decompressedFile);
			}

			FileUtilities.deleteNow(tempExecFolder);
		}

		return reportInto;
	}

	@Override
	public SequestDeploymentResult performUndeployment(final DeploymentRequest request) {
		final SequestDeploymentResult reportResult = new SequestDeploymentResult();

		final File deployedFile = getDeployedFastaFile(request);

		cleanUpDeployedFiles(deployedFile, reportResult);

		return reportResult;
	}

	@Override
	protected void validateAndDeleteDeploymentRelatedFiles(final File deployedFastaFile, final File deploymentFolder, final List<File> deletedFiles, final List<File> notDeletedFiles) {
		if (FileUtilities.deleteNow(deploymentFolder)) {
			deletedFiles.add(deploymentFolder);
		} else {
			notDeletedFiles.add(deploymentFolder);
		}
	}


	public SequestToMakeDBConverter getConverter() {
		return converter;
	}

	public void setConverter(final SequestToMakeDBConverter converter) {
		this.converter = converter;
	}

	private File getDeployedFastaFile(final DeploymentRequest request) {
		return new File(getCurrentDeploymentFolder(request), request.getShortName() + ".fasta");
	}

	/**
	 * Create a make db.params file from the sequest.params file
	 *
	 * @param sequestFile the sequest.params file to convert
	 * @param fastaFile   a fasta file we want to use in the makedb.params file
	 * @param makeDbFile  the path to the makedb.params file
	 */
	private void generateMakeDbFile(final File sequestFile, final File fastaFile, final File makeDbFile) {
		try {
			converter.writeMakedbParams(
					converter.convertSequestParamsFileIntoMakeDBPIC(sequestFile, fastaFile, sequestMappingFactory).toString(),
					makeDbFile);
		} catch (IOException e) {
			throw new MprcException("Could not generate the makedb.params file", e);
		}
	}

	/**
	 * This will go through and find any hdr files that match the given request.  If so then we will return true after
	 * adding the hdr and other pertinent information to the reportInto variable
	 * <p/>
	 * If the sequest params file indicates that it is a non-specific search then true will be returned and the fasta file
	 * will just be passed back to the caller.
	 *
	 * @param request    the request we want to see if there is a previous deployment that matches.
	 * @param reportInto a result we can add any previous matching deployments to.
	 * @return true if there is an existing deployment that maches
	 */
	public boolean isNoDeploymentNecessary(final DeploymentRequest request, final SequestDeploymentResult reportInto) {
		final File sequestParams = (File) request.getProperty(SEQUEST_PARAMS_FILE);
		if (specifiesNoEnzyme(sequestParams)) {
			LOGGER.debug("It has been determined that the params file indicates a non-specific enzyme search so skipping deployment.  " +
					"Search should just be performed against the raw fasta file.");
			reportInto.setFileToSearchAgainst(request.getCurationFile());
			return true;
		}

		final File fastaFile = getDeployedFastaFile(request);

		try {
			final String want = converter.convertSequestParamsFileIntoMakeDBPIC(sequestParams, fastaFile, sequestMappingFactory).toString();
			final File depdir = getCurrentDeploymentFolder(request);

			if (depdir.exists()) {

				final FilenameFilter makedbFileFilter = new FilenameFilter() {
					@Override
					public boolean accept(final File dir, final String name) {
						return name.endsWith("makedb.params");
					}
				};

				//look for all makedb.params files in this folder and if one would seem to create the same as the given params file would
				//then we want to find the hdr file that was created from that makedb.params file this will be a hdr file with the same name
				for (final File file : depdir.listFiles(makedbFileFilter)) {
					final String found = Files.toString(file, Charsets.US_ASCII);
					if (found.equals(want)) {
						final String commonName = file.getAbsolutePath().replace(".makedb.params", "");
						final File hdr = new File(commonName + ".fasta.hdr");
						if (hdr.exists()) {
							reportInto.setFileToSearchAgainst(hdr);
							reportInto.addMessage("No deployment necessary, it was previously deployed.");
							return true;
						} else {
							throw new FileNotFoundException("A makedb file was found but we could not find an associated .fasta.hdr file: " + file.getAbsolutePath());
						}
					}
				}
			}
			return false;
		} catch (Exception t) {
			throw new MprcException(t);
		}
	}

	protected String getParamSetName(final DeploymentRequest request) {
		final File originalSequestParamsFile;
		final Object givenProperty = request.getProperty(SEQUEST_PARAMS_FILE);
		if (givenProperty != null) {
			originalSequestParamsFile = (File) givenProperty;
		} else {
			throw new MprcException("Could not find the key 'sequestParamsFile' which signifies a lack of a params file being given.");
		}

		return originalSequestParamsFile.getParentFile().getName();
	}

	/**
	 * This finds a folder that we can use for executing.  The folder will have the name that should be used as a the unique prefix name
	 * for the files that are generated as well.  This should really only be called once but just in case it will store the
	 * execution file it returns on the request and check there first to make sure.
	 *
	 * @param request a request that contains a unique name
	 * @return an existing folder where we can execute the deployment in
	 */
	protected synchronized File getExecutionFolder(final DeploymentRequest request) {
		File executionFolder = (File) request.getProperty("executionFolder");
		if (executionFolder != null) {
			return executionFolder;
		}

		String compositeName = request.getShortName() + "_" + getParamSetName(request);
		if (compositeName.length() > MAX_SEQUEST_INDEX_LENGTH - 2) {
			compositeName = compositeName.substring(0, MAX_SEQUEST_INDEX_LENGTH - 2);
		}

		char incrementer = 'A';
		do {
			executionFolder = new File(getCurrentDeploymentFolder(request), compositeName + "_" + incrementer++);
		}//we need to check for execution folder existence before checking for previous deployment result files to make sure that we are not currently moving files.
		while (executionFolder.exists() || !checkForPreviousValidDeployment(getCurrentDeploymentFolder(request), executionFolder.getName()));

		request.addProperty("executionFolder", executionFolder);

		LOGGER.debug("Creating execution folder: " + executionFolder.getAbsolutePath());
		try {
			FileUtilities.ensureFolderExists(executionFolder);
		} catch (Exception t) {
			throw new MprcException("Could not create an execution folder for Sequest deployment at " + executionFolder.getAbsolutePath(), t);
		}


		return executionFolder;
	}

	/**
	 * this will take a folder and a prefix for the prefix for all key sequest files to look for in the given folder.
	 * <p/>
	 * If all neccisary files exist then false will be returned indicating that we should not use that name
	 * <p/>
	 * If not all files exists then any that do will be deleted.  If any could not be deleted then none will.  If they
	 * could be deleted then true will be returned meaning that the prefix is now available.  If they could not be deleted
	 * then false is returned indicating that the prefix should not be used.
	 *
	 * @param deploymentFolder the folder to search for files in
	 * @param prefix           the prefix for the filenames which should be unique for a sequest deployment
	 * @return true if the prefix is available for use else false
	 */
	protected synchronized boolean checkForPreviousValidDeployment(final File deploymentFolder, final String prefix) {
		final Set<File> necessaryFiles = new HashSet<File>();
		necessaryFiles.add(new File(deploymentFolder, prefix + ".fasta.dgt"));
		necessaryFiles.add(new File(deploymentFolder, prefix + ".fasta.hdr"));
		necessaryFiles.add(new File(deploymentFolder, prefix + ".fasta.idx"));
		necessaryFiles.add(new File(deploymentFolder, prefix + ".makedb.log"));
		necessaryFiles.add(new File(deploymentFolder, prefix + ".makedb.params"));

		final Set<File> existingFiles = new HashSet<File>();
		for (final File necessary : necessaryFiles) {
			if (necessary.exists()) {
				existingFiles.add(necessary);
			}
		}

		//if none of the files existed then return true
		if (existingFiles.isEmpty()) {
			return true;
		} else if (existingFiles.size() == necessaryFiles.size()) {
			return false;
		} else if (existingFiles.size() < necessaryFiles.size()) {
			//if we can't delete all of the files then we won't delete all and we will just return false
			for (final File exisiting : existingFiles) {
				if (!exisiting.canWrite()) {
					return false;
				}
			}
			//if we can delete all then do it
			for (final File existing : existingFiles) {
				LOGGER.info("Deleting file: " + existing.getAbsolutePath());
				FileUtilities.quietDelete(existing);
			}
			return true;
		} else {
			return false;
		}
	}

	boolean specifiesNoEnzyme(final File sequestParamsFile) {
		InputStream is = null;
		try {
			is = new FileInputStream(sequestParamsFile);
			//co opting the the properties functionality nafariously
			final Properties p = new Properties();
			p.load(is);
			return (p.getProperty("enzyme_info").equals("Non-Specific 0 0 - -"));
		} catch (FileNotFoundException e) {
			throw new MprcException("Could not find the params file specified at " + sequestParamsFile.getAbsolutePath(), e);
		} catch (IOException e) {
			throw new MprcException("Could not read the sequest params file specified at " + sequestParamsFile.getAbsolutePath(), e);
		} finally {
			FileUtilities.closeQuietly(is);
		}

	}

	/**
	 * determines the name of the deployment folder we should use.
	 *
	 * @param request the request we can find a unique name from
	 * @return the directory we want to have the created files reside in.
	 */
	protected File getCurrentDeploymentFolder(final DeploymentRequest request) {
		return new File(getDeployableDbFolder(), request.getShortName());
	}

	private static final Map<String, List<ProgressReporter>> CO_DEPLOYMENTS = new HashMap<String, List<ProgressReporter>>();

	@Override
	public Map<String, List<ProgressReporter>> getCoDeployments() {
		return CO_DEPLOYMENTS;
	}

	@Override
	public String check() {
		return null;
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config extends ResourceConfigBase {
		public Config() {
		}

		public Config(final String deployableDbFolder, final String engineRootFolder, final String wineWrapperScript) {
			put(DEPLOYABLE_DB_FOLDER, deployableDbFolder);
			put(ENGINE_ROOT_FOLDER, engineRootFolder);
			put(WINE_WRAPPER_SCRIPT, wineWrapperScript);
		}
	}

	@Component("sequestDeployerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {

		private SequestMappingFactory sequestMappingFactory;
		private SequestToMakeDBConverter converter;

		public Factory() {
		}

		public SequestMappingFactory getSequestMappingFactory() {
			return sequestMappingFactory;
		}

		@Resource(name = "sequestMappingFactory")
		public void setSequestMappingFactory(final SequestMappingFactory sequestMappingFactory) {
			this.sequestMappingFactory = sequestMappingFactory;
		}

		public SequestToMakeDBConverter getConverter() {
			return converter;
		}

		@Resource(name = "sequestToMakeDbConverter")
		public void setConverter(final SequestToMakeDBConverter converter) {
			this.converter = converter;
		}

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final SequestDeploymentService worker = new SequestDeploymentService();
			worker.setConverter(getConverter());
			worker.setSequestMappingFactory(getSequestMappingFactory());
			worker.setEngineRootFolder(new File(config.get(ENGINE_ROOT_FOLDER)).getAbsoluteFile());
			worker.setDeployableDbFolder(new File(config.get(DEPLOYABLE_DB_FOLDER)).getAbsoluteFile());
			worker.setWineWrapperScript(config.get(WINE_WRAPPER_SCRIPT));
			return worker;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(DEPLOYABLE_DB_FOLDER, "Database Folder", "Sequest .fasta index files will be put here.<br/>" +
							"Warning: Sequest is sensitive to path length to database index. If you are getting Sequest errors, check that the database index is placed " +
							"at a reasonably short path.")
					.required()
					.existingDirectory()
					.defaultValue("var/sequest_index")

					.property(ENGINE_ROOT_FOLDER, "Makedb Folder",
							"Path to the makedb package which can be found in the Swift installation directory:" +
									"<br/><tt>bin/makedb/</tt>")
					.required()
					.existingDirectory()

					.property(WINE_WRAPPER_SCRIPT, "Wine Wrapper Script",
							"Sequest deployer executable wine wrapper script, for example, wine and wineconsole." +
									" The wine executables can be found at <a href=\"http://www.winehq.org/\">http://www.winehq.org</a>")
					.executable(Arrays.asList("-v"))
					.defaultValue(daemon.getWrapperScript());
		}
	}
}
