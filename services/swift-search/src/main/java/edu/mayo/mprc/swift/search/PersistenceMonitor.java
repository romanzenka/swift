package edu.mayo.mprc.swift.search;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.AssignedTaskData;
import edu.mayo.mprc.daemon.worker.log.NewLogFiles;
import edu.mayo.mprc.daemon.TaskWarning;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.LogData;
import edu.mayo.mprc.swift.dbmapping.TaskData;
import edu.mayo.mprc.swift.search.task.AsyncTaskBase;
import edu.mayo.mprc.utilities.progress.PercentDone;
import edu.mayo.mprc.utilities.progress.ProgressReport;
import edu.mayo.mprc.workflow.engine.SearchMonitor;
import edu.mayo.mprc.workflow.engine.TaskBase;
import edu.mayo.mprc.workflow.persistence.TaskState;
import org.apache.log4j.Logger;

public final class PersistenceMonitor implements SearchMonitor {

	private static final Logger LOGGER = Logger.getLogger(PersistenceMonitor.class);

	private int searchRunId;
	private SwiftDao swiftDao;
	private final LogMap logMap = new LogMap();

	public PersistenceMonitor(final int searchRunId, final SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
		this.searchRunId = searchRunId;
	}

	@Override
	public void updateStatistics(final ProgressReport report) {
		swiftDao.begin();
		try {
			swiftDao.reportSearchRunProgress(searchRunId, report);
			swiftDao.commit();
		} catch (final Exception t) {
			swiftDao.rollback();
			throw new MprcException("Could not store search progress information to the database", t);
		}
	}

	@Override
	public void taskChange(final TaskBase task) {
		// We do not report tasks failing internally
		if (task.getState() == TaskState.INIT_FAILED || task.getState() == TaskState.UNINITIALIZED) {
			return;
		}

		swiftDao.begin();
		try {
			syncTaskBase(task, task.getState());
			swiftDao.commit();
		} catch (final Exception t) {
			throw new MprcException("Could not store change in task information", t);
		}
	}

	@Override
	public void error(final TaskBase task, final Throwable t) {
		swiftDao.begin();
		try {
			syncTaskBase(task, TaskState.RUN_FAILED);
			swiftDao.commit();
		} catch (final Exception e) {
			// SWALLOWED: just log
			LOGGER.error("Could not store " + task.getName() + " task exception into the database (" + t.getMessage() + ").", e);
			swiftDao.rollback();
		}
	}

	@Override
	public void error(final Throwable t) {
		// TODO: Ideally this would store the exception in a separate table that SearchRun table links to
		LOGGER.error("Workflow engine error (logged here as it currently cannot be fully stored in the database):", t);
		String message = MprcException.getDetailedMessage(t);
		if (message.length() > 496) {
			message = message.substring(0, 496) + "...";
		}
		swiftDao.begin();
		try {
			swiftDao.searchRunFailed(searchRunId, message);
			swiftDao.commit();
		} catch (final Exception e) {
			// SWALLOWED: just log
			LOGGER.error("Could not store " + searchRunId + " search run exception into the database (" + t.getMessage() + ").", e);
			swiftDao.rollback();
		}
	}

	/**
	 * Task progress information arrived. This is called after the task has a chance to process the progress info.
	 */
	@Override
	public void taskProgress(final TaskBase task, final Object progressInfo) {
		if (task instanceof AsyncTaskBase) {
			if (progressInfo instanceof AssignedTaskData) {
				swiftDao.begin();
				final TaskData data = syncTaskBase(task, task.getState());
				try {
					swiftDao.storeAssignedTaskData(data, (AssignedTaskData) progressInfo);
					swiftDao.commit();
				} catch (final Exception t) {
					// SWALLOWED: just log
					LOGGER.error("Could not store " + task.getName() + " assigned task data into the database (" + t.getMessage() + ").", t);
					swiftDao.rollback();
				}
			} else if (progressInfo instanceof NewLogFiles) {
				swiftDao.begin();
				final TaskData data = syncTaskBase(task, task.getState());
				try {
					final LogData logData = new LogData(
							data,
							logMap.getLogData(data, ((NewLogFiles) progressInfo).getParentLogId()),
							((NewLogFiles) progressInfo).getOutputLogFile(),
							((NewLogFiles) progressInfo).getErrorLogFile());
					final LogData savedLogData = swiftDao.storeLogData(logData);
					logMap.addLogData(((NewLogFiles) progressInfo).getLogId(), savedLogData);
					swiftDao.commit();
				} catch (final Exception t) {
					// SWALLOWED: just log
					LOGGER.error("Could not store " + task.getName() + " log data into the database (" + t.getMessage() + ").", t);
					swiftDao.rollback();
				}
			} else if (progressInfo instanceof TaskWarning) {
				swiftDao.begin();
				final TaskData data = syncTaskBase(task, task.getState());
				try {
					data.setWarningMessage(((TaskWarning) progressInfo).getWarningMessage());
					swiftDao.commit();
				} catch (final Exception t) {
					// SWALLOWED: just log
					LOGGER.error("Could not store " + task.getName() + " assigned task data into the database (" + t.getMessage() + ").", t);
					swiftDao.rollback();
				}
			} else {
				// No matter what happened, we just update the task
				swiftDao.begin();
				try {
					final TaskData data = syncTaskBase(task, task.getState());
					// We got PercentDone message, let's store that
					if (progressInfo instanceof PercentDone) {
						final PercentDone done = (PercentDone) progressInfo;
						data.setPercentDone(done.getPercentDone());
					}
					swiftDao.commit();
				} catch (final Exception t) {
					// SWALLOWED: just log
					LOGGER.error("Could not store " + task.getName() + " task progress into the database (" + t.getMessage() + ").", t);
					swiftDao.rollback();
				}
			}
		}
	}

	private TaskData syncTaskBase(final TaskBase task, final TaskState state) {
		final Integer id = task.getTaskDataId();
		final TaskData data;
		if (id == null) {
			data = swiftDao.createTask(searchRunId, task.getName(), task.getDescription(), state);
			task.setTaskDataId(data.getId());
		} else {
			data = swiftDao.getTaskData(id);
		}
		// Transfer the data from the task to the TaskData object.
		if (task.getLastError() != null) {
			data.setException(task.getLastError());
			data.setErrorCode(1);
		}
		if (task.getLastWarning() != null) {
			data.setWarningMessage(task.getLastWarning());
		}
		data.setTaskState(swiftDao.getTaskState(task.getState()));
		data.setTaskName(task.getName());
		data.setDescriptionLong(task.getDescription());
		if (task instanceof AsyncTaskBase) {
			final AsyncTaskBase asyncTask = (AsyncTaskBase) task;
			data.setQueueTimestamp(asyncTask.getTaskEnqueued());
			data.setStartTimestamp(asyncTask.getTaskProcessingStarted());
		} else {
			data.setQueueTimestamp(task.getExecutionStarted());
			data.setStartTimestamp(task.getExecutionStarted());
		}
		data.setEndTimestamp(task.getExecutionFinished());
		data.setHostString(task.getExecutedOnHost());
		if (task.isDone()) {
			logMap.removeTask(data);
		}
		return data;
	}

	public Integer getSearchRunId() {
		return searchRunId;
	}
}
