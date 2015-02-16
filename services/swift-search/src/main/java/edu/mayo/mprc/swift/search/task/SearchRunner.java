package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Preconditions;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.comet.CometWorker;
import edu.mayo.mprc.config.Lifecycle;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.DaemonUtilities;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.CurationDao;
import edu.mayo.mprc.scaffold.ScaffoldWorker;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SpectrumQa;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.ExtractMsnSettings;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import edu.mayo.mprc.swift.params2.mapping.ParamsInfo;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.StringUtilities;
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
 * {@link #searchDefinitionToTasks(edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition)} turns
 * the search definition into a list of tasks to do.
 * <p/>
 * The list of tasks get collected and added to the workflow engine by {@link #collectAllTasks()}.
 * <h3>Workflow execution</h3>
 * {@link #run()} method performs next step of the search by calling the workflow
 * engine.
 */
public final class SearchRunner implements Runnable, Lifecycle {
	private static final Logger LOGGER = Logger.getLogger(SearchRunner.class);
	public static final String MGF = "mgf";
	public static final String MZ_ML = "mzML";
	public static final ExtractMsnSettings MSCONVERT_MZML = new ExtractMsnSettings(ExtractMsnSettings.MZML_MODE, ExtractMsnSettings.MSCONVERT);
	public static final ExtractMsnSettings MSCONVERT_MS2 = new ExtractMsnSettings(ExtractMsnSettings.MS2_MODE, ExtractMsnSettings.MSCONVERT);
	/**
	 * We idpQonvert tasks with specific FDR for QuaMeter. This is hardcoded, does not reflect how are actual searches run.
	 */
	public static final double QUAMETER_FDR = 0.02;

	private boolean running;
	private boolean semitrypticQuameter;
	private final boolean fromScratch;
	private final SwiftSearchDefinition searchDefinition;

	private final CurationDao curationDao;
	private final SwiftDao swiftDao;
	private final ParamsInfo paramsInfo;

	/**
	 * Database record of the search we are currently running.
	 */
	private SearchRun searchRun;

	private final HashMap<Task, Task> tasks = new LinkedHashMap<Task, Task>(20);

	/**
	 * Key: input file<br/>
	 * Value: SpectrumQaTask task
	 */
	private final Map<File, SpectrumQaTask> spectrumQaTasks = new HashMap<File, SpectrumQaTask>(10);

	/**
	 * One and only QA task for the entire search == more practical
	 */
	private QaTask qaTask;

	/**
	 * Set of all Scaffold tasks
	 */
	private final Collection<ScaffoldTaskI> scaffoldCalls = new LinkedHashSet<ScaffoldTaskI>(5);

	private final DaemonConnection raw2mgfDaemon;
	private final DaemonConnection msconvertDaemon;
	private final DaemonConnection mgfCleanupDaemon;
	private final DaemonConnection rawDumpDaemon;
	private final DaemonConnection msmsEvalDaemon;
	private final DaemonConnection scaffoldReportDaemon;
	private final DaemonConnection qaDaemon;
	private final DaemonConnection fastaDbDaemon;
	private final DaemonConnection searchDbDaemon;
	private final DaemonConnection quameterDbDaemon;
	private final boolean reportDecoyHits;

	private final WorkflowEngine workflowEngine;

	private boolean initializationDone;

	private final ProgressReporter reporter;
	private final ExecutorService service;

	private final DatabaseFileTokenFactory fileTokenFactory;
	private SearchEngineParametersCollection parameterFiles;
	private final SearchEngineList engines;

	/**
	 * Making files distinct in case the search uses same file name several times.
	 */
	private final DistinctFiles distinctFiles = new DistinctFiles();
	private static final String DEFAULT_SPECTRUM_QA_FOLDER = "spectrum_qa";

	public SearchRunner(
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
			final DaemonConnection quameterDbDaemon,
			final Collection<SearchEngine> searchEngines,
			final ProgressReporter reporter,
			final ExecutorService service,
			final CurationDao curationDao,
			final SwiftDao swiftDao,
			final DatabaseFileTokenFactory fileTokenFactory,
			final SearchRun searchRun,
			final boolean reportDecoyHits,
			final boolean semitrypticQuameter,
			final int priority,
			final ParamsInfo paramsInfo,
			final String taskId,
			final boolean fromScratch) {
		this.searchDefinition = searchDefinition;
		this.raw2mgfDaemon = raw2mgfDaemon;
		this.msconvertDaemon = msconvertDaemon;
		this.mgfCleanupDaemon = mgfCleanupDaemon;
		this.rawDumpDaemon = rawDumpDaemon;
		this.msmsEvalDaemon = msmsEvalDaemon;
		this.scaffoldReportDaemon = scaffoldReportDaemon;
		this.qaDaemon = qaDaemon;
		this.fastaDbDaemon = fastaDbDaemon;
		this.searchDbDaemon = searchDbDaemon;
		this.quameterDbDaemon = quameterDbDaemon;
		this.reporter = reporter;
		this.service = service;
		this.curationDao = curationDao;
		this.swiftDao = swiftDao;
		this.fileTokenFactory = fileTokenFactory;
		this.searchRun = searchRun;
		this.reportDecoyHits = reportDecoyHits;
		this.semitrypticQuameter = semitrypticQuameter;
		workflowEngine = new WorkflowEngine(taskId);
		workflowEngine.setPriority(priority);
		this.paramsInfo = paramsInfo;
		this.fromScratch = fromScratch;
		engines = new SearchEngineList(searchEngines, searchDefinition);
		assertValid();
	}

	public void initialize() {
		if (!initializationDone) {
			LOGGER.debug("Initializing search " + searchDefinition.getTitle());
			parameterFiles = new SearchEngineParametersCollection(searchDefinition.getOutputFolder(), paramsInfo);
			searchDefinitionToTasks(searchDefinition);
			addReportTasks(searchDefinition);
			collectAllTasks();
			assertValid();
			initializationDone = true;
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				workflowEngine.run();
				if (workflowEngine.isDone()) {
					reporter.reportSuccess();
					break;
				} else if (workflowEngine.isWorkAvailable()) {
					// Yield - go into the loop again and process the available work
					yield();
				} else {
					workflowEngine.resumeOnWork(new MyResumer(this));
					break;
				}
			} catch (final Exception t) {
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

		if (searchDefinition != null) {
			assert workflowEngine.getNumTasks() == tasks.size() : "All tasks must be a collection of *ALL* tasks";
		}
	}

	private void collectAllTasks() {
		workflowEngine.addAllTasks(tasks.values());
	}

	private void addReportTasks(final SwiftSearchDefinition searchDefinition) {
		if (searchDefinition.getPeptideReport() != null) {
			addScaffoldReportStep(searchDefinition);
		}
	}

	/**
	 * Will implement the search by creating all the tasks that are needed to run it.
	 *
	 * @param searchDefinition Search to run.
	 */
	private void searchDefinitionToTasks(final SwiftSearchDefinition searchDefinition) {
		// Now let us fill in all the lists
		File file = null;

		for (final FileSearch inputFile : searchDefinition.getInputFiles()) {
			file = inputFile.getInputFile();
			if (file.exists()) {
				inputFileToTasks(inputFile, searchDefinition.getSearchParameters(), Boolean.TRUE.equals(searchDefinition.getPublicSearchFiles()));
			} else {
				LOGGER.info("Skipping nonexistent input file [" + file.getAbsolutePath() + "]");
			}
		}
	}

	private boolean isQualityControlEnabled() {
		if (engines.getQuameterEngine() != null) {
			return getSearchDefinition().getSearchParameters().getEnabledEngines().isEnabled(engines.getQuameterEngine().getEngineConfig());
		} else {
			return false;
		}
	}

	/**
	 * Take an input file and create all the tasks that are processing it.
	 * That means going through all the search engines, all the file analysis (msmsEval), the raw dumping
	 * and the qa task.
	 *
	 * @param inputFile               File to process (e.g. .RAW, .mgf, .mzML, .d)
	 * @param defaultSearchParameters What parameters to use when searching, if the file does not specify otherwise.
	 * @param publicSearchFiles       True when the intermediate search files should be made public.
	 */
	void inputFileToTasks(final FileSearch inputFile, final SearchEngineParameters defaultSearchParameters, final boolean publicSearchFiles) {
		final SearchEngineParameters searchParameters = inputFile.getSearchParametersWithDefault(defaultSearchParameters);

		final FileProducingTask conversion = addRawConversionTask(inputFile, null, false);

		addInputAnalysis(inputFile, conversion);

		final SearchEngine scaffold = engines.getScaffoldEngine();
		final Curation database = searchParameters.getDatabase();

		final DatabaseDeployment scaffoldDeployment = addScaffoldDatabaseDeployment(inputFile, scaffold, database);

		final SearchEngine idpQonvert = engines.getIdpQonvertEngine();

		ScaffoldSpectrumTask scaffoldTask = null;

		// Go through all possible search engines this file requires
		for (final SearchEngine engine : engines.getEngines()) {
			// All 'normal' searches get normal entries
			// While building these, the Scaffold search entry itself is initialized
			// The IdpQonvert search is special as well, it is set up to process the results of the myrimatch search
			if (isNormalEngine(engine) && isEngineEnabled(engine, inputFile, defaultSearchParameters)) {
				final FileProducingTask search = addEngineSearchTask(engine, inputFile, conversion, searchParameters, publicSearchFiles, null);

				scaffoldTask = addScaffoldAndQaTasks(scaffoldTask, inputFile, conversion, scaffoldDeployment, engine, search);
				if (searchWithIdpQonvert() && myrimatch(engine)) {
					addIdpQonvertTask(idpQonvert, search, true);
				}
			}
		}


		SearchDbTask searchDbTask = null;
		if (searchDbDaemon != null && rawDumpDaemon != null && scaffoldTask != null) {
			// Ask for dumping the .RAW file since the QA might be disabled
			if (isRawFile(inputFile)) {
				final RAWDumpTask rawDumpTask = addRawDumpTask(inputFile.getInputFile(), QaTask.getQaSubdirectory(scaffoldTask.getScaffoldXmlFile()));
				searchDbTask = addSearchDbCall(scaffoldTask, rawDumpTask);
			}
		}

		if (isRawFile(inputFile) && isQualityControlEnabled()) {
			final FileProducingTask mzmlFile = addRawConversionTask(inputFile, MSCONVERT_MZML, true/* We need MS1 for Comet and IdpQonvert */);

			final SearchEngine comet = engines.getCometEngine();
			if (comet == null) {
				throw new MprcException("The Comet search engine must be available for Quality Control to function");
			}
			// Force Comet to produce .pep.xml output for idpqonvert
			final FileProducingTask cometSearch = addEngineSearchTask(comet, inputFile, mzmlFile, forceSemitrypsin(searchParameters, semitrypticQuameter), publicSearchFiles,
					CometWorker.PEP_XML);
			final IdpQonvertTask idpQonvertTask = addIdpQonvertTask(idpQonvert, cometSearch, false/* Do not publish the idpDB file, temp only*/);
			idpQonvertTask.setEmbedSpectrumScanTimes(true);
			idpQonvertTask.setMaxFDR(QUAMETER_FDR);
			addQuameterTask(engines.getQuameterEngine(), idpQonvertTask, mzmlFile, searchDbTask, scaffoldTask, inputFile, publicSearchFiles);
		}
	}

	private boolean isEngineEnabled(final SearchEngine engine, final FileSearch inputFile, final SearchEngineParameters defaultParameters) {
		if (inputFile.getSearchParameters() != null) {
			return inputFile.getSearchParameters().getEnabledEngines().isEnabled(engine.getEngineConfig());
		}
		return defaultParameters.getEnabledEngines().isEnabled(engine.getEngineConfig());

	}

	/**
	 * Create a version of input parameters that has semitryptic forced to given value.
	 *
	 * @param searchParameters Input parameters.
	 * @return Semi-tryptic version if we want one. Non-semi-tryptic version if we do NOT want one. This
	 * provides consistency in results no matter how the other parameters were set.
	 */
	private SearchEngineParameters forceSemitrypsin(final SearchEngineParameters searchParameters, final boolean semitrypticQuameter) {
		final SearchEngineParameters semiTrypticParameters = searchParameters.copyNullId();

		if (semitrypticQuameter) {
			semiTrypticParameters.setMinTerminiCleavages(1);
		} else {
			semiTrypticParameters.setMinTerminiCleavages(2);
		}
		return semiTrypticParameters;
	}

	private DatabaseDeployment addScaffoldDatabaseDeployment(final FileSearch inputFile, final SearchEngine scaffold, final Curation database) {
		if (scaffold != null && scaffoldVersion() != null) {
			return addDatabaseDeployment(scaffold, null/*scaffold has no param file*/, database);
		}
		return null;
	}

	private FileProducingTask addEngineSearchTask(final SearchEngine engine, final FileSearch inputFile,
	                                              final FileProducingTask convertedFile, final SearchEngineParameters searchParameters,
	                                              final boolean publicSearchFiles,
	                                              final String forceExtension) {

		// Get the parameter string
		final String param = parameterFiles.getParamString(engine, searchParameters);

		// Create a dump of the parameters on the disk
		final File paramFile = parameterFiles.saveParamFile(engine, param);

		final Curation database = searchParameters.getDatabase();

		DatabaseDeploymentResult deploymentResult = null;
		// Sequest deployment is counter-productive for particular input fasta file
		if (sequest(engine) && noSequestDeployment(searchParameters)) {
			deploymentResult = new NoSequestDeploymentResult(
					curationDao.findCuration(database.getShortName()).getCurationFile(),
					database);
		} else {
			if (engine.getDbDeployDaemon() != null) {
				deploymentResult = addDatabaseDeployment(engine, paramFile, database);
			}
		}
		final File outputFolder = getOutputFolderForSearchEngine(engine);

		final boolean cometSearch = comet(engine);

		final FileProducingTask converted;
		if (cometSearch) {
			// Comet is special. It requires a mzML file as input
			converted = addRawConversionTask(inputFile, MSCONVERT_MZML, false);
		} else {
			converted = convertedFile;
		}

		final EngineSearchTask engineSearchTask = addEngineSearch(engine,
				param,
				inputFile.getInputFile(),
				outputFolder,
				converted,
				database,
				deploymentResult,
				publicSearchFiles,
				forceExtension);

		if (cometSearch && forceExtension == null) {
			FileProducingTask ms2Task = addRawConversionTask(inputFile, MSCONVERT_MS2, false);
			final SqtMs2CombinerTask combinerTask = addTask(new SqtMs2CombinerTask(workflowEngine, engineSearchTask, ms2Task));
			combinerTask.addDependency(engineSearchTask);
			combinerTask.addDependency(ms2Task);
			return combinerTask;
		}

		return engineSearchTask;
	}

	private IdpQonvertTask addIdpQonvertTask(final SearchEngine idpQonvert, final FileProducingTask search, final boolean publishResult) {
		final IdpQonvertTask task = addTask(new IdpQonvertTask(workflowEngine, swiftDao, searchRun,
				getSearchDefinition(), idpQonvert.getSearchDaemon(),
				search, getOutputFolderForSearchEngine(idpQonvert), publishResult, fileTokenFactory, isFromScratch()));
		task.addDependency(search);
		return task;
	}

	private QuameterTask addQuameterTask(final SearchEngine quaMeter, final IdpQonvertTask search, final FileProducingTask rawFile,
	                                     final SearchDbTask searchDbTask, final ScaffoldTaskI scaffoldTask, final FileSearch fileSearch,
	                                     final boolean publicSearchFiles) {
		final QuameterTask task = addTask(
				new QuameterTask(workflowEngine,
						getSearchDefinition(), quaMeter.getSearchDaemon(),
						search, rawFile, getOutputFolderForSearchEngine(quaMeter), fileTokenFactory, isFromScratch(), publicSearchFiles)
		);
		task.addDependency(search);
		if (searchDbTask != null) {
			final QuameterDbTask dbTask = addTask(
					new QuameterDbTask(workflowEngine,
							quameterDbDaemon,
							fileTokenFactory,
							isFromScratch(),
							searchDbTask,
							task,
							scaffoldTask,
							fileSearch)
			);
			dbTask.addDependency(task);
			dbTask.addDependency(searchDbTask);
		}
		return task;
	}

	public boolean scaffoldShouldUseEngine(final SearchEngine engine, final String scaffoldVersion) {
		/* Scaffold cannot process myrimatch inputs correctly, fixed 4.3.2 */
		return !myrimatch(engine) || StringUtilities.compareVersions(scaffoldVersion, "4.3.2") >= 0;
	}

	private ScaffoldSpectrumTask addScaffoldAndQaTasks(final ScaffoldSpectrumTask previousScaffoldTask,
	                                                   final FileSearch inputFile, final FileProducingTask conversion,
	                                                   final DatabaseDeployment scaffoldDeployment,
	                                                   final SearchEngine engine, final FileProducingTask search) {
		ScaffoldSpectrumTask scaffoldTask = previousScaffoldTask;
		final String scaffoldVersion = scaffoldVersion();
		if (scaffoldVersion != null && scaffoldShouldUseEngine(engine, scaffoldVersion)) {
			if (scaffoldDeployment == null) {
				throw new MprcException("Scaffold search submitted without having Scaffold service enabled.");
			}

			scaffoldTask = addScaffoldCall(scaffoldVersion, inputFile, search, scaffoldDeployment);

			if (searchDefinition.getQa() != null) {
				addQaTask(inputFile, scaffoldTask, conversion);
			}
		}
		return scaffoldTask;
	}

	private String scaffoldVersion() {
		return getSearchDefinition().getEnabledEngines().getEngineVersion("SCAFFOLD");
	}

	private boolean searchWithIdpQonvert() {
		return getSearchDefinition().getEnabledEngines().isEnabled("IDPQONVERT");
	}

	private boolean sequest(final SearchEngine engine) {
		return "SEQUEST".equalsIgnoreCase(engine.getCode());
	}

	private boolean myrimatch(final SearchEngine engine) {
		return "MYRIMATCH".equalsIgnoreCase(engine.getCode());
	}

	private boolean comet(final SearchEngine engine) {
		return "COMET".equalsIgnoreCase(engine.getCode());
	}

	private boolean isNormalEngine(final SearchEngine engine) {
		return !engine.getEngineMetadata().isAggregator();
	}

	/**
	 * Adds steps to analyze the contents of the input file. This means spectrum QA (e.g. using msmsEval)
	 * as well as metadata extraction.
	 *
	 * @param inputFile      Input file to analyze.
	 * @param conversionTask Mgf of the input file.
	 */
	private void addInputAnalysis(final FileSearch inputFile, final FileProducingTask conversionTask) {
		// TODO: Extract metadata from the input file

		// Analyze spectrum quality if requested
		if (searchDefinition.getQa() != null && searchDefinition.getQa().getParamFilePath() != null) {
			addSpectrumQualityAnalysis(inputFile, conversionTask);
		}
	}

	private File getOutputFolderForSearchEngine(final SearchEngine engine) {
		return new File(searchDefinition.getOutputFolder(), engine.getOutputDirName());
	}

	/**
	 * @return {@code true} if the input file has Sequest deployment disabled.
	 */
	private boolean noSequestDeployment(final SearchEngineParameters parameters) {
		// Non-specific proteases (do not define restrictions for Rn-1 and Rn prevent sequest from deploying database index
		return "".equals(parameters.getProtease().getRn()) &&
				"".equals(parameters.getProtease().getRnminus1());
	}

	/**
	 * Add a process that converts a RAW file
	 * <ul>
	 * <li>If the file is a .RAW, we perform conversion either to .mgf or .mzML.</li>
	 * <li>If the file is already in .mgf format, instead of converting raw->mgf,
	 * we clean the mgf up, making sure the title contains expected information.</li>
	 * <li>Dtto for mzML</li>
	 * </ul>
	 *
	 * @param inputFile        file to convert.
	 * @param overrideSettings Conversion settings - if null, the default is used.
	 * @return Task capable of producing an mgf (either by conversion or by cleaning up an existing mgf).
	 */
	FileProducingTask addRawConversionTask(final FileSearch inputFile, final ExtractMsnSettings overrideSettings, final boolean includeMs1) {
		final File file = inputFile.getInputFile();

		FileProducingTask mgfOutput = null;
		// First, make sure we have a valid mgf, no matter what input we got
		final String extension = FileUtilities.getExtension(file.getName());
		if (MGF.equals(extension)) {
			mgfOutput = addMgfCleanupStep(inputFile);
		} else if (MZ_ML.equals(extension)) {
			mgfOutput = addMzMlCleanupStep(inputFile);
		} else {
			mgfOutput = addRawConversionStep(inputFile, getConversionSettings(inputFile, overrideSettings), includeMs1);
		}
		return mgfOutput;
	}

	private ExtractMsnSettings getConversionSettings(final FileSearch inputFile, final ExtractMsnSettings overrideSettings) {
		final ExtractMsnSettings conversionSettings;
		if (overrideSettings == null) {
			final SearchEngineParameters searchParameters = inputFile.getSearchParametersWithDefault(searchDefinition.getSearchParameters());
			conversionSettings = searchParameters.getExtractMsnSettings();
		} else {
			conversionSettings = overrideSettings;
		}
		return conversionSettings;
	}

	<T extends Task> T addTask(final T task) {
		final T existing = (T) tasks.get(task);
		if (existing == null) {
			tasks.put(task, task);
			return task;
		} else {
			// We already have a task that is equal to the new one
			// However, the new "equal" task can be set to do more work
			// Upgrade the original task to do all work that is needed
			existing.upgrade(task);
		}
		return existing;
	}

	/**
	 * @param inputFile          The file to convert
	 * @param conversionSettings How to convert the file
	 * @param includeMs1         Include MS1 spectra in the output
	 * @return A file that produces the converted output
	 */
	private FileProducingTask addRawConversionStep(final FileSearch inputFile, final ExtractMsnSettings conversionSettings, final boolean includeMs1) {
		final File file = inputFile.getInputFile();

		if (ExtractMsnSettings.EXTRACT_MSN.equals(conversionSettings.getCommand())) {

			final RawToMgfTask rawToMgfTask = new RawToMgfTask(workflowEngine,
						/*Input file*/ file,
						/*Mgf file location not known yet*/ null,
						/*raw2mgf command line*/ conversionSettings.getCommandLineSwitches(),
					Boolean.TRUE.equals(searchDefinition.getPublicMgfFiles()),
					raw2mgfDaemon, fileTokenFactory, isFromScratch());

			// Determine where would the mgf file ideally go (distinct files ensured)
			final File mgfFile = getMgfFileLocation(inputFile, rawToMgfTask);

			rawToMgfTask.setOutputFile(mgfFile);

			final RawToMgfTask task = addTask(rawToMgfTask);

			if (Boolean.TRUE.equals(searchDefinition.getPublicMzxmlFiles())) {
				throw new MprcException("Cannot provide .mzxml files with extract_msn. Please use msconvert");
			}

			return task;
		} else if (ExtractMsnSettings.MSCONVERT.equals(conversionSettings.getCommand())) {
			final MsconvertTask msconvertTask = new MsconvertTask(
					workflowEngine,
							/*Input file*/ file,
							/*Output file location not known, but must provide correct extension */ getOutputFile(inputFile, conversionSettings, null),
					includeMs1,
					Boolean.TRUE.equals(searchDefinition.getPublicMgfFiles()),
					msconvertDaemon, fileTokenFactory, isFromScratch());


			final File outputFile = getOutputFile(inputFile, conversionSettings, msconvertTask);
			msconvertTask.setOutputFile(outputFile);

			final MsconvertTask task = addTask(msconvertTask);

			if (Boolean.TRUE.equals(searchDefinition.getPublicMzxmlFiles())) {
				// We specifically want mzXML output
				final MsconvertTask mzxmlTask = new MsconvertTask(
						workflowEngine, file,
						new File("dummy.mzXML") /* we will patch it below */,
						true,
						false /* The mzXML output has no MS1 data */,
						msconvertDaemon, fileTokenFactory, isFromScratch());

				final File mzxmlFile = getMzxmlFileLocation(inputFile, mzxmlTask);

				mzxmlTask.setOutputFile(mzxmlFile);

				addTask(mzxmlTask);
			}

			return task;
		} else {
			throw new MprcException("Unsupported conversion engine: [" + conversionSettings.getCommand() + "]");
		}
	}

	private File getOutputFile(final FileSearch inputFile, final ExtractMsnSettings conversionSettings, final MsconvertTask msconvertTask) {
		final File outputFile;
		if (conversionSettings.isMzMlMode()) {
			outputFile = getMzMlFileLocation(inputFile, msconvertTask);
		} else if (conversionSettings.isMs2Mode()) {
			outputFile = getMs2FileLocation(inputFile, msconvertTask);
		} else if (conversionSettings.isMgfMode()) {
			outputFile = getMgfFileLocation(inputFile, msconvertTask);
		} else {
			throw new MprcException(String.format("Unsupported conversion settings %s", conversionSettings.getCommandLineSwitches()));
		}
		return outputFile;
	}

	/**
	 * We got a pre-made .mgf file. Because it can be problematic, we need to clean it up
	 */
	private FileProducingTask addMgfCleanupStep(final FileSearch inputFile) {
		final File file = inputFile.getInputFile();
		final MgfTitleCleanupTask cleanupTask = new MgfTitleCleanupTask(workflowEngine, mgfCleanupDaemon, file, null, fileTokenFactory, isFromScratch());

		final File outputFile = getMgfFileLocation(inputFile, cleanupTask);

		cleanupTask.setCleanedMgf(outputFile);

		return addTask(cleanupTask);
	}

	/**
	 * We got a pre-made .mzML file. Because it can be problematic, we need to clean it up
	 */
	private FileProducingTask addMzMlCleanupStep(final FileSearch inputFile) {
		return addTask(new MzMlCleanupTask(workflowEngine, inputFile.getInputFile(), fileTokenFactory));
	}

	/**
	 * Adds steps needed to analyze quality of the spectra. This can be done with a tool such as msmsEval or similar.
	 *
	 * @param inputFile      Input file
	 * @param conversionTask conversion for the input file
	 */
	private void addSpectrumQualityAnalysis(final FileSearch inputFile, final FileProducingTask conversionTask) {
		if (inputFile == null) {
			throw new MprcException("Bug: Input file must not be null");
		}
		final SpectrumQa spectrumQa = searchDefinition.getQa();

		if (spectrumQa == null) {
			throw new MprcException("Bug: The spectrum QA step must be enabled to be used");
		}
		// TODO: Check for spectrumQa.paramFile to be != null. Current code is kind of a hack.
		final File file = inputFile.getInputFile();

		final SpectrumQaTask spectrumQaTask = addTask(new SpectrumQaTask(workflowEngine,
				msmsEvalDaemon,
				conversionTask,
				spectrumQa.paramFile() == null ? null : spectrumQa.paramFile().getAbsoluteFile(),
				getSpectrumQaOutputFolder(inputFile),
				fileTokenFactory, isFromScratch()));

		spectrumQaTask.addDependency(conversionTask);

		spectrumQaTasks.put(getSpectrumQaHashKey(file), spectrumQaTask);
	}

	private void addScaffoldReportStep(final SwiftSearchDefinition searchDefinition) {
		if (scaffoldVersion() != null) {
			final List<File> scaffoldOutputFiles = new ArrayList<File>(scaffoldCalls.size());

			for (final ScaffoldTaskI scaffoldTask : scaffoldCalls) {
				scaffoldOutputFiles.add(scaffoldTask.getScaffoldSpectraFile());
			}

			final File peptideReportFile = new File(scaffoldOutputFiles.get(0).getParentFile(), "Swift Peptide Report For " + searchDefinition.getTitle() + ".xls");
			final File proteinReportFile = new File(scaffoldOutputFiles.get(0).getParentFile(), "Swift Protein Report For " + searchDefinition.getTitle() + ".xls");

			final ScaffoldReportTask scaffoldReportTask = addTask(
					new ScaffoldReportTask(workflowEngine, scaffoldReportDaemon, scaffoldOutputFiles,
							peptideReportFile, proteinReportFile, fileTokenFactory, isFromScratch())
			);

			for (final ScaffoldTaskI scaffoldTask : scaffoldCalls) {
				scaffoldReportTask.addDependency(scaffoldTask);
			}
		}
	}

	private boolean isFromScratch() {
		return fromScratch;
	}

	private void addQaTask(final FileSearch inputFile, final ScaffoldTaskI scaffoldTask, final FileProducingTask mgfOutput) {
		if (qaDaemon != null) {
			if (qaTask == null) {
				qaTask = addTask(new QaTask(workflowEngine, qaDaemon, fileTokenFactory, getSearchRun().getTitle(), isFromScratch()));
			}

			// Set up a new experiment dependency. All entries called from now on would be added under that experiment
			qaTask.addExperiment(scaffoldTask);
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
		return addTask(new RAWDumpTask(workflowEngine, rawFile, outputFolder, rawDumpDaemon, fileTokenFactory, isFromScratch()));
	}

	private static boolean isRawFile(final FileSearch inputFile) {
		final String name = inputFile.getInputFile().getName().toLowerCase(Locale.US);
		return name.endsWith(".raw") || name.endsWith(".d");
	}

	/**
	 * @param inputFile The input file entry from the search definition.
	 * @return Where does the output file go.
	 */
	private File getMgfFileLocation(final FileSearch inputFile, final FileProducingTask task) {
		return getOutputFileLocation(inputFile, "dta", ".mgf", task);
	}

	/**
	 * @param inputFile The input file entry from the search definition.
	 * @return Where does the output file go.
	 */
	private File getMzMlFileLocation(final FileSearch inputFile, final FileProducingTask task) {
		return getOutputFileLocation(inputFile, "mzml", ".mzML", task);
	}

	/**
	 * @param inputFile The input file entry from the search definition.
	 * @return Where does the output file go.
	 */
	private File getMs2FileLocation(final FileSearch inputFile, final FileProducingTask task) {
		return getOutputFileLocation(inputFile, "ms2", ".ms2", task);
	}


	/**
	 * @param inputFile The input file entry from the search definition.
	 * @return Where does the output file go.
	 */
	private File getMzxmlFileLocation(final FileSearch inputFile, final FileProducingTask task) {
		return getOutputFileLocation(inputFile, "mzxml", ".mzXML", task);
	}

	/**
	 * This will give location of an output file for given input one. When the task producing the output file is known,
	 * this is taken into consideration when attempting to make output file names distinct, Otherwise, when the task is set
	 * to null, we do not try to get a distinct filename.
	 */
	private File getOutputFileLocation(final FileSearch inputFile, final String folder, final String extension, final FileProducingTask task) {
		final File file = inputFile.getInputFile();
		final String outputDir = new File(
				new File(searchDefinition.getOutputFolder(), folder),
				getFileTitle(file)).getPath();
		final File outputFile = new File(outputDir, replaceFileExtension(file, extension).getName());
		if (task == null) {
			// We are just getting preliminary file output. Do not try to get a distinct path
			return outputFile;
		}
		// Make sure we never produce same file twice (for instance when we get two identical input mgf file names as input that differ only in the folder).
		// However, if the file to be produced is the result of an identical task, then it is okay to reuse the same name
		return distinctFiles.getDistinctFile(outputFile, task, extension);
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
		return distinctFiles.getDistinctFile(outputFolder, inputFile);
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
	private File getSearchResultLocation(final SearchEngine engine,
	                                     final File searchOutputFolder,
	                                     final File file,
	                                     final Task task,
	                                     final String forceExtension) {
		final String fileTitle = FileUtilities.stripExtension(file.getName());
		final String newFileName = fileTitle + (forceExtension == null ? engine.getResultExtension() : forceExtension);
		final File resultFile = new File(searchOutputFolder, newFileName);
		// Make sure we never produce two identical result files.
		// We suggest what the extension is, to prevent turning e.g. a.pep.xml into a.pep_2.xml instead of a_2.pep.xml
		return distinctFiles.getDistinctFile(resultFile, task, engine.getResultExtension());
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
		return addTask(new DatabaseDeployment(workflowEngine, engine.getCode(), engine.getFriendlyName(), engine.getDbDeployDaemon(), paramFile, dbToDeploy, fileTokenFactory, isFromScratch()));
	}

	/**
	 * Make a record for the search itself.
	 * The search depends on the engine, and the file to be searched.
	 * If these two things are identical for two entries, then the search can be performed just once.
	 * <p/>
	 * The search also knows about the conversion and db deployment so it can determine when it can run.
	 *
	 * @param forceExtension - extension to force (including the ".") - otherwise the default engine extension is used
	 *                       if null
	 */
	private EngineSearchTask addEngineSearch(final SearchEngine engine,
	                                         final String param,
	                                         final File inputFile,
	                                         final File searchOutputFolder,
	                                         final FileProducingTask convertedFile,
	                                         final Curation curation,
	                                         final DatabaseDeploymentResult deploymentResult,
	                                         final boolean publicSearchFiles,
	                                         final String forceExtension) {
		final EngineSearchTask searchEngineTask = new EngineSearchTask(
				workflowEngine,
				engine,
				inputFile.getName(),
				convertedFile,
				curation,
				deploymentResult,
				null,
				param,
				publicSearchFiles,
				engine.getSearchDaemon(),
				fileTokenFactory,
				isFromScratch());
		final File outputFile = getSearchResultLocation(engine, searchOutputFolder, inputFile, searchEngineTask, forceExtension);
		searchEngineTask.setOutputFile(outputFile);

		final EngineSearchTask search = addTask(searchEngineTask);

		// Depend on the .mgf to be done and on the database deployment
		search.addDependency(convertedFile);
		if (deploymentResult instanceof Task) {
			search.addDependency((Task) deploymentResult);
		}

		return search;
	}

	/**
	 * Add a scaffold 3 call (or update existing one) that depends on this input file to be sought through
	 * the given engine search.
	 */
	private ScaffoldSpectrumTask addScaffoldCall(final String scaffoldVersion, final FileSearch inputFile, final FileProducingTask search, final DatabaseDeployment scaffoldDbDeployment) {
		final String experiment = inputFile.getExperiment();
		final SearchEngine scaffoldEngine = engines.getScaffoldEngine();
		final File scaffoldUnimod = getScaffoldUnimod(scaffoldEngine);
		final File scaffoldOutputDir = getOutputFolderForSearchEngine(scaffoldEngine);
		final ScaffoldSpectrumTask scaffoldTask = addTask(new ScaffoldSpectrumTask(
				workflowEngine,
				scaffoldVersion,
				experiment,
				searchDefinition,
				scaffoldEngine.getSearchDaemon(),
				swiftDao, searchRun,
				scaffoldUnimod,
				scaffoldOutputDir,
				fileTokenFactory,
				reportDecoyHits,
				isFromScratch()));

		scaffoldTask.addInput(inputFile, search);
		scaffoldTask.addDatabase(scaffoldDbDeployment.getShortDbName(), scaffoldDbDeployment);
		scaffoldTask.addDependency(search);
		scaffoldTask.addDependency(scaffoldDbDeployment);

		scaffoldCalls.add(scaffoldTask);

		return scaffoldTask;
	}

	private File getScaffoldUnimod(final SearchEngine scaffoldEngine) {
		if (scaffoldEngine != null && scaffoldEngine.getConfig() != null && scaffoldEngine.getConfig().getWorker() != null &&
				scaffoldEngine.getConfig().getWorker().getRunner() != null && scaffoldEngine.getConfig().getWorker().getRunner().getWorkerConfiguration() != null) {
			final ResourceConfig workerConfiguration = scaffoldEngine.getConfig().getWorker().getRunner().getWorkerConfiguration();
			if (!(workerConfiguration instanceof ScaffoldWorker.Config)) {
				ExceptionUtilities.throwCastException(workerConfiguration, ScaffoldWorker.Config.class);
				return null;
			}
			final File scaffoldUnimod = new File(((ScaffoldWorker.Config) workerConfiguration).get(ScaffoldWorker.SCAFFOLD_UNIMOD));
			return scaffoldUnimod;
		}
		return null;
	}

	private FastaDbTask addFastaDbCall(final Curation curation) {
		if (fastaDbDaemon != null) {
			return addTask(new FastaDbTask(workflowEngine, fastaDbDaemon, fileTokenFactory, false, curation));
		}
		return null;
	}

	private SearchDbTask addSearchDbCall(final ScaffoldSpectrumTask scaffoldTask, final RAWDumpTask rawDumpTask) {
		final SearchDbTask searchDbTask = addTask(new SearchDbTask(workflowEngine, searchDbDaemon, fileTokenFactory, false, scaffoldTask));

		for (final FileSearch fileSearch : searchDefinition.getInputFiles()) {
			final FastaDbTask fastaDbTask = addFastaDbCall(fileSearch.getSearchParametersWithDefault(searchDefinition.getSearchParameters()).getDatabase());
			searchDbTask.addDependency(fastaDbTask);
		}

		searchDbTask.addDependency(scaffoldTask);
		// We depend on all raw dump tasks for loading metadata about the files
		searchDbTask.addRawDumpTask(rawDumpTask);
		searchDbTask.addDependency(rawDumpTask);
		return searchDbTask;
	}

	private static File getSpectrumQaHashKey(final File file) {
		return file.getAbsoluteFile();
	}

	public SwiftSearchDefinition getSearchDefinition() {
		return searchDefinition;
	}

	public void addSearchMonitor(final SearchMonitor monitor) {
		workflowEngine.addMonitor(monitor);
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void start() {
		if (!isRunning()) {
			DaemonUtilities.startDaemonConnections(
					raw2mgfDaemon,
					msconvertDaemon,
					mgfCleanupDaemon,
					rawDumpDaemon,
					msmsEvalDaemon,
					scaffoldReportDaemon,
					qaDaemon,
					fastaDbDaemon,
					searchDbDaemon);
			running = true;
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			running = false;
		}
	}

	private static final class MyResumer implements Resumer {
		private final SearchRunner runner;

		private MyResumer(final SearchRunner runner) {
			this.runner = runner;
		}

		@Override
		public void resume() {
			runner.service.execute(runner);
		}
	}
}
