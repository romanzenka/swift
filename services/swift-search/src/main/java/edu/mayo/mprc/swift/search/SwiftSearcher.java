package edu.mayo.mprc.swift.search;

import com.google.common.collect.ImmutableMap;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ServiceConfig;
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
import edu.mayo.mprc.idpicker.IdpickerCache;
import edu.mayo.mprc.idpicker.IdpickerDeploymentService;
import edu.mayo.mprc.idpicker.IdpickerWorker;
import edu.mayo.mprc.mascot.MascotCache;
import edu.mayo.mprc.mascot.MascotDeploymentService;
import edu.mayo.mprc.mascot.MascotWorker;
import edu.mayo.mprc.mascot.MockMascotDeploymentService;
import edu.mayo.mprc.mgf2mgf.MgfToMgfWorker;
import edu.mayo.mprc.msconvert.MsconvertCache;
import edu.mayo.mprc.msconvert.MsconvertWorker;
import edu.mayo.mprc.msmseval.MSMSEvalWorker;
import edu.mayo.mprc.msmseval.MsmsEvalCache;
import edu.mayo.mprc.myrimatch.MyrimatchCache;
import edu.mayo.mprc.myrimatch.MyrimatchDeploymentService;
import edu.mayo.mprc.myrimatch.MyrimatchWorker;
import edu.mayo.mprc.omssa.OmssaCache;
import edu.mayo.mprc.omssa.OmssaDeploymentService;
import edu.mayo.mprc.omssa.OmssaWorker;
import edu.mayo.mprc.qa.QaWorker;
import edu.mayo.mprc.qa.RAWDumpCache;
import edu.mayo.mprc.qa.RAWDumpWorker;
import edu.mayo.mprc.raw2mgf.RawToMgfCache;
import edu.mayo.mprc.raw2mgf.RawToMgfWorker;
import edu.mayo.mprc.scaffold.ScaffoldDeploymentService;
import edu.mayo.mprc.scaffold.ScaffoldWorker;
import edu.mayo.mprc.scaffold.report.ScaffoldReportWorker;
import edu.mayo.mprc.scaffold3.Scaffold3DeploymentService;
import edu.mayo.mprc.scaffold3.Scaffold3Worker;
import edu.mayo.mprc.searchdb.SearchDbWorker;
import edu.mayo.mprc.sequest.SequestCache;
import edu.mayo.mprc.sequest.SequestDeploymentService;
import edu.mayo.mprc.sequest.SequestWorker;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.search.task.SearchRunner;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import edu.mayo.mprc.xtandem.XTandemCache;
import edu.mayo.mprc.xtandem.XTandemDeploymentService;
import edu.mayo.mprc.xtandem.XTandemWorker;

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
	private Collection<SearchEngine> searchEngines;
	private static final ExecutorService service = new SimpleThreadPoolExecutor(1, "swiftSearcher", false/* do not block*/);
	private boolean reportDecoyHits;

	private CurationDao curationDao;
	private SwiftDao swiftDao;

	private static final String FASTA_PATH = "fastaPath";
	private static final String FASTA_ARCHIVE_PATH = "fastaArchivePath";
	private static final String FASTA_UPLOAD_PATH = "fastaUploadPath";
	private static final String RAW_2_MGF = "raw2mgf";
	private static final String MSCONVERT = "msconvert";
	private static final String MGF_2_MGF = "mgf2mgf";
	private static final String RAWDUMP = "rawdump";
	private static final String MASCOT = "mascot";
	private static final String MASCOT_DEPLOYER = "mascotDeployer";
	private static final String SEQUEST = "sequest";
	private static final String SEQUEST_DEPLOYER = "sequestDeployer";
	private static final String TANDEM = "tandem";
	private static final String TANDEM_DEPLOYER = "tandemDeployer";
	private static final String OMSSA = "omssa";
	private static final String OMSSA_DEPLOYER = "omssaDeployer";
	private static final String MYRIMATCH = "myrimatch";
	private static final String MYRIMATCH_DEPLOYER = "myrimatchDeployer";
	private static final String IDPICKER = "idpicker";
	private static final String IDPICKER_DEPLOYER = "idpickerDeployer";
	private static final String SCAFFOLD = "scaffold";
	private static final String SCAFFOLD_DEPLOYER = "scaffoldDeployer";
	private static final String SCAFFOLD3 = "scaffold3";
	private static final String SCAFFOLD3_DEPLOYER = "scaffold3Deployer";
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

	public void setMsconvertEnabled(boolean msconvertEnabled) {
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

	public Collection<SearchEngine> getSearchEngines() {
		return searchEngines;
	}

	/**
	 * When all engines are set, the support engines list is populated automatically.
	 *
	 * @param searchEngines List of all available search engines.
	 */
	public void setSearchEngines(final Collection<SearchEngine> searchEngines) {
		this.searchEngines = searchEngines;
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

	public void setMsconvertDaemon(DaemonConnection msconvertDaemon) {
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

	public void setReportDecoyHits(boolean reportDecoyHits) {
		this.reportDecoyHits = reportDecoyHits;
	}

	public void assertValid() {
		assert supportedEngines != null : "Supported engines must not be null";
		assert !raw2mgfEnabled || raw2mgfDaemon != null : "Raw2mgf daemon must be set up if it is enabled";
		assert !mgf2mgfEnabled || mgfCleanupDaemon != null : "MgfCleanup daemon must be set up if it is enabled";
		assert !msconvertEnabled || msconvertDaemon != null : "Msconvert daemon must be set up if it is enabled";
	}

	public void processRequest(final WorkPacket workPacket, final ProgressReporter progressReporter) {
		try {
			assertValid();
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
					swiftSearchWorkPacket.getPriority());

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
	public static final class Factory extends WorkerFactoryBase<Config> {
		private Collection<SearchEngine> searchEngines;
		private CurationDao curationDao;
		private SwiftDao swiftDao;
		private FileTokenFactory fileTokenFactory;

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final SwiftSearcher worker = new SwiftSearcher(curationDao, swiftDao, fileTokenFactory);

			// Fill the search engine list with daemon connections, if we have both the deployer and the searcher defined
			final List<SearchEngine> connectedSearchEngines = new ArrayList<SearchEngine>();
			for (final SearchEngine engine : searchEngines) {
				fillEngineDaemons(engine, connectedSearchEngines, "MASCOT", config.mascot, config.mascotDeployer, dependencies);
				fillEngineDaemons(engine, connectedSearchEngines, "SEQUEST", config.sequest, config.sequestDeployer, dependencies);
				fillEngineDaemons(engine, connectedSearchEngines, "TANDEM", config.tandem, config.tandemDeployer, dependencies);
				fillEngineDaemons(engine, connectedSearchEngines, "OMSSA", config.omssa, config.omssaDeployer, dependencies);
				fillEngineDaemons(engine, connectedSearchEngines, "MYRIMATCH", config.myrimatch, config.myrimatchDeployer, dependencies);
				fillEngineDaemons(engine, connectedSearchEngines, "IDPICKER", config.idpicker, config.idpickerDeployer, dependencies);
				fillEngineDaemons(engine, connectedSearchEngines, "SCAFFOLD", config.scaffold, config.scaffoldDeployer, dependencies);
				fillEngineDaemons(engine, connectedSearchEngines, "SCAFFOLD3", config.scaffold3, config.scaffold3Deployer, dependencies);
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

		private void fillEngineDaemons(final SearchEngine engineToFill, final List<SearchEngine> filledList, final String engineCode, final ServiceConfig daemonConfig, final ServiceConfig dbDeployerConfig, final DependencyResolver dependencies) {
			if (engineCode.equals(engineToFill.getCode()) && daemonConfig != null && dbDeployerConfig != null) {
				SearchEngine clone = null;
				try {
					clone = (SearchEngine) engineToFill.clone();
				} catch (CloneNotSupportedException e) {
					throw new MprcException("Cannot clone search engine " + engineCode, e);
				}
				clone.setSearchDaemon((DaemonConnection) dependencies.createSingleton(daemonConfig));
				clone.setDbDeployDaemon((DaemonConnection) dependencies.createSingleton(dbDeployerConfig));
				filledList.add(clone);
			}
		}

		public Collection<SearchEngine> getSearchEngines() {
			return searchEngines;
		}

		public void setSearchEngines(final Collection<SearchEngine> searchEngines) {
			this.searchEngines = searchEngines;
		}

		public CurationDao getCurationDao() {
			return curationDao;
		}

		public void setCurationDao(final CurationDao curationDao) {
			this.curationDao = curationDao;
		}

		public SwiftDao getSwiftDao() {
			return swiftDao;
		}

		public void setSwiftDao(final SwiftDao swiftDao) {
			this.swiftDao = swiftDao;
		}

		public FileTokenFactory getFileTokenFactory() {
			return fileTokenFactory;
		}

		public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
			this.fileTokenFactory = fileTokenFactory;
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
		private ServiceConfig mascot;
		private ServiceConfig mascotDeployer;
		private ServiceConfig sequest;
		private ServiceConfig sequestDeployer;
		private ServiceConfig tandem;
		private ServiceConfig tandemDeployer;
		private ServiceConfig omssa;
		private ServiceConfig omssaDeployer;
		private ServiceConfig myrimatch;
		private ServiceConfig myrimatchDeployer;
		private ServiceConfig idpicker;
		private ServiceConfig idpickerDeployer;
		private ServiceConfig scaffold;
		private ServiceConfig scaffoldDeployer;
		private ServiceConfig scaffoldReport;
		private ServiceConfig scaffold3;
		private ServiceConfig scaffold3Deployer;
		private ServiceConfig qa;
		private ServiceConfig fastaDb;
		private ServiceConfig searchDb;
		private ServiceConfig msmsEval;
		private DatabaseFactory.Config database;

		public Config() {
		}

		public Config(final String fastaPath, final String fastaArchivePath, final String fastaUploadPath
				, final ServiceConfig raw2mgf, final ServiceConfig msconvert,
				      final ServiceConfig mgf2mgf, final ServiceConfig rawdump, final ServiceConfig mascot, final ServiceConfig mascotDeployer
				, final ServiceConfig sequest, final ServiceConfig sequestDeployer, final ServiceConfig tandem, final ServiceConfig tandemDeployer
				, final ServiceConfig omssa, final ServiceConfig omssaDeployer
				, final ServiceConfig myrimatch, final ServiceConfig myrimatchDeployer
				, final ServiceConfig idpicker, final ServiceConfig idpickerDeployer
				, final ServiceConfig scaffold, final ServiceConfig scaffoldDeployer
				, final ServiceConfig scaffold3, final ServiceConfig scaffold3Deployer
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
			this.mascot = mascot;
			this.mascotDeployer = mascotDeployer;
			this.sequest = sequest;
			this.sequestDeployer = sequestDeployer;
			this.tandem = tandem;
			this.tandemDeployer = tandemDeployer;
			this.omssa = omssa;
			this.omssaDeployer = omssaDeployer;
			this.myrimatch = myrimatch;
			this.myrimatchDeployer = myrimatchDeployer;
			this.idpicker = idpicker;
			this.idpickerDeployer = idpickerDeployer;
			this.scaffold = scaffold;
			this.scaffoldDeployer = scaffoldDeployer;
			this.scaffold3 = scaffold3;
			this.scaffold3Deployer = scaffold3Deployer;
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

		public ServiceConfig getMascot() {
			return mascot;
		}

		public ServiceConfig getMascotDeployer() {
			return mascotDeployer;
		}

		public ServiceConfig getSequest() {
			return sequest;
		}

		public ServiceConfig getSequestDeployer() {
			return sequestDeployer;
		}

		public ServiceConfig getTandem() {
			return tandem;
		}

		public ServiceConfig getTandemDeployer() {
			return tandemDeployer;
		}

		public ServiceConfig getOmssa() {
			return omssa;
		}

		public ServiceConfig getOmssaDeployer() {
			return omssaDeployer;
		}

		public ServiceConfig getMyrimatch() {
			return myrimatch;
		}

		public ServiceConfig getMyrimatchDeployer() {
			return myrimatchDeployer;
		}

		public ServiceConfig getIdpicker() {
			return idpicker;
		}

		public ServiceConfig getIdpickerDeployer() {
			return idpickerDeployer;
		}

		public ServiceConfig getScaffold() {
			return scaffold;
		}

		public ServiceConfig getScaffoldDeployer() {
			return scaffoldDeployer;
		}

		public ServiceConfig getScaffold3() {
			return scaffold3;
		}

		public ServiceConfig getScaffold3Deployer() {
			return scaffold3Deployer;
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

		public DatabaseFactory.Config getDatabase() {
			return database;
		}

		public boolean isReportDecoyHits() {
			return reportDecoyHits;
		}

		@Override
		public Map<String, String> save(final DependencyResolver resolver) {
			final Map<String, String> map = new TreeMap<String, String>();
			map.put(FASTA_PATH, fastaPath);
			map.put(FASTA_ARCHIVE_PATH, fastaArchivePath);
			map.put(FASTA_UPLOAD_PATH, fastaUploadPath);
			map.put(RAW_2_MGF, resolver.getIdFromConfig(raw2mgf));
			map.put(MSCONVERT, resolver.getIdFromConfig(msconvert));
			map.put(MGF_2_MGF, resolver.getIdFromConfig(mgf2mgf));
			map.put(RAWDUMP, resolver.getIdFromConfig(rawdump));
			map.put(MASCOT, resolver.getIdFromConfig(mascot));
			map.put(MASCOT_DEPLOYER, resolver.getIdFromConfig(mascotDeployer));
			map.put(SEQUEST, resolver.getIdFromConfig(sequest));
			map.put(SEQUEST_DEPLOYER, resolver.getIdFromConfig(sequestDeployer));
			map.put(TANDEM, resolver.getIdFromConfig(tandem));
			map.put(TANDEM_DEPLOYER, resolver.getIdFromConfig(tandemDeployer));
			map.put(OMSSA, resolver.getIdFromConfig(omssa));
			map.put(OMSSA_DEPLOYER, resolver.getIdFromConfig(omssaDeployer));
			map.put(MYRIMATCH, resolver.getIdFromConfig(myrimatch));
			map.put(MYRIMATCH_DEPLOYER, resolver.getIdFromConfig(myrimatchDeployer));
			map.put(IDPICKER, resolver.getIdFromConfig(idpicker));
			map.put(IDPICKER_DEPLOYER, resolver.getIdFromConfig(idpickerDeployer));
			map.put(SCAFFOLD, resolver.getIdFromConfig(scaffold));
			map.put(SCAFFOLD_DEPLOYER, resolver.getIdFromConfig(scaffoldDeployer));
			map.put(SCAFFOLD3, resolver.getIdFromConfig(scaffold3));
			map.put(SCAFFOLD3_DEPLOYER, resolver.getIdFromConfig(scaffold3Deployer));
			map.put(SCAFFOLD_REPORT, resolver.getIdFromConfig(scaffoldReport));
			map.put(QA, resolver.getIdFromConfig(qa));
			map.put(FASTA_DB, resolver.getIdFromConfig(fastaDb));
			map.put(SEARCH_DB, resolver.getIdFromConfig(searchDb));
			map.put(MSMS_EVAL, resolver.getIdFromConfig(msmsEval));
			map.put(DATABASE, resolver.getIdFromConfig(database));
			map.put(REPORT_DECOY_HITS, Boolean.toString(reportDecoyHits));
			return map;
		}

		public void load(final Map<String, String> values, final DependencyResolver resolver) {
			fastaPath = values.get(FASTA_PATH);
			fastaArchivePath = values.get(FASTA_ARCHIVE_PATH);
			fastaUploadPath = values.get(FASTA_UPLOAD_PATH);
			raw2mgf = (ServiceConfig) resolver.getConfigFromId(values.get(RAW_2_MGF));
			msconvert = (ServiceConfig) resolver.getConfigFromId(values.get(MSCONVERT));
			mgf2mgf = (ServiceConfig) resolver.getConfigFromId(values.get(MGF_2_MGF));
			rawdump = (ServiceConfig) resolver.getConfigFromId(values.get(RAWDUMP));
			mascot = (ServiceConfig) resolver.getConfigFromId(values.get(MASCOT));
			mascotDeployer = (ServiceConfig) resolver.getConfigFromId(values.get(MASCOT_DEPLOYER));
			sequest = (ServiceConfig) resolver.getConfigFromId(values.get(SEQUEST));
			sequestDeployer = (ServiceConfig) resolver.getConfigFromId(values.get(SEQUEST_DEPLOYER));
			tandem = (ServiceConfig) resolver.getConfigFromId(values.get(TANDEM));
			tandemDeployer = (ServiceConfig) resolver.getConfigFromId(values.get(TANDEM_DEPLOYER));
			omssa = (ServiceConfig) resolver.getConfigFromId(values.get(OMSSA));
			omssaDeployer = (ServiceConfig) resolver.getConfigFromId(values.get(OMSSA_DEPLOYER));
			myrimatch = (ServiceConfig) resolver.getConfigFromId(values.get(MYRIMATCH));
			myrimatchDeployer = (ServiceConfig) resolver.getConfigFromId(values.get(MYRIMATCH_DEPLOYER));
			idpicker = (ServiceConfig) resolver.getConfigFromId(values.get(IDPICKER));
			idpickerDeployer = (ServiceConfig) resolver.getConfigFromId(values.get(IDPICKER_DEPLOYER));
			scaffold = (ServiceConfig) resolver.getConfigFromId(values.get(SCAFFOLD));
			scaffoldDeployer = (ServiceConfig) resolver.getConfigFromId(values.get(SCAFFOLD_DEPLOYER));
			scaffold3 = (ServiceConfig) resolver.getConfigFromId(values.get(SCAFFOLD3));
			scaffold3Deployer = (ServiceConfig) resolver.getConfigFromId(values.get(SCAFFOLD3_DEPLOYER));
			scaffoldReport = (ServiceConfig) resolver.getConfigFromId(values.get(SCAFFOLD_REPORT));
			qa = (ServiceConfig) resolver.getConfigFromId(values.get(QA));
			fastaDb = (ServiceConfig) resolver.getConfigFromId(values.get(FASTA_DB));
			searchDb = (ServiceConfig) resolver.getConfigFromId(values.get(SEARCH_DB));
			msmsEval = (ServiceConfig) resolver.getConfigFromId(values.get(MSMS_EVAL));
			database = (DatabaseFactory.Config) resolver.getConfigFromId(values.get(DATABASE));
			reportDecoyHits = Boolean.parseBoolean(values.get(REPORT_DECOY_HITS));
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {

		private DatabaseValidator validator;

		public Ui(final DatabaseValidator validator) {
			this.validator = validator;
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
					.reference(RAWDumpWorker.TYPE, RAWDumpCache.TYPE, UiBuilder.NONE_TYPE)

					.property(MASCOT, MascotWorker.NAME, "")
					.reference(MascotWorker.TYPE, MascotCache.TYPE, UiBuilder.NONE_TYPE)

					.property(MASCOT_DEPLOYER, MascotDeploymentService.NAME, "<p>If you want to use Mascot, you have to have a database deployer set up, typically on the same computer where Mascot is installed, since we modify the <tt>mascot.dat</tt> config file directly to perform the deployment. " +
							"A version that would use just the Mascot URL is in development.</p>" +
							"<p>There is an option to use 'mock' Mascot deployer, which just fools Swift into thinking that all the databases were already deployed, and do the database deployment manually.</p>")
					.reference(MascotDeploymentService.TYPE, MockMascotDeploymentService.TYPE, UiBuilder.NONE_TYPE)

					.property(SEQUEST, SequestWorker.NAME, "")
					.reference(SequestWorker.TYPE, SequestCache.TYPE, UiBuilder.NONE_TYPE)

					.property(SEQUEST_DEPLOYER, SequestDeploymentService.NAME, "If you want to use Sequest, you have to have a database deployer set up. Sequest database deployment can be very time consuming, when large .fasta files are processed. The deployment however happends only once per database + modification set.")
					.reference(SequestDeploymentService.TYPE, UiBuilder.NONE_TYPE)

					.property(TANDEM, XTandemWorker.NAME, "")
					.reference(XTandemWorker.TYPE, XTandemCache.TYPE, UiBuilder.NONE_TYPE)

					.property(TANDEM_DEPLOYER, XTandemDeploymentService.NAME, "If you want to use X!Tandem, you have to have a database deployer set up. This deployer does virtually nothing, so it can be installed even on a very loaded machine.")
					.reference(XTandemDeploymentService.TYPE, UiBuilder.NONE_TYPE)

					.property(OMSSA, OmssaWorker.NAME, "")
					.reference(OmssaWorker.TYPE, OmssaCache.TYPE, UiBuilder.NONE_TYPE)

					.property(OMSSA_DEPLOYER, OmssaDeploymentService.NAME, "If you want to use OMSSA, you have to have a database deployer set up. OMSSA deployment converts the .fasta into several index files.")
					.reference(OmssaDeploymentService.TYPE, UiBuilder.NONE_TYPE)

					.property(MYRIMATCH, MyrimatchWorker.NAME, "MyriMatch is used to augment the search results. Not fully integrated into Scaffold.")
					.reference(MyrimatchWorker.TYPE, MyrimatchCache.TYPE, UiBuilder.NONE_TYPE)

					.property(MYRIMATCH_DEPLOYER, MyrimatchDeploymentService.NAME, "If you want to use Myrimatch, you have to have a database deployer set up. Myrimatch deployment detects the prefix of decoy sequences.")
					.reference(MyrimatchDeploymentService.TYPE, UiBuilder.NONE_TYPE)

					.property(IDPICKER, IdpickerWorker.NAME, "Idpicker is roughly equivalent to Scaffold. At the moment it can process Myrimatch files.")
					.reference(IdpickerWorker.TYPE, IdpickerCache.TYPE, UiBuilder.NONE_TYPE)

					.property(IDPICKER_DEPLOYER, IdpickerDeploymentService.NAME, "If you want to use IDPicker, you have to have a database deployer set up. This deployer does virtually nothing, so it can be installed even on a very loaded machine.")
					.reference(IdpickerDeploymentService.TYPE, UiBuilder.NONE_TYPE)

					.property(SCAFFOLD, ScaffoldWorker.NAME, "Scaffold 2 batch searcher by Proteome Software")
					.reference(ScaffoldWorker.TYPE, UiBuilder.NONE_TYPE)

					.property(SCAFFOLD_DEPLOYER, ScaffoldDeploymentService.NAME, "If you want to use Scaffold 2, you have to have a database deployer set up. The deployment is trivial in case of Scaffold 2, so it can be installed even on a loaded machine.")
					.reference(ScaffoldDeploymentService.TYPE, UiBuilder.NONE_TYPE)

					.property(SCAFFOLD3, Scaffold3Worker.NAME, "Scaffold 3 batch searcher by Proteome Software.")
					.reference(Scaffold3Worker.TYPE, UiBuilder.NONE_TYPE)

					.property(SCAFFOLD3_DEPLOYER, Scaffold3DeploymentService.NAME, "If you want to use Scaffold 3, you have to have a database deployer set up. The deployment is trivial in case of Scaffold 3, so it can be installed even on a loaded machine.")
					.reference(Scaffold3DeploymentService.TYPE, UiBuilder.NONE_TYPE)

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