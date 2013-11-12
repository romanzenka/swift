package edu.mayo.mprc.swift.ui.server;

import edu.mayo.mprc.GWTServiceExceptionFactory;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.common.client.GWTServiceException;
import edu.mayo.mprc.common.server.SpringGwtServlet;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.CurationDao;
import edu.mayo.mprc.dbundeploy.DatabaseUndeployerCaller;
import edu.mayo.mprc.dbundeploy.DatabaseUndeployerProgress;
import edu.mayo.mprc.msmseval.MSMSEvalParamFile;
import edu.mayo.mprc.swift.MainFactoryContext;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.ParamName;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.swift.params2.SavedSearchEngineParameters;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import edu.mayo.mprc.swift.params2.mapping.ParamsInfo;
import edu.mayo.mprc.swift.params2.mapping.ParamsValidations;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import edu.mayo.mprc.swift.search.SwiftSearcherCaller;
import edu.mayo.mprc.swift.ui.client.InitialPageData;
import edu.mayo.mprc.swift.ui.client.Service;
import edu.mayo.mprc.swift.ui.client.rpc.*;
import edu.mayo.mprc.swift.ui.client.rpc.files.*;
import edu.mayo.mprc.unimod.UnimodDao;
import edu.mayo.mprc.utilities.NotHiddenFilter;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDao;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Lists files and folders for given path (relative to specified root).
 * The result is returned as a tree of objects.
 * <p/>
 * Supports the concept of "expanded paths" - those paths are recursed and returned fully,
 * otherwise the result of a single call lists just contents of a single directory without recursion.
 */
public final class ServiceImpl extends SpringGwtServlet implements Service, ApplicationContextAware {
	private static final long serialVersionUID = 20071220L;
	private static final Logger LOGGER = Logger.getLogger(ServiceImpl.class);

	// String of allowed extensions, separated by | signs. The extensions are case insensitive.
	private static final String ALLOWED_EXTENSIONS = ".RAW|.raw|.mgf";
	public static final InputFileFilter FILTER = new InputFileFilter(ALLOWED_EXTENSIONS, ".d", true);
	private static final InputFileFilter FILTER_DIRS = new InputFileFilter(ALLOWED_EXTENSIONS, "", true);

	private static final ClientUser[] EMPTY_USER_LIST = new ClientUser[0];
	public static final String AGILENT_FOLDER_SUFFIX = ".d";

	private WebUiHolder webUiHolder;

	private SwiftDao swiftDao;
	private CurationDao curationDao;
	private ParamsDao paramsDao;
	private ParamsInfo paramsInfo;
	private UnimodDao unimodDao;
	private WorkspaceDao workspaceDao;
	private SwiftSearcherCaller swiftSearcherCaller;

	public ServiceImpl() {
	}

	@Override
	public Entry listFiles(final String relativePath, final String[] expandedPaths) throws GWTServiceException {
		try {
			final Entry rootEntry = new DirectoryEntry("(root)");
			final File[] expandedFiles;
			if (expandedPaths == null) {
				expandedFiles = EMPTY_EXPANDED_FILES;
			} else {
				expandedFiles = new File[expandedPaths.length];
				for (int i = 0; i < expandedPaths.length; i++) {
					expandedFiles[i] = new File(getBrowseRoot(), expandedPaths[i]);
				}
			}
			listDirectoryContents(rootEntry, new File(getBrowseRoot(), relativePath), expandedFiles);
			return rootEntry;
		} catch (Exception t) {
			LOGGER.error("Could not list files", t);
			throw GWTServiceExceptionFactory.createException("Could not list files", t);
		}
	}

	@Override
	public ClientUser[] listUsers() throws GWTServiceException {
		try {
			getWorkspaceDao().begin();
			final List<User> users = getWorkspaceDao().getUsers(true);

			final ClientUser[] result;
			if (users != null) {
				result = new ClientUser[users.size()];
				for (int i = 0; i < users.size(); i++) {
					final User user = users.get(i);
					result[i] = getClientProxyGenerator().convertTo(user);
				}
			} else {
				result = EMPTY_USER_LIST;
			}
			getWorkspaceDao().commit();
			return result;
		} catch (Exception t) {
			getWorkspaceDao().rollback();
			LOGGER.error("Could not list users", t);
			throw GWTServiceExceptionFactory.createException("Could not list users", t);
		}
	}

	/**
	 * TODO: This shoudl be in {@link edu.mayo.mprc.swift.search.DefaultSwiftSearcherCaller}, however the caller has no clue about {@link ClientSwiftSearchDefinition}.
	 */
	@Override
	public void startSearch(final ClientSwiftSearchDefinition def) throws GWTServiceException {
		try {
			final HttpSession session = getSession();
			getSwiftDao().begin();
			final ParameterSetCache cache = new ParameterSetCache(session, getParamsDao());

			SearchEngineParameters parameters = cache.getFromCacheHibernate(def.getParamSet());
			if (parameters.getId() == null) {
				parameters = getParamsDao().addSearchEngineParameters(parameters);
			}


			SwiftSearchDefinition swiftSearch = getClientProxyGenerator().convertFrom(def, parameters);

			final SearchRun previousSearchRun = getPreviousSearchRun(def.getPreviousSearchRunId(), swiftSearch);

			final SwiftSearchDefinition newSwiftSearch = getSwiftSearcherCaller().validateAndSaveSearchDefinition(swiftSearch);

			getSwiftDao().commit(); // We must commit here before we send the search over (it is loaded from the database)

			getSwiftSearcherCaller().submitSearch(newSwiftSearch.getId(),
					newSwiftSearch.getTitle(),
					previousSearchRun == null ? 0 : previousSearchRun.getId(),
					def.isFromScratch(),
					def.isLowPriority() ? -1 : 0);
		} catch (Exception e) {
			getSwiftDao().rollback();
			LOGGER.error("Search could not be started", e);
			throw GWTServiceExceptionFactory.createException("Search could not be started", e);
		}
	}

	private SearchRun getPreviousSearchRun(final int previousSearchRunId, final SwiftSearchDefinition swiftSearch) {
		boolean rerunPreviousSearch = false;
		SearchRun previousSearchRun;
		if (previousSearchRunId > 0) {
			// We already ran the search before.
			previousSearchRun = getSwiftDao().getSearchRunForId(previousSearchRunId);
			if (previousSearchRun.getTitle().equals(swiftSearch.getTitle())) {
				// The titles match, output folders match, but since this is a reload, it is okay
				rerunPreviousSearch = true;
				final Integer searchId = previousSearchRun.getSwiftSearch();
				final SwiftSearchDefinition searchDefinition = getSwiftDao().getSwiftSearchDefinition(searchId);
				if (searchDefinition == null || !searchDefinition.getOutputFolder().equals(swiftSearch.getOutputFolder())) {
					previousSearchRun = null;
					// Since the folders do not match, we will rerun, but will not hide the previous search run
					// from the list
				}
			} else {
				previousSearchRun = null;
			}
		} else {
			previousSearchRun = null;
		}
		return previousSearchRun;
	}

	@Override
	public ClientLoadedSearch loadSearch(final int searchRunId) throws GWTServiceException {
		try {
			final HttpSession session = getSession();
			getSwiftDao().begin();
			final ParameterSetCache cache = new ParameterSetCache(session, getParamsDao());
			final SearchRun searchRun = getSwiftDao().getSearchRunForId(searchRunId);
			final SwiftSearchDefinition original = getSwiftDao().getSwiftSearchDefinition(searchRun.getSwiftSearch());
			final Resolver resolver = new Resolver(cache);
			final ClientSwiftSearchDefinition proxy = getClientProxyGenerator().convertTo(original, resolver);
			final ClientLoadedSearch result = new ClientLoadedSearch(searchRunId, proxy, resolver.isClientParamSetListChanged() ? makeParamSetList(cache) : null);
			getSwiftDao().commit();
			return result;
		} catch (Exception e) {
			getSwiftDao().rollback();
			LOGGER.error("Could not load existing search for id: " + searchRunId, e);
			throw GWTServiceExceptionFactory.createException("Search could not be loaded for id: " + searchRunId, e);
		}
	}

	@Override
	public List<ClientSearchEngine> listSearchEngines() throws GWTServiceException {
		final List<ClientSearchEngine> infos = new ArrayList<ClientSearchEngine>();
		for (final SearchEngine engine : getSearchEngines()) {
			infos.add(new ClientSearchEngine(
					new ClientSearchEngineConfig(engine.getCode(), engine.getVersion()),
					engine.getFriendlyName(), engine.isOnByDefault(),
					engine.getEngineMetadata().getOrder()));
		}
		return infos;
	}

	@Override
	public List<SpectrumQaParamFileInfo> listSpectrumQaParamFiles() throws GWTServiceException {

		final List<SpectrumQaParamFileInfo> paramFiles = new ArrayList<SpectrumQaParamFileInfo>();
		if (isMsmsEval()) {
			for (final MSMSEvalParamFile paramFile : getSpectrumQaParamFiles()) {
				paramFiles.add(new SpectrumQaParamFileInfo(paramFile.getPath(), paramFile.getDescription()));
			}
		}
		return paramFiles;
	}

	@Override
	public boolean isScaffoldReportEnabled() throws GWTServiceException {
		return isScaffoldReport();
	}

	@Override
	public String getUserMessage() throws GWTServiceException {
		// TODO - re-add support for user messages
		return null;
	}

	@Override
	public FileInfo[] findFiles(final String[] relativePaths) throws GWTServiceException {
		try {
			// Filter paths so we never have both parent and a child in the list.
			final List<String> filteredPaths = new ArrayList<String>();
			for (int i = 0; i < relativePaths.length; i++) {
				int j;
				for (j = 0; j < relativePaths.length; j++) {
					// Our path is a child, throw it away
					if (i != j && relativePaths[i].startsWith(relativePaths[j])) {
						break;
					}
				}
				// This path is legitimate, not a child of anything
				if (j == relativePaths.length) {
					filteredPaths.add(relativePaths[i]);
				}
			}

			final List<FileInfo> list = new ArrayList<FileInfo>();

			for (final String path : filteredPaths) {
				addPathToList(new File(getBrowseRoot(), path), list);
			}

			final FileInfo[] results = new FileInfo[list.size()];
			return list.toArray(results);
		} catch (Exception t) {
			LOGGER.error("Could not find files", t);
			throw GWTServiceExceptionFactory.createException("Could not find files", t);
		}
	}

	private void addPathToList(final File file, final List<FileInfo> list) {
		if (!file.exists() || file.isHidden()) {
			return;
		}
		if (file.isDirectory() && !isDirectoryTreatedAsFile(file)) {
			// Recursively add the contents
			// TODO: Limit the running time
			final File[] files = file.listFiles(FILTER_DIRS);
			Arrays.sort(files);
			for (final File path : files) {
				addPathToList(path, list);
			}
		} else {
			if (FILTER.accept(file.getParentFile(), file.getName())) {
				String path = file.getAbsolutePath();
				path = path.substring(getBrowseRoot().getAbsolutePath().length());
				path = path.replaceAll(Pattern.quote(File.separator), "/");
				list.add(new FileInfo(path, file.length()));
			}
		}
	}

	private boolean isDirectoryTreatedAsFile(final File file) {
		return file.getName().endsWith(AGILENT_FOLDER_SUFFIX);
	}

	/**
	 * Returns true if at least one of the subs is subfolder (direct or indirect) of dir.
	 * If subs are null, the result is false, if dir is null, the result is true. This
	 * follows the logic, that the subs are a list of "expanded" paths and we are trying to determine,
	 * whether the given folder should be expanded or not.
	 *
	 * @param dir  The folder that has to contain at least one of the subs.
	 * @param subs Array of subfolder paths.
	 * @return True when at least one of the subs is inside dir (directly or not).
	 */
	private boolean isSubfolder(final File dir, final File[] subs) {
		if (subs == null) {
			return false;
		}
		if (dir == null) {
			return true;
		}
		for (final File sub : subs) {
			if (sub.getPath().startsWith(dir.getPath())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Lists contents of given directory and appends them to given file element.
	 * Errors are appended as special "error node".
	 *
	 * @param rootEntry     Entry to insert contents into.
	 * @param root          Root directory.
	 * @param expandedPaths List of paths that have to be expanded in the listing.
	 */
	private void listDirectoryContents(final Entry rootEntry, final File root, final File[] expandedPaths) {
		// find all directories
		File dirs[] = null;
		try {
			dirs = root.listFiles(new NotHiddenFilter());
		} catch (SecurityException e) {
			LOGGER.debug("Could not list contents of " + root.getAbsolutePath(), e);
			rootEntry.addChild(new ErrorEntry(MessageFormat.format("Could not list contents of {0}: {1}", root.getAbsolutePath(), e.getMessage())));
			return;
		}

		if (dirs != null) {
			Arrays.sort(dirs, new FilenameComparator());
			for (final File dir : dirs) {
				if (dir.isDirectory()) {
					final DirectoryEntry directory = new DirectoryEntry(dir.getName());
					rootEntry.addChild(directory);
					// If this directory should be expanded
					if (isSubfolder(dir, expandedPaths)) {
						listDirectoryContents(directory, dir, expandedPaths);
					}
				}
			}
		}

		// find all the files with allowed extension
		File[] files = null;
		try {
			files = root.listFiles(FILTER);
		} catch (SecurityException e) {
			LOGGER.debug("Could not list contents of " + root.getAbsolutePath(), e);
			rootEntry.addChild(new ErrorEntry(MessageFormat.format("Could not list contents of {0}: {1}", root.getAbsolutePath(), e.getMessage())));
			return;
		}
		if (files != null) {
			Arrays.sort(files, new FilenameComparator());
			for (final File file : files) {
				if (!file.isDirectory()) {
					rootEntry.addChild(new FileEntry(file.getName()));
				}
			}
		}
	}

	private static final File[] EMPTY_EXPANDED_FILES = new File[0];

	@Override
	public Boolean login(final String userName, final String password) throws GWTServiceException {
		return true;
	}

	@Override
	public synchronized ClientParamSet save(final ClientParamSet toCopy, final String newName, final String ownerEmail,
	                                        final String ownerInitials,
	                                        final boolean permanent) throws GWTServiceException {
		try {
			final HttpSession session = getSession();
			getParamsDao().begin();
			final ParameterSetCache cache = new ParameterSetCache(session, getParamsDao());
			SearchEngineParameters ps = cache.getFromCacheHibernate(toCopy);
			final ClientParamSet ret;
			if (!permanent) {
				ret = cache.installTemporary(toCopy);
			} else {
				final Change change = new Change("Saving parameter set " + newName, new DateTime());

				// Delete if already exists
				final SavedSearchEngineParameters params = getParamsDao().findSavedSearchEngineParameters(newName);
				if (params != null) {
					getParamsDao().deleteSavedSearchEngineParameters(params, change);
				}

				ps = getParamsDao().addSearchEngineParameters(ps);
				final User user = getWorkspaceDao().getUserByEmail(ownerEmail);

				SavedSearchEngineParameters newParams = new SavedSearchEngineParameters(
						newName, user, ps);

				newParams = getParamsDao().addSavedSearchEngineParameters(newParams, change);

				if (toCopy.isTemporary()) {
					cache.removeFromCache(toCopy);
				}
				ret = new ClientParamSet(newParams.getId(), newName, ownerEmail, ownerInitials);
			}
			getParamsDao().commit();
			return ret;
		} catch (Exception e) {
			getParamsDao().rollback();
			LOGGER.error("Could not save client parameter set", e);
			throw GWTServiceExceptionFactory.createException("Could not save client parameter set", e);
		}
	}


	@Override
	public ClientParamFile[] getFiles(final ClientParamSet paramSet) throws GWTServiceException {
		try {
			final HttpSession session = getSession();
			getParamsDao().begin();
			final ParameterSetCache cache = new ParameterSetCache(session, getParamsDao());
			final SearchEngineParameters ps = cache.getFromCache(paramSet);
			final ClientParamFile[] files = new ClientParamFile[getSearchEngines().size()];

			int i = 0;
			for (final SearchEngine engine : getSearchEngines()) {
				final String parameters = engine.writeSearchEngineParameterString(ps, null, paramsInfo);
				files[i++] = new ClientParamFile(engine.getFriendlyName(), parameters);
			}
			getParamsDao().commit();
			return files;
		} catch (Exception e) {
			getParamsDao().rollback();
			LOGGER.error("Could not get parameter files", e);
			throw GWTServiceExceptionFactory.createException("Could not get parameter files", e);
		}
	}

	@Override
	public ClientParamSetValues getParamSetValues(final ClientParamSet paramSet) throws GWTServiceException {
		try {
			final HttpSession session = getSession();
			getParamsDao().begin();
			final ParameterSetCache cache = new ParameterSetCache(session, getParamsDao());
			final SearchEngineParameters ps = cache.getFromCache(paramSet);
			final ParamsValidations paramsValidations = SearchEngine.validate(ps, getSearchEngines(), paramsInfo);
			final ClientParamSetValues clientParamSetValues = getClientProxyGenerator().convertValues(ps, paramsValidations);
			getParamsDao().commit();
			return clientParamSetValues;
		} catch (Exception e) {
			getParamsDao().rollback();
			LOGGER.error("Could not get parameter set values", e);
			throw GWTServiceExceptionFactory.createException("Could not get parameter set values", e);
		}
	}

	@Override
	public synchronized ClientParamSetList getParamSetList() throws GWTServiceException {
		try {
			final HttpSession session = getSession();
			getParamsDao().begin();
			final ParameterSetCache cache = new ParameterSetCache(session, getParamsDao());
			final ClientParamSetList paramSetList = makeParamSetList(cache);
			getParamsDao().commit();
			return paramSetList;
		} catch (Exception e) {
			getParamsDao().rollback();
			LOGGER.error("Could not obtain parameter set list", e);
			throw GWTServiceExceptionFactory.createException("Could not obtain parameter set list", e);
		}
	}

	private ClientParamSetList makeParamSetList(final ParameterSetCache cache) {
		final List<SavedSearchEngineParameters> engineParametersList = getParamsDao().savedSearchEngineParameters();
		return ClientProxyGenerator.getClientParamSetList(engineParametersList, cache.getTemporaryClientParamList());
	}

	@Override
	public List<List<ClientValue>> getAllowedValues(final String[] params) throws GWTServiceException {
		try {
			getParamsDao().begin();

			final List<List<ClientValue>> values = new ArrayList<List<ClientValue>>();
			for (final String param : params) {
				final ParamName name = ParamName.getById(param);
				values.add(getClientProxyGenerator().getAllowedValues(name, getParamsInfo()));
			}
			getParamsDao().commit();
			return values;
		} catch (Exception e) {
			getParamsDao().rollback();
			LOGGER.error("Could not get parameter set allowed values", e);
			throw GWTServiceExceptionFactory.createException("Could not refresh parameter set", e);
		}
	}

	@Override
	public ClientParamsValidations update(final ClientParamSet paramSet, final String param, final ClientValue value) throws GWTServiceException {
		try {
			final HttpSession session = getSession();
			getParamsDao().begin();
			final ParameterSetCache cache = new ParameterSetCache(session, getParamsDao());

			final SearchEngineParameters ps = cache.getFromCache(paramSet);
			try {
				final ParamName name = ParamName.getById(param);
				ps.setValue(name, getClientProxyGenerator().convert(value, getParamsInfo().getAllowedValues(name)));
				final ParamsValidations validations = SearchEngine.validate(ps, getSearchEngines(), paramsInfo);
				final ClientParamsValidations validationList = getClientProxyGenerator().convertTo(validations);
				getParamsDao().commit();
				return validationList;
			} catch (ClientProxyGenerator.ConversionException e) {
				return getValidationForException(param, e.getCause());
			} catch (Exception e) {
				return getValidationForException(param, e);
			}
		} catch (Exception e) {
			getParamsDao().rollback();
			LOGGER.error("Could not update parameter set", e);
			throw GWTServiceExceptionFactory.createException("Could not update parameter set", e);
		}
	}

	@Override
	public void delete(final ClientParamSet paramSet) throws GWTServiceException {
		try {
			final HttpSession session = getSession();
			getParamsDao().begin();
			final SavedSearchEngineParameters savedSearchEngineParameters = getParamsDao().findSavedSearchEngineParameters(paramSet.getName());
			if (savedSearchEngineParameters != null) {
				getParamsDao().deleteSavedSearchEngineParameters(savedSearchEngineParameters, new Change("Deleting saved search engine parameters [" + savedSearchEngineParameters.getName() + "] with id " + savedSearchEngineParameters.getId(), new DateTime()));
				final ParameterSetCache cache = new ParameterSetCache(session, getParamsDao());
				cache.removeFromCache(paramSet);
			}
			getParamsDao().commit();
		} catch (Exception e) {
			getParamsDao().rollback();
			final String errMsg = "Could not delete parameter set " + (paramSet != null ? paramSet.getName() : "");
			LOGGER.error(errMsg, e);
			throw GWTServiceExceptionFactory.createException(errMsg, e);
		}
	}

	private ClientParamsValidations getValidationForException(final String param, final Throwable e) {
		final ClientValidationList list = new ClientValidationList();
		final ClientValidation cv = new ClientValidation(e.getMessage());
		cv.setThrowableStackTrace(ExceptionUtilities.stringifyStackTrace(e));
		cv.setParamId(param);
		cv.setSeverity(ClientValidation.SEVERITY_ERROR);
		cv.setThrowableMessage(MprcException.getDetailedMessage(e));
		list.add(cv);
		final Map<String, ClientValidationList> map = new HashMap<String, ClientValidationList>();
		map.put(param, list);
		return new ClientParamsValidations(map);
	}

	@Override
	public ClientDatabaseUndeployerProgress undeployDatabase(final String dbToUndeploy) throws GWTServiceException {
		try {
			getCurationDao().begin();
			final Curation curation = getCurationDao().findCuration(dbToUndeploy);
			final DatabaseUndeployerProgress progressMessage = DatabaseUndeployerCaller.callDatabaseUndeployer(getDatabaseUndeployerDaemonConnection(), curation);
			getCurationDao().commit();

			return getDbUndeployerProgressMessageProxy(progressMessage);
		} catch (Exception e) {
			getCurationDao().rollback();
			final String errMsg = "Could not undeploy database " + dbToUndeploy;
			LOGGER.error(errMsg, e);
			throw GWTServiceExceptionFactory.createException(errMsg, e);
		}
	}

	@Override
	public ClientDatabaseUndeployerProgress getProgressMessageForDatabaseUndeployment(final Long taskId) throws GWTServiceException {
		return getDbUndeployerProgressMessageProxy(DatabaseUndeployerCaller.getMessageFromQueue(taskId));
	}

	@Override
	public boolean isDatabaseUndeployerEnabled() throws GWTServiceException {
		return getWebUi().isDatabaseUndeployerEnabled();
	}

	@Override
	public InitialPageData getInitialPageData(final Integer previousSearchId) throws GWTServiceException {
		final String[] params = {
				"sequence.database",
				"sequence.enzyme",
				"sequence.missed_cleavages",
				"modifications.fixed",
				"modifications.variable",
				"tolerance.peptide",
				"tolerance.fragment",
				"instrument",
				"extractMsnSettings",
				"scaffoldSettings",
		};
		final List<List<ClientValue>> allowedValues = getAllowedValues(params);
		final HashMap<String, List<ClientValue>> map = new HashMap<String, List<ClientValue>>(params.length);

		final Iterator<List<ClientValue>> iterator = allowedValues.iterator();
		for (final String param : params) {
			map.put(param, iterator.next());
		}

		return new InitialPageData(
				listUsers(),
				previousSearchId == null ? null : loadSearch(previousSearchId),
				getUserMessage(),
				getParamSetList(),
				map,
				isDatabaseUndeployerEnabled(),
				listSearchEngines(),
				listSpectrumQaParamFiles(),
				isScaffoldReportEnabled());
	}

	@Override
	public boolean outputFolderExists(final String outputFolder) throws GWTServiceException {
		final File file = new File(getBrowseRoot(), outputFolder);
		return file.exists();
	}

	/**
	 * Compare session id from Cookie with passed in token to prevent CSRF.
	 *
	 * @see <a href="http://groups.google.com/group/Google-Web-Toolkit/web/security-for-gwt-applications">Security for GWT applications</a>
	 * @see <a href="http://en.wikipedia.org/wiki/Cross-site_request_forgery">Cross-site request forgery</a>
	 */
	private HttpSession getSession() {
		return getThreadLocalRequest().getSession();
	}

	private ClientDatabaseUndeployerProgress getDbUndeployerProgressMessageProxy(final DatabaseUndeployerProgress progressMessage) {
		return new ClientDatabaseUndeployerProgress(progressMessage.getDatabaseUndeployerTaskId(), progressMessage.getProgressMessage(), progressMessage.isLast());
	}

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	public void setWebUiHolder(final WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}

	public WebUi getWebUi() {
		return getWebUiHolder().getWebUi();
	}

	public Collection<SearchEngine> getSearchEngines() {
		return getWebUi().getSearchEngines();
	}

	public File getBrowseRoot() {
		return getWebUi().getBrowseRoot();
	}

	public void setSwiftDao(final SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	public void setWorkspaceDao(final WorkspaceDao workspaceDao) {
		this.workspaceDao = workspaceDao;
	}

	public WorkspaceDao getWorkspaceDao() {
		return workspaceDao;
	}

	public void setCurationDao(final CurationDao curationDao) {
		this.curationDao = curationDao;
	}

	public CurationDao getCurationDao() {
		return curationDao;
	}

	public void setParamsDao(final ParamsDao paramsDao) {
		this.paramsDao = paramsDao;
	}

	public ParamsDao getParamsDao() {
		return paramsDao;
	}

	public void setParamsInfo(final ParamsInfo paramsInfo) {
		this.paramsInfo = paramsInfo;
	}

	public ParamsInfo getParamsInfo() {
		return paramsInfo;
	}

	public SwiftSearcherCaller getSwiftSearcherCaller() {
		return swiftSearcherCaller;
	}

	public void setSwiftSearcherCaller(SwiftSearcherCaller swiftSearcherCaller) {
		this.swiftSearcherCaller = swiftSearcherCaller;
	}

	public DaemonConnection getDatabaseUndeployerDaemonConnection() {
		return getWebUi().getDatabaseUndeployerDaemonConnection();
	}

	public ClientProxyGenerator getClientProxyGenerator() {
		return new ClientProxyGenerator(getUnimodDao(), getWorkspaceDao(), getSwiftDao(), getBrowseRoot());
	}

	public List<MSMSEvalParamFile> getSpectrumQaParamFiles() {
		return getWebUi().getSpectrumQaParamFiles();
	}

	public boolean isMsmsEval() {
		return getWebUi().isMsmsEval();
	}

	public boolean isScaffoldReport() {
		return getWebUi().isScaffoldReport();
	}

	public void setUnimodDao(final UnimodDao unimodDao) {
		this.unimodDao = unimodDao;
	}

	public UnimodDao getUnimodDao() {
		return unimodDao;
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		MainFactoryContext.setContext(applicationContext);
	}

	private class Resolver implements ClientParamSetResolver {

		private ParameterSetCache cache;
		private boolean clientParamSetListChanged;

		Resolver(final ParameterSetCache cache) {
			this.cache = cache;
			clientParamSetListChanged = false;
		}

		@Override
		public ClientParamSet resolve(final SearchEngineParameters parameters, final User user) {
			// Find all saved parameter sets that match these parameters
			final SavedSearchEngineParameters bestSavedSearchEngineParameters = getParamsDao().findBestSavedSearchEngineParameters(parameters, user);
			if (bestSavedSearchEngineParameters != null) {
				return new ClientParamSet(
						bestSavedSearchEngineParameters.getId(),
						bestSavedSearchEngineParameters.getName(),
						bestSavedSearchEngineParameters.getUser().getUserName(),
						bestSavedSearchEngineParameters.getUser().getInitials());
			}

			// We did not find a perfect match within saved search parameters
			// Try the temporary ones
			final ClientParamSet matchingTemporaryParamSet = cache.findMatchingTemporaryParamSet(parameters);
			if (matchingTemporaryParamSet != null) {
				return matchingTemporaryParamSet;
			}

			// We will make a plain temporary parameter set and return that one
			clientParamSetListChanged = true;
			return cache.installTemporary("Previous search parameters", user.getUserName(), user.getInitials(), parameters.copy());
		}

		@Override
		public boolean isClientParamSetListChanged() {
			return clientParamSetListChanged;
		}

	}
}
