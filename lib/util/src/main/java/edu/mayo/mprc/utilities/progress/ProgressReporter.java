package edu.mayo.mprc.utilities.progress;

/**
 * Interface allowing the user to report progress of a daemon worker.
 * <p/>
 * Use this interface separately when you only want to give your method the
 * progress reporting capabilities, nothing else.
 * <p/>
 * You may want to use {@link UserProgressReporter} if you are taking care of the big reports
 * (work started/succeeded/failed) and you only want the implementer to send additional information.
 */
public interface ProgressReporter extends UserProgressReporter {
	/**
	 * Reports that the worker has started processing. This is implemented so e.g. a worker cache can
	 * postpone this report until it hears from its child worker.
	 */
	void reportStart(String hostString);

	/**
	 * Reports success. There must be no more reports after this one.
	 * The method is guaranteed to never throw an exception.
	 */
	void reportSuccess();

	/**
	 * Reports failure that leads to termination of the worker. There must be no reports after this one.
	 * The method is guaranteed to never throw an exception.
	 *
	 * @param t Exception the worker ended with.
	 */
	void reportFailure(Throwable t);
}
