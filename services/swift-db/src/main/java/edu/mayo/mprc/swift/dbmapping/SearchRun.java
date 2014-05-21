package edu.mayo.mprc.swift.dbmapping;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.workspace.User;
import org.hibernate.criterion.Criterion;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * A single run of a given search.
 */
public class SearchRun extends PersistableBase implements Serializable {
	private static final long serialVersionUID = 20080115L;

	// Duplicate of {@link SwiftSearchDefinition}.title to prevent loading of a complex table
	private String title;
	// Duplicate of {@link SwiftSearchDefinition}.user to prevent loading of a complex table
	private User submittingUser;
	private Integer swiftSearch;
	private Date startTimestamp;
	private Date endTimestamp;
	private int errorCode;
	private String errorMessage;
	private int numTasks;
	private int tasksWithWarning;
	private int tasksFailed;
	private int tasksCompleted;
	private Set<ReportData> reports = new HashSet<ReportData>();
	private static final long MAX_MILLIS_PER_SEARCH = 3L * 60L * 60L * 1000L;

	private int hidden;

	public SearchRun() {
	}

	public SearchRun(final String title, final User submittingUser, final SwiftSearchDefinition swiftSearch, final Date startTimestamp, final Date endTimestamp,
	                 final int errorCode, final String errorMessage,
	                 final int numTasks, final int tasksWithWarning, final int taskFailed, final int tasksCompleted, final boolean hidden) {
		this.title = title;
		this.submittingUser = submittingUser;
		this.swiftSearch = swiftSearch == null ? 0 : swiftSearch.getId();
		this.startTimestamp = startTimestamp;
		this.endTimestamp = endTimestamp;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.numTasks = numTasks;
		this.tasksWithWarning = tasksWithWarning;
		tasksFailed = taskFailed;
		this.tasksCompleted = tasksCompleted;
		this.hidden = hidden ? 1 : 0;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setSubmittingUser(final User submittingUser) {
		this.submittingUser = submittingUser;
	}

	public User getSubmittingUser() {
		return submittingUser;
	}

	/**
	 * ID of the {@link SwiftSearchDefinition} describing this search run. A hack to optimize loading of the objects.
	 */
	public Integer getSwiftSearch() {
		return swiftSearch;
	}

	/**
	 * @param swiftSearch ID of related {@link SwiftSearchDefinition}.
	 */
	public void setSwiftSearch(final Integer swiftSearch) {
		this.swiftSearch = swiftSearch;
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

	public void setErrorMessage(final String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setNumTasks(final int numTasks) {
		this.numTasks = numTasks;
	}

	public int getNumTasks() {
		return numTasks;
	}

	public void setTasksFailed(final int tasksFailed) {
		this.tasksFailed = tasksFailed;
	}

	public int getTasksFailed() {
		return tasksFailed;
	}

	public void setTasksCompleted(final int tasksCompleted) {
		this.tasksCompleted = tasksCompleted;
	}

	public int getTasksCompleted() {
		return tasksCompleted;
	}

	public void setTasksWithWarning(final int tasksWithWarning) {
		this.tasksWithWarning = tasksWithWarning;
	}

	public int getTasksWithWarning() {
		return tasksWithWarning;
	}

	public Set<ReportData> getReports() {
		return reports;
	}

	public void setReports(final Set<ReportData> reports) {
		this.reports = reports;
	}

	public int getHidden() {
		return hidden;
	}

	public void setHidden(final int hidden) {
		this.hidden = hidden;
	}

	/**
	 * @return true if this search is running longer than it should.
	 */
	public boolean isRunningTooLong() {
		if (!isCompleted() && getTasksFailed() == 0 && getStartTimestamp() != null) {
			Date now = new Date();
			if (now.getTime() - getStartTimestamp().getTime() > MAX_MILLIS_PER_SEARCH) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether the search run completed (does not mean successfully completed, just that it will not run anymore).
	 * <p/>
	 * This is done by checkeing whether the total
	 * amount of tasks that are ok/with warnings/failed equals the total amount of tasks.
	 *
	 * @return True if the search will not run anymore.
	 */
	public boolean isCompleted() {
		return (getTasksCompleted() + getTasksWithWarning() + getTasksFailed()) >= getNumTasks();
	}

	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof SearchRun)) {
			return false;
		}

		final SearchRun that = (SearchRun) o;

		if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		final int result;
		result = (getId() != null ? getId().hashCode() : 0);
		return result;
	}

	public String toString() {
		return getId() + ": " + getTitle() + " (" + getSubmittingUser() + ")";
	}

	@Override
	public Criterion getEqualityCriteria() {
		throw new MprcException("SearchRun does not support equality checking");
	}
}
