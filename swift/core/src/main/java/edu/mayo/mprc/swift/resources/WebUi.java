package edu.mayo.mprc.swift.resources;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.ReleaseInfoCore;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.*;
import edu.mayo.mprc.daemon.AbstractRunner;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.UiConfigurationProvider;
import edu.mayo.mprc.dbcurator.model.CurationContext;
import edu.mayo.mprc.msmseval.MSMSEvalParamFile;
import edu.mayo.mprc.msmseval.MSMSEvalWorker;
import edu.mayo.mprc.msmseval.MsmsEvalCache;
import edu.mayo.mprc.qstat.QstatDaemonWorker;
import edu.mayo.mprc.quameterdb.InstrumentNameMapper;
import edu.mayo.mprc.quameterdb.QuameterUi;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

/**
 * A class holding information about WebUI configuration.
 */
public final class WebUi implements Checkable {
	private static final Logger LOGGER = Logger.getLogger(WebUi.class);

	public static final String TYPE = "webUi";
	public static final String NAME = "Swift Website";
	public static final String DESC = "Swift's web user interface.<p>The daemon that contains the web interface will run within a web server.</p>";
	public static final UserMessage USER_MESSAGE = new UserMessage();
	private File browseRoot;
	private DaemonConnection qstatDaemonConnection;
	private String browseWebRoot;
	private File newConfigFile;
	private DaemonConnection swiftSearcherDaemonConnection;
	private Collection<SearchEngine> searchEngines;
	private boolean scaffoldReport;
	private boolean msmsEval;
	private List<MSMSEvalParamFile> spectrumQaParamFiles;
	private CurationContext curationContext;
	private String title;
	private DatabaseFileTokenFactory fileTokenFactory;
	private Daemon mainDaemon;
	private String scaffoldViewerUrl; // Where does the Scaffold Viewer live
	private boolean extractMsn;
	private boolean msconvert;
	private InstrumentNameMapper quameterUi;

	public static final String SEARCHER = "searcher";
	public static final String TITLE = "title";
	public static final String PORT = "port";
	public static final String BROWSE_ROOT = "browseRoot";
	public static final String BROWSE_WEB_ROOT = "browseWebRoot";
	public static final String NEW_CONFIG_FILE = "newConfigFile";
	public static final String QSTAT = "qstat";
	public static final String SCAFFOLD_VIEWER_URL = "scaffoldViewerUrl";
	public static final String QUAMETER_UI = "quameterUi";

	private static final String DEFAULT_SCAFFOLD_VIEWER_URL = "http://www.proteomesoftware.com/products/free-viewer/";

	public WebUi() {
		// USER_MESSAGE.setMessage("Swift's new database deployment has been temporarily disabled. Swift needs to be upgraded to support Mascot's Database Manager. If you need a new database, please ask Roman.");
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

	public void setBrowseRoot(File browseRoot) {
		this.browseRoot = browseRoot;
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
	 * is pointed to by this web ui.
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

	public List<MSMSEvalParamFile> getSpectrumQaParamFiles() {
		return spectrumQaParamFiles;
	}

	public String getTitle() {
		return title;
	}

	public DatabaseFileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	public void setFileTokenFactory(DatabaseFileTokenFactory fileTokenFactory) {
		this.fileTokenFactory = fileTokenFactory;
	}

	public UserMessage getUserMessage() {
		// TODO - re-enabled message support
		return USER_MESSAGE;
	}

	public Daemon getMainDaemon() {
		return mainDaemon;
	}

	public void setMainDaemon(Daemon mainDaemon) {
		this.mainDaemon = mainDaemon;
	}

	public File getNewConfigFile() {
		return newConfigFile;
	}

	public void setNewConfigFile(File newConfigFile) {
		this.newConfigFile = newConfigFile;
	}

	@Override
	public String check() {
		LOGGER.info("Checking web user interface");
		if (browseRoot == null) {
			return BROWSE_ROOT + " must be set";
		}
		if (!browseRoot.isDirectory()) {
			return BROWSE_ROOT + " must refer to a directory";
		}
		if (!browseRoot.canRead()) {
			return BROWSE_ROOT + " must be readable";
		}
		return null;
	}

	public boolean isExtractMsn() {
		return extractMsn;
	}

	public boolean isMsconvert() {
		return msconvert;
	}

	/**
	 * @return A map containing the collected user interface configuration.
	 * This is done by collecting all configuration elements across
	 * all the resources/plugins for this daemon.
	 */
	public Map<String, String> getUiConfiguration() {
		final Map<String, String> uiConfiguration = new HashMap<String, String>(10);
		for (final Object resource : getMainDaemon().getResources()) {
			if (resource instanceof UiConfigurationProvider) {
				final UiConfigurationProvider provider = (UiConfigurationProvider) resource;
				provider.provideConfiguration(uiConfiguration);
			}
		}
		for (final AbstractRunner runner : getMainDaemon().getRunners()) {
			runner.provideConfiguration(uiConfiguration);
		}
		return uiConfiguration;
	}

	/**
	 * Replace instrument names with human readable ones for a list of searches.
	 *
	 * @param searches Searches to replace the instrument names in.
	 */
	public void mapInstrumentSerialNumbers(List<SearchRun> searches) {
		for (final SearchRun searchRun : searches) {
			searchRun.setInstruments(
					mapInstrumentSerialNumbers(
							searchRun.getInstruments()));
		}
	}

	/**
	 * @param instruments Comma-separated instrument names
	 * @return Instrument names translated as per QuaMeter
	 */
	public String mapInstrumentSerialNumbers(final String instruments) {
		if (quameterUi == null) {
			return instruments;
		}
		final Iterable<String> split = Splitter.on(',').trimResults().omitEmptyStrings().split(instruments);
		final List<String> mapped = new ArrayList<String>(5);
		for (final String serial : split) {
			mapped.add(quameterUi.mapInstrument(serial));
		}
		final String result = Joiner.on(", ").join(
				Ordering.from(String.CASE_INSENSITIVE_ORDER)
						.sortedCopy(mapped));
		return result;
	}

	public String getScaffoldViewerUrl() {
		return scaffoldViewerUrl;
	}

	public void setScaffoldViewerUrl(String scaffoldViewerUrl) {
		this.scaffoldViewerUrl = scaffoldViewerUrl;
	}

	/**
	 * A factory capable of creating the web ui class.
	 */
	public static final class Factory extends FactoryBase<Config, WebUi> implements FactoryDescriptor {
		private DatabaseFileTokenFactory fileTokenFactory;
		private SearchEngine.Factory searchEngineFactory;
		private CurationContext curationContext;

		@Override
		public WebUi create(final Config config, final DependencyResolver dependencies) {
			WebUi ui = null;
			try {
				ui = new WebUi();
				ui.title = config.getTitle();
				ui.browseRoot = new File(config.getBrowseRoot());
				ui.browseWebRoot = config.getBrowseWebRoot();
				ui.newConfigFile = config.getNewConfigFile() == null ? null : new File(config.getNewConfigFile());
				ui.setFileTokenFactory(getFileTokenFactory());
				ui.setScaffoldViewerUrl(config.getScaffoldViewerUrl());

				if (config.getQstat() != null) {
					ui.qstatDaemonConnection = (DaemonConnection) dependencies.createSingleton(config.getQstat());
				}

				if (config.getQuameterUi() != null) {
					ui.quameterUi = (QuameterUi) dependencies.createSingleton(config.getQuameterUi());
				}

				// Harvest the param files from searcher config
				if (config.getSearcher() != null) {
					ui.swiftSearcherDaemonConnection = (DaemonConnection) dependencies.createSingleton(config.getSearcher());
					final SwiftSearcher.Config searcherConfig = (SwiftSearcher.Config) config.getSearcher().getRunner().getWorkerConfiguration();

					ui.curationContext = curationContext;
					ui.curationContext.initialize(new File(searcherConfig.getFastaPath()),
							new File(searcherConfig.getFastaUploadPath()),
							new File(searcherConfig.getFastaArchivePath()),
							// TODO: Fix this - the curator will keep creating temp folders and never deleting them
							// TODO: Also, the user should be able to specify where the temp files should go
							FileUtilities.createTempFolder());

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
							ui.spectrumQaParamFiles = parseSpectrumQaParamFiles(msmsEvalConfig.get(MSMSEvalWorker.PARAM_FILES));
						}
					}

					if (searcherConfig.getRaw2mgf() != null) {
						ui.extractMsn = true;
					}

					if (searcherConfig.getMsconvert() != null) {
						ui.msconvert = true;
					}

					if (searcherConfig.getScaffoldReport() != null) {
						ui.scaffoldReport = true;
					}

					final Collection<SearchEngine> searchEngines = Lists.newArrayList();
					if (searcherConfig.getEngines() != null) {
						for (final SearchEngine.Config engineConfig : searcherConfig.getEngines()) {
							if (engineConfig.getWorker() != null) {
								final SearchEngine engine = getSearchEngineFactory().create(engineConfig, dependencies);
								searchEngines.add(engine);
							}
						}
					}
					ui.setSearchEngines(searchEngines);
				}
			} catch (Exception e) {
				throw new MprcException("Web UI class could not be created.", e);
			}
			return ui;
		}

		public DatabaseFileTokenFactory getFileTokenFactory() {
			return fileTokenFactory;
		}

		public void setFileTokenFactory(DatabaseFileTokenFactory fileTokenFactory) {
			this.fileTokenFactory = fileTokenFactory;
		}

		public SearchEngine.Factory getSearchEngineFactory() {
			return searchEngineFactory;
		}

		public void setSearchEngineFactory(SearchEngine.Factory searchEngineFactory) {
			this.searchEngineFactory = searchEngineFactory;
		}

		public CurationContext getCurationContext() {
			return curationContext;
		}

		public void setCurationContext(CurationContext curationContext) {
			this.curationContext = curationContext;
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
		// This variable is special - it is "secret" in the sense that the UI does not show it
		// It is used solely as a way for an installer to tweak where does the newly saved config get saved.
		private String newConfigFile;
		private ServiceConfig qstat;
		private String scaffoldViewerUrl;
		private ResourceConfig quameterUi;

		public Config() {
		}

		public Config(final ServiceConfig searcher, final String port, final String title, final String browseRoot,
		              final String browseWebRoot, final String newConfigFile, final ServiceConfig qstat,
		              final String scaffoldViewerUrl, final ResourceConfig quameterUi) {
			this.searcher = searcher;
			this.port = port;
			this.title = title;
			this.browseRoot = browseRoot;
			this.browseWebRoot = browseWebRoot;
			this.newConfigFile = newConfigFile;
			this.qstat = qstat;
			this.scaffoldViewerUrl = scaffoldViewerUrl;
			this.quameterUi = quameterUi;
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
			writer.put(NEW_CONFIG_FILE, getNewConfigFile());
			writer.put(QSTAT, getQstat());
			writer.put(SCAFFOLD_VIEWER_URL, getScaffoldViewerUrl());
			writer.put(QUAMETER_UI, getQuameterUi());
		}

		@Override
		public void load(final ConfigReader reader) {
			searcher = (ServiceConfig) reader.getObject(SEARCHER);
			title = reader.get(TITLE);
			port = reader.get(PORT);
			browseRoot = reader.get(BROWSE_ROOT);
			browseWebRoot = reader.get(BROWSE_WEB_ROOT);
			newConfigFile = reader.get(NEW_CONFIG_FILE);
			qstat = (ServiceConfig) reader.getObject(QSTAT);
			scaffoldViewerUrl = reader.get(SCAFFOLD_VIEWER_URL, DEFAULT_SCAFFOLD_VIEWER_URL);
			quameterUi = reader.getObject(QUAMETER_UI);
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

		public ResourceConfig getQuameterUi() {
			return quameterUi;
		}

		public String getScaffoldViewerUrl() {
			return scaffoldViewerUrl;
		}

		public void setNewConfigFile(final String newConfigFile) {
			this.newConfigFile = newConfigFile;
		}

		public String getNewConfigFile() {
			return newConfigFile;
		}
	}

	/**
	 * Parses a comma delimited string in {@code desc1, file1, desc2, file2, ...} format
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

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			final ResourceConfig swiftSearcher = daemon.firstServiceOfType(SwiftSearcher.Config.class);

			builder
					.property(TITLE, "Installation Title", "This is displayed as the title of the Swift web pages.<br/>" +
							"You can use it to distinguish between Swift installs.")
					.required()
					.defaultValue("Swift " + ReleaseInfoCore.buildVersion())

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

					.property(QSTAT, "Qstat", "If you are running in Sun Grid Engine and want to have the job status available from the web interface, add a Qstat module. This is completely optional and provided solely for user convenience.")
					.reference(QstatDaemonWorker.TYPE, UiBuilder.NONE_TYPE)

					.property(SCAFFOLD_VIEWER_URL, "Scaffold Viewer URL", "Display this URL so the users can download a specific version of Scaffold viewer.<br><p>The default download location is: <a href=\"" + DEFAULT_SCAFFOLD_VIEWER_URL + "\">" + DEFAULT_SCAFFOLD_VIEWER_URL + "</a>")
					.defaultValue(DEFAULT_SCAFFOLD_VIEWER_URL)

					.property(QUAMETER_UI, "Quameter UI", "We use the instrument name map from QuaMeter to display our instruments in a more user friendly manner")
					.reference(QuameterUi.TYPE, UiBuilder.NONE_TYPE)

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

