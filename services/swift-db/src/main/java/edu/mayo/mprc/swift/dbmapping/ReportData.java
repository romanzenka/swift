package edu.mayo.mprc.swift.dbmapping;


import org.joda.time.DateTime;

import java.io.File;
import java.text.MessageFormat;

/**
 * A report that was created by Swift that should be publicly downloadable.
 * <p/>
 * Can be either Scaffold, IdpQonvert or IDPicker document.
 */
public class ReportData {

	private Long id;
	private File reportFile;
	private DateTime dateCreated;
	private SearchRun searchRun;
	private Integer analysisId;

	public ReportData() {
	}

	public ReportData(final File file, final DateTime dateCreated, final SearchRun searchRun) {
		reportFile = file;
		this.dateCreated = dateCreated;
		this.searchRun = searchRun;
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public File getReportFile() {
		return reportFile;
	}

	public void setReportFile(final File reportFile) {
		this.reportFile = reportFile;
	}

	public DateTime getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(final DateTime dateCreated) {
		this.dateCreated = dateCreated;
	}

	public SearchRun getSearchRun() {
		return searchRun;
	}

	public void setSearchRun(final SearchRun searchRun) {
		this.searchRun = searchRun;
	}

	public Integer getAnalysisId() {
		return analysisId;
	}

	public void setAnalysisId(Integer analysisId) {
		this.analysisId = analysisId;
	}

	public String toString() {
		return MessageFormat.format("{0}: {1} {2}",
				getId(),
				getSearchRun() == null ? "no search run" : (getSearchRun().getTitle() + "(" + getSearchRun().getId() + ")"),
				getReportFile().toString());
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ReportData)) {
			return false;
		}

		final ReportData that = (ReportData) o;

		if (getDateCreated() != null ? !getDateCreated().equals(that.getDateCreated()) : that.getDateCreated() != null) {
			return false;
		}
		if (getReportFile() != null ? !getReportFile().getAbsoluteFile().equals(that.getReportFile().getAbsoluteFile()) : that.getReportFile() != null) {
			return false;
		}
		if (getSearchRun() != null ? !getSearchRun().equals(that.getSearchRun()) : that.getSearchRun() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getReportFile() != null ? getReportFile().hashCode() : 0;
		result = 31 * result + (getDateCreated() != null ? getDateCreated().hashCode() : 0);
		result = 31 * result + (getSearchRun() != null ? getSearchRun().hashCode() : 0);
		return result;
	}
}
