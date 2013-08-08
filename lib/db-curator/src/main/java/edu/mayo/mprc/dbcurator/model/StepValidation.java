package edu.mayo.mprc.dbcurator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to be used when it is important to communicate the validity of a step either before or after the step is
 * performed.  It also is used to communicate the number of sequences that were in the database after
 * <p/>
 * A successfull pre-validation does not ensure a successful post-validation although that is the goal.
 *
 * @author Eric J. Winter Date: Apr 6, 2007 Time: 8:51:04 AM
 * @version 1.0
 */
public final class StepValidation {
	/**
	 * a simple message indicating any problems, "success" if not issues were observed
	 */
	private List<String> messages = new ArrayList<String>();

	/**
	 * the number of sequences in the database upon step completion (-1 if N/A, or unknown)
	 */
	private int completionCount;

	/**
	 * a list of any exceptions that might have been thrown just for those who are curious
	 */
	private List<Exception> wrappedExceptions;

	/**
	 * A null constructor that sets the message to "undetermined" and completion count to -1
	 */
	public StepValidation() {
		this.setCompletionCount(-1);
		this.wrappedExceptions = new ArrayList<Exception>();
	}

	/**
	 * Determine if a step will be or was successful(true) or not (false)
	 *
	 * @return true if the validation indicates success or else false
	 */
	public boolean isOK() {
		return this.messages.isEmpty();
	}

	/**
	 * Get the messages that have been added to this validation.
	 */
	public List<String> getMessages() {
		return Collections.unmodifiableList(messages);
	}

	public void addMessage(final String message) {
		this.messages.add(message);
	}

	/**
	 * gets the number of sequences in the database when the step completed or -1 if not applicable
	 */
	public int getCompletionCount() {
		return completionCount;
	}

	public void setCompletionCount(final int completionCount) {
		this.completionCount = completionCount;
	}


	/**
	 * Exceptions may have been thrown either during the validation or performing the step and if this is the case you
	 * may be interested in see what exceptions were thrown.
	 *
	 * @return the list of exceptions that may have been thrown as part of the step or validation
	 */
	public List<Exception> getWrappedExceptions() {
		return this.wrappedExceptions;
	}

	/**
	 * take an exception and sw
	 * @param e
	 */
	//void wrapException(Exception e) {
	//	e.printStackTrace();
	//	this.wrappedExceptions.add(e);
	//}

	/**
	 * adds a message as well as wrapping an exception.  The exception will have its stack trace printed to the set OutputStream
	 * or System.out as a default
	 *
	 * @param message
	 * @param e
	 */
	public void addMessageAndException(final String message, final Exception e) {
		this.messages.add(message);
		if (e != null) {
			this.wrappedExceptions.add(e);
		}
	}

}

