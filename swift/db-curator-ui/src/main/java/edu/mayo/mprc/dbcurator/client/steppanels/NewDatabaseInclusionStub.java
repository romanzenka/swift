package edu.mayo.mprc.dbcurator.client.steppanels;

/**
 * A stub for the NewDatabaseInclusionStep that contains only the properties and a few client required methods.
 */
public final class NewDatabaseInclusionStub extends CurationStepStub {
	private static final long serialVersionUID = 20101221L;

	/**
	 * the url where we want to download files from.  Currently only site with anonymous access is supported
	 */
	public String url = "";

	/**
	 * {@inheritDoc}
	 * <br>
	 * In this case a StepPanelShell containing a NewDatabaseInclusionPanel will be returned.
	 */
	@Override
	public AbstractStepPanel getStepPanel() {
		//create a panel and add this stub to it
		final NewDatabaseInclusionPanel panel = new NewDatabaseInclusionPanel();
		panel.setContainedStep(this);
		return panel;
	}
}
