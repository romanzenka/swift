package edu.mayo.mprc.dbcurator.client.steppanels;

/**
 * @author Eric Winter
 */
public final class HeaderFilterStepStub extends CurationStepStub {
	private static final long serialVersionUID = 20101221L;
	/**
	 * the mode that this step will perform under (any / all / none)
	 */
	public String matchMode = "simple";

	/**
	 * the type of text that this step will accept (simple / regex);
	 */
	public String textMode = "any";

	/**
	 * the actual string we want to search for, simple will be space delimitted
	 */
	public String criteria = "";

	@Override
	public AbstractStepPanel getStepPanel() {
		final HeaderFilterStepPanel panel = new HeaderFilterStepPanel();
		panel.setContainedStep(this);
		return panel;
	}
}
