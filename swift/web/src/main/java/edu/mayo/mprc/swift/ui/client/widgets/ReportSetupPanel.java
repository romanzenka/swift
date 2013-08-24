package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import edu.mayo.mprc.swift.ui.client.rpc.ClientPeptideReport;

/**
 * Panel allows user to enable reports.
 */
public final class ReportSetupPanel extends HorizontalPanel {

	private final CheckBox scaffoldReport;

	/**
	 * Null Constructor
	 */
	public ReportSetupPanel() {
		this(false);
	}

	public ReportSetupPanel(final boolean enableScaffoldReport) {
		scaffoldReport = new CheckBox("Generate Peptide Report");
		scaffoldReport.setChecked(enableScaffoldReport);
		add(scaffoldReport);
	}

	public boolean isScaffoldReportEnable() {
		return scaffoldReport.isChecked();
	}

	public void setScaffoldReportEnable(final boolean enable) {
		scaffoldReport.setChecked(enable);
	}

	public ClientPeptideReport getParameters() {
		return new ClientPeptideReport(isScaffoldReportEnable());
	}

	public void setParameters(final ClientPeptideReport peptideReport) {
		scaffoldReport.setChecked(peptideReport != null && peptideReport.isScaffoldPeptideReportEnabled());
	}
}
