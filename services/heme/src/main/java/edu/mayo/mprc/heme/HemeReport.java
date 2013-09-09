package edu.mayo.mprc.heme;

import java.util.List;

/**
 * @author Roman Zenka
 */
public final class HemeReport {
	private List<HemeReportEntry> entries;

	public HemeReport(List<HemeReportEntry> entries) {
		this.entries = entries;
	}

	public List<HemeReportEntry> getEntries() {
		return entries;
	}
}
