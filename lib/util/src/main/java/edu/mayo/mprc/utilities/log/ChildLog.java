package edu.mayo.mprc.utilities.log;

import org.apache.log4j.Logger;

/**
 * @author Roman Zenka
 */
public interface ChildLog extends ParentLog {
	/**
	 * Make sure that from now on, for this thread, log4j is set up to properly redirect
	 * logging to the log this object represents.
	 */
	void startLogging();

	/**
	 * Reset log4j so it no longer redirects this thread to the log represented by the particular object.
	 */
	void stopLogging();

	/**
	 * @return Log4j logger to store the standard output messages.
	 */
	Logger getOutputLogger();

	/**
	 * @return Log4j logger to store the standard error messages.
	 */
	Logger getErrorLogger();
}
