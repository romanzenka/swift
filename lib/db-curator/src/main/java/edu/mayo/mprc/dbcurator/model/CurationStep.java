package edu.mayo.mprc.dbcurator.model;

import java.io.Serializable;

/**
 * A curation step is an Interface used to represent a step in the curation process. A curation step will have support
 * for prevalidating the state of the step as well as providing feedback after a step has completed via the
 * StepValidation class.
 * <p/>
 * The action occurs in the performStep() call that takes a CurationExecutor object to perform the step on.
 * <p/>
 * There is also a method for creating a copy of the step because we want to allow users to copy a sequence of steps
 * from one curation to another.
 */
public interface CurationStep extends Serializable {

	/**
	 * perfom the step on a given local database.  If the step could not be performed then a CurationStepException is
	 * thrown.  This indicates that the PostValidation will be unsuccessful and will contain a message indicating why it
	 * was unsuccesfull.
	 * <p/>
	 * There are obviously a wide variety of things that could go wrong with a call to perform step.
	 *
	 * @param exe the CurationExecutor that this step will be working for.
	 * @return the post validation.
	 * @see CurationExecutor
	 */
	StepValidation performStep(CurationExecutor exe);

	/**
	 * Call this method if you want to see if the step is ready to be run and if any issues have been predicted.  NOTE:
	 * succesfull prevalidation can not guarentee<sp> successful processing.
	 *
	 * @param curationDao DAO is needed in some of the steps.
	 * @return the @see StepValidation to interrogate for issues
	 */
	StepValidation preValidate(CurationDao curationDao);

	/**
	 * Creates a copy of this step.  Only persistent properties are included in the copy.
	 *
	 * @return a cropy of this step
	 */
	CurationStep createCopy();

	/**
	 * the id of the curation step used for perstant reasons
	 *
	 * @return
	 */
	Integer getId();

	/**
	 * the id of the curation step.  When this is called it will only be changed if it has not previously been set (is currently null)
	 *
	 * @param id
	 */
	void setId(Integer id);

	/**
	 * gets the number of steps that were present in the curation the last time this step was run
	 *
	 * @return the last run sequence count of this step
	 */
	Integer getLastRunCompletionCount();

	/**
	 * set the number of sequences present after the last run
	 *
	 * @param count last run sequence count
	 */
	void setLastRunCompletionCount(Integer count);

	String simpleDescription();
}
