package edu.mayo.mprc.swift.dbmapping;


import org.joda.time.DateTime;

import java.io.File;
import java.text.MessageFormat;

/**
 * A report that was created by Swift that should be publicly downloadable.
 * <p/>
 * Currently this is always a Scaffold document.
 */
public class ReportData {

	private Long id;
	private File reportFileId;
	private DateTime dateCreated;
	private SearchRun searchRun;


	public ReportData() {
	}

	public ReportData(File file, DateTime dateCreated, SearchRun searchRun) {
		this.reportFileId = file;
		this.dateCreated = dateCreated;
		this.searchRun = searchRun;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public File getReportFileId() {
		return reportFileId;
	}

	public void setReportFileId(File reportFileId) {
		this.reportFileId = reportFileId;
	}

	public DateTime getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(DateTime dateCreated) {
		this.dateCreated = dateCreated;
	}

	public SearchRun getSearchRun() {
		return searchRun;
	}

	public void setSearchRun(SearchRun searchRun) {
		this.searchRun = searchRun;
	}

	public String toString() {
		return MessageFormat.format("{0}: {1} {2}",
				getId(),
				getSearchRun() == null ? "no search run" : (getSearchRun().getTitle() + "(" + getSearchRun().getId() + ")"),
				getReportFileId().toString());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ReportData)) {
			return false;
		}

		ReportData that = (ReportData) o;

		if (getDateCreated() != null ? !getDateCreated().equals(that.getDateCreated()) : that.getDateCreated() != null) {
			return false;
		}
		if (getReportFileId() != null ? !getReportFileId().getAbsoluteFile().equals(that.getReportFileId().getAbsoluteFile()) : that.getReportFileId() != null) {
			return false;
		}
		if (getSearchRun() != null ? !getSearchRun().equals(that.getSearchRun()) : that.getSearchRun() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getReportFileId() != null ? getReportFileId().hashCode() : 0;
		result = 31 * result + (getDateCreated() != null ? getDateCreated().hashCode() : 0);
		result = 31 * result + (getSearchRun() != null ? getSearchRun().hashCode() : 0);
		return result;
	}
}
