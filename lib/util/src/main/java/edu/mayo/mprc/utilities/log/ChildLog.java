package edu.mayo.mprc.utilities.log;

import org.apache.log4j.Logger;

import java.io.Closeable;

/**
 * The logging interface provides TWO distinct methods of logging.
 * <p/>
 * One of them configures log4j so every call to loggers from now on gets redirected.
 * You set this up by calling {@link #startLogging()} and {@link #stopLogging()} method pair.
 * <p/>
 * The other method will give you output and error loggers. These need to be closed.
 * Use {@link #getOutputLogger()} and {@link #getErrorLogger()} with {@link #close()}.
 *
 * @author Roman Zenka
 */
public interface ChildLog extends ParentLog, Closeable {
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
	 * Warning - this logger holds a file handle open
	 * and as such needs to be closed using {@link #close()}
	 *
	 * @return Log4j logger to store the standard output messages.
	 */
	Logger getOutputLogger();

	/**
	 * Warning - this logger holds a file handle open
	 * and as such needs to be closed using {@link #close()}
	 *
	 * @return Log4j logger to store the standard error messages.
	 */
	Logger getErrorLogger();

	/**
	 * You must close the loggers obtained using {@link #getOutputLogger} and {@link #getErrorLogger} using this method.
	 */
	void close();
}
