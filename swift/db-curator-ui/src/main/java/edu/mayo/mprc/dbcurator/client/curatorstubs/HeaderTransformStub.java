package edu.mayo.mprc.dbcurator.client.curatorstubs;

import edu.mayo.mprc.dbcurator.client.steppanels.AbstractStepPanel;
import edu.mayo.mprc.dbcurator.client.steppanels.HeaderTransformPanel;

/**
 * @author Eric Winter
 */
public final class HeaderTransformStub extends CurationStepStub {
	private static final long serialVersionUID = 20101221L;

	public int id;
	public String description;
	public String matchPattern;
	public String subPattern;

	@Override
	public AbstractStepPanel getStepPanel() {
		final HeaderTransformPanel panel = new HeaderTransformPanel();
		panel.setContainedStep(this);
		return panel;
	}

	public String toString() {
		return description + " --> " + matchPattern + " --> " + subPattern;
	}
}
