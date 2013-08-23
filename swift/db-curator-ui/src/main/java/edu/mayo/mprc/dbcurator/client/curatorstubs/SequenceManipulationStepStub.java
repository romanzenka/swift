package edu.mayo.mprc.dbcurator.client.curatorstubs;

import edu.mayo.mprc.dbcurator.client.steppanels.AbstractStepPanel;
import edu.mayo.mprc.dbcurator.client.steppanels.SequenceManipulationPanel;

/**
 * A CurationStepStub that will act as a front for SequenceManipulationSteps.
 *
 * @author Eric Winter
 */
public final class SequenceManipulationStepStub extends CurationStepStub {
	private static final long serialVersionUID = 20101221L;

	public static final String REVERSAL = "reversal";
	public static final String SCRAMBLE = "scramble";

	public boolean overwrite = false;
	public String manipulationType = REVERSAL;

	/**
	 * {@inheritDoc}
	 * <p/>
	 * In this case we will create a StepPanelShell that will contain a SequenceManipulationPanel
	 */
	@Override
	public AbstractStepPanel getStepPanel() {
		final SequenceManipulationPanel panel = new SequenceManipulationPanel();
		panel.setContainedStep(this);
		return panel;
	}
}