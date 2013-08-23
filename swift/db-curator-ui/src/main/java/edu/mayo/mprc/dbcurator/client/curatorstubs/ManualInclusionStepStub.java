package edu.mayo.mprc.dbcurator.client.curatorstubs;

import edu.mayo.mprc.dbcurator.client.steppanels.AbstractStepPanel;
import edu.mayo.mprc.dbcurator.client.steppanels.ManualInclusionPanel;

/**
 * @author Eric Winter
 */
public final class ManualInclusionStepStub extends CurationStepStub {
	private static final long serialVersionUID = 20101221L;

	public String header = "";
	public String sequence = "";


	/**
	 * {@inheritDoc}
	 * in this case we will return a StepPanelShell that will contain a ManualInclusionPanel
	 */
	@Override
	public AbstractStepPanel getStepPanel() {
		final ManualInclusionPanel panel = new ManualInclusionPanel();
		panel.setContainedStep(this);
		return panel;
	}

}