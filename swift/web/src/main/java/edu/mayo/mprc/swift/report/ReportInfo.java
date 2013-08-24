package edu.mayo.mprc.swift.report;

/**
 * Report information. A report is a Scaffold output file which can have Analysis attached to it.
 *
 * @author Roman Zenka
 */
public class ReportInfo implements Comparable<ReportInfo> {
	private long reportId;
	private String filePath;
	private boolean hasAnalysis;

	public ReportInfo(final long reportId, final String filePath, final boolean hasAnalysis) {
		this.reportId = reportId;
		this.filePath = filePath;
		this.hasAnalysis = hasAnalysis;
	}

	public long getReportId() {
		return reportId;
	}

	public String getFilePath() {
		return filePath;
	}

	public boolean isHasAnalysis() {
		return hasAnalysis;
	}

	@Override
	public int compareTo(final ReportInfo reportInfo) {
		return getFilePath().compareTo(reportInfo.getFilePath());
	}
}
