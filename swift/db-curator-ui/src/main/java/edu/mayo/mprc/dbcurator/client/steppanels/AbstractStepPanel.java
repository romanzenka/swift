package edu.mayo.mprc.dbcurator.client.steppanels;

import com.google.gwt.user.client.ui.Composite;
import edu.mayo.mprc.dbcurator.client.curatorstubs.CurationStepStub;

/**
 * The abstract step mainPanel that will be implemented by the Panels that will contain the step specific infromation entry
 * and display.
 *
 * @author Roman Zenka
 */
public abstract class AbstractStepPanel extends Composite {
// ------------------------------ FIELDS ------------------------------

	/**
	 * the title of the mainPanel that will be placed on the upper left corner
	 */
	protected String title = "Abstract Step";

// --------------------- GETTER / SETTER METHODS ---------------------

	/**
	 * gets the title of this mainPanel (examples incluse "Manual Inclusion Step" or "New Database Inclusion Step"
	 *
	 * @return the title that should be given for this step mainPanel
	 */
	@Override
	public String getTitle() {
		return title;
	}

	/**
	 * sets the title of this mainPanel (examples incluse "Manual Inclusion Step" or "New Database Inclusion Step"
	 *
	 * @param title the new title
	 */
	@Override
	public void setTitle(final String title) {
		this.title = title;
	}

// -------------------------- OTHER METHODS --------------------------

	/**
	 * returns the step that this mainPanel will represent.  This is used to get generic step information such as the
	 * completion count, list of messages, etc.
	 *
	 * @return the step that this mainPanel represents
	 */
	public abstract CurationStepStub getContainedStep();

	/**
	 * gets the url of an image that can be used to represent this step
	 *
	 * @return the url to an image for use with this step
	 */
	public abstract String getImageURL();

	/**
	 * gets a css style name that should be associated with this mainPanel.
	 *
	 * @return a css style to use in conjunction with this mainPanel
	 */
	public abstract String getStyle();


	/**
	 * Set the step associated with this StepPanel.
	 *
	 * @param step the step you want this mainPanel to represent
	 * @throws ClassCastException if the step passed in wasn't the type that the Panel can represent
	 */
	public abstract void setContainedStep(CurationStepStub step) throws ClassCastException;

	/**
	 * @return true if the step is editable else false
	 */
	public boolean isEditable() {
		return getContainedStep().isEditable();
	}

	/**
	 * call this method when this mainPanel should look for updates in its contained step
	 */
	public abstract void update();

	/**
	 * gets a shell that contains this step panel
	 *
	 * @param container the panel that will contain the returned shell
	 * @return the shell that holds this panel
	 */
	public StepPanelShell getShell(final StepPanelContainer container) {
		return new StepPanelShell(this, container);
	}
}
