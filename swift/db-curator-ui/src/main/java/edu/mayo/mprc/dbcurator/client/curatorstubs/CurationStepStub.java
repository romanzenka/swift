package edu.mayo.mprc.dbcurator.client.curatorstubs;

import edu.mayo.mprc.dbcurator.client.steppanels.AbstractStepPanel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A CurationStepStub is an object that contains the information pertinent to displaying a CurationStep and can be passed
 * between Client and Server using GWT-RPC.  We use the getID for synchronizing the step with an existing step but if ID
 * is null then we have a new step and if a curation was sent out with a step with a given id and comes back without that step
 * then we need to remove that object on the server side.<br>
 * Messaging is currently global to a step.  Any errors will need to be shown by the step as a whole.  If we decide it is
 * worth it we can change modify it to localize the error messages somehow.
 */
public abstract class CurationStepStub implements Serializable {
// ------------------------------ FIELDS ------------------------------

	/**
	 * the id of the step so we can sychnronize with the server properly
	 */
	protected Integer id;

	/**
	 * the progress of the current step if it is running 0-100
	 */
	protected Integer progress;

	protected List<String> messages = new ArrayList<String>();

	/**
	 * the number of sequences in the curation the last time this step was run
	 */
	protected Integer completionCount;

	protected boolean editable = true;
	private static final long serialVersionUID = 20080303L;

// --------------------- GETTER / SETTER METHODS ---------------------

	/**
	 * the number of sequences that were in the curation when the step was complete
	 */
	public Integer getCompletionCount() {
		return completionCount;
	}

	/**
	 * sets the number of sequences that were in the database after completing this step
	 *
	 * @param completionCount
	 */
	public void setCompletionCount(final Integer completionCount) {
		this.completionCount = completionCount;
	}

	/**
	 * gets the id of this step.  The id should not be set except on creation time.  If the id has already been set then
	 * it should not be allowed to be changed.
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Sets the id of the step this should only be changed on creation time
	 */
	public void setId(final Integer id) {
		this.id = id;
	}

	/**
	 * get the progress of teh step if it is executing else null
	 */
	public Integer getProgress() {
		return progress;
	}

	/**
	 * set the progress of the step if it is executing else null
	 */
	public void setProgress(final Integer progress) {
		this.progress = progress;
	}

// -------------------------- OTHER METHODS --------------------------

	/**
	 * add a message to the step stub.
	 *
	 * @param msg
	 */
	public void addMessage(final String msg) {
		messages.add(msg);
	}

	/**
	 * gets any error messages that should be associated with the Step
	 */
	public List<String> getErrorMessages() {
		return messages;
	}

	/**
	 * gets the appropriate StepPanel to represent this CurationStep.  This depends on the type of step since
	 * different steps will have different forms that need to be completed.
	 */
	public abstract AbstractStepPanel getStepPanel();

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(final boolean editable) {
		this.editable = editable;
	}
}
