package edu.mayo.mprc.dbcurator.model;

import java.util.List;

/**
 * A CurationStatus is an object that represents the status of a CuratorExecutor that in in execution.
 *
 * @author Eric J. Winter Date: Apr 17, 2007 Time: 2:08:22 PM
 */
public interface CurationStatus {
	/**
	 * gets an unmodifiable list of the messages that have been added to this object since creation
	 *
	 * @return an unmodifiable list of Strings as messages
	 */
	List<String> getMessages();

	/**
	 * Gets the progress of the currently executing step
	 *
	 * @return the current progress.
	 */
	float getCurrentStepProgress();

	/**
	 * sets the progress of the current step
	 *
	 * @param progress
	 */
	void setCurrentStepProgress(float progress);

	/**
	 * gets a list of validation steps that have completed.  This can be used to indicate the step that were were
	 * completed successfully.
	 *
	 * @return an unmodifable list of StepValidation returned as the steps are completed
	 */
	List<StepValidation> getCompletedStepValidations();

	/**
	 * gets a list of failed step validations.  There will probably only be a single step in here since when one fails
	 * the execution should cease.
	 *
	 * @return an unmodifable list of of StepValidation returned as steps are completed
	 */
	List<StepValidation> getFailedStepValidations();

	/**
	 * call this when you want to find out if the executor is finished.
	 *
	 * @return true if the executor is in progress else false
	 */
	boolean isInProgress();

	/**
	 * if this has been interrupted by someone return <code>true</code> else return <code>false</code>
	 *
	 * @return true if we have been interrupted else false
	 */
	boolean isInterrupted();

	/**
	 * gets which step is currently being worked on (base 1)
	 *
	 * @return the index of the currently executing step
	 */
	int getCurrentStepNumber();

	/**
	 * Finds out how many sequences were in the database after the last step
	 *
	 * @return the number of sequence that were in the output file after the last step was completed.  -1 if no steps
	 *         have been completed.
	 */
	int getLastStepSequenceCount();

	/**
	 * add a message to the status object
	 *
	 * @param toAdd the message to add
	 */
	void addMessage(String toAdd);

}
