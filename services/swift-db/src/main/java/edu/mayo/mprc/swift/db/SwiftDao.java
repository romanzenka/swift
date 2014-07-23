package edu.mayo.mprc.swift.db;

import edu.mayo.mprc.config.RuntimeInitializer;
import edu.mayo.mprc.daemon.AssignedTaskData;
import edu.mayo.mprc.daemon.NewLogFile;
import edu.mayo.mprc.database.Dao;
import edu.mayo.mprc.swift.dbmapping.*;
import edu.mayo.mprc.utilities.progress.ProgressReport;
import edu.mayo.mprc.workflow.persistence.TaskState;
import edu.mayo.mprc.workspace.User;
import org.hibernate.Session;
import org.joda.time.DateTime;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

public interface SwiftDao extends Dao, RuntimeInitializer {
	/**
	 * see if a title already exists
	 *
	 * @param title Title of the search (as the user entered it)
	 * @param user  The user to run the search.
	 * @return true when the given user already entered transaction of given title.
	 */
	boolean isExistingTitle(String title, User user);

	/**
	 * retrieve the tasks data list for given search run id.
	 * <p/>
	 * The tasks are ordered descending by their start time.
	 *
	 * @param searchRunId Hibernate ID of the search run
	 * @return List of tasks
	 */
	List<TaskData> getTaskDataList(int searchRunId);

	TaskData getTaskData(Integer taskId);

	/**
	 * get the status information for all the search runs. The list is considered read-only - do not modify those searches.
	 *
	 * @param filter      Filter for the search runs.
	 * @param withReports If true, the reports for the search are fetched as well.
	 * @return List of all search runs filtered and ordered as {@link SearchRunFilter specifies}.
	 */
	List<SearchRun> getSearchRunList(SearchRunFilter filter, boolean withReports);

	/**
	 * find number of running tasks for a search run
	 *
	 * @param searchRun - the search run record object  @see SearchRun
	 * @return - how many tasks are running
	 */
	int getNumberRunningTasksForSearchRun(SearchRun searchRun);

	/**
	 * This will find any search runs that have either started or ended since a give time.
	 *
	 * @param updatedSince will only return search runs updated since this time
	 * @return
	 */
	Set<SearchRun> getSearchRuns(boolean showSuccess, boolean showFailure, boolean showWarnings, Date updatedSince);

	SearchRun getSearchRunForId(int searchRunId);

	SpectrumQa addSpectrumQa(SpectrumQa spectrumQa);

	PeptideReport addPeptideReport(PeptideReport peptideReport);

	SwiftSearchDefinition addSwiftSearchDefinition(SwiftSearchDefinition definition);

	SwiftSearchDefinition getSwiftSearchDefinition(Integer swiftSearchId);

	void reportSearchRunProgress(int searchRunId, ProgressReport progress);

	/**
	 * Create a new search run, fill it with initial values, put it in the database.
	 *
	 * @param swiftSearch Search to be run.
	 * @return Search run serialized into the database.
	 */
	SearchRun fillSearchRun(SwiftSearchDefinition swiftSearch);

	/**
	 * @param task Task to update in the database.
	 * @return The updated version of the task.
	 */
	TaskData updateTask(TaskData task);

	TaskStateData getTaskState(Session session, TaskState state);

	/**
	 * Loads the entire task state table into a hash map so it can function at reasonable speeed.
	 *
	 * @param state State to translate to {@link TaskStateData}
	 * @return {@link TaskStateData} for given {@link TaskState}.
	 */
	TaskStateData getTaskState(TaskState state);

	/**
	 * Add a new task state (if it does not exist already in the database).
	 * Flushes the task state cache (for now).
	 *
	 * @param state State to be added.
	 */
	void addTaskState(final TaskState state);

	TaskData createTask(int searchRunId, String name, String descriptionLong, TaskState taskState);

	ReportData storeReport(int searchRunId, File resultFile);

	ReportData storeReport(int searchRunId, File resultFile, DateTime reportDate);


	/**
	 * Find search report file for given report id. Used to retrieve {@link #storeReport} result
	 * after only the report ID got transfered over the network.
	 *
	 * @param reportDataId Id of the {@link ReportData} object.
	 * @return {@link ReportData} for the given id.
	 */
	ReportData getReportForId(long reportDataId);

	void storeAssignedTaskData(TaskData taskData, AssignedTaskData assignedTaskData);

	/**
	 * For a given task, store information about one of its logs or sub-logs.
	 *
	 * @param taskData Task whose log we are storing.
	 * @param logData  Information about the log files.
	 */
	void storeLogData(final TaskData taskData, final NewLogFile logData);

	void searchRunFailed(int searchRunId, String message);

	/**
	 * Go through the database. Find every reference to a file within {@code from} directory.
	 * If the file does not exist, but it does exist in {@code to} directory, update the reference.
	 *
	 * @param from From where did the files move.
	 * @param to   To where did the files move.
	 */
	void renameAllFileReferences(File from, File to);

	/**
	 * For a given list of files to be searched, list runs that:
	 * 1) were successful
	 * 2) are not hidden
	 * 3) are the newest, in case multiple runs were executed with the same name
	 *
	 * @param files Files that have to be contained in the searches (at least one file)
	 * @return All runs that touch on the given input files (sans broken/hidden/old ones)
	 */
	List<SearchRun> findSearchRunsForFiles(List<FileSearch> files);

	/**
	 * If the given input file is utilized in a given search run, return true.
	 *
	 * @param inputFile File to check for.
	 * @param run       Search run that has to contain the file.
	 * @return True if the file is being processed in the given search run.
	 */
	boolean isFileInSearchRun(String inputFile, SearchRun run);

	/**
	 * Hide all {@link SearchRun} instances whose output folder matches the specified one.
	 *
	 * @param outputFolder Output folder. Searches where this matches get hidden.
	 */
	void hideSearchesWithOutputFolder(File outputFolder);

	/**
	 * Just return {@link FileSearch} object for given id.
	 */
	FileSearch getFileSearchForId(int fileSearchId);

	/**
	 * Add a file search object (for testing purposes).
	 */
	FileSearch addFileSearch(final FileSearch fileSearch);
}
