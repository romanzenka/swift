package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import edu.mayo.mprc.swift.ui.client.rpc.ClientPeptideReport;

/**
 * Panel allows user to enable reports.
 */
public final class ReportSetupPanel extends HorizontalPanel {

	private final CheckBox scaffoldReport;

	private final QuameterCategorySelector quameterCategorySelector;

	/**
	 * Null Constructor
	 */
	public ReportSetupPanel(final QuameterCategorySelector quameterCategorySelector) {
		this(false, quameterCategorySelector);
	}

	public ReportSetupPanel(final boolean enableScaffoldReport, final QuameterCategorySelector quameterCategorySelector) {
		scaffoldReport = new CheckBox("Generate Peptide Report");
		scaffoldReport.setValue(enableScaffoldReport);
		add(scaffoldReport);

		this.quameterCategorySelector = quameterCategorySelector;
		if (quameterCategorySelector != null) {
			add(this.quameterCategorySelector);
		}
	}

	public boolean isScaffoldReportEnable() {
		return Boolean.TRUE.equals(scaffoldReport.getValue());
	}

	public void setScaffoldReportEnable(final boolean enable) {
		scaffoldReport.setValue(enable);
	}

	public ClientPeptideReport getParameters() {
		return new ClientPeptideReport(isScaffoldReportEnable());
	}

	public void setParameters(final ClientPeptideReport peptideReport) {
		scaffoldReport.setValue(peptideReport != null && peptideReport.isScaffoldPeptideReportEnabled());
	}
}
