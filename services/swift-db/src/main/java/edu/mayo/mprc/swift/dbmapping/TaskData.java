package edu.mayo.mprc.swift.dbmapping;

import com.google.common.base.Objects;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.workflow.persistence.TaskState;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

public class TaskData {
	private Integer id;
	private String taskName;
	private Date queueTimestamp;
	private Date startTimestamp;
	private Date endTimestamp;
	private int errorCode;

	private String errorMessage;
	private SearchRun searchRun;
	private TaskStateData taskState;
	private String descriptionLong;
	private String gridJobId;
	private String host;
	private String exceptionString;
	private String warningMessage;
	private Float percentDone;
	private List<LogData> logs;

	public TaskData() {
		setTaskState(new TaskStateData(TaskState.UNINITIALIZED.getText()));
	}

	public TaskData(final String name,
	                final Date queueTimeStamp,
	                final Date startTimeStamp,
	                final Date endTimeStamp,
	                final SearchRun searchRun, final TaskStateData taskState, final String descriptionLong) {
		setId(null);
		taskName = name;
		queueTimestamp = queueTimeStamp;
		startTimestamp = startTimeStamp;
		endTimestamp = endTimeStamp;
		errorCode = 0;
		errorMessage = null;
		this.searchRun = searchRun;
		setTaskState(taskState);
		this.descriptionLong = descriptionLong;
		exceptionString = null;
		percentDone = null;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public void setTaskName(final String taskName) {
		this.taskName = taskName;
	}

	public String getTaskName() {
		return taskName;
	}

	public Date getQueueTimestamp() {
		return queueTimestamp;
	}

	public void setQueueTimestamp(final Date queueTimestamp) {
		this.queueTimestamp = queueTimestamp;
	}

	public void setStartTimestamp(final Date startTimestamp) {
		this.startTimestamp = startTimestamp;
	}

	public Date getStartTimestamp() {
		return startTimestamp;
	}

	public void setEndTimestamp(final Date endTimestamp) {
		this.endTimestamp = endTimestamp;
	}

	public Date getEndTimestamp() {
		return endTimestamp;
	}

	public void setErrorCode(final int errorCode) {
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public SearchRun getSearchRun() {
		return searchRun;
	}

	public void setSearchRun(final SearchRun searchRun) {
		this.searchRun = searchRun;
	}

	public void setTaskState(final TaskStateData taskState) {
		assert (taskState != null);
		this.taskState = taskState;
	}

	public TaskStateData getTaskState() {
		return taskState;
	}

	public void setDescriptionLong(final String descriptionLong) {
		this.descriptionLong = descriptionLong;
	}

	public String getDescriptionLong() {
		return descriptionLong;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(final String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getGridJobId() {
		return gridJobId;
	}

	public void setGridJobId(final String gridJobId) {
		if (gridJobId == null && this.gridJobId != null) {
			// TODO: Hack. This should never happen in practice.
			return;
		}
		this.gridJobId = gridJobId;
	}

	public void setHostString(final String hostString) {
		host = hostString;
	}

	public String getHostString() {
		return host;
	}

	public void setExceptionString(final String exceptionString) {
		this.exceptionString = exceptionString;
	}

	public String getExceptionString() {
		return exceptionString;
	}

	private String getHost() {
		return host;
	}

	public Float getPercentDone() {
		return percentDone;
	}

	public void setPercentDone(final Float percentDone) {
		this.percentDone = percentDone;
	}

	public List<LogData> getLogs() {
		return logs;
	}

	public void setLogs(List<LogData> logs) {
		this.logs = logs;
	}

	public void setException(final Throwable t) {
		setErrorMessage(MprcException.getDetailedMessage(t));
		final StringWriter sw = new StringWriter();
		final PrintWriter writer = new PrintWriter(sw);
		try {
			t.printStackTrace(writer);
			final String stackTrace = sw.toString();
			setExceptionString(stackTrace);
		} finally {
			writer.close();
		}
	}

	public String getWarningMessage() {
		return warningMessage;
	}

	public void setWarningMessage(String warningMessage) {
		this.warningMessage = warningMessage;
	}

	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || !(obj instanceof TaskData)) {
			return false;
		}

		final TaskData taskData = (TaskData) obj;

		return Objects.equal(getEndTimestamp(), taskData.getEndTimestamp()) &&
				Objects.equal(getHost(), taskData.getHost()) &&
				Objects.equal(getQueueTimestamp(), taskData.getQueueTimestamp()) &&
				Objects.equal(getStartTimestamp(), taskData.getStartTimestamp()) &&
				Objects.equal(getTaskName(), taskData.getTaskName()) &&
				Objects.equal(getDescriptionLong(), taskData.getDescriptionLong()) &&
				Objects.equal(getPercentDone(), taskData.getPercentDone());
	}

	public int hashCode() {
		return Objects.hashCode(getTaskName(),
				getQueueTimestamp(),
				getStartTimestamp(),
				getEndTimestamp(),
				getErrorCode(),
				getDescriptionLong(),
				getHost(),
				getPercentDone(),
				getWarningMessage());
	}

	public String toString() {
		return "TaskData{" +
				"id=" + getId() +
				", taskName='" + getTaskName() + '\'' +
				", queueTimestamp=" + getQueueTimestamp() +
				", startTimestamp=" + getStartTimestamp() +
				", endTimestamp=" + getEndTimestamp() +
				", errorCode=" + getErrorCode() +
				", errorMessage='" + getErrorMessage() + '\'' +
				", searchRun=" + getSearchRun() +
				", taskState=" + getTaskState() +
				", descriptionLong='" + getDescriptionLong() + '\'' +
				", gridId=" + getGridJobId() +
				", host='" + getHost() + '\'' +
				", exceptionString='" + getExceptionString() + '\'' +
				", percentDone='" + getPercentDone() + '\'' +
				", warningMessage='" + getWarningMessage() + '\'' +
				'}';
	}
}

