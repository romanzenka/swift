package edu.mayo.mprc.swift.db;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.AssignedTaskData;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.database.DatabaseUtilities;
import edu.mayo.mprc.dbcurator.model.CurationDao;
import edu.mayo.mprc.swift.dbmapping.*;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.unimod.Unimod;
import edu.mayo.mprc.unimod.UnimodDao;
import edu.mayo.mprc.utilities.ComparisonChain;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressReport;
import edu.mayo.mprc.workflow.persistence.TaskState;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDao;
import edu.mayo.mprc.workspace.WorkspaceDaoHibernate;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
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

	private DatabaseFileTokenFactory fileTokenFactory;
	private final Object taskStatesLock = new Object();
	private Map<TaskState, TaskStateData> taskStates = null;
	private WorkspaceDao workspaceDao;
	private CurationDao curationDao;
	private ParamsDao paramsDao;
	private UnimodDao unimodDao;

	/**
	 * The version of the Swift database we are expecting to see.
	 */
	private final int CURRENT_DATABASE_VERSION = 59;

	public SwiftDaoHibernate() {
		super(null);
	}

	public SwiftDaoHibernate(final Database database) {
		super(database);
	}

	public SwiftDaoHibernate(final WorkspaceDao workspaceDao, final CurationDao curationDao, final ParamsDao paramsDao, final UnimodDao unimodDao) {
		this.workspaceDao = workspaceDao;
		this.curationDao = curationDao;
		this.paramsDao = paramsDao;
		this.unimodDao = unimodDao;
	}

	@Override
	public Collection<String> getHibernateMappings() {
		final List<String> list = new ArrayList<String>(Arrays.asList(
				MAP + "FileSearch.hbm.xml",
				MAP + "LogData.hbm.xml",
				MAP + "PeptideReport.hbm.xml",
				MAP + "ReportData.hbm.xml",
				MAP + "SearchRun.hbm.xml",
				MAP + "SpectrumQa.hbm.xml",
				MAP + "SwiftDBVersion.hbm.xml",
				MAP + "SwiftSearchDefinition.hbm.xml",
				MAP + "TaskData.hbm.xml",
				MAP + "TaskStateData.hbm.xml"
		));
		list.addAll(super.getHibernateMappings());
		return list;
	}

	@Override
	public List<TaskData> getTaskDataList(final int searchRunId) {
		try {
			// We want to sort by start timestamp, if not available, by queue start timestamp
			final List<TaskData> list = (List<TaskData>) getSession().createQuery("from TaskData t " +
					" join fetch t.taskState " +
					" where " +
					" t.searchRun.id=:searchRunId")
					.setInteger("searchRunId", searchRunId)
					.list();
			Collections.sort(list, new TaskDataComparator());
			return list;
		} catch (final Exception t) {
			throw new MprcException("Cannot obtain task status list", t);
		}
	}

	@Override
	public TaskData getTaskData(final Integer taskId) {
		try {
			return (TaskData) getSession().get(TaskData.class, taskId);
		} catch (final Exception t) {
			throw new MprcException("Cannot obtain task data for id " + taskId, t);
		}
	}

	@Override
	public void fillExtraFields(List<SearchRun> searchRuns) {
		List<SearchRun> unfinished = new ArrayList<SearchRun>(searchRuns.size());
		for (final SearchRun run : searchRuns) {
			if (run.isCompleted()) {
				run.setRunningTasks(0);
			} else {
				unfinished.add(run);
			}
		}

		final Integer[] allIds = DatabaseUtilities.getIdList(searchRuns);
		final Set<Integer> searchesWithQuameter =
				Sets.newHashSet(getSession().createQuery("select sr.id " +
						" from TaskData t, " +
						" SearchRun sr," +
						" SwiftSearchDefinition sd," +
						" SearchEngineParameters sp, " +
						" EnabledEngines e," +
						" SearchEngineConfig c " +
						" where sr = t.searchRun " +
						" and sr.id in (:ids) " +
						" and sr.swiftSearch = sd.id " +
						" and sd.searchParameters = sp " +
						" and sp.enabledEngines = e " +
						" and c in elements(e.engineConfigs)" +
						" and c.code = 'QUAMETER' " +
						" group by sr.id")
						.setParameterList("ids", allIds)
						.list());

		for (final SearchRun run : searchRuns) {
			run.setQuameter(searchesWithQuameter.contains(run.getId()));
		}

		// Fill in comments
		{
			final List raw = getSession().createQuery("select sr.id as srid, md" +
					" from SearchRun sr, " +
					" SwiftSearchDefinition sd join sd.metadata md " +
					" where sr.id in (:ids) " +
					" and sr.swiftSearch = sd.id and" +
					" index(md) = 'comment' ")
					.setParameterList("ids", allIds)
					.list();

			Map<Integer, String> idsToComments = new HashMap<Integer, String>(raw.size());
			for (Object o : raw) {
				if (o instanceof Object[]) {
					final Object[] a = (Object[]) o;
					final Integer id = getInteger(a[0]);
					idsToComments.put(id, (String) a[1]);
				}
			}

			for (final SearchRun run : searchRuns) {
				final String comment = idsToComments.get(run.getId());
				run.setComment(comment);
			}
		}

		if (unfinished.size() > 0) {
			final Integer[] ids = DatabaseUtilities.getIdList(unfinished);

			// Load counts of tasks + enabled engine ids for each search run
			final List raw = getSession().createQuery("select sr.id, count(t) " +
					" from TaskData t, " +
					" SearchRun sr" +
					" where sr = t.searchRun " +
					" and sr.id in (:ids) " +
					" and t.taskState.description='" + TaskState.RUNNING.getText() + "'" +
					" group by sr.id")
					.setParameterList("ids", ids)
					.list();

			Map<Integer, Integer> idsToCounts = new HashMap<Integer, Integer>(raw.size());
			for (Object o : raw) {
				if (o instanceof Object[]) {
					final Object[] a = (Object[]) o;
					final Integer id = getInteger(a[0]);
					idsToCounts.put(id, getInteger(a[1]));
				}
			}

			for (final SearchRun run : searchRuns) {
				final Integer runningTasks = idsToCounts.get(run.getId());
				run.setRunningTasks(runningTasks == null ? 0 : runningTasks);
			}
		}
	}

	private static Integer getInteger(Object o) {
		Integer count;
		if (o instanceof Long) {
			Long l = (Long) o;
			count = l.intValue();
		} else {
			count = (Integer) o;
		}
		return count;
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

		} catch (final Exception t) {
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
		} catch (final Exception t) {
			throw new MprcException("Cannot obtain search run for id " + searchRunId, t);
		}
	}

	public SpectrumQa addSpectrumQa(final SpectrumQa spectrumQa) {
		try {
			return save(spectrumQa, false);
		} catch (final Exception t) {
			throw new MprcException("Could not add spectrum QA", t);
		}
	}

	public PeptideReport addPeptideReport(final PeptideReport peptideReport) {
		try {
			return save(peptideReport, false);
		} catch (final Exception t) {
			throw new MprcException("Could not add peptide report", t);
		}
	}

	/**
	 * Add a file search object (for testing purposes).
	 */
	public FileSearch addFileSearch(final FileSearch fileSearch) {
		try {
			if (fileSearch.getSwiftSearchDefinition() == null) {
				throw new MprcException("FileSearch must be a part of SwiftSearchDefinition");
			}
			return save(fileSearch, false);
		} catch (final Exception t) {
			throw new MprcException("Could not add file search information", t);
		}
	}

	@Override
	public List<LogData> getLogsForTask(final TaskData data) {
		final Session session = getSession();
		final Criteria criteria = session.createCriteria(LogData.class)
				.add(nullSafeEq("task", data));
		return listAndCast(criteria);
	}

	@Override
	public int getDatabaseVersion() {
		final Object o = getSession().get(SwiftDBVersion.class, 1);
		if (o == null) {
			return 0;
		}
		if (o instanceof SwiftDBVersion) {
			final Integer version = ((SwiftDBVersion) o).getVersion();
			return version == null ? 0 : version;
		}
		return 0;
	}

	@Override
	public void cleanupAfterStartup() {
		final Session session = getSession();
		final Query query = session.createQuery(
				"update SearchRun r " +
						"set r.endTimestamp=:time, " +
						"    r.errorMessage='Swift restarted', " +
						"    r.errorCode=1 " +
						"where r.endTimestamp is null and r.errorMessage is null")
				.setParameter("time", new Date());
		query.executeUpdate();
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

				definition = useExistingOrSave(definition, definition.getEqualityCriteria());
			}
			return definition;

		} catch (final Exception t) {
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
		} catch (final Exception t) {
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
		} catch (final Exception t) {
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
					false,
					null);

			try {
				getSession().saveOrUpdate(data);
			} catch (final Exception t) {
				throw new MprcException("Cannot update search run [" + data.getTitle() + "] in the database", t);
			}
			return data;
		} catch (final Exception t) {
			throw new MprcException("Cannot fill search run", t);
		}
	}

	public TaskData updateTask(final TaskData task) {
		LOGGER.debug("Updating task\t'" + task.getTaskName());
		try {
			getSession().saveOrUpdate(task);
		} catch (final Exception t) {
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

	/**
	 * Add a new task state (if it does not exist already in the database).
	 * Flushes the task state cache (for now).
	 *
	 * @param state State to be added.
	 */
	private void addTaskState(final TaskState state) {
		final TaskStateData taskState = getTaskState(state);
		if (taskState != null) {
			return;
		}
		final TaskStateData taskStateData = new TaskStateData(state.getText());
		save(taskStateData, true);
		synchronized (taskStatesLock) {
			// Flush the cache
			taskStates = null;
		}
	}

	private TaskStateData getTaskState(final Session session, final TaskState state) {
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
				} catch (final Exception t) {
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

		} catch (final Exception t) {
			throw new MprcException("Cannot create a new task " + name + " (" + descriptionLong + ")", t);
		}
	}

	@Override
	public ReportData storeReport(final int searchRunId, final File resultFile) {
		return storeReport(searchRunId, resultFile, new DateTime(resultFile.lastModified()));
	}

	@Override
	public ReportData storeReport(final int searchRunId, final File resultFile, final DateTime reportDate) {
		try {
			final SearchRun searchRun = getSearchRunForId(searchRunId);
			final ReportData r = new ReportData(resultFile, reportDate, searchRun);
			getSession().saveOrUpdate(r);
			searchRun.getReports().add(r);
			return r;
		} catch (final Exception t) {
			throw new MprcException("Cannot store search run " + searchRunId, t);
		}
	}


	@Override
	public ReportData getReportForId(final long reportDataId) {
		final ReportData reportData = (ReportData) getSession().createCriteria(ReportData.class)
				.add(Restrictions.eq("id", reportDataId))
				.setFetchMode("searchRun", FetchMode.JOIN)
				.uniqueResult();
		return reportData;
	}

	@Override
	public void storeAssignedTaskData(final TaskData taskData, final AssignedTaskData assignedTaskData) {
		try {
			taskData.setGridJobId(assignedTaskData.getAssignedId());
//			taskData.setOutputLogDatabaseToken(fileTokenFactory.fileToDatabaseToken(assignedTaskData.getOutputLogFile()));
//			taskData.setErrorLogDatabaseToken(fileTokenFactory.fileToDatabaseToken(assignedTaskData.getErrorLogFile()));
		} catch (final Exception t) {
			throw new MprcException("Cannot store task grid request id " + assignedTaskData.getAssignedId() + " for task " + taskData, t);
		}
	}

	@Override
	public LogData storeLogData(final LogData logData) {
		getSession().saveOrUpdate(logData);
		return logData;
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
					.createQuery("select distinct f.swiftSearchDefinition.id from FileSearch f where f.inputFile = :file")
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

	@Override
	public void hideSearchesWithOutputFolder(final File outputFolder) {
		// This could be done with a single SQL query that updates the database directly, but Hibernate does not support
		// updates with joins.
		final List<SearchRun> searchRuns = (List<SearchRun>) getSession()
				.createQuery("select r from SearchRun as r, SwiftSearchDefinition as d where d.id = r.swiftSearch and d.outputFolder = :outputFolder")
				.setParameter("outputFolder", outputFolder)
				.list();
		for (final SearchRun run : searchRuns) {
			run.setHidden(1);
		}
	}

	@Override
	public FileSearch getFileSearchForId(final int fileSearchId) {
		try {
			final FileSearch data = (FileSearch) getSession().get(FileSearch.class, fileSearchId);
			if (data == null) {
				throw new MprcException("getFileSearchForId : file search id=" + fileSearchId + " was not found.");
			}
			return data;
		} catch (final Exception t) {
			throw new MprcException("Cannot obtain file search for id " + fileSearchId, t);
		}
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
	public void setFileTokenFactory(final DatabaseFileTokenFactory fileTokenFactory) {
		this.fileTokenFactory = fileTokenFactory;
	}

	@Override
	public String check() {
		LOGGER.info("Checking swift DAO");
		// First, the workspace has to be defined, with a user
		final String workspaceCheck = workspaceDao.check();
		if (workspaceCheck != null) {
			return workspaceCheck;
		}

		final int version = getDatabaseVersion();
		if (version != CURRENT_DATABASE_VERSION) {
			return String.format("The database version needs to be upgraded. Was %d, needs to be %d", version, CURRENT_DATABASE_VERSION);
		}

		if (rowCount(TaskStateData.class) != (long) TaskState.values().length) {
			return "The task state enumeration is not up to date";
		}

		return null;
	}

	@Override
	public void install(final Map<String, String> params) {
		// Initialize the dependent DAO
		paramsDao.install(params); // Initialized curation and workspace
		unimodDao.install(params);

		installTaskStates();

		if (params.containsKey("test")) {
			LOGGER.info("Installing test data for " + getClass().getName());

			if (rowCount(SpectrumQa.class) != 0) {
				// We have already done our work
				return;
			}

			LOGGER.info("Installing test data for " + this.getClass().getName());
			// Install the test data - a sample search that successfully ran with mascot and scaffold

			final File outputFolder = new File("output-folder");

			final SpectrumQa spectrumQa = addSpectrumQa(new SpectrumQa(
					"orbitrap.param", "Orbi"
			));

			final PeptideReport peptideReport = addPeptideReport(new PeptideReport());

			final Unimod mods = unimodDao.load();
			final List<ModSpecificity> carbC = mods.getSpecificitiesByMascotName("Carbamidomethyl (C)");
			final ModSet fixed = new ModSet();
			fixed.addAll(carbC);

			final ModSet variable = new ModSet();

			EnabledEngines enabledEngines = new EnabledEngines();
			enabledEngines.add(new SearchEngineConfig("MASCOT", "2.4"));
			enabledEngines.add(new SearchEngineConfig("SCAFFOLD", "4.3.3"));
			enabledEngines = paramsDao.addEnabledEngines(enabledEngines);

			final SearchEngineParameters searchParameters = paramsDao.addSearchEngineParameters(
					new SearchEngineParameters(
							getCurationDao().findCuration("ShortTest"),
							paramsDao.getProteaseByName(Protease.getTrypsinAllowP().getName()),
							2,
							1,
							fixed,
							variable,
							new Tolerance(0.5, MassUnit.Da),
							new Tolerance(10, MassUnit.Ppm),
							paramsDao.getInstrumentByName(Instrument.ORBITRAP.getName()),
							ExtractMsnSettings.getMsconvertSettings(),
							new ScaffoldSettingsBuilder().createScaffoldSettings(),
							enabledEngines, ""));

			final List<FileSearch> inputFiles = Lists.newArrayList();
			FileSearch fileSearch1 = new FileSearch(new File("test.RAW"), "sample1", "none", "Test Search 1", null);
			FileSearch fileSearch2 = new FileSearch(new File("test2.RAW"), "sample2", "none", "Test Search 1", null);
			inputFiles.add(fileSearch1);
			inputFiles.add(fileSearch2);

			final Map<String, String> metadata = Maps.newHashMap();
			metadata.put("quameter.category", "AL-Kappa");
			metadata.put("comment", "comment for Kappa search");

			final User user = workspaceDao.getUserByEmail(WorkspaceDaoHibernate.USER1_EMAIL);
			if (user == null) {
				throw new MprcException("There is no user with e-mail " + WorkspaceDaoHibernate.USER1_EMAIL);
			}

			final SwiftSearchDefinition definition =
					addSwiftSearchDefinition(
							new SwiftSearchDefinition("Test Search 1",
									user,
									outputFolder,
									spectrumQa,
									peptideReport,
									searchParameters,
									inputFiles,
									false,
									false,
									false,
									metadata));

			final SearchRun run = fillSearchRun(definition);
			run.setNumTasks(2);

			TaskData task1 = new TaskData("mascot", new Date(), new Date(), new Date(), run, getTaskState(TaskState.COMPLETED_SUCCESFULLY), "Mascot search");
			TaskData task2 = new TaskData("scaffold", new Date(), new Date(), new Date(), run, getTaskState(TaskState.COMPLETED_SUCCESFULLY), "Scaffold search");
			updateTask(task1);
			updateTask(task2);

			run.setTasksCompleted(2);
			LOGGER.debug("Test data for " + getClass().getName() + " installed");
		}
	}

	private void installTaskStates() {
		if (rowCount(TaskStateData.class) != TaskState.values().length) {
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

	public CurationDao getCurationDao() {
		return curationDao;
	}

	@Resource(name = "curationDao")
	public void setCurationDao(final CurationDao curationDao) {
		this.curationDao = curationDao;
	}

	public ParamsDao getParamsDao() {
		return paramsDao;
	}

	@Resource(name = "paramsDao")
	public void setParamsDao(final ParamsDao paramsDao) {
		this.paramsDao = paramsDao;
	}

	public UnimodDao getUnimodDao() {
		return unimodDao;
	}

	@Resource(name = "unimodDao")
	public void setUnimodDao(final UnimodDao unimodDao) {
		this.unimodDao = unimodDao;
	}

	public static class TaskDataComparator implements Comparator<TaskData> {
		/**
		 * Sort descending based on start timestamps. If those are null, use queue timestamps.
		 */
		@Override
		public int compare(final TaskData taskData, final TaskData t1) {
			Date start1 = taskData.getStartTimestamp() == null ? taskData.getQueueTimestamp() : taskData.getStartTimestamp();
			Date start2 = t1.getStartTimestamp() == null ? t1.getQueueTimestamp() : t1.getStartTimestamp();
			return -ComparisonChain.start()
					.nullsFirst()
					.compare(start1, start2)
					.compare(taskData.getEndTimestamp(), t1.getEndTimestamp())
					.result();
		}
	}
}