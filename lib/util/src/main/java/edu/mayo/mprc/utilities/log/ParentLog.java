package edu.mayo.mprc.utilities.log;

import java.io.Serializable;

/**
 * This object is handed down from the caller and represents the parent log. The only thing the child can do with this
 * is to decide to create one (or multiple) child logs and start logging on them.
 * <p/>
 * A child log can be passed on as {@link ParentLog} again.
 *
 * @author Roman Zenka
 */
public interface ParentLog extends Serializable {
	/**
	 * Create a new object representing a child log.
	 * When this object is created, the parent logging object will be notified that a new child appeared,
	 * so it can keep track of all the children that were spawned from it.
	 *
	 * @return A new object for accessing the log.
	 */
	ChildLog createChildLog();

	ChildLog createChildLog(String outputLogFilePath, String errorLogFilePath);
}
