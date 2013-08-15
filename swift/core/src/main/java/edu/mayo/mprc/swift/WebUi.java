package edu.mayo.mprc.swift;

import com.google.common.collect.Lists;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.PropertyChangeListener;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.config.ui.UiResponse;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.msmseval.MSMSEvalParamFile;
import edu.mayo.mprc.msmseval.MSMSEvalWorker;
import edu.mayo.mprc.msmseval.MsmsEvalCache;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.workspace.WorkspaceDao;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A class holding information about WebUI configuration.
 */
public final class WebUi {
	public static final String TYPE = "webUi";
	public static final String NAME = "Swift Website";
	public static final String DESC = "Swift's web user interface.<p>The daemon that contains the web interface will run within a web server.</p>";
	public static final UserMessage USER_MESSAGE = new UserMessage();
	private File browseRoot;
	private DaemonConnection databaseUndeployerDaemonConnection;
	private DaemonConnection qstatDaemonConnection;
	private String browseWebRoot;
	private DaemonConnection swiftSearcherDaemonConnection;
	private Collection<SearchEngine> searchEngines;
	private boolean scaffoldReport;
	private boolean qa;
	private boolean msmsEval;
	private List<MSMSEvalParamFile> spectrumQaParamFiles;
	private File fastaUploadFolder;
	private File fastaFolder;
	private File fastaArchiveFolder;
	private String title;
	private FileTokenFactory fileTokenFactory;
	private SwiftMonitor swiftMonitor;
	private Daemon mainDaemon;
	private SwiftDao swiftDao;
	private WorkspaceDao workspaceDao;

	public static final String SEARCHER = "searcher";
	public static final String TITLE = "title";
	public static final String PORT = "port";
	public static final String BROWSE_ROOT = "browseRoot";
	public static final String BROWSE_WEB_ROOT = "browseWebRoot";
	public static final String QSTAT = "qstat";
	public static final String DATABASE_UNDEPLOYER = "databaseUndeployer";

	public WebUi() {
		USER_MESSAGE.setMessage("Swift's new database deployment has been temporarily disabled. Swift needs to be upgraded to support Mascot's Database Manager. If you need a new database, please ask Roman.");
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

	/**
	 * Turn a given file to a link that can be sent to the user over the web.
	 * Caveats:  We naively assume the file is linkable at the moment.
	 *
	 * @param file File to link to.
	 * @return Hyperlink to the file.
	 */
	public String fileToUserLink(final File file) {
		return getBrowseWebRoot() + file.getAbsolutePath().substring(getBrowseRoot().getAbsolutePath().length() + 1);
	}

	public DaemonConnection getSwiftSearcherDaemonConnection() {
		return swiftSearcherDaemonConnection;
	}

	/**
	 * @return All the search engines that are available to the {@link SwiftSearcher} that
	 *         is pointed to by this web ui.
	 */
	public Collection<SearchEngine> getSearchEngines() {
		return searchEngines;
	}

	public void setSearchEngines(Collection<SearchEngine> searchEngines) {
		this.searchEngines = searchEngines;
	}

	public boolean isMsmsEval() {
		return msmsEval;
	}

	public boolean isScaffoldReport() {
		return scaffoldReport;
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

	public FileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	public void setFileTokenFactory(FileTokenFactory fileTokenFactory) {
		this.fileTokenFactory = fileTokenFactory;
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

	public Daemon getMainDaemon() {
		return mainDaemon;
	}

	public void setMainDaemon(Daemon mainDaemon) {
		this.mainDaemon = mainDaemon;
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	public void setSwiftDao(SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}

	public WorkspaceDao getWorkspaceDao() {
		return workspaceDao;
	}

	public void setWorkspaceDao(WorkspaceDao workspaceDao) {
		this.workspaceDao = workspaceDao;
	}

	public void stopSwiftMonitor() {
		if (getSwiftMonitor() != null) {
			getSwiftMonitor().stop();
		}
	}

	/**
	 * A factory capable of creating the web ui class.
	 */
	public static final class Factory extends FactoryBase<Config, WebUi> implements FactoryDescriptor {
		private SwiftMonitor swiftMonitor;
		private FileTokenFactory fileTokenFactory;
		private SwiftDao swiftDao;
		private WorkspaceDao workspaceDao;
		private SearchEngine.Factory searchEngineFactory;

		@Override
		public WebUi create(final Config config, final DependencyResolver dependencies) {
			WebUi ui = null;
			try {
				ui = new WebUi();
				ui.title = config.getTitle();
				ui.browseRoot = new File(config.getBrowseRoot());
				ui.browseWebRoot = config.getBrowseWebRoot();
				ui.swiftMonitor = getSwiftMonitor();
				ui.setFileTokenFactory(getFileTokenFactory());
				ui.setSwiftDao(getSwiftDao());
				ui.setWorkspaceDao(getWorkspaceDao());

				if (config.getQstat() != null) {
					ui.qstatDaemonConnection = (DaemonConnection) dependencies.createSingleton(config.getQstat());
				}

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

					final Collection<SearchEngine> searchEngines = Lists.newArrayList();
					for (final SearchEngine.Config engineConfig : searcherConfig.getEngines()) {
						if (engineConfig.getWorker() != null) {
							final SearchEngine engine = getSearchEngineFactory().create(engineConfig, dependencies);
							searchEngines.add(engine);
						}
					}
					ui.setSearchEngines(searchEngines);
				}

				if (config.getDatabaseUndeployer() != null) {
					ui.databaseUndeployerDaemonConnection = (DaemonConnection) dependencies.createSingleton(config.getDatabaseUndeployer());
				}

			} catch (Exception e) {
				throw new MprcException("Web UI class could not be created.", e);
			}
			return ui;
		}

		public SwiftMonitor getSwiftMonitor() {
			return swiftMonitor;
		}

		public void setSwiftMonitor(final SwiftMonitor swiftMonitor) {
			this.swiftMonitor = swiftMonitor;
		}

		public FileTokenFactory getFileTokenFactory() {
			return fileTokenFactory;
		}

		public void setFileTokenFactory(FileTokenFactory fileTokenFactory) {
			this.fileTokenFactory = fileTokenFactory;
		}

		public SwiftDao getSwiftDao() {
			return swiftDao;
		}

		public void setSwiftDao(SwiftDao swiftDao) {
			this.swiftDao = swiftDao;
		}

		public WorkspaceDao getWorkspaceDao() {
			return workspaceDao;
		}

		public void setWorkspaceDao(WorkspaceDao workspaceDao) {
			this.workspaceDao = workspaceDao;
		}

		public SearchEngine.Factory getSearchEngineFactory() {
			return searchEngineFactory;
		}

		public void setSearchEngineFactory(SearchEngine.Factory searchEngineFactory) {
			this.searchEngineFactory = searchEngineFactory;
		}

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public String getUserName() {
			return NAME;
		}

		@Override
		public String getDescription() {
			return DESC;
		}

		@Override
		public Class<? extends ResourceConfig> getConfigClass() {
			return Config.class;
		}

		@Override
		public ServiceUiFactory getServiceUiFactory() {
			return new Ui();
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

		public Config() {
		}

		public Config(final ServiceConfig searcher, final String port, final String title, final String browseRoot, final String browseWebRoot, final ServiceConfig qstat, final ServiceConfig databaseUndeployer) {
			this.searcher = searcher;
			this.port = port;
			this.title = title;
			this.browseRoot = browseRoot;
			this.browseWebRoot = browseWebRoot;
			this.qstat = qstat;
			this.databaseUndeployer = databaseUndeployer;
		}

		public void setSearcher(ServiceConfig searcher) {
			this.searcher = searcher;
		}

		public ServiceConfig getSearcher() {
			return searcher;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(SEARCHER, getSearcher());
			writer.put(TITLE, getTitle());
			writer.put(PORT, getPort());
			writer.put(BROWSE_ROOT, getBrowseRoot());
			writer.put(BROWSE_WEB_ROOT, getBrowseWebRoot());
			writer.put(QSTAT, getQstat());
			writer.put(DATABASE_UNDEPLOYER, getDatabaseUndeployer());
		}

		@Override
		public void load(final ConfigReader reader) {
			searcher = (ServiceConfig) reader.getObject(SEARCHER);
			title = reader.get(TITLE);
			port = reader.get(PORT);
			browseRoot = reader.get(BROWSE_ROOT);
			browseWebRoot = reader.get(BROWSE_WEB_ROOT);
			qstat = (ServiceConfig) reader.getObject(QSTAT);
			databaseUndeployer = (ServiceConfig) reader.getObject(DATABASE_UNDEPLOYER);
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

