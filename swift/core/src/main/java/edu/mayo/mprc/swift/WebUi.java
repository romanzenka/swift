package edu.mayo.mprc.swift;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.PropertyChangeListener;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.config.ui.UiResponse;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.database.DatabasePlaceholder;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.mascot.MascotMappingFactory;
import edu.mayo.mprc.msmseval.MSMSEvalParamFile;
import edu.mayo.mprc.msmseval.MSMSEvalWorker;
import edu.mayo.mprc.msmseval.MsmsEvalCache;
import edu.mayo.mprc.myrimatch.MyrimatchMappingFactory;
import edu.mayo.mprc.omssa.OmssaMappingFactory;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.sequest.SequestMappingFactory;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.swift.params2.mapping.ParamsInfo;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.unimod.UnimodDao;
import edu.mayo.mprc.workspace.WorkspaceDao;
import edu.mayo.mprc.xtandem.XTandemMappingFactory;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

/**
 * A class holding information about WebUI configuration.
 */
public final class WebUi {
	public static final String TYPE = "webUi";
	public static final String NAME = "Swift Website";
	public static final String DESC = "Swift's web user interface.<p>The daemon that contains the web interface will run within a web server.</p>";
	public static final UserMessage USER_MESSAGE = new UserMessage();
	private SwiftDao swiftDao;
	private File browseRoot;
	private DaemonConnection databaseUndeployerDaemonConnection;
	private DaemonConnection qstatDaemonConnection;
	private String browseWebRoot;
	private DaemonConnection swiftSearcherDaemonConnection;
	private CurationDao curationDao;
	private ParamsDao paramsDao;
	private WorkspaceDao workspaceDao;
	private UnimodDao unimodDao;
	private SearchDbDao searchDbDao;
	private Collection<SearchEngine> searchEngines;
	private boolean scaffoldReport;
	private boolean qa;
	private boolean msmsEval;
	private List<MSMSEvalParamFile> spectrumQaParamFiles;
	private File fastaUploadFolder;
	private File fastaFolder;
	private File fastaArchiveFolder;
	private String title;
	private ParamsInfo paramsInfo;
	private FileTokenFactory fileTokenFactory;
	private SwiftMonitor swiftMonitor;

	public static final String SEARCHER = "searcher";
	public static final String TITLE = "title";
	public static final String PORT = "port";
	public static final String BROWSE_ROOT = "browseRoot";
	public static final String BROWSE_WEB_ROOT = "browseWebRoot";
	public static final String QSTAT = "qstat";
	public static final String DATABASE_UNDEPLOYER = "databaseUndeployer";
	public static final String SEARCHES_FOLDER = "searchesFolder";

	public WebUi() {
		USER_MESSAGE.setMessage("Swift's new database deployment has been temporarily disabled. Swift needs to be upgraded to support Mascot's Database Manager. If you need a new database, please ask Roman.");
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	public WorkspaceDao getWorkspaceDao() {
		return workspaceDao;
	}

	public UnimodDao getUnimodDao() {
		return unimodDao;
	}

	public SearchDbDao getSearchDbDao() {
		return searchDbDao;
	}

	public File getFastaUploadFolder() {
		return fastaUploadFolder;
	}

	public File getBrowseRoot() {
		return browseRoot;
	}

	public DaemonConnection getQstatDaemonConnection() {
		return qstatDaemonConnection;
	}

	public String getBrowseWebRoot() {
		if (!browseWebRoot.endsWith("/")) {
			return browseWebRoot + "/";
		} else {
			return browseWebRoot;
		}
	}

	public DaemonConnection getSwiftSearcherDaemonConnection() {
		return swiftSearcherDaemonConnection;
	}

	public CurationDao getCurationDao() {
		return curationDao;
	}

	public ParamsDao getParamsDao() {
		return paramsDao;
	}

	public Collection<SearchEngine> getSearchEngines() {
		return searchEngines;
	}

	public boolean isMsmsEval() {
		return msmsEval;
	}

	public boolean isScaffoldReport() {
		return scaffoldReport;
	}

	public boolean isQa() {
		return qa;
	}

	public boolean isDatabaseUndeployerEnabled() {
		return databaseUndeployerDaemonConnection != null;
	}

	public List<MSMSEvalParamFile> getSpectrumQaParamFiles() {
		return spectrumQaParamFiles;
	}

	public File getFastaFolder() {
		return fastaFolder;
	}

	public File getFastaArchiveFolder() {
		return fastaArchiveFolder;
	}

	public String getTitle() {
		return title;
	}

	public ParamsInfo getParamsInfo() {
		return paramsInfo;
	}

	public FileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	public DaemonConnection getDatabaseUndeployerDaemonConnection() {
		return databaseUndeployerDaemonConnection;
	}

	public UserMessage getUserMessage() {
		// TODO - re-enabled message support
		return USER_MESSAGE;
	}

	public SwiftMonitor getSwiftMonitor() {
		return swiftMonitor;
	}

	/**
	 * A factory capable of creating the web ui class.
	 */
	public static final class Factory extends FactoryBase<Config, WebUi> {
		private SwiftDao swiftDao;
		private CurationDao curationDao;
		private WorkspaceDao workspaceDao;
		private ParamsDao paramsDao;
		private UnimodDao unimodDao;
		private SearchDbDao searchDbDao;
		private DatabasePlaceholder databasePlaceholder;
		private ParamsInfo paramsInfo;
		private Collection<SearchEngine> searchEngines;
		private FileTokenFactory fileTokenFactory;
		private SwiftMonitor swiftMonitor;

		@Override
		public WebUi create(final Config config, final DependencyResolver dependencies) {
			WebUi ui = null;
			try {
				ui = new WebUi();
				ui.title = config.getTitle();
				ui.browseRoot = new File(config.getBrowseRoot());
				ui.browseWebRoot = config.getBrowseWebRoot();
				ui.swiftDao = getSwiftDao();
				ui.curationDao = getCurationDao();
				ui.workspaceDao = getWorkspaceDao();
				ui.paramsDao = getParamsDao();
				ui.unimodDao = getUnimodDao();
				ui.searchDbDao = getSearchDbDao();
				ui.swiftMonitor = getSwiftMonitor();

				if (config.getQstat() != null) {
					ui.qstatDaemonConnection = (DaemonConnection) dependencies.createSingleton(config.getQstat());
				}
				ui.paramsInfo = paramsInfo;
				ui.fileTokenFactory = fileTokenFactory;

				// Harvest the param files from searcher config
				if (config.getSearcher() != null) {
					ui.swiftSearcherDaemonConnection = (DaemonConnection) dependencies.createSingleton(config.getSearcher());
					final SwiftSearcher.Config searcherConfig = (SwiftSearcher.Config) config.getSearcher().getRunner().getWorkerConfiguration();

					ui.fastaUploadFolder = new File(searcherConfig.getFastaUploadPath());
					ui.fastaArchiveFolder = new File(searcherConfig.getFastaArchivePath());
					ui.fastaFolder = new File(searcherConfig.getFastaPath());

					if (searcherConfig.getMsmsEval() != null) {
						// We got msmsEval, take spectrumQaParamFiles from it
						ui.msmsEval = true;
						ResourceConfig msmsEvalWorkerConfig = searcherConfig.getMsmsEval().getRunner().getWorkerConfiguration();
						if (msmsEvalWorkerConfig instanceof MsmsEvalCache.Config) {
							// It is a cache - skip to the actual worker
							final MsmsEvalCache.Config cacheConfig = (MsmsEvalCache.Config) msmsEvalWorkerConfig;
							if (cacheConfig.getService().getRunner().getWorkerConfiguration() instanceof MSMSEvalWorker.Config) {
								msmsEvalWorkerConfig = cacheConfig.getService().getRunner().getWorkerConfiguration();
							}
						}
						if (msmsEvalWorkerConfig instanceof MSMSEvalWorker.Config) {
							final MSMSEvalWorker.Config msmsEvalConfig = (MSMSEvalWorker.Config) msmsEvalWorkerConfig;
							ui.spectrumQaParamFiles = parseSpectrumQaParamFiles(msmsEvalConfig.getParamFiles());
						}
					}

					if (searcherConfig.getScaffoldReport() != null) {
						ui.scaffoldReport = true;
					}
					if (searcherConfig.getQa() != null) {
						ui.qa = true;
					}

					final List<SearchEngine> clonedSearchEngines = new ArrayList<SearchEngine>();
					for (final SearchEngine engine : getSearchEngines()) {
						fillEngineDaemons(engine, clonedSearchEngines, MascotMappingFactory.MASCOT, searcherConfig.getMascot(), searcherConfig.getMascotDeployer(), dependencies);
						fillEngineDaemons(engine, clonedSearchEngines, SequestMappingFactory.SEQUEST, searcherConfig.getSequest(), searcherConfig.getSequestDeployer(), dependencies);
						fillEngineDaemons(engine, clonedSearchEngines, XTandemMappingFactory.TANDEM, searcherConfig.getTandem(), searcherConfig.getTandemDeployer(), dependencies);
						fillEngineDaemons(engine, clonedSearchEngines, OmssaMappingFactory.OMSSA, searcherConfig.getOmssa(), searcherConfig.getOmssaDeployer(), dependencies);
						fillEngineDaemons(engine, clonedSearchEngines, MyrimatchMappingFactory.MYRIMATCH, searcherConfig.getMyrimatch(), searcherConfig.getMyrimatchDeployer(), dependencies);
						fillEngineDaemons(engine, clonedSearchEngines, "IDPICKER", searcherConfig.getIdpicker(), searcherConfig.getIdpickerDeployer(), dependencies);
						fillEngineDaemons(engine, clonedSearchEngines, "SCAFFOLD", searcherConfig.getScaffold(), searcherConfig.getScaffoldDeployer(), dependencies);
						fillEngineDaemons(engine, clonedSearchEngines, "SCAFFOLD3", searcherConfig.getScaffold3(), searcherConfig.getScaffold3Deployer(), dependencies);
					}
					ui.searchEngines = clonedSearchEngines;
				}

				if (config.getDatabaseUndeployer() != null) {
					ui.databaseUndeployerDaemonConnection = (DaemonConnection) dependencies.createSingleton(config.getDatabaseUndeployer());
				}

			} catch (Exception e) {
				throw new MprcException("Web UI class could not be created.", e);
			}
			return ui;
		}

		private static void fillEngineDaemons(final SearchEngine engineToFill, final List<SearchEngine> filledList, final String engineCode, final ServiceConfig daemonConfig, final ServiceConfig dbDeployerConfig, final DependencyResolver dependencies) {
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

		public SwiftDao getSwiftDao() {
			return swiftDao;
		}

		public void setSwiftDao(final SwiftDao swiftDao) {
			this.swiftDao = swiftDao;
		}

		public CurationDao getCurationDao() {
			return curationDao;
		}

		public ParamsDao getParamsDao() {
			return paramsDao;
		}

		public void setParamsDao(final ParamsDao paramsDao) {
			this.paramsDao = paramsDao;
		}

		public void setCurationDao(final CurationDao curationDao) {
			this.curationDao = curationDao;
		}

		public WorkspaceDao getWorkspaceDao() {
			return workspaceDao;
		}

		public void setWorkspaceDao(final WorkspaceDao workspaceDao) {
			this.workspaceDao = workspaceDao;
		}

		public DatabasePlaceholder getDatabasePlaceholder() {
			return databasePlaceholder;
		}

		public void setDatabasePlaceholder(final DatabasePlaceholder databasePlaceholder) {
			this.databasePlaceholder = databasePlaceholder;
		}

		public ParamsInfo getAbstractParamsInfo() {
			return paramsInfo;
		}

		public void setAbstractParamsInfo(final ParamsInfo paramsInfo) {
			this.paramsInfo = paramsInfo;
		}

		public Collection<SearchEngine> getSearchEngines() {
			return searchEngines;
		}

		public void setSearchEngines(final Collection<SearchEngine> searchEngines) {
			this.searchEngines = searchEngines;
		}

		public FileTokenFactory getFileTokenFactory() {
			return fileTokenFactory;
		}

		public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
			this.fileTokenFactory = fileTokenFactory;
		}

		public UnimodDao getUnimodDao() {
			return unimodDao;
		}

		public void setUnimodDao(final UnimodDao unimodDao) {
			this.unimodDao = unimodDao;
		}

		public SearchDbDao getSearchDbDao() {
			return searchDbDao;
		}

		public void setSearchDbDao(final SearchDbDao searchDbDao) {
			this.searchDbDao = searchDbDao;
		}

		public SwiftMonitor getSwiftMonitor() {
			return swiftMonitor;
		}

		public void setSwiftMonitor(final SwiftMonitor swiftMonitor) {
			this.swiftMonitor = swiftMonitor;
		}
	}


	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private ServiceConfig searcher;
		private String port;
		private String title;
		private String browseRoot;
		private String browseWebRoot;
		private ServiceConfig qstat;
		private ServiceConfig databaseUndeployer;
		private String searchesFolder;

		public Config() {
		}

		public Config(final ServiceConfig searcher, final String port, final String title, final String browseRoot, final String browseWebRoot, final ServiceConfig qstat, final ServiceConfig databaseUndeployer, final String searchesFolder) {
			this.searcher = searcher;
			this.port = port;
			this.title = title;
			this.browseRoot = browseRoot;
			this.browseWebRoot = browseWebRoot;
			this.qstat = qstat;
			this.databaseUndeployer = databaseUndeployer;
			this.searchesFolder = searchesFolder;
		}

		public ServiceConfig getSearcher() {
			return searcher;
		}

		public Map<String, String> save(final DependencyResolver resolver) {
			final Map<String, String> map = new TreeMap<String, String>();
			map.put(SEARCHER, resolver.getIdFromConfig(getSearcher()));
			map.put(TITLE, getTitle());
			map.put(PORT, getPort());
			map.put(BROWSE_ROOT, getBrowseRoot());
			map.put(BROWSE_WEB_ROOT, getBrowseWebRoot());
			map.put(QSTAT, resolver.getIdFromConfig(getQstat()));
			map.put(DATABASE_UNDEPLOYER, resolver.getIdFromConfig(getDatabaseUndeployer()));
			map.put(SEARCHES_FOLDER, getSearchesFolder());
			return map;
		}

		public void load(final Map<String, String> values, final DependencyResolver resolver) {
			searcher = (ServiceConfig) resolver.getConfigFromId(values.get(SEARCHER));
			title = values.get(TITLE);
			port = values.get(PORT);
			browseRoot = values.get(BROWSE_ROOT);
			browseWebRoot = values.get(BROWSE_WEB_ROOT);
			qstat = (ServiceConfig) resolver.getConfigFromId(values.get(QSTAT));
			databaseUndeployer = (ServiceConfig) resolver.getConfigFromId(values.get(DATABASE_UNDEPLOYER));
			searchesFolder = values.get(SEARCHES_FOLDER);
		}

		@Override
		public int getPriority() {
			return 0;
		}

		public String getPort() {
			return port;
		}

		public String getTitle() {
			return title;
		}

		public String getBrowseRoot() {
			return browseRoot;
		}

		public String getBrowseWebRoot() {
			return browseWebRoot;
		}

		public ServiceConfig getQstat() {
			return qstat;
		}

		public ServiceConfig getDatabaseUndeployer() {
			return databaseUndeployer;
		}

		public String getSearchesFolder() {
			return searchesFolder;
		}
	}

	/**
	 * Parses a comma delimited string in <code>desc1,file1,desc2,file2,...</code> format
	 * into a list of {@link edu.mayo.mprc.msmseval.MSMSEvalParamFile}
	 */
	public static List<MSMSEvalParamFile> parseSpectrumQaParamFiles(final String paramFileString) {
		final String[] tokens = paramFileString.split(",");
		if (tokens.length % 2 != 0) {
			throw new MprcException(MessageFormat.format("Spectrum QA parameter file definition does not match the expected format <description1>,<file1>,<description2>,<file2>,... :{0}\nCorrect the install.properties file and restart the application.", paramFileString));
		}

		final List<MSMSEvalParamFile> result = new ArrayList<MSMSEvalParamFile>();
		for (int i = 0; i < tokens.length; i += 2) {
			result.add(new MSMSEvalParamFile(tokens[i + 1], tokens[i]));
		}
		return result;
	}

	/**
	 * Swift web interface setup.
	 */
	public static final class Ui implements ServiceUiFactory {
		private static final String WINDOWS_ROOT = "C:\\";
		private static final String WINDOWS_WEB_ROOT = "file:///C:/";
		private static final String LINUX_ROOT = "/";
		private static final String LINUX_WEB_ROOT = "file:////";

		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			final ResourceConfig swiftSearcher = daemon.firstServiceOfType(SwiftSearcher.Config.class);

			builder
					.property(TITLE, "Installation Title", "This is displayed as the title of the Swift web pages.<br/>" +
							"You can use it to distinguish between Swift installs.")
					.required()
					.defaultValue("Swift 2.5")

					.property("port", "Web server port", "The web interface port." +
							" Standard HTTP port number is <tt>80</tt>. If your system is already running a web server, port 80 is probably taken, that is why we suggest running Swift at <tt>8080</tt> by default." +
							" <p>Swift web user interface will be available at:</p><p><tt>http://" + daemon.getHostName() + ":&ltport&gt;/</tt></p>")
					.required()
					.integerValue(1, 65535)
					.defaultValue("8080")

					.property(BROWSE_ROOT, "Root folder", "The users are allowed to browse this folder only.<br/>"
							+ "Set it to the root of your disk if you want to provide access to all files, or limit the users only to areas with actual MS data.")
					.required()
					.existingDirectory()
					.defaultValue(getDefaultRoot(daemon))

					.property(BROWSE_WEB_ROOT, "Web access to root folder", "Search results that Swift generates will be somewhere within root folder (see above)<br/>"
							+ "The users need to access these files through the web interface.<br/>"
							+ "For instance, if root folder is set to <tt>C:/data/spectra</tt>, and you have a web server set up "
							+ "to map this folder to <tt>http://server/spectra</tt>, then enter "
							+ "<tt>http://server/spectra</tt> into this box.<br/><br/>"
							+ "If you do not have a web server running to allow access to the files, use file URLs, "
							+ "and enter for example <tt>file:///c:/data/spectra</tt>. This will instruct the browser to go directly to your disk.")
					.required()
					.defaultValue(getDefaultWebRoot(daemon))

					.property(SEARCHER, "Swift searcher", "The module that performs the actual Swift search has to be referenced here.")
					.required()
					.reference("searcher", UiBuilder.NONE_TYPE)
					.defaultValue(swiftSearcher)

					.property(DATABASE_UNDEPLOYER, "Database Undeployer", "The module that performs search engine database undeployments.")
					.reference("databaseUndeployer", UiBuilder.NONE_TYPE)

					.property(QSTAT, "Qstat", "If you are running in Sun Grid Engine and want to have the job status available from the web interface, add a Qstat module. This is completely optional and provided solely for user convenience.")
					.reference("qstat", UiBuilder.NONE_TYPE)

					.property(SEARCHES_FOLDER, "Search definition folder", "When Swift starts a new search, the search definition is written to this folder. "
							+ "<p>This is going to be soon replaced by a direct write to the database.</p>")
					.required()
					.existingDirectory()
					.defaultValue("var/searches")
					.addDaemonChangeListener(new PropertyChangeListener() {
						@Override
						public void propertyChanged(final ResourceConfig config, final String propertyName, final String newValue, final UiResponse response, final boolean validationRequested) {
							response.setProperty(resource, BROWSE_ROOT, getDefaultRoot(daemon));
							response.setProperty(resource, BROWSE_WEB_ROOT, getDefaultWebRoot(daemon));
						}

						@Override
						public void fixError(final ResourceConfig config, final String propertyName, final String action) {
						}
					});
		}

		private static String getDefaultWebRoot(final DaemonConfig daemon) {
			if (daemon.isWindows()) {
				return WINDOWS_WEB_ROOT;
			}
			return LINUX_WEB_ROOT;
		}

		private static String getDefaultRoot(final DaemonConfig daemon) {
			if (daemon.isWindows()) {
				return WINDOWS_ROOT;
			}
			return LINUX_ROOT;
		}
	}
}

