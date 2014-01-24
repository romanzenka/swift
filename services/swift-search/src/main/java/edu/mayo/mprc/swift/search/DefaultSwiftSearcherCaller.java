package edu.mayo.mprc.swift.search;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.AssignedTaskData;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.DaemonUtilities;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.*;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.swift.params2.SavedSearchEngineParameters;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to send a socket request for a search run to {@link SwiftSearcher}.
 * Must be initialized with the daemon connection.
 */
@Component("swiftSearcherCaller")
public final class DefaultSwiftSearcherCaller implements SwiftSearcherCaller {
	private static final Logger LOGGER = Logger.getLogger(DefaultSwiftSearcherCaller.class);

	// After 10 seconds without hearing from the other side the search attempt timeouts
	private static final int SEARCH_TIMEOUT = 10 * 1000;

	private static final Pattern BAD_TITLE_CHARACTER = Pattern.compile("[^a-zA-Z0-9-+._()\\[\\]{}=# ]");

	private DaemonConnection swiftSearcherConnection;
	private SwiftDao swiftDao;
	private WorkspaceDao workspaceDao;
	private ParamsDao paramsDao;
	private File browseRoot;
	private boolean running;

	private DefaultSwiftSearcherCaller() {
	}

	@Override
	public void resubmitSearchRun(final SearchRun td, final ProgressListener listener) {
		try {
			final String sBatchName = td.getTitle();
			sendCallToDispatcher(getSwiftSearcherConnection(), td.getSwiftSearch(), sBatchName, false, 0, td.getId(), listener);
		} catch (Exception t) {
			throw new MprcException("resubmitSearchRun : failure sending call to dispatcher, " + t.getMessage(), t);
		}
	}

	@Override
	public long startSearchRestful(final SearchInput searchInput) {
		try {
			getSwiftDao().begin();
			final SwiftSearchDefinition swiftSearch = createSwiftSearchDefinition(searchInput);

			final SwiftSearchDefinition newSwiftSearch = validateAndSaveSearchDefinition(swiftSearch);

			getSwiftDao().commit();
			getSwiftDao().begin();
			final long searchRunId = submitSearch(newSwiftSearch.getId(),
					newSwiftSearch.getTitle(),
					0,
					searchInput.isFromScratch(),
					searchInput.isLowPriority() ? -1 : 0);
			getSwiftDao().commit();
			return searchRunId;
		} catch (Exception e) {
			getSwiftDao().rollback();
			throw new MprcException("New Swift search could not be started", e);
		}
	}

	@Override
	public SwiftSearchDefinition validateAndSaveSearchDefinition(final SwiftSearchDefinition swiftSearch) {
		validateSearch(swiftSearch);
		hideOlderSearches(swiftSearch);
		return getSwiftDao().addSwiftSearchDefinition(swiftSearch);
	}

	/**
	 * When an older search exists in the database that writes output to the same folder,
	 * it should get hidden so it does not get confused with this search.
	 *
	 * @param swiftSearch Current search to hide the older searches.
	 */
	private void hideOlderSearches(final SwiftSearchDefinition swiftSearch) {
		getSwiftDao().hideSearchesWithOutputFolder(swiftSearch.getOutputFolder());
	}

	private void validateSearch(final SwiftSearchDefinition swiftSearch) {
		final String title = swiftSearch.getTitle();
		if (title.isEmpty()) {
			throw new MprcException("Cannot run Swift search with an empty title");
		}
		validateTitleCharacters(title, "Search title");
		for (final FileSearch fileSearch : swiftSearch.getInputFiles()) {
			validateTitleCharacters(fileSearch.getExperiment(), "Experiment name " + fileSearch.getExperiment());
		}
	}

	static void validateTitleCharacters(final String title, final String location) {
		final Matcher badTitleMatcher = BAD_TITLE_CHARACTER.matcher(title);
		if (badTitleMatcher.find()) {
			throw new MprcException(location + " must not contain '" + badTitleMatcher.group() + "'");
		}
	}

	private SwiftSearchDefinition createSwiftSearchDefinition(final SearchInput searchInput) {
		final User user = getWorkspaceDao().getUserByEmail(searchInput.getUserEmail());
		final File outputFolder = new File(getBrowseRoot(), searchInput.getOutputFolderName());
		// TODO: This needs to be passed as a parameter
		final SpectrumQa qa = new SpectrumQa("conf/msmseval/msmsEval-orbi.params", SpectrumQa.DEFAULT_ENGINE);
		final PeptideReport report = searchInput.isPeptideReport() ? new PeptideReport() : null;

		final List<FileSearch> inputFiles = new ArrayList<FileSearch>(searchInput.getInputFilePaths().length);
		final EnabledEngines enabledEngines = getEnabledEngines(searchInput.getEnabledEngines());
		final int[] paramSetIds = searchInput.getParamSetIds();

		for (int i = 0; i < searchInput.getInputFilePaths().length; i++) {
			final String inputFilePath = searchInput.getInputFilePaths()[i];
			final File inputFile = new File(getBrowseRoot(), inputFilePath);
			final String biologicalSample = searchInput.getBiologicalSamples()[i];
			final String categoryName = searchInput.getCategoryNames()[i];
			final String experiment = searchInput.getExperiments()[i];

			inputFiles.add(new FileSearch(inputFile, biologicalSample, categoryName, experiment, enabledEngines, getSearchEngineParameters(paramSetIds[i])));
		}

		return new SwiftSearchDefinition(searchInput.getTitle(),
				user, outputFolder, qa, report, getSearchEngineParameters(searchInput.getParamSetId()), inputFiles,
				searchInput.isPublicMgfFiles(),
				searchInput.isPublicMzxmlFiles(),
				searchInput.isPublicSearchFiles()
		);
	}

	private SearchEngineParameters getSearchEngineParameters(final int paramSetId) {
		final SearchEngineParameters searchEngineParameters;
		final SavedSearchEngineParameters searchParameters;
		searchParameters = getParamsDao().getSavedSearchEngineParameters(paramSetId);
		if (searchParameters == null) {
			throw new MprcException("Could not find saved search parameters for ID: " + paramSetId);
		}
		searchEngineParameters = searchParameters.getParameters();
		return searchEngineParameters;
	}

	/**
	 * Convert a list of codes to enabled engines.
	 *
	 * @param enabledEngines Codes to convert.
	 * @return List of enabled engines.
	 */
	private static EnabledEngines getEnabledEngines(final String[] enabledEngines) {
		final EnabledEngines result = new EnabledEngines();
		final int length = enabledEngines.length;
		for (int i = 0; i < length; i++) {
			final String engine = enabledEngines[i];
			final int split = engine.indexOf('-');
			if (split < 0) {
				throw new MprcException("Engine [" + engine + "] is not in <engine>-<version> format.");
			}
			final SearchEngineConfig config = new SearchEngineConfig(engine.substring(0, split), engine.substring(split + 1));
			result.add(config);
		}
		return result;
	}

	@Override
	public long submitSearch(final int searchId, final String batchName, final int previousSearchRunId, final boolean fromScratch, final int priority) throws InterruptedException {
		final DefaultSwiftSearcherCaller.SearchProgressListener listener =
				startSearch(searchId, batchName, fromScratch, priority,
						previousSearchRunId);
		listener.waitForSearchReady(SEARCH_TIMEOUT);
		if (listener.getException() != null) {
			throw new MprcException(listener.getException());
		}

		if (listener.getSearchRunId() == 0) {
			throw new MprcException("The search was not started within timeout.");
		}
		return listener.getSearchRunId();
	}


	private SearchProgressListener startSearch(final int swiftSearchId, final String batchName, final boolean fromScratch, final int priority, final int previousSearchId) {
		final SearchProgressListener listener = new SearchProgressListener();
		sendCallToDispatcher(getSwiftSearcherConnection(), swiftSearchId, batchName, fromScratch, priority, previousSearchId, listener);
		return listener;
	}

	private static void sendCallToDispatcher(final DaemonConnection connection, final Integer swiftSearchId, final String sBatchName, final boolean fromScratch, final int priority, final int previousSearchId, final ProgressListener listener) {
		// Send work. We are not interested in progress at all, but we must specify progress listener
		final SwiftSearchWorkPacket workPacket = new SwiftSearchWorkPacket(swiftSearchId, sBatchName, fromScratch, previousSearchId);
		workPacket.setPriority(priority);
		connection.sendWork(workPacket, listener);
	}

	public DaemonConnection getSwiftSearcherConnection() {
		return swiftSearcherConnection;
	}

	public void setSwiftSearcherConnection(final DaemonConnection swiftSearcherConnection) {
		this.swiftSearcherConnection = swiftSearcherConnection;
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	@Resource(name = "swiftDao")
	public void setSwiftDao(final SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}

	public WorkspaceDao getWorkspaceDao() {
		return workspaceDao;
	}

	@Resource(name = "workspaceDao")
	public void setWorkspaceDao(final WorkspaceDao workspaceDao) {
		this.workspaceDao = workspaceDao;
	}

	public ParamsDao getParamsDao() {
		return paramsDao;
	}

	@Resource(name = "paramsDao")
	public void setParamsDao(final ParamsDao paramsDao) {
		this.paramsDao = paramsDao;
	}

	@Override
	public File getBrowseRoot() {
		return browseRoot;
	}

	public void setBrowseRoot(File browseRoot) {
		this.browseRoot = browseRoot;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void start() {
		if (!isRunning()) {
			DaemonUtilities.startDaemonConnections(swiftSearcherConnection);
			running = true;
		}
	}

	@Override
	public void stop() {
	}

	private static class SearchProgressListener implements ProgressListener {
		private Exception exception;
		private long searchId;
		private boolean running = true;
		private volatile boolean ready;
		private final Object monitor = new Object();

		SearchProgressListener() {
			exception = null;
		}

		public boolean isRunning() {
			synchronized (monitor) {
				return running;
			}
		}

		public long getSearchRunId() {
			synchronized (monitor) {
				return searchId;
			}
		}

		public Exception getException() {
			synchronized (monitor) {
				return exception;
			}
		}

		@Override
		public void requestEnqueued(final String hostString) {
			LOGGER.debug("Request enqueued " + hostString);
		}

		@Override
		public void requestProcessingStarted(final String hostString) {
			LOGGER.debug("Request processing started " + hostString);
		}

		public void waitForSearchReady(final long timeout) throws InterruptedException {
			synchronized (monitor) {
				while (!ready) {
					monitor.wait(timeout);
				}
			}
		}

		@Override
		public void requestProcessingFinished() {
			LOGGER.debug("Request processing finished successfully");
			synchronized (monitor) {
				ready = true;
				running = false;
				monitor.notifyAll();
			}
		}

		@Override
		public void requestTerminated(final Exception e) {
			LOGGER.debug("Request terminated with error", e);
			synchronized (monitor) {
				ready = true;
				running = false;
				exception = e;
				monitor.notifyAll();
			}
		}

		@Override
		public void userProgressInformation(final ProgressInfo progressInfo) {
			synchronized (monitor) {
				if (progressInfo instanceof AssignedSearchRunId) {
					searchId = ((AssignedSearchRunId) progressInfo).getSearchRunId();
				}

				// First ProgressInfo object must be the AssignedTaskData
				if (progressInfo instanceof AssignedTaskData) {
					return;
				}

				ready = true;
				monitor.notifyAll();
			}
		}
	}
}
