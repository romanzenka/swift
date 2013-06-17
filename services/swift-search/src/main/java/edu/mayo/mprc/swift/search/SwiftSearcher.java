package edu.mayo.mprc.swift.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.PropertyChangeListener;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.config.ui.UiResponse;
import edu.mayo.mprc.daemon.*;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.database.DatabaseFactory;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.fastadb.FastaDbWorker;
import edu.mayo.mprc.mgf2mgf.MgfToMgfWorker;
import edu.mayo.mprc.msconvert.MsconvertCache;
import edu.mayo.mprc.msconvert.MsconvertWorker;
import edu.mayo.mprc.msmseval.MSMSEvalWorker;
import edu.mayo.mprc.msmseval.MsmsEvalCache;
import edu.mayo.mprc.qa.QaWorker;
import edu.mayo.mprc.qa.RAWDumpCache;
import edu.mayo.mprc.qa.RAWDumpWorker;
import edu.mayo.mprc.raw2mgf.RawToMgfCache;
import edu.mayo.mprc.raw2mgf.RawToMgfWorker;
import edu.mayo.mprc.scaffold.report.ScaffoldReportWorker;
import edu.mayo.mprc.searchdb.SearchDbWorker;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.swift.db.EngineFactoriesList;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.mapping.ParamsInfo;
import edu.mayo.mprc.swift.search.task.SearchRunner;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * Swift search daemon. Converts a given work packet into an instance of {@link SearchRunner} which holds
 * the state of the search and is responsible for the execution.
 */
public final class SwiftSearcher implements Worker {
	public static final String TYPE = "searcher";
	public static final String NAME = "Swift Searcher";
	public static final String DESC = "Runs the Swift search, orchestrating all the other modules.";

	public static final String ENGINE_PREFIX = "engine.";
	public static final String ENGINE_WORKER_SUFFIX = ".worker";
	public static final String ENGINE_DEPLOYER_SUFFIX = ".deployer";
	public static final String ENGINE_VERSION_SUFFIX = ".version";
	public static final String ENGINE_CODE_SUFFIX = ".code";

	private boolean raw2mgfEnabled = false;
	private boolean mgf2mgfEnabled = false;
	private boolean msconvertEnabled = false;
	private boolean rawdumpEnabled = false;
	private boolean msmsEvalEnabled = false;
	private boolean scaffoldReportEnabled = false;
	private boolean qaEnabled = false;
	private boolean dbLoadEnabled = false;

	private Collection<SearchEngine> supportedEngines = new HashSet<SearchEngine>();

	private DaemonConnection raw2mgfDaemon;
	private DaemonConnection msconvertDaemon;
	private DaemonConnection mgfCleanupDaemon;
	private DaemonConnection rawDumpDaemon;
	private DaemonConnection msmsEvalDaemon;
	private DaemonConnection scaffoldReportDaemon;
	private DaemonConnection qaDaemon;
	private DaemonConnection fastaDbDaemon;
	private DaemonConnection searchDbDaemon;
	private static final ExecutorService service = new SimpleThreadPoolExecutor(1, "swiftSearcher", false/* do not block*/);
	private boolean reportDecoyHits;

	private CurationDao curationDao;
	private SwiftDao swiftDao;
	private ParamsInfo paramsInfo;

	private static final String FASTA_PATH = "fastaPath";
	private static final String FASTA_ARCHIVE_PATH = "fastaArchivePath";
	private static final String FASTA_UPLOAD_PATH = "fastaUploadPath";
	private static final String RAW_2_MGF = "raw2mgf";
	private static final String MSCONVERT = "msconvert";
	private static final String MGF_2_MGF = "mgf2mgf";
	private static final String RAWDUMP = "rawdump";

	private static final String SCAFFOLD_REPORT = "scaffoldReport";
	private static final String QA = "qa";
	private static final String MSMS_EVAL = "msmsEval";
	private static final String DATABASE = "database";
	private static final String FASTA_DB = "fastaDb";
	private static final String SEARCH_DB = "searchDb";
	private static final String REPORT_DECOY_HITS = "reportDecoyHits";

	private FileTokenFactory fileTokenFactory;

	public SwiftSearcher(final CurationDao curationDao, final SwiftDao swiftDao, final FileTokenFactory fileTokenFactory) {
		// We execute the switch workflows in a single thread
		this.curationDao = curationDao;
		this.swiftDao = swiftDao;
		this.fileTokenFactory = fileTokenFactory;
	}

	public boolean isRaw2mgfEnabled() {
		return raw2mgfEnabled;
	}

	public void setRaw2mgfEnabled(final boolean raw2mgfEnabled) {
		this.raw2mgfEnabled = raw2mgfEnabled;
	}

	public boolean isMgf2mgfEnabled() {
		return mgf2mgfEnabled;
	}

	public void setMgf2mgfEnabled(final boolean mgf2mgfEnabled) {
		this.mgf2mgfEnabled = mgf2mgfEnabled;
	}

	public boolean isMsconvertEnabled() {
		return msconvertEnabled;
	}

	public void setMsconvertEnabled(final boolean msconvertEnabled) {
		this.msconvertEnabled = msconvertEnabled;
	}

	public boolean isRawdumpEnabled() {
		return rawdumpEnabled;
	}

	public void setRawdumpEnabled(final boolean rawdumpEnabled) {
		this.rawdumpEnabled = rawdumpEnabled;
	}

	public boolean isMsmsEvalEnabled() {
		return msmsEvalEnabled;
	}

	public void setMsmsEvalEnabled(final boolean msmsEvalEnabled) {
		this.msmsEvalEnabled = msmsEvalEnabled;
	}

	public boolean isScaffoldReportEnabled() {
		return scaffoldReportEnabled;
	}

	public void setScaffoldReportEnabled(final boolean scaffoldReportEnabled) {
		this.scaffoldReportEnabled = scaffoldReportEnabled;
	}

	public boolean isQaEnabled() {
		return qaEnabled;
	}

	public void setQaEnabled(final boolean qaEnabled) {
		this.qaEnabled = qaEnabled;
	}

	public boolean isDbLoadEnabled() {
		return dbLoadEnabled;
	}

	public void setDbLoadEnabled(final boolean dbLoadEnabled) {
		this.dbLoadEnabled = dbLoadEnabled;
	}

	public Collection<SearchEngine> getSupportedEngines() {
		return Collections.unmodifiableCollection(supportedEngines);
	}

	public void setSupportedEngines(final Collection<SearchEngine> supportedEngines) {
		this.supportedEngines = supportedEngines;
	}

	public ParamsInfo getParamsInfo() {
		return paramsInfo;
	}

	public void setParamsInfo(final ParamsInfo paramsInfo) {
		this.paramsInfo = paramsInfo;
	}

	/**
	 * When all engines are set, the support engines list is populated automatically.
	 *
	 * @param searchEngines List of all available search engines.
	 */
	public void setSearchEngines(final Collection<SearchEngine> searchEngines) {
		supportedEngines = new HashSet<SearchEngine>();
		for (final SearchEngine engine : searchEngines) {
			if (engine.isEnabled()) {
				supportedEngines.add(engine);
			}
		}
	}

	public DaemonConnection getRaw2mgfDaemon() {
		return raw2mgfDaemon;
	}

	public void setRaw2mgfDaemon(final DaemonConnection raw2mgfDaemon) {
		this.raw2mgfDaemon = raw2mgfDaemon;
	}

	public DaemonConnection getMsconvertDaemon() {
		return msconvertDaemon;
	}

	public void setMsconvertDaemon(final DaemonConnection msconvertDaemon) {
		this.msconvertDaemon = msconvertDaemon;
	}

	public DaemonConnection getMgfCleanupDaemon() {
		return mgfCleanupDaemon;
	}

	public void setMgfCleanupDaemon(final DaemonConnection mgfCleanupDaemon) {
		this.mgfCleanupDaemon = mgfCleanupDaemon;
	}

	public DaemonConnection getRawDumpDaemon() {
		return rawDumpDaemon;
	}

	public void setRawDumpDaemon(final DaemonConnection rawDumpDaemon) {
		this.rawDumpDaemon = rawDumpDaemon;
	}

	public void setMsmsEvalDaemon(final DaemonConnection msmsEvalDaemon) {
		this.msmsEvalDaemon = msmsEvalDaemon;
	}

	public DaemonConnection getMsmsEvalDaemon() {
		return msmsEvalDaemon;
	}

	public DaemonConnection getScaffoldReportDaemon() {
		return scaffoldReportDaemon;
	}

	public void setScaffoldReportDaemon(final DaemonConnection scaffoldReportDaemon) {
		this.scaffoldReportDaemon = scaffoldReportDaemon;
	}

	public DaemonConnection getQaDaemon() {
		return qaDaemon;
	}

	public void setQaDaemon(final DaemonConnection qaDaemon) {
		this.qaDaemon = qaDaemon;
	}

	public DaemonConnection getFastaDbDaemon() {
		return fastaDbDaemon;
	}

	public void setFastaDbDaemon(final DaemonConnection fastaDbDaemon) {
		this.fastaDbDaemon = fastaDbDaemon;
	}

	public DaemonConnection getSearchDbDaemon() {
		return searchDbDaemon;
	}

	public void setSearchDbDaemon(final DaemonConnection searchDbDaemon) {
		this.searchDbDaemon = searchDbDaemon;
	}

	public boolean isReportDecoyHits() {
		return reportDecoyHits;
	}

	public void setReportDecoyHits(final boolean reportDecoyHits) {
		this.reportDecoyHits = reportDecoyHits;
	}

	public void processRequest(final WorkPacket workPacket, final ProgressReporter progressReporter) {
		try {
			if (!(workPacket instanceof SwiftSearchWorkPacket)) {
				throw new DaemonException("Unknown request type: " + workPacket.getClass().getName());
			}

			final SwiftSearchWorkPacket swiftSearchWorkPacket = (SwiftSearchWorkPacket) workPacket;

			progressReporter.reportStart();

			final SearchRunner searchRunner = createSearchRunner(swiftSearchWorkPacket, progressReporter);

			// Run the search. The search is responsible for reporting success/failure on termination
			service.execute(searchRunner);
		} catch (Exception t) {
			progressReporter.reportFailure(t);
		}
	}

	@Override
	public void check() {
		if (supportedEngines != null) {
			throw new MprcException("Supported engines must not be null");
		}
		if (!(!raw2mgfEnabled || raw2mgfDaemon != null)) {
			throw new MprcException("Raw2mgf daemon must be set up if it is enabled");
		}
		if (!(!mgf2mgfEnabled || mgfCleanupDaemon != null)) {
			throw new MprcException("MgfCleanup daemon must be set up if it is enabled");
		}
		if (!(!msconvertEnabled || msconvertDaemon != null)) {
			throw new MprcException("Msconvert daemon must be set up if it is enabled");
		}
	}

	private SearchRunner createSearchRunner(final SwiftSearchWorkPacket swiftSearchWorkPacket, final ProgressReporter progressReporter) {
		swiftDao.begin();
		try {
			final SwiftSearchDefinition swiftSearchDefinition = swiftDao.getSwiftSearchDefinition(swiftSearchWorkPacket.getSwiftSearchId());
			final SearchRun searchRun = swiftDao.fillSearchRun(swiftSearchDefinition);

			final SearchRunner searchRunner = new SearchRunner(
					swiftSearchWorkPacket,
					swiftSearchDefinition,
					raw2mgfDaemon,
					msconvertDaemon,
					mgfCleanupDaemon,
					rawDumpDaemon,
					msmsEvalDaemon,
					scaffoldReportDaemon,
					qaDaemon,
					fastaDbDaemon,
					searchDbDaemon,
					supportedEngines,
					progressReporter,
					service,
					curationDao,
					swiftDao,
					fileTokenFactory,
					searchRun,
					reportDecoyHits,
					swiftSearchWorkPacket.getPriority(),
					paramsInfo);

			searchRunner.initialize();

			// Check whether we can actually do what they want us to do
			checkSearchCapabilities(searchRunner.getSearchDefinition());

			final PersistenceMonitor monitor = new PersistenceMonitor(searchRun.getId(), swiftDao);
			searchRunner.addSearchMonitor(monitor);

			reportNewSearchRunId(progressReporter, monitor.getSearchRunId());

			if (previousSearchRunning(swiftSearchWorkPacket)) {
				hidePreviousSearchRun(swiftSearchWorkPacket);
			}

			swiftDao.commit();
			return searchRunner;
		} catch (Exception t) {
			swiftDao.rollback();
			throw new MprcException("Could not load Swift search definition", t);
		}
	}

	/**
	 * When the search is started, the search run id created by the searchers is reported to the caller.
	 */
	private void reportNewSearchRunId(final ProgressReporter progressReporter, final int searchRunId) {
		progressReporter.reportProgress(new AssignedSearchRunId(searchRunId));
	}

	private boolean previousSearchRunning(final SwiftSearchWorkPacket swiftSearchWorkPacket) {
		return swiftSearchWorkPacket.getPreviousSearchRunId() > 0;
	}

	private void hidePreviousSearchRun(final SwiftSearchWorkPacket swiftSearchWorkPacket) {
		final SearchRun searchRun = swiftDao.getSearchRunForId(swiftSearchWorkPacket.getPreviousSearchRunId());
		searchRun.setHidden(1);
	}

	/**
	 * Makes sure that the search can be actually performed using our runtime.
	 * Throw an execption if it cannot possibly perform given the search.
	 *
	 * @param definition Search definition.
	 */
	private void checkSearchCapabilities(final SwiftSearchDefinition definition) {
		boolean raw2mgfProblem = false;
		try {
			final Set<SearchEngine> problematicEngines = new HashSet<SearchEngine>();
			for (final FileSearch inputFile : definition.getInputFiles()) {
				if (!inputFile.getInputFile().getName().endsWith(".mgf") && !(this.raw2mgfEnabled || this.msconvertEnabled)) {
					raw2mgfProblem = true;
				}

				for (final SearchEngine engine : supportedEngines) {
					if (inputFile.isSearch(engine.getCode()) && !engine.isEnabled()) {
						problematicEngines.add(engine);
					}
				}

			}

			final StringBuilder errorMessage = new StringBuilder();
			if (raw2mgfProblem) {
				errorMessage.append("RAW->MGF conversion, ");
			}
			if (definition.getQa() != null && !isMsmsEvalEnabled()) {
				errorMessage.append("msmsEval, ");
			}
			appendEngines(problematicEngines, errorMessage);

			if (errorMessage.length() > 2) {
				errorMessage.setLength(errorMessage.length() - 2);
				throw new DaemonException("Search cannot be performed, we lack following capabilities: " + errorMessage.toString());
			}
		} catch (MprcException e) {
			throw new DaemonException(e);
		}
	}

	private static void appendEngines(final Collection<SearchEngine> engineSet, final StringBuilder builder) {
		for (final SearchEngine e : engineSet) {
			builder.append(e.getFriendlyName()).append(", ");
		}
	}

	public String toString() {
		final StringBuilder result = new StringBuilder(NAME).append(" capable of running ");
		if (raw2mgfEnabled || msconvertEnabled) {
			result.append("Raw->MGF");
		}
		appendEngines(supportedEngines, result);
		return result.toString();
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("swiftSearcherFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		private CurationDao curationDao;
		private SwiftDao swiftDao;
		private FileTokenFactory fileTokenFactory;
		private DatabaseValidator databaseValidator;
		private ParamsInfo paramsInfo;
		private EngineFactoriesList engineFactoriesList;

		@Override
		public ServiceUiFactory getServiceUiFactory() {
			return new Ui(getDatabaseValidator(), getEngineFactoriesList());
		}

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final SwiftSearcher worker = new SwiftSearcher(getCurationDao(), getSwiftDao(), getFileTokenFactory());
			worker.setParamsInfo(paramsInfo);

			final List<SearchEngine> connectedSearchEngines = new ArrayList<SearchEngine>();
			for (final SearchEngine.Config engineConfig : config.getEngines()) {
				if (engineConfig.getWorker() != null && engineConfig.getDeployer() != null) {
					final SearchEngine engine = (SearchEngine) dependencies.createSingleton(engineConfig);
					connectedSearchEngines.add(engine);
				}

			}
			worker.setSearchEngines(connectedSearchEngines);
			if (config.raw2mgf != null) {
				worker.setRaw2mgfDaemon((DaemonConnection) dependencies.createSingleton(config.raw2mgf));
				worker.setRaw2mgfEnabled(true);
			}
			if (config.msconvert != null) {
				worker.setMsconvertDaemon((DaemonConnection) dependencies.createSingleton(config.msconvert));
				worker.setMsconvertEnabled(true);
			}
			if (config.mgf2mgf != null) {
				worker.setMgfCleanupDaemon((DaemonConnection) dependencies.createSingleton(config.mgf2mgf));
				worker.setMgf2mgfEnabled(true);
			}
			if (config.rawdump != null) {
				worker.setRawDumpDaemon((DaemonConnection) dependencies.createSingleton(config.rawdump));
				worker.setRawdumpEnabled(true);
			}
			if (config.msmsEval != null) {
				worker.setMsmsEvalDaemon((DaemonConnection) dependencies.createSingleton(config.msmsEval));
				worker.setMsmsEvalEnabled(true);
			}
			if (config.scaffoldReport != null) {
				worker.setScaffoldReportDaemon((DaemonConnection) dependencies.createSingleton(config.scaffoldReport));
				worker.setScaffoldReportEnabled(true);
			}
			if (config.qa != null) {
				worker.setQaDaemon((DaemonConnection) dependencies.createSingleton(config.qa));
				worker.setQaEnabled(true);
			}
			if (config.fastaDb != null) {
				worker.setFastaDbDaemon((DaemonConnection) dependencies.createSingleton(config.fastaDb));
			}
			if (config.searchDb != null) {
				worker.setSearchDbDaemon((DaemonConnection) dependencies.createSingleton(config.searchDb));
			}
			if (config.fastaDb != null && config.searchDb != null) {
				worker.setDbLoadEnabled(true);
			}

			worker.setReportDecoyHits(config.reportDecoyHits);

			return worker;
		}

		public CurationDao getCurationDao() {
			return curationDao;
		}

		@Resource(name = "curationDao")
		public void setCurationDao(final CurationDao curationDao) {
			this.curationDao = curationDao;
		}

		public SwiftDao getSwiftDao() {
			return swiftDao;
		}

		@Resource(name = "swiftDao")
		public void setSwiftDao(final SwiftDao swiftDao) {
			this.swiftDao = swiftDao;
		}

		public FileTokenFactory getFileTokenFactory() {
			return fileTokenFactory;
		}

		@Resource(name = "fileTokenFactory")
		public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
			this.fileTokenFactory = fileTokenFactory;
		}

		public DatabaseValidator getDatabaseValidator() {
			return databaseValidator;
		}

		@Resource(name = "databaseValidator")
		public void setDatabaseValidator(final DatabaseValidator databaseValidator) {
			this.databaseValidator = databaseValidator;
		}

		public ParamsInfo getParamsInfo() {
			return paramsInfo;
		}

		@Resource(name = "paramsInfo")
		public void setParamsInfo(final ParamsInfo paramsInfo) {
			this.paramsInfo = paramsInfo;
		}

		public EngineFactoriesList getEngineFactoriesList() {
			return engineFactoriesList;
		}

		@Resource(name = "engineFactoriesList")
		public void setEngineFactoriesList(EngineFactoriesList engineFactoriesList) {
			this.engineFactoriesList = engineFactoriesList;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private String fastaPath;
		private String fastaArchivePath;
		private String fastaUploadPath;
		private boolean reportDecoyHits;

		private ServiceConfig raw2mgf;
		private ServiceConfig msconvert;
		private ServiceConfig mgf2mgf;
		private ServiceConfig rawdump;

		private Collection<SearchEngine.Config> engines;

		private ServiceConfig scaffoldReport;

		private ServiceConfig qa;
		private ServiceConfig fastaDb;
		private ServiceConfig searchDb;
		private ServiceConfig msmsEval;
		private DatabaseFactory.Config database;

		public Config() {
			engines = new ArrayList<SearchEngine.Config>(10);
		}

		public Config(final String fastaPath, final String fastaArchivePath, final String fastaUploadPath
				, final ServiceConfig raw2mgf, final ServiceConfig msconvert
				, final ServiceConfig mgf2mgf, final ServiceConfig rawdump
				, final Collection<SearchEngine.Config> engines
				, final ServiceConfig scaffoldReport, final ServiceConfig qa
				, final ServiceConfig fastaDb, final ServiceConfig searchDb
				, final ServiceConfig msmsEval
				, final DatabaseFactory.Config database) {
			this.fastaPath = fastaPath;
			this.fastaArchivePath = fastaArchivePath;
			this.fastaUploadPath = fastaUploadPath;
			this.raw2mgf = raw2mgf;
			this.msconvert = msconvert;
			this.mgf2mgf = mgf2mgf;
			this.rawdump = rawdump;
			this.engines = engines;
			this.scaffoldReport = scaffoldReport;
			this.qa = qa;
			this.fastaDb = fastaDb;
			this.searchDb = searchDb;
			this.msmsEval = msmsEval;
			this.database = database;
			this.reportDecoyHits = true;
		}

		public ServiceConfig getMsmsEval() {
			return msmsEval;
		}

		public String getFastaPath() {
			return fastaPath;
		}

		public String getFastaArchivePath() {
			return fastaArchivePath;
		}

		public String getFastaUploadPath() {
			return fastaUploadPath;
		}

		public ServiceConfig getRaw2mgf() {
			return raw2mgf;
		}

		public ServiceConfig getMsconvert() {
			return msconvert;
		}

		public ServiceConfig getMgf2mgf() {
			return mgf2mgf;
		}

		public ServiceConfig getRawdump() {
			return rawdump;
		}

		public ServiceConfig getScaffoldReport() {
			return scaffoldReport;
		}

		public ServiceConfig getQa() {
			return qa;
		}

		public ServiceConfig getFastaDb() {
			return fastaDb;
		}

		public ServiceConfig getSearchDb() {
			return searchDb;
		}

		public void setDatabase(DatabaseFactory.Config database) {
			this.database = database;
		}

		public DatabaseFactory.Config getDatabase() {
			return database;
		}

		public boolean isReportDecoyHits() {
			return reportDecoyHits;
		}

		public Collection<SearchEngine.Config> getEngines() {
			return engines;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(FASTA_PATH, getFastaPath());
			writer.put(FASTA_ARCHIVE_PATH, getFastaArchivePath());
			writer.put(FASTA_UPLOAD_PATH, getFastaUploadPath());
			writer.put(RAW_2_MGF, writer.save(getRaw2mgf()));
			writer.put(MSCONVERT, writer.save(getMsconvert()));
			writer.put(MGF_2_MGF, writer.save(getMgf2mgf()));
			writer.put(RAWDUMP, writer.save(getRawdump()));

			int i = 0;
			for (final SearchEngine.Config engineConfig : getEngines()) {
				i++;
				writer.put(ENGINE_PREFIX + i + ENGINE_CODE_SUFFIX, engineConfig.getCode());
				writer.put(ENGINE_PREFIX + i + ENGINE_VERSION_SUFFIX, engineConfig.getVersion());
				writer.put(ENGINE_PREFIX + i + ENGINE_WORKER_SUFFIX, writer.save(engineConfig.getWorker()));
				writer.put(ENGINE_PREFIX + i + ENGINE_DEPLOYER_SUFFIX, writer.save(engineConfig.getDeployer()));
			}

			writer.put(SCAFFOLD_REPORT, writer.save(getScaffoldReport()));
			writer.put(QA, writer.save(getQa()));
			writer.put(FASTA_DB, writer.save(getFastaDb()));
			writer.put(SEARCH_DB, writer.save(getSearchDb()));
			writer.put(MSMS_EVAL, writer.save(getMsmsEval()));
			writer.put(DATABASE, writer.save(getDatabase()));
			writer.put(REPORT_DECOY_HITS, isReportDecoyHits());
		}

		public void load(final ConfigReader reader) {
			fastaPath = reader.get(FASTA_PATH);
			fastaArchivePath = reader.get(FASTA_ARCHIVE_PATH);
			fastaUploadPath = reader.get(FASTA_UPLOAD_PATH);
			raw2mgf = (ServiceConfig) reader.getObject(RAW_2_MGF);
			msconvert = (ServiceConfig) reader.getObject(MSCONVERT);
			mgf2mgf = (ServiceConfig) reader.getObject(MGF_2_MGF);
			rawdump = (ServiceConfig) reader.getObject(RAWDUMP);

			final Map<Integer, SearchEngine.Config> engineConfigs = Maps.newTreeMap();
			for (final String key : reader.getKeys()) {
				if (key.startsWith(ENGINE_PREFIX) &&
						key.endsWith(ENGINE_CODE_SUFFIX)) {
					setupEngine(key.substring(
							ENGINE_PREFIX.length(), key.length() - ENGINE_CODE_SUFFIX.length()), engineConfigs, reader);
				}
			}
			engines = engineConfigs.values();

			scaffoldReport = (ServiceConfig) reader.getObject(SCAFFOLD_REPORT);
			qa = (ServiceConfig) reader.getObject(QA);
			fastaDb = (ServiceConfig) reader.getObject(FASTA_DB);
			searchDb = (ServiceConfig) reader.getObject(SEARCH_DB);
			msmsEval = (ServiceConfig) reader.getObject(MSMS_EVAL);
			database = (DatabaseFactory.Config) reader.getObject(DATABASE);
			reportDecoyHits = reader.getBoolean(REPORT_DECOY_HITS);
		}

		private void setupEngine(final String number, final Map<Integer, SearchEngine.Config> engineConfigs, ConfigReader reader) {
			int engineNumber = Integer.parseInt(number);
			final String code = reader.get(ENGINE_PREFIX + number + ENGINE_CODE_SUFFIX);
			final ServiceConfig worker = (ServiceConfig) reader.getObject(ENGINE_PREFIX + number + ENGINE_WORKER_SUFFIX);
			final ServiceConfig deployer = (ServiceConfig) reader.getObject(ENGINE_PREFIX + number + ENGINE_DEPLOYER_SUFFIX);
			final String version = reader.get(ENGINE_PREFIX + number + ENGINE_VERSION_SUFFIX);

			engineConfigs.put(engineNumber, new SearchEngine.Config(code, version, worker, deployer));
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {

		private DatabaseValidator validator;

		private EngineFactoriesList engineFactoriesList;

		public Ui(final DatabaseValidator validator, final EngineFactoriesList engineFactoriesList) {
			this.validator = validator;
			this.engineFactoriesList = engineFactoriesList;
		}

		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			final DatabaseFactory.Config database = (DatabaseFactory.Config) daemon.firstResourceOfType(DatabaseFactory.Config.class);

			builder
					.property(FASTA_PATH, "FASTA Database Path", "When Swift filters a database, the results go here.<p>" +
							"Back this folder up, although it should be possible to recreate its contents manually, providing you keep the source databases.</p>")
					.required()
					.existingDirectory().defaultValue("var/fasta")

					.property(FASTA_ARCHIVE_PATH, "FASTA Archive Path", "Original downloaded databases (like SwissProt) go here.<br/>This folder should be carefully backed up, it allows you to go back and redo old searches with their original databases.")
					.required()
					.existingDirectory().defaultValue("var/dbcurator/archive")

					.property(FASTA_UPLOAD_PATH, "FASTA Upload Path", "User uploaded databases go here. This should be backed up, just like the archive path, but is usually less critical, since the users usually keep the uploaded databases also on their disk.").existingDirectory()
					.required()
					.existingDirectory().defaultValue("var/dbcurator/uploads")

					.property(DATABASE, "Swift Database",
							"<b>Important!</b> Make sure to test the database before running Swift. If the database does not exist, the test will let you set it up.")
					.required()
					.validateOnDemand(new PropertyChangeListener() {
						@Override
						public void propertyChanged(final ResourceConfig config, final String propertyName, final String newValue, final UiResponse response, final boolean validationRequested) {
							if (validationRequested && (config instanceof Config)) {
								final Config searcher = (Config) config;
								validator.setSearcherConfig(searcher);
								validator.setDaemonConfig(daemon);
								final String error = validator.check(new HashMap<String, String>(0));
								if (error != null) {
									response.displayPropertyError(config, DATABASE, error);
								}
							}
						}

						@Override
						public void fixError(final ResourceConfig config, final String propertyName, final String action) {
							if (!(config instanceof Config)) {
								ExceptionUtilities.throwCastException(config, Config.class);
								return;
							}
							final Config searcher = (Config) config;
							validator.setSearcherConfig(searcher);
							validator.setDaemonConfig(daemon);
							validator.initialize(new ImmutableMap.Builder<String, String>()
									.put("action", action)
									.build());
						}
					})
					.reference(DatabaseFactory.TYPE, UiBuilder.NONE_TYPE)
					.defaultValue(database)

					.property(REPORT_DECOY_HITS, "Report Decoy Hits",
							"<p>When checked, Scaffold will utilize the accession number patterns to distinguish decoy from forward hits.<p>" +
									"<p>This causes FDR rates to be calculated using the number of decoy hits. Scaffold will also display the reverse hits in pink.</p>")
					.boolValue()
					.defaultValue(Boolean.toString(Boolean.TRUE))

					.property(RAW_2_MGF, RawToMgfWorker.NAME, "Search Thermo's .RAW files by converting them to .mgf automatically with this module. Requires <tt>extract_msn</tt> running either on a Windows machine or on a linux box through wine.")
					.reference(RawToMgfWorker.TYPE, RawToMgfCache.TYPE, UiBuilder.NONE_TYPE)

					.property(MSCONVERT, MsconvertWorker.NAME, "Search Thermo's .RAW files by converting them to .mgf using ProteoWizard's msconvert. Requires <tt>msconvert</tt> running either on a Windows machine or on a linux box through wine.")
					.reference(MsconvertWorker.TYPE, MsconvertCache.TYPE, UiBuilder.NONE_TYPE)

					.property(MGF_2_MGF, MgfToMgfWorker.NAME, "Search .mgf files directly. This module cleans up the .mgf headers so they can be used by Scaffold when merging search engine results.")
					.reference(MgfToMgfWorker.TYPE, UiBuilder.NONE_TYPE)

					.property(RAWDUMP, RAWDumpWorker.NAME, "Extracts information about experiment and spectra from RAW files.")
					.reference(RAWDumpWorker.TYPE, RAWDumpCache.TYPE, UiBuilder.NONE_TYPE);

			// Engines here
			int i = 0;
			for (final EngineMetadata metadata : engineFactoriesList.getEngineMetadata()) {
				i++;
				builder
						.property(ENGINE_PREFIX + i + ENGINE_CODE_SUFFIX, "Engine #" + i + " code", "Temporary, to be removed with better UI")
						.defaultValue(metadata.getCode())
						.required()

						.property(ENGINE_PREFIX + i + ENGINE_VERSION_SUFFIX, "Engine #" + i + " version", "Put in the version of the particular engine you are using")
						.required()

						.property(ENGINE_PREFIX + i + ENGINE_WORKER_SUFFIX, "Engine #" + i + " worker", "The service that actually does the work of this engine")
						.reference(ObjectArrays.concat(
								ObjectArrays.concat(metadata.getWorkerTypes(), metadata.getCacheTypes(), String.class), UiBuilder.NONE_TYPE))

						.property(ENGINE_PREFIX + i + ENGINE_DEPLOYER_SUFFIX, "Engine #" + i + " deployer", "The service that prepares the environment for this engine to work efficiently")
						.reference(ObjectArrays.concat(metadata.getDeployerTypes(), UiBuilder.NONE_TYPE));
			}

			builder
					.property(MSMS_EVAL, MSMSEvalWorker.NAME, "Run msmsEval on the spectra to determine their quality. Results obtained from this module are used in the QA graphs. Eventually we could utilize spectrum quality information to optimize Peaks Online.")
					.reference(MSMSEvalWorker.TYPE, MsmsEvalCache.TYPE, UiBuilder.NONE_TYPE)

					.property(SCAFFOLD_REPORT, ScaffoldReportWorker.NAME, "A specialized tool for MPRC - produces a condensed spreadsheet with Scaffold output. Requires Scaffold Batch version 2.3 or later.")
					.reference(ScaffoldReportWorker.TYPE, UiBuilder.NONE_TYPE)

					.property(QA, QaWorker.NAME, "Generate statistics about the input files and search performance.")
					.reference(QaWorker.TYPE, UiBuilder.NONE_TYPE)

					.property(FASTA_DB, FastaDbWorker.NAME, "Load FASTA entries into a database.<p>This is required in order to load Scaffold search results into a database (see below)</p>")
					.reference(FastaDbWorker.TYPE, UiBuilder.NONE_TYPE)

					.property(SEARCH_DB, SearchDbWorker.NAME, "Load Scaffold search results into a database")
					.reference(SearchDbWorker.TYPE, UiBuilder.NONE_TYPE);
		}
	}
}