package edu.mayo.mprc.swift.dbmapping;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.PersistableBase;
import org.hibernate.criterion.Criterion;

import java.io.File;

/**
 * Information about a particular log.
 * <p/>
 * Refers to files with standard output and standard error.
 *
 * @author Roman Zenka
 */
public final class LogData extends PersistableBase {
	private TaskData task;
	private LogData parentLog;
	private File outputLog;
	private File errorLog;

	public LogData() {
	}

	public LogData(final TaskData task, final LogData parentLog, final File outputLog, final File errorLog) {
		this.task = task;
		this.parentLog = parentLog;
		this.outputLog = outputLog;
		this.errorLog = errorLog;
	}

	public TaskData getTask() {
		return task;
	}

	public void setTask(final TaskData task) {
		this.task = task;
	}

	public LogData getParentLog() {
		return parentLog;
	}

	public void setParentLog(final LogData parentLog) {
		this.parentLog = parentLog;
	}

	public File getOutputLog() {
		return outputLog;
	}

	public void setOutputLog(final File outputLog) {
		this.outputLog = outputLog;
	}

	public File getErrorLog() {
		return errorLog;
	}

	public void setErrorLog(final File errorLog) {
		this.errorLog = errorLog;
	}

	@Override
	public Criterion getEqualityCriteria() {
		throw new MprcException(getClass().getSimpleName() + " does not support equality criteria");
	}
}
