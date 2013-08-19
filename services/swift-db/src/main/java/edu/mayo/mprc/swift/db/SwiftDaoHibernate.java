package edu.mayo.mprc.swift.db;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.AssignedTaskData;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.DatabasePlaceholder;
import edu.mayo.mprc.swift.dbmapping.*;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressReport;
import edu.mayo.mprc.workflow.persistence.TaskState;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDao;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

@Repository("swiftDao")
public final class SwiftDaoHibernate extends DaoBase implements SwiftDao {
	private static final Logger LOGGER = Logger.getLogger(SwiftDaoHibernate.class);
	private static final String MAP = "edu/mayo/mprc/swift/dbmapping/";

	private FileTokenFactory fileTokenFactory;
	private final Object taskStatesLock = new Object();
	private Map<TaskState, TaskStateData> taskStates = null;
	private WorkspaceDao workspaceDao;

	public SwiftDaoHibernate() {
		super(null);
	}

	public SwiftDaoHibernate(final DatabasePlaceholder databasePlaceholder) {
		super(databasePlaceholder);
	}

	@Override
	public Collection<String> getHibernateMappings() {
		return Arrays.asList(
				MAP + "EnabledEngines.hbm.xml",
				MAP + "FileSearch.hbm.xml",
				MAP + "PeptideReport.hbm.xml",
				MAP + "ReportData.hbm.xml",
				MAP + "SearchEngineConfig.hbm.xml",
				MAP + "SearchRun.hbm.xml",
				MAP + "SpectrumQa.hbm.xml",
				MAP + "SwiftDBVersion.hbm.xml",
				MAP + "SwiftSearchDefinition.hbm.xml",
				MAP + "TaskData.hbm.xml",
				MAP + "TaskStateData.hbm.xml"
		);
	}

	@Override
	public boolean isExistingTitle(final String title, final User user) {
		try {
			final Number qusers = (Number) getSession().createQuery("select count(*) from edu.mayo.mprc.swift.dbmapping.SearchRun t where t.title=:title and t.submittingUser.id=:userId")
					.setString("title", title)
					.setParameter("userId", user.getId())
					.uniqueResult();
			return qusers.intValue() > 0;
		} catch (Exception t) {
			throw new MprcException("Cannot determine whether title " + title + " exists for user " + user, t);
		}
	}

	@Override
	public List<TaskData> getTaskDataList(final int searchRunId) {
		try {
			return (List<TaskData>) getSession().createQuery("from TaskData t where t.searchRun.id=:searchRunId order by t.startTimestamp desc")
					.setInteger("searchRunId", searchRunId)
					.list();
		} catch (Exception t) {
			throw new MprcException("Cannot obtain task status list", t);
		}
	}

	@Override
	public TaskData getTaskData(final Integer taskId) {
		try {
			return (TaskData) getSession().get(TaskData.class, taskId);
		} catch (Exception t) {
			throw new MprcException("Cannot obtain task data for id " + taskId, t);
		}
	}

	@Override
	public List<SearchRun> getSearchRunList(final SearchRunFilter filter, final boolean withReports) {
		try {
			final Criteria criteria = getSession().createCriteria(SearchRun.class);
			filter.updateCriteria(criteria);
			if (withReports) {
				criteria.setFetchMode("reports", FetchMode.SELECT);
			}
			criteria.setCacheable(true)
					.setReadOnly(true);

			return criteria.list();
		} catch (Exception t) {
			throw new MprcException("Cannot obtain search run status list for filter: " + filter, t);
		}
	}

	@Override
	public int getNumberRunningTasksForSearchRun(final SearchRun searchRun) {
		// Do not hit database for finished search runs. Counting running tasks is costly
		if (searchRun.isCompleted()) {
			return 0;
		}
		try {
			final long howmanyrunning = (Long) getSession().createQuery("select count(t) from TaskData t where t.searchRun=:searchRun and t.taskState.description='" + TaskState.RUNNING.getText() + "'")
					.setParameter("searchRun", searchRun)
					.uniqueResult();
			return (int) howmanyrunning;
		} catch (Exception t) {
			throw new MprcException("Cannot determine number of running tasks for search run " + searchRun.getTitle(), t);
		}
	}

	@Override
	public Set<SearchRun> getSearchRuns(final boolean showSuccess, final boolean showFailure, final boolean showWarnings, final Date updatedSince) {

		final Set<SearchRun> resultSet = new HashSet<SearchRun>();

		final Session session = getSession();
		try {
			final LogicalExpression timeCriteria;
			if (updatedSince == null) {
				timeCriteria = null;
			} else {
				timeCriteria = Restrictions.or(
						Restrictions.gt("startTimestamp", updatedSince),
						Restrictions.gt("endTimestamp", updatedSince));
			}

			if (showSuccess) {
				final Criteria criteriaQuery = session.createCriteria(SearchRun.class);
				if (timeCriteria != null) {
					criteriaQuery.add(timeCriteria);
				}
				criteriaQuery.add(Restrictions.and(Restrictions.isNotNull("endTimestamp"), Restrictions.eq("tasksFailed", 0)));
				resultSet.addAll(criteriaQuery.list());
			}

			if (showFailure) {
				final Criteria criteriaQuery = session.createCriteria(SearchRun.class);
				if (timeCriteria != null) {
					criteriaQuery.add(timeCriteria);
				}
				criteriaQuery.add(Restrictions.gt("tasksFailed", 0));
				resultSet.addAll(criteriaQuery.list());
			}

			if (showWarnings) {
				final Criteria criteriaQuery = session.createCriteria(SearchRun.class);
				if (timeCriteria != null) {
					criteriaQuery.add(timeCriteria);
				}
				criteriaQuery.add(Restrictions.gt("tasksWithWarning", 0));
				resultSet.addAll(criteriaQuery.list());
			}

		} catch (Exception t) {
			throw new MprcException("Cannot obtain a list search runs from the database.", t);
		}

		return resultSet;
	}

	@Override
	public SearchRun getSearchRunForId(final int searchRunId) {
		try {
			final SearchRun data = (SearchRun) getSession().get(SearchRun.class, searchRunId);
			if (data == null) {
				throw new MprcException("getSearchRunForId : search run id=" + searchRunId + " was not found.");
			}
			return data;
		} catch (Exception t) {
			throw new MprcException("Cannot obtain search run for id " + searchRunId, t);
		}
	}

	private static Criterion getSearchEngineEqualityCriteria(final SearchEngineConfig searchEngineConfig) {
		return Restrictions.and(
				DaoBase.nullSafeEq("code", searchEngineConfig.getCode()),
				DaoBase.nullSafeEq("version", searchEngineConfig.getVersion())
		);
	}

	@Override
	public SearchEngineConfig addSearchEngineConfig(final SearchEngineConfig config) {
		try {
			return save(config, getSearchEngineEqualityCriteria(config), false);
		} catch (Exception t) {
			throw new MprcException("Cannot add new search engine config '" + config.getCode() + "'", t);
		}
	}

	@Override
	public EnabledEngines addEnabledEngineSet(final Iterable<SearchEngineConfig> searchEngineConfigs) {
		try {
			final EnabledEngines engines = new EnabledEngines();
			for (final SearchEngineConfig config : searchEngineConfigs) {
				final SearchEngineConfig searchEngineConfig = addSearchEngineConfig(config);
				engines.add(searchEngineConfig);
			}

			return updateCollection(engines, engines.getEngineConfigs(), "engineConfigs");

		} catch (Exception t) {
			throw new MprcException("Could not add search engine set", t);
		}
	}

	@Override
	public EnabledEngines addEnabledEngines(final EnabledEngines engines) {
		if (engines.getId() != null) {
			return engines;
		}
		Preconditions.checkNotNull(engines, "Enabled engine list must not be null");
		try {
			final EnabledEngines result = new EnabledEngines();
			result.setId(engines.getId());
			for (final SearchEngineConfig searchEngineConfig : engines.getEngineConfigs()) {
				result.add(addSearchEngineConfig(searchEngineConfig));
			}
			return updateCollection(result, result.getEngineConfigs(), "engineConfigs");
		} catch (Exception t) {
			throw new MprcException("Could not add search engine set", t);
		}
	}

	private Criterion getSpectrumQaEqualityCriteria(final SpectrumQa spectrumQa) {
		return Restrictions.and(
				DaoBase.nullSafeEq("engine", spectrumQa.getEngine()),
				DaoBase.nullSafeEq("paramFilePath", spectrumQa.getParamFilePath()));
	}

	@Override
	public SpectrumQa addSpectrumQa(final SpectrumQa spectrumQa) {
		try {
			return save(spectrumQa, getSpectrumQaEqualityCriteria(spectrumQa), false);
		} catch (Exception t) {
			throw new MprcException("Could not add spectrum QA", t);
		}
	}

	private Criterion getPeptideReportEqualityCriteria(final PeptideReport peptideReport) {
		return Restrictions.isNotNull("id");
	}

	@Override
	public PeptideReport addPeptideReport(final PeptideReport peptideReport) {
		try {
			return save(peptideReport, getPeptideReportEqualityCriteria(peptideReport), false);
		} catch (Exception t) {
			throw new MprcException("Could not add peptide report", t);
		}
	}

	private FileSearch addFileSearch(final FileSearch fileSearch) {
		try {
			fileSearch.setEnabledEngines(addEnabledEngines(fileSearch.getEnabledEngines()));
			return save(fileSearch, getFileSearchEqualityCriteria(fileSearch), false);
		} catch (Exception t) {
			throw new MprcException("Could not add file search information", t);
		}
	}

	private Criterion getFileSearchEqualityCriteria(final FileSearch fileSearch) {
		return Restrictions.conjunction()
				.add(DaoBase.nullSafeEq("inputFile", fileSearch.getInputFile()))
				.add(DaoBase.nullSafeEq("biologicalSample", fileSearch.getBiologicalSample()))
				.add(DaoBase.nullSafeEq("categoryName", fileSearch.getCategoryName()))
				.add(DaoBase.nullSafeEq("experiment", fileSearch.getExperiment()))
				.add(DaoBase.associationEq("enabledEngines", fileSearch.getEnabledEngines()))
				.add(DaoBase.nullSafeEq("swiftSearchDefinitionId", fileSearch.getSwiftSearchDefinitionId()));
	}

	private Criterion getSwiftSearchDefinitionEqualityCriteria(final SwiftSearchDefinition definition) {
		return Restrictions.conjunction()
				.add(DaoBase.nullSafeEq("title", definition.getTitle()))
				.add(DaoBase.associationEq("user", definition.getUser()))
				.add(DaoBase.nullSafeEq("outputFolder", definition.getOutputFolder()))
				.add(DaoBase.associationEq("qa", definition.getQa()))
				.add(DaoBase.associationEq("peptideReport", definition.getPeptideReport()));
	}

	@Override
	public SwiftSearchDefinition addSwiftSearchDefinition(SwiftSearchDefinition definition) {
		try {
			if (definition.getId() == null) {
				// We only save search definition that was not previously saved.
				// Once saved, the definition is immutable.

				// Save all the complex objects first, so we can ensure they get stored properly
				if (definition.getQa() != null) {
					definition.setQa(addSpectrumQa(definition.getQa()));
				}
				if (definition.getPeptideReport() != null) {
					definition.setPeptideReport(addPeptideReport(definition.getPeptideReport()));
				}

				final List<FileSearch> inputFiles = new ArrayList<FileSearch>();
				for (final FileSearch fileSearch : definition.getInputFiles()) {
					inputFiles.add(addFileSearch(fileSearch));
				}
				definition.setInputFiles(inputFiles);
				definition = saveLaxEquality(definition, getSwiftSearchDefinitionEqualityCriteria(definition), false);
			}
			return definition;

		} catch (Exception t) {
			throw new MprcException("Could not add swift search definition", t);
		}
	}

	@Override
	public SwiftSearchDefinition getSwiftSearchDefinition(final Integer swiftSearchId) {
		if (swiftSearchId == null || swiftSearchId == 0) {
			return null;
		}
		try {
			return (SwiftSearchDefinition) getSession().load(SwiftSearchDefinition.class, swiftSearchId);
		} catch (Exception t) {
			throw new MprcException("Cannot obtain swift search definition for id " + swiftSearchId, t);
		}
	}

	@Override
	public void reportSearchRunProgress(final int searchRunId, final ProgressReport progress) {
		try {
			final SearchRun searchRun = getSearchRunForId(searchRunId);
			LOGGER.debug("Persisting search run progress " + searchRun.getTitle() + "\n" + progress.toString());
			if (progress.getSucceeded() + progress.getFailed() == progress.getTotal() && searchRun.getEndTimestamp() == null) {
				searchRun.setEndTimestamp(new Date());
			}
			searchRun.setNumTasks(progress.getTotal());
			searchRun.setTasksCompleted(progress.getSucceeded());
			searchRun.setTasksFailed(progress.getFailed() - progress.getInitFailed());
			searchRun.setTasksWithWarning(progress.getWarning());
		} catch (Exception t) {
			throw new MprcException("Cannot persist search run progress", t);
		}
	}

	@Override
	public SearchRun fillSearchRun(final SwiftSearchDefinition swiftSearch) {
		LOGGER.debug("Producing search run");

		try {
			// Lookup user
			final String userName = swiftSearch == null ? null : swiftSearch.getUser().getUserName();
			User user = null;
			if (userName != null) {
				user = (User) getSession().createQuery("from User u where u.userName='" + userName + "' and u.deletion=null").uniqueResult();
				if (user == null) {
					throw new MprcException("Unknown user: " + userName);
				}
			}

			// Lookup unknown report type

			final SearchRun data = new SearchRun(
					swiftSearch == null ? null : swiftSearch.getTitle(),
					user,
					swiftSearch,
					new Date(),
					null,
					0,
					null,
					1,
					0,
					0,
					0,
					false);

			try {
				getSession().saveOrUpdate(data);
			} catch (Exception t) {
				throw new MprcException("Cannot update search run [" + data.getTitle() + "] in the database", t);
			}
			return data;
		} catch (Exception t) {
			throw new MprcException("Cannot fill search run", t);
		}
	}

	@Override
	public TaskData updateTask(final TaskData task) {
		LOGGER.debug("Updating task\t'" + task.getTaskName());
		try {
			getSession().saveOrUpdate(task);
		} catch (Exception t) {
			throw new MprcException("Cannot update task " + task, t);
		}

		return task;
	}

	private void listToTaskStateMap(final List<?> list) {
		taskStates = new HashMap<TaskState, TaskStateData>(list.size());
		for (final Object o : list) {
			if (o instanceof TaskStateData) {
				final TaskStateData stateData = (TaskStateData) o;
				taskStates.put(TaskState.fromText(stateData.getDescription()), stateData);
			}
		}
	}

	@Override
	public void addTaskState(final TaskState state) {
		final TaskStateData taskState = getTaskState(state);
		if (taskState != null) {
			return;
		}
		final TaskStateData taskStateData = new TaskStateData(state.getText());
		save(taskStateData, getTaskStateDataEqualityCriteria(taskStateData), true);
		synchronized (taskStatesLock) {
			// Flush the cache
			taskStates = null;
		}
	}

	private Criterion getTaskStateDataEqualityCriteria(final TaskStateData taskStateData) {
		return DaoBase.nullSafeEq("description", taskStateData.getDescription());
	}

	@Override
	public TaskStateData getTaskState(final Session session, final TaskState state) {
		synchronized (taskStatesLock) {
			if (taskStates == null) {
				listToTaskStateMap(session.createQuery("from TaskStateData").list());
			}
			return taskStates.get(state);
		}

	}

	@Override
	public TaskStateData getTaskState(final TaskState state) {
		synchronized (taskStatesLock) {
			if (taskStates == null) {
				List<?> list = null;
				try {
					list = (List<?>) getSession().createQuery("from TaskStateData").list();
				} catch (Exception t) {
					throw new MprcException("", t);
				}
				listToTaskStateMap(list);
			}
			return taskStates.get(state);
		}
	}

	@Override
	public TaskData createTask(final int searchRunId, final String name, final String descriptionLong, final TaskState taskState) {
		LOGGER.debug("Creating new task " + name + " " + descriptionLong + " " + taskState);
		final Session session = getSession();
		try {
			final SearchRun searchRun = getSearchRunForId(searchRunId);
			final TaskData task = new TaskData(
					name,
					/*queueStamp*/ null,
					/*startStamp*/ null,
					/*endStamp*/ null,
					searchRun,
					getTaskState(session, taskState),
					descriptionLong);

			session.saveOrUpdate(task);
			return task;

		} catch (Exception t) {
			throw new MprcException("Cannot create a new task " + name + " (" + descriptionLong + ")", t);
		}
	}

	@Override
	public ReportData storeReport(final int searchRunId, final File resultFile) {
		try {
			final SearchRun searchRun = getSearchRunForId(searchRunId);
			final ReportData r = new ReportData(resultFile, new DateTime(), searchRun);
			searchRun.getReports().add(r);
			getSession().saveOrUpdate(r);
			return r;
		} catch (Exception t) {
			throw new MprcException("Cannot store search run " + searchRunId, t);
		}
	}

	@Override
	public ReportData getReportForId(final long reportDataId) {
		return (ReportData) getSession().load(ReportData.class, reportDataId);
	}

	@Override
	public void storeAssignedTaskData(final TaskData taskData, final AssignedTaskData assignedTaskData) {
		try {
			taskData.setGridJobId(assignedTaskData.getAssignedId());
			taskData.setOutputLogDatabaseToken(fileTokenFactory.fileToDatabaseToken(assignedTaskData.getOutputLogFile()));
			taskData.setErrorLogDatabaseToken(fileTokenFactory.fileToDatabaseToken(assignedTaskData.getErrorLogFile()));
		} catch (Exception t) {
			throw new MprcException("Cannot store task grid request id " + assignedTaskData.getAssignedId() + " for task " + taskData, t);
		}
	}

	@Override
	public void searchRunFailed(final int searchRunId, final String message) {
		final SearchRun searchRun = getSearchRunForId(searchRunId);
		searchRun.setErrorMessage(message);
		searchRun.setEndTimestamp(new Date());
	}

	@Override
	public void renameAllFileReferences(final File from, final File to) {
		// Move everything in FileSearch
		renameFileReferences(from, to, "FileSearch", "inputFile");
		renameFileReferences(from, to, "ReportData", "reportFile");
		renameFileReferences(from, to, "SwiftSearchDefinition", "outputFolder");
		renameFileReferences(from, to, "TandemMassSpectrometrySample", "file");
	}

	@Override
	public List<SearchRun> findSearchRunsForFiles(final List<FileSearch> files) {
		final Set<SearchRun> searchRuns = new HashSet<SearchRun>();
		for (final FileSearch fileSearch : files) {
			final List<Integer> searchDefinitions = (List<Integer>) getSession()
					.createQuery("select distinct f.swiftSearchDefinitionId from FileSearch f where f.inputFile = :file")
					.setParameter("file", fileSearch.getInputFile())
					.list();
			for (final Integer def : searchDefinitions) {
				final List<SearchRun> runs = (List<SearchRun>) getSession()
						.createQuery("from SearchRun s where s.swiftSearch = :id" +
								" and s.hidden=0 and s.endTimestamp is not null and s.tasksFailed=0 and s.errorMessage is null and s.errorCode=0" +
								" order by s.endTimestamp desc")
						.setInteger("id", def)
						.setMaxResults(1)
						.list();
				searchRuns.addAll(runs);
			}
		}
		return Lists.newArrayList(searchRuns);
	}

	@Override
	public boolean isFileInSearchRun(final String inputFile, final SearchRun run) {
		final SwiftSearchDefinition swiftSearchDefinition = getSwiftSearchDefinition(run.getSwiftSearch());
		for (final FileSearch fileSearch : swiftSearchDefinition.getInputFiles()) {
			if (inputFile.equalsIgnoreCase(fileSearch.getInputFile().getPath())) {
				return true;
			}
		}
		return false;
	}

	private void renameFileReferences(final File from, final File to, final String table, final String field) {
		LOGGER.info("Renaming all " + table + "." + field);
		final Query query = getSession().createQuery("update " + table + " as f set f." + field + "=:file where f.id=:id");

		final List list = getSession().createQuery("select f.id, f." + field + " from " + table + " as f").list();
		LOGGER.info("\tChecking " + list.size() + " entries");
		long totalMoves = 0;
		for (final Object o : list) {
			final Object[] array = (Object[]) o;
			final Number id = (Number) array[0];
			final File file = (File) array[1];
			final String relativePath = FileUtilities.getRelativePathToParent(from.getAbsolutePath(), file.getAbsolutePath(), "/", true);
			if (relativePath != null) {
				final File newFile = new File(to, relativePath);
				if (!file.exists() && newFile.exists()) {
					LOGGER.debug("\tMoving " + file.getAbsolutePath() + "\t->\t" + newFile.getAbsolutePath());
					query
							.setParameter("file", newFile)
							.setParameter("id", id)
							.executeUpdate();
					totalMoves++;
				}
			}
		}
		LOGGER.info("\tMove complete, total items updated: " + totalMoves);
	}

	public FileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	@Resource(name = "fileTokenFactory")
	public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
		this.fileTokenFactory = fileTokenFactory;
	}

	@Override
	public String check(final Map<String, String> params) {
		// First, the workspace has to be defined, with a user
		final String workspaceCheck = workspaceDao.check(params);
		if (workspaceCheck != null) {
			return workspaceCheck;
		}

		if (rowCount(TaskStateData.class) != (long) TaskState.values().length) {
			return "The task state enumeration is not up to date";
		}
		if (rowCount(SearchRun.class) == 0) {
			return "There were no searches previously run";
		}
		return null;
	}

	@Override
	public void initialize(final Map<String, String> params) {
		// Initialize the dependent DAO
		workspaceDao.initialize(params);

		if (rowCount(TaskStateData.class) != (long) TaskState.values().length) {
			LOGGER.info("Initializing task state enumeration");
			for (final TaskState state : TaskState.values()) {
				addTaskState(state);
			}
		}
	}

	public WorkspaceDao getWorkspaceDao() {
		return workspaceDao;
	}

	@Resource(name = "workspaceDao")
	public void setWorkspaceDao(final WorkspaceDao workspaceDao) {
		this.workspaceDao = workspaceDao;
	}
}