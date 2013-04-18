package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.*;
import edu.mayo.mprc.swift.params2.ExtractMsnSettings;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import edu.mayo.mprc.swift.params2.mapping.ParamsInfo;
import edu.mayo.mprc.swift.search.SwiftSearchWorkPacket;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.Tuple;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import edu.mayo.mprc.workflow.engine.Resumer;
import edu.mayo.mprc.workflow.engine.SearchMonitor;
import edu.mayo.mprc.workflow.engine.Task;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * Performs swift search, one {@link #run()} call at a time. To do that, it
 * first creates a workflow, that is then being executed by {@link edu.mayo.mprc.workflow.engine.WorkflowEngine}.
 * <h3>Workflow creation</h3>
 * {@link #searchDefinitionToLists(edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition)} turns
 * the search definition into lists of tasks to do (
 * {@link #rawToMgfConversions},
 * {@link #msconvertConversions},
 * {@link #mgfCleanups},
 * {@link #databaseDeployments},
 * {@link #engineSearches},
 * {@link #scaffoldCalls}) and {@link #spectrumQaTasks}.
 * <p/>
 * The lists of tasks get collected and added to the workflow engine by {@link #collectAllTasks()}.
 * <h3>Workflow execution</h3>
 * {@link #run()} method performs next step of the search by calling the workflow
 * engine.
 */
public final class SearchRunner implements Runnable {
	private static final Logger LOGGER = Logger.getLogger(SearchRunner.class);

	private SwiftSearchWorkPacket packet;
	private SwiftSearchDefinition searchDefinition;
	private int priority;

	private CurationDao curationDao;
	private SwiftDao swiftDao;
	private ParamsInfo paramsInfo;

	/**
	 * Database record of the search we are currently running.
	 */
	private SearchRun searchRun;

	/**
	 * Key: (raw file, raw settings) tuple, obtained by {@link #getRawToMgfConversionHashKey(java.io.File, edu.mayo.mprc.swift.params2.ExtractMsnSettings)}.<br/>
	 * Value: Raw->MGF conversion task.
	 */
	private Map<Tuple<String, File>, RawToMgfTask> rawToMgfConversions = new HashMap<Tuple<String, File>, RawToMgfTask>();

	/**
	 * Key: (raw file, raw settings) tuple, obtained by {@link #getRawToMgfConversionHashKey(java.io.File, edu.mayo.mprc.swift.params2.ExtractMsnSettings)}.<br/>
	 * Value: Raw->MGF conversion task.
	 */
	private Map<Tuple<String, File>, MsconvertTask> msconvertConversions = new HashMap<Tuple<String, File>, MsconvertTask>();

	/**
	 * Key: .mgf file obtained by {@link #getMgfCleanupHashKey(java.io.File)}.<br/>
	 * Value: Mgf cleanup task
	 */
	private Map<File, FileProducingTask> mgfCleanups = new HashMap<File, FileProducingTask>();

	/**
	 * Key: raw file<br/>
	 * Value: RAW dump task
	 */
	private Map<File, RAWDumpTask> rawDumpTasks = new HashMap<File, RAWDumpTask>();

	/**
	 * Key: input file<br/>
	 * Value: MSMSEvalFilter task
	 */
	private Map<File, SpectrumQaTask> spectrumQaTasks = new HashMap<File, SpectrumQaTask>();

	/**
	 * Key: (engine, param file) tuple, obtained by {@link #getDbDeploymentHashKey(edu.mayo.mprc.swift.db.SearchEngine)}.<br/>
	 * Value: Database deployment task.
	 */
	private Map<SearchEngine, DatabaseDeployment> databaseDeployments = new HashMap<SearchEngine, DatabaseDeployment>();

	/**
	 * Key: "engine:input file" tuple, obtained by {@link #getEngineSearchHashKey(edu.mayo.mprc.swift.db.SearchEngine, java.io.File)}.<br/>
	 * Value: Engine search task.
	 */
	private Map<String, EngineSearchTask> engineSearches = new HashMap<String, EngineSearchTask>();

	/**
	 * Key: scaffold call specification<br/>
	 * Value: Scaffold caller task
	 */
	private Map<ScaffoldCall, ScaffoldTaskI> scaffoldCalls = new HashMap<ScaffoldCall, ScaffoldTaskI>();

	/**
	 * Key: input file for IdPicker<br/>
	 * Value: Idpicker caller task
	 */
	private Map<String, IdpickerTask> idpickerCalls = Maps.newHashMap();

	/**
	 * Key: Curation ID
	 * Value: FastaDb loader (will load FASTA into a relational database)
	 */
	private Map<Integer, FastaDbTask> fastaDbCalls = new HashMap<Integer, FastaDbTask>();

	/**
	 * List of search db tasks, mapped to scaffold calls and corresponding fastaDb calls.
	 */
	private Map<File, SearchDbTask> searchDbCalls = new HashMap<File, SearchDbTask>();

	/**
	 * One and only QA task for the entire search == more practical
	 */
	private QaTask qaTask;

	/**
	 * List of tasks producing protein reports.
	 */
	private List<Task> reportCalls = new LinkedList<Task>();

	private DaemonConnection raw2mgfDaemon;
	private DaemonConnection msconvertDaemon;
	private DaemonConnection mgfCleanupDaemon;
	private DaemonConnection rawDumpDaemon;
	private DaemonConnection msmsEvalDaemon;
	private DaemonConnection scaffoldReportDaemon;
	private DaemonConnection qaDaemon;
	private DaemonConnection fastaDbDaemon;
	private DaemonConnection searchDbDaemon;
	private boolean reportDecoyHits;

	private Collection<SearchEngine> searchEngines = null;

	private WorkflowEngine workflowEngine;

	private boolean initializationDone = false;

	private ProgressReporter reporter;
	private ExecutorService service;

	private FileTokenFactory fileTokenFactory;
	private Map<SearchEngine, File> parameterFiles;

	/**
	 * Making files distinct in case the search uses same file name several times.
	 */
	private DistinctFiles distinctFiles = new DistinctFiles();
	private static final String DEFAULT_SPECTRUM_QA_FOLDER = "spectrum_qa";
	private static final String DEFAULT_PARAMS_FOLDER = "params";

	public SearchRunner(
			final SwiftSearchWorkPacket packet,
			final SwiftSearchDefinition searchDefinition,
			final DaemonConnection raw2mgfDaemon,
			final DaemonConnection msconvertDaemon,
			final DaemonConnection mgfCleanupDaemon,
			final DaemonConnection rawDumpDaemon,
			final DaemonConnection msmsEvalDaemon,
			final DaemonConnection scaffoldReportDaemon,
			final DaemonConnection qaDaemon,
			final DaemonConnection fastaDbDaemon,
			final DaemonConnection searchDbDaemon,
			final Collection<SearchEngine> searchEngines,
			final ProgressReporter reporter,
			final ExecutorService service,
			final CurationDao curationDao,
			final SwiftDao swiftDao,
			final FileTokenFactory fileTokenFactory,
			final SearchRun searchRun,
			final boolean reportDecoyHits,
			final int priority,
			final ParamsInfo paramsInfo) {
		this.searchDefinition = searchDefinition;
		this.packet = packet;
		this.raw2mgfDaemon = raw2mgfDaemon;
		this.msconvertDaemon = msconvertDaemon;
		this.mgfCleanupDaemon = mgfCleanupDaemon;
		this.rawDumpDaemon = rawDumpDaemon;
		this.msmsEvalDaemon = msmsEvalDaemon;
		this.scaffoldReportDaemon = scaffoldReportDaemon;
		this.qaDaemon = qaDaemon;
		this.fastaDbDaemon = fastaDbDaemon;
		this.searchDbDaemon = searchDbDaemon;
		this.searchEngines = searchEngines;
		this.reporter = reporter;
		this.service = service;
		this.curationDao = curationDao;
		this.swiftDao = swiftDao;
		this.fileTokenFactory = fileTokenFactory;
		this.searchRun = searchRun;
		this.reportDecoyHits = reportDecoyHits;
		this.priority = priority;
		this.workflowEngine = new WorkflowEngine(packet.getTaskId());
		this.workflowEngine.setPriority(priority);
		this.paramsInfo = paramsInfo;
		assertValid();
	}

	public void initialize() {
		if (!initializationDone) {
			LOGGER.debug("Initializing search " + this.searchDefinition.getTitle());
			createParameterFiles();
			searchDefinitionToLists(this.searchDefinition);
			addReportTasks(this.searchDefinition);
			collectAllTasks();
			assertValid();
			initializationDone = true;
		}
	}

	public void run() {
		while (true) {
			try {
				workflowEngine.run();
				if (workflowEngine.isDone()) {
					packet.synchronizeFileTokensOnReceiver();
					reporter.reportSuccess();
					break;
				} else if (workflowEngine.isWorkAvailable()) {
					// Yield - go into the loop again and process the available work
					yield();
				} else {
					workflowEngine.resumeOnWork(new MyResumer(this));
					break;
				}
			} catch (Exception t) {
				workflowEngine.reportError(t);
				reporter.reportFailure(t);
				break;
			}
		}
	}

	public SearchRun getSearchRun() {
		return searchRun;
	}

	public void setSearchRun(final SearchRun searchRun) {
		this.searchRun = searchRun;
	}

	public WorkflowEngine getWorkflowEngine() {
		return workflowEngine;
	}

	private void yield() {
		// Currently does nothing, the engine immediately keeps processing more work
	}

	public void assertValid() {
		Preconditions.checkNotNull(curationDao, "Curation DAO has to be set up");
		Preconditions.checkNotNull(swiftDao, "Swift DAO has to be set up");

		assert searchEngines != null : "Search engine set must not be null";
		if (this.searchDefinition != null) {
			assert workflowEngine.getNumTasks() ==
					databaseDeployments.size() +
							rawToMgfConversions.size() +
							msconvertConversions.size() +
							mgfCleanups.size() +
							rawDumpTasks.size() +
							spectrumQaTasks.size() +
							engineSearches.size() +
							scaffoldCalls.size() +
							idpickerCalls.size() +
							fastaDbCalls.size() +
							reportCalls.size() +
							searchDbCalls.size() +
							(qaTask == null ? 0 : 1) : "All tasks must be a collection of *ALL* tasks";
		}
	}

	private void collectAllTasks() {
		workflowEngine.addAllTasks(databaseDeployments.values());
		workflowEngine.addAllTasks(rawToMgfConversions.values());
		workflowEngine.addAllTasks(msconvertConversions.values());
		workflowEngine.addAllTasks(mgfCleanups.values());
		workflowEngine.addAllTasks(rawDumpTasks.values());
		workflowEngine.addAllTasks(spectrumQaTasks.values());
		workflowEngine.addAllTasks(engineSearches.values());
		workflowEngine.addAllTasks(scaffoldCalls.values());
		workflowEngine.addAllTasks(idpickerCalls.values());
		workflowEngine.addAllTasks(fastaDbCalls.values());
		workflowEngine.addAllTasks(reportCalls);
		workflowEngine.addAllTasks(searchDbCalls.values());
		if (qaTask != null) {
			workflowEngine.addTask(qaTask);
		}
	}

	private void addReportTasks(final SwiftSearchDefinition searchDefinition) {
		if (searchDefinition.getPeptideReport() != null) {
			addScaffoldReportStep(searchDefinition);
		}
	}

	private void searchDefinitionToLists(final SwiftSearchDefinition searchDefinition) {
		// Now let us fill in all the lists
		File file = null;

		for (final FileSearch inputFile : searchDefinition.getInputFiles()) {
			file = inputFile.getInputFile();
			if (file.exists()) {
				addInputFileToLists(inputFile, Boolean.TRUE.equals(searchDefinition.getPublicSearchFiles()));
			} else {
				LOGGER.info("Skipping nonexistent input file [" + file.getAbsolutePath() + "]");
			}
		}

		addFastaDbCall(searchDefinition.getSearchParameters().getDatabase());
	}

	private SearchEngine getSearchEngine(final String code) {
		String version = "";
		for(SearchEngineConfig config : searchDefinition.getInputFiles().get(0).getEnabledEngines().getEngineConfigs()) {
			if(config.getCode().equals(code)) {
				version = config.getVersion();
				break;
			}
		}

		for (final SearchEngine engine : searchEngines) {
			if (engine.getCode().equalsIgnoreCase(code) && engine.getVersion().equalsIgnoreCase(version)) {
				return engine;
			}
		}
		return null;
	}

	private SearchEngine getScaffoldEngine() {
		return getSearchEngine("SCAFFOLD");
	}

	private SearchEngine getIdpickerEngine() {
		return getSearchEngine("IDPICKER");
	}

	/**
	 * Save parameter files to the disk.
	 */
	private void createParameterFiles() {
		// Obtain a set of all search engines that were requested
		// This way we only create config files that we need
		final Set<String> enabledEngines = new HashSet<String>();
		for (final FileSearch fileSearch : searchDefinition.getInputFiles()) {
			if (fileSearch != null) {
				for (final SearchEngineConfig config : fileSearch.getEnabledEngines().getEngineConfigs()) {
					enabledEngines.add(config.getCode());
				}
			}
		}

		final File paramFolder = new File(searchDefinition.getOutputFolder(), DEFAULT_PARAMS_FOLDER);
		FileUtilities.ensureFolderExists(paramFolder);
		parameterFiles = new HashMap<SearchEngine, File>();
		final SearchEngineParameters params = searchDefinition.getSearchParameters();
		if (!enabledEngines.isEmpty()) {
			FileUtilities.ensureFolderExists(paramFolder);
			for (final String engineCode : enabledEngines) {
				final SearchEngine engine = getSearchEngine(engineCode);
				final File file = engine.writeSearchEngineParameterFile(paramFolder, params, null /*We do not validate, validation should be already done*/, paramsInfo);
				addParamFile(engineCode, file);
			}
		}
	}

	void addInputFileToLists(final FileSearch inputFile, final boolean publicSearchFiles) {
		final FileProducingTask mgfOutput = addMgfProducingProcess(inputFile);
		addInputAnalysis(inputFile, mgfOutput);

		final SearchEngine scaffold3 = getScaffoldEngine();
		DatabaseDeployment scaffoldDeployment = null;
		if (scaffold3 != null && searchWithScaffold(inputFile)) {
			scaffoldDeployment =
					addDatabaseDeployment(scaffold3, null/*scaffold has no param file*/,
							searchDefinition.getSearchParameters().getDatabase());
		}
		final SearchEngine idpicker = getIdpickerEngine();
		DatabaseDeployment idpickerDeployment = null;
		if (idpicker != null && searchWithIdpicker(inputFile)) {
			idpickerDeployment =
					addDatabaseDeployment(idpicker, null/*idpicker has no param file*/,
							searchDefinition.getSearchParameters().getDatabase());
		}

		ScaffoldTask scaffoldTask = null;

		// Go through all possible search engines this file requires
		for (final SearchEngine engine : searchEngines) {
			// All 'normal' searches get normal entries
			// While building these, the Scaffold search entry itself is initialized in a separate list
			// The IDPicker search is special as well, it is set up to process the results of the myrimatch search
			if (isNormalEngine(engine) && inputFile.getEnabledEngines().isEnabled(engine.getCode())) {
				final File paramFile = getParamFile(engine);

				DatabaseDeploymentResult deploymentResult = null;
				// Sequest deployment is counter-productive for particular input fasta file
				if (sequest(engine) && noSequestDeployment()) {
					deploymentResult = new NoSequestDeploymentResult(curationDao.findCuration(searchDefinition.getSearchParameters().getDatabase().getShortName()).getCurationFile());
				} else {
					deploymentResult = addDatabaseDeployment(engine, paramFile, searchDefinition.getSearchParameters().getDatabase());
				}
				final File outputFolder = getOutputFolderForSearchEngine(engine);
				final EngineSearchTask search = addEngineSearch(engine, paramFile, inputFile.getInputFile(), outputFolder, mgfOutput, deploymentResult, publicSearchFiles);
				if (searchWithScaffold(inputFile)) {
					if (scaffoldDeployment == null) {
						throw new MprcException("Scaffold search submitted without having Scaffold 3 service enabled.");
					}

					scaffoldTask = addScaffoldCall(inputFile, search, scaffoldDeployment);

					if (searchDefinition.getQa() != null) {
						addQaTask(inputFile, scaffoldTask, mgfOutput);
					}
				}
				// If IDPIcker is on, we chain an IDPicker call to the output of the previous search engine.
				// We support Myrimatch only for now
				if (searchWithIdpicker(inputFile) && "MYRIMATCH".equals(engine.getCode())) {
					addIdpickerCall(
							idpicker,
							getOutputFolderForSearchEngine(idpicker),
							search,
							idpickerDeployment);
				}
			}
		}

		if (searchDbDaemon != null && rawDumpDaemon != null && scaffoldTask != null) {
			// Ask for dumping the .RAW file since the QA might be disabled
			if (isRawFile(inputFile)) {
				final RAWDumpTask rawDumpTask = addRawDumpTask(inputFile.getInputFile(), QaTask.getQaSubdirectory(scaffoldTask.getScaffoldXmlFile()));
				addSearchDbCall(scaffoldTask, rawDumpTask, searchDefinition.getSearchParameters().getDatabase());
			}
		}
	}

	private boolean searchWithScaffold(final FileSearch inputFile) {
		return inputFile.isSearch("SCAFFOLD");
	}

	private boolean searchWithIdpicker(final FileSearch inputFile) {
		return inputFile.isSearch("IDPICKER");
	}

	private boolean sequest(final SearchEngine engine) {
		return "SEQUEST".equalsIgnoreCase(engine.getCode());
	}

	private boolean isNormalEngine(final SearchEngine engine) {
		return !engine.getEngineMetadata().isAggregator();
	}

	private void addParamFile(final String engineCode, final File file) {
		parameterFiles.put(getSearchEngine(engineCode), file);
	}

	private File getParamFile(final SearchEngine engine) {
		return parameterFiles.get(engine);
	}

	/**
	 * Adds steps to analyze the contents of the input file. This means spectrum QA (e.g. using msmsEval)
	 * as well as metadata extraction.
	 *
	 * @param inputFile Input file to analyze.
	 * @param mgf       Mgf of the input file.
	 */
	private void addInputAnalysis(final FileSearch inputFile, final FileProducingTask mgf) {
		// TODO: Extract metadata from the input file

		// Analyze spectrum quality if requested
		if (searchDefinition.getQa() != null && searchDefinition.getQa().getParamFilePath() != null) {
			addSpectrumQualityAnalysis(inputFile, mgf);
		}
	}

	private File getOutputFolderForSearchEngine(final SearchEngine engine) {
		return new File(searchDefinition.getOutputFolder(), engine.getOutputDirName());
	}

	/**
	 * @return <code>true</code> if the input file has Sequest deployment disabled.
	 */
	private boolean noSequestDeployment() {
		// Non-specific proteases (do not define restrictions for Rn-1 and Rn prevent sequest from deploying database index
		return "".equals(searchDefinition.getSearchParameters().getProtease().getRn()) &&
				"".equals(searchDefinition.getSearchParameters().getProtease().getRnminus1());
	}

	/**
	 * Add a process that produces an mgf file.
	 * <ul>
	 * <li>If the file is a .RAW, we perform conversion.</li>
	 * <li>If the file is already in .mgf format, instead of converting raw->mgf,
	 * we clean the mgf up, making sure the title contains expected information.</li>
	 * </ul>
	 *
	 * @param inputFile file to convert.
	 * @return Task capable of producing an mgf (either by conversion or by cleaning up an existing mgf).
	 */
	FileProducingTask addMgfProducingProcess(final FileSearch inputFile) {
		final File file = inputFile.getInputFile();

		FileProducingTask mgfOutput = null;
		// First, make sure we have a valid mgf, no matter what input we got
		if (file.getName().endsWith(".mgf")) {
			mgfOutput = addMgfCleanupStep(inputFile);
		} else {
			mgfOutput = addRaw2MgfConversionStep(inputFile);
		}
		return mgfOutput;
	}

	private FileProducingTask addRaw2MgfConversionStep(final FileSearch inputFile) {
		final File file = inputFile.getInputFile();
		final ExtractMsnSettings conversionSettings = searchDefinition.getSearchParameters().getExtractMsnSettings();

		final Tuple<String, File> hashKey = getRawToMgfConversionHashKey(file, conversionSettings);

		if (ExtractMsnSettings.EXTRACT_MSN.equals(conversionSettings.getCommand())) {
			RawToMgfTask task = rawToMgfConversions.get(hashKey);

			if (task == null) {
				final File mgfFile = getMgfFileLocation(inputFile);

				task = new RawToMgfTask(
						/*Input file*/ file,
						/*Mgf file location*/ mgfFile,
						/*raw2mgf command line*/ searchDefinition.getSearchParameters().getExtractMsnSettings().getCommandLineSwitches(),
						Boolean.TRUE.equals(searchDefinition.getPublicMgfFiles()),
						raw2mgfDaemon, fileTokenFactory, isFromScratch());

				rawToMgfConversions.put(hashKey, task);
			}
			return task;
		} else {
			MsconvertTask task = msconvertConversions.get(hashKey);

			if (task == null) {
				final File mgfFile = getMgfFileLocation(inputFile);

				task = new MsconvertTask(
						/*Input file*/ file,
						/*Mgf file location*/ mgfFile,
						Boolean.TRUE.equals(searchDefinition.getPublicMgfFiles()),
						msconvertDaemon, fileTokenFactory, isFromScratch());

				msconvertConversions.put(hashKey, task);
			}
			return task;
		}
	}

	/**
	 * We have already made .mgf file. Because it can be problematic, we need to clean it up
	 */
	private FileProducingTask addMgfCleanupStep(final FileSearch inputFile) {
		final File file = inputFile.getInputFile();
		FileProducingTask mgfOutput = mgfCleanups.get(getMgfCleanupHashKey(file));
		if (mgfOutput == null) {
			final File outputFile = getMgfFileLocation(inputFile);
			mgfOutput = new MgfTitleCleanupTask(mgfCleanupDaemon, file, outputFile, fileTokenFactory, isFromScratch());
			mgfCleanups.put(getMgfCleanupHashKey(file), mgfOutput);
		}
		return mgfOutput;
	}

	/**
	 * Adds steps needed to analyze quality of the spectra. This can be done with a tool such as msmsEval or similar.
	 *
	 * @param inputFile Input file
	 * @param mgf       .mgf for the input file
	 */
	private void addSpectrumQualityAnalysis(final FileSearch inputFile, final FileProducingTask mgf) {
		if (inputFile == null) {
			throw new MprcException("Bug: Input file must not be null");
		}
		final SpectrumQa spectrumQa = searchDefinition.getQa();

		if (spectrumQa == null) {
			throw new MprcException("Bug: The spectrum QA step must be enabled to be used");
		}
		// TODO: Check for spectrumQa.paramFile to be != null. Current code is kind of a hack.
		final File file = inputFile.getInputFile();

		if (spectrumQaTasks.get(getSpectrumQaHashKey(file)) == null) {
			final SpectrumQaTask spectrumQaTask = new SpectrumQaTask(
					msmsEvalDaemon,
					mgf,
					spectrumQa.paramFile() == null ? null : spectrumQa.paramFile().getAbsoluteFile(),
					getSpectrumQaOutputFolder(inputFile),
					fileTokenFactory, isFromScratch());
			spectrumQaTask.addDependency(mgf);

			spectrumQaTasks.put(getSpectrumQaHashKey(file), spectrumQaTask);
		}
	}

	private void addScaffoldReportStep(final SwiftSearchDefinition searchDefinition) {

		final List<File> scaffoldOutputFiles = new ArrayList<File>(scaffoldCalls.size());

		for (final ScaffoldTaskI scaffoldTask : scaffoldCalls.values()) {
			scaffoldOutputFiles.add(scaffoldTask.getScaffoldPeptideReportFile());
		}

		final File peptideReportFile = new File(scaffoldOutputFiles.get(0).getParentFile(), "Swift Peptide Report For " + searchDefinition.getTitle() + ".xls");
		final File proteinReportFile = new File(scaffoldOutputFiles.get(0).getParentFile(), "Swift Protein Report For " + searchDefinition.getTitle() + ".xls");

		final ScaffoldReportTask scaffoldReportTask = new ScaffoldReportTask(scaffoldReportDaemon, scaffoldOutputFiles, peptideReportFile, proteinReportFile, fileTokenFactory, isFromScratch());

		for (final ScaffoldTaskI scaffoldTask : scaffoldCalls.values()) {
			scaffoldReportTask.addDependency(scaffoldTask);
		}

		reportCalls.add(scaffoldReportTask);
	}

	private boolean isFromScratch() {
		return packet.isFromScratch();
	}

	private void addQaTask(final FileSearch inputFile, final ScaffoldTaskI scaffoldTask, final FileProducingTask mgfOutput) {
		if (qaDaemon != null) {
			if (qaTask == null) {
				qaTask = new QaTask(qaDaemon, fileTokenFactory, isFromScratch());
			}

			// Set up a new experiment dependency. All entries called from now on would be added under that experiment
			qaTask.addExperiment(scaffoldTask.getScaffoldXmlFile(), scaffoldTask.getScaffoldSpectraFile());
			qaTask.addDependency(scaffoldTask);
			qaTask.addDependency(mgfOutput);

			if (isRawFile(inputFile)) {
				final File file = inputFile.getInputFile();

				RAWDumpTask rawDumpTask = null;

				if (rawDumpDaemon != null && isRawFile(inputFile)) {
					rawDumpTask = addRawDumpTask(file, qaTask.getQaReportFolder());
					qaTask.addDependency(rawDumpTask);
				}

				qaTask.addMgfToRawEntry(mgfOutput, file, rawDumpTask);
			}

			final SpectrumQaTask spectrumQaTask;

			if ((spectrumQaTask = spectrumQaTasks.get(getSpectrumQaHashKey(inputFile.getInputFile()))) != null) {
				qaTask.addMgfToMsmsEvalEntry(mgfOutput, spectrumQaTask);
				qaTask.addDependency(spectrumQaTask);
			}
		}
	}

	private RAWDumpTask addRawDumpTask(final File rawFile, final File outputFolder) {
		RAWDumpTask task = rawDumpTasks.get(rawFile);

		if (task == null) {
			task = new RAWDumpTask(rawFile, outputFolder, rawDumpDaemon, fileTokenFactory, isFromScratch());
		}

		rawDumpTasks.put(rawFile, task);

		return task;
	}

	private RAWDumpTask getRawDumpTaskForInputFile(final FileSearch inputFile) {
		return rawDumpTasks.get(inputFile.getInputFile());
	}

	private static boolean isRawFile(final FileSearch inputFile) {
		return !inputFile.getInputFile().getName().endsWith(".mgf");
	}

	/**
	 * @param inputFile The input file entry from the search definition.
	 * @return
	 */
	private File getMgfFileLocation(final FileSearch inputFile) {
		final File file = inputFile.getInputFile();
		final String mgfOutputDir = new File(
				new File(searchDefinition.getOutputFolder(), "dta"),
				getFileTitle(file)).getPath();
		final File mgfFile = new File(mgfOutputDir, replaceFileExtension(file, ".mgf").getName());
		// Make sure we never produce same mgf file twice (for instance when we get two identical input mgf file names as input that differ only in the folder).
		return distinctFiles.getDistinctFile(mgfFile);
	}

	/**
	 * @param inputFile The input file entry from the search definition.
	 * @return The location of the msmsEval filtered output for the given input file
	 */
	private File getSpectrumQaOutputFolder(final FileSearch inputFile) {
		final File file = inputFile.getInputFile();
		// msmsEval directory should be right next to the "dta" folder
		final File msmsEvalFolder = getSpectrumQaOutputDirLocation();
		final File outputFolder =
				new File(
						msmsEvalFolder,
						getFileTitle(file));
		// Make sure we never produce same folder twice (for instance when we get two identical input mgf file names that should be processed with different params).
		return distinctFiles.getDistinctFile(outputFolder);
	}

	/**
	 * Returns spectrum QA output folder location. If the setting is missing, looks at the dta folder and makes spectrum QA folder next to it.
	 */
	private File getSpectrumQaOutputDirLocation() {
		return new File(searchDefinition.getOutputFolder(), DEFAULT_SPECTRUM_QA_FOLDER);
	}

	/**
	 * Returns output file given search engine, search output folder and name of the input file.
	 */
	private File getSearchResultLocation(final SearchEngine engine, final File searchOutputFolder, final File file) {
		final String fileTitle = FileUtilities.stripExtension(file.getName());
		final String newFileName = fileTitle + engine.getResultExtension();
		final File resultFile = new File(searchOutputFolder, newFileName);
		// Make sure we never produce two identical result files.
		return distinctFiles.getDistinctFile(resultFile);
	}

	private static String getFileTitle(final File file) {
		return FileUtilities.stripExtension(file.getName());
	}

	private static File replaceFileExtension(final File file, final String newExtension) {
		return new File(FileUtilities.stripExtension(file.getName()) + newExtension);
	}

	/**
	 * Make a record for db deployment, if we do not have one already
	 */
	DatabaseDeployment addDatabaseDeployment(final SearchEngine engine, final File paramFile, final Curation dbToDeploy) {
		final SearchEngine hashKey;
		// The DB deployment is defined by engine for which it is done
		hashKey = getDbDeploymentHashKey(engine);

		DatabaseDeployment deployment = databaseDeployments.get(hashKey);
		if (deployment == null) {
			deployment = new DatabaseDeployment(engine.getCode(), engine.getFriendlyName(), engine.getDbDeployDaemon(), paramFile, dbToDeploy, fileTokenFactory, isFromScratch());
			databaseDeployments.put(hashKey, deployment);
		}
		return deployment;
	}

	/**
	 * Make a record for the search itself.
	 * The search depends on the engine, and the file to be searched.
	 * If these two things are identical for two entries, then the search can be performed just once.
	 * <p/>
	 * The search also knows about the conversion and db deployment so it can determine when it can run.
	 */
	private EngineSearchTask addEngineSearch(final SearchEngine engine, final File paramFile, final File inputFile, final File searchOutputFolder, final FileProducingTask fileProducingTask, final DatabaseDeploymentResult deploymentResult, final boolean publicSearchFiles) {
		final String searchKey = getEngineSearchHashKey(engine, inputFile);
		EngineSearchTask search = engineSearches.get(searchKey);
		if (search == null) {
			final File outputFile = getSearchResultLocation(engine, searchOutputFolder, inputFile);
			search = new EngineSearchTask(
					engine,
					inputFile.getName(),
					fileProducingTask,
					deploymentResult,
					outputFile,
					paramFile,
					publicSearchFiles,
					engine.getSearchDaemon(),
					fileTokenFactory,
					isFromScratch());

			// Depend on the .mgf to be done and on the database deployment
			search.addDependency(fileProducingTask);
			if (deploymentResult instanceof Task) {
				search.addDependency((Task) deploymentResult);
			}
			engineSearches.put(searchKey, search);
		}
		return search;
	}

	/**
	 * Add a scaffold 3 call (or update existing one) that depends on this input file to be sought through
	 * the given engine search.
	 */
	private ScaffoldTask addScaffoldCall(final FileSearch inputFile, final EngineSearchTask search, final DatabaseDeployment scaffoldDbDeployment) {
		final String experiment = inputFile.getExperiment();
		final ScaffoldCall key = new ScaffoldCall(experiment, "3");
		final ScaffoldTaskI scaffoldTaskI = scaffoldCalls.get(key);
		if (scaffoldTaskI != null && !(scaffoldTaskI instanceof ScaffoldTask)) {
			ExceptionUtilities.throwCastException(scaffoldTaskI, ScaffoldTask.class);
			return null;
		}
		ScaffoldTask scaffoldTask = (ScaffoldTask) scaffoldTaskI;

		if (scaffoldTask == null) {
			final File scaffoldOutputDir = getOutputFolderForSearchEngine(getScaffoldEngine());
			scaffoldTask = new ScaffoldTask(
					experiment,
					searchDefinition,
					getScaffoldEngine().getSearchDaemon(),
					swiftDao, searchRun,
					scaffoldOutputDir,
					fileTokenFactory,
					reportDecoyHits,
					isFromScratch());
			scaffoldCalls.put(key, scaffoldTask);
		}
		scaffoldTask.addInput(inputFile, search);
		scaffoldTask.addDatabase(scaffoldDbDeployment.getShortDbName(), scaffoldDbDeployment);
		scaffoldTask.addDependency(search);
		scaffoldTask.addDependency(scaffoldDbDeployment);

		return scaffoldTask;
	}

	private IdpickerTask addIdpickerCall(final SearchEngine idpicker, final File outputFolder,
	                                     final EngineSearchTask search, final DatabaseDeployment idpickerDeployment) {
		final String key = search.getOutputFile().getAbsolutePath();
		if (idpickerCalls.containsKey(key)) {
			return idpickerCalls.get(key);
		}
		final IdpickerTask task = new IdpickerTask(swiftDao, searchRun,
				getSearchDefinition(), idpicker.getSearchDaemon(),
				search, idpickerDeployment, outputFolder, fileTokenFactory, isFromScratch());
		idpickerCalls.put(key, task);
		task.addDependency(search);
		return task;
	}


	private FastaDbTask addFastaDbCall(final Curation curation) {
		if (fastaDbDaemon != null) {
			final int id = curation.getId();
			final FastaDbTask task = fastaDbCalls.get(id);
			if (task == null) {
				final FastaDbTask newTask = new FastaDbTask(fastaDbDaemon, fileTokenFactory, false, curation);
				fastaDbCalls.put(id, newTask);
				return newTask;
			} else {
				return task;
			}
		}
		return null;
	}

	private SearchDbTask addSearchDbCall(final ScaffoldTask scaffoldTask, final RAWDumpTask rawDumpTask, final Curation curation) {
		final File file = scaffoldTask.getScaffoldSpectraFile();
		SearchDbTask searchDbTask = searchDbCalls.get(file);
		if (searchDbTask == null) {
			final FastaDbTask fastaDbTask = addFastaDbCall(curation);
			final SearchDbTask task = new SearchDbTask(searchDbDaemon, fileTokenFactory, false, scaffoldTask);
			task.addDependency(fastaDbTask);
			task.addDependency(scaffoldTask);
			searchDbCalls.put(file, task);
			searchDbTask = task;
		}
		// We depend on all raw dump tasks for loading metadata about the files
		searchDbTask.addRawDumpTask(rawDumpTask);
		searchDbTask.addDependency(rawDumpTask);
		return searchDbTask;
	}

	private static String getEngineSearchHashKey(final SearchEngine engine, final File file) {
		return engine.getCode() + ':' + file.getAbsolutePath();
	}

	private static File getMgfCleanupHashKey(final File file) {
		return file.getAbsoluteFile();
	}

	private static File getSpectrumQaHashKey(final File file) {
		return file.getAbsoluteFile();
	}

	private static Tuple<String, File> getRawToMgfConversionHashKey(final File inputFile, final ExtractMsnSettings extractMsnSettings) {
		return new Tuple<String, File>(extractMsnSettings.getCommandLineSwitches(), inputFile);
	}

	private static SearchEngine getDbDeploymentHashKey(final SearchEngine engine) {
		return engine;
	}

	public SwiftSearchDefinition getSearchDefinition() {
		return searchDefinition;
	}

	public void addSearchMonitor(final SearchMonitor monitor) {
		this.workflowEngine.addMonitor(monitor);
	}

	private static final class MyResumer implements Resumer {
		private SearchRunner runner;

		private MyResumer(final SearchRunner runner) {
			this.runner = runner;
		}

		public void resume() {
			runner.service.execute(runner);
		}
	}
}
