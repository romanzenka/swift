package edu.mayo.mprc.utilities.progress;

/**
 * Listens to daemon progress.
 */
public interface ProgressListener {
	/**
	 * The request was received and put into daemon's queue on the user/host/JVM described by the given string.
	 */
	void requestEnqueued(String hostString);

	/**
	 * The request processing started, potentially on a different host
	 */
	void requestProcessingStarted(String hostString);

	/**
	 * The request processing finished successfully. No more notifications will arrive after this one.
	 */
	void requestProcessingFinished();

	/**
	 * The request processing terminated with a failure. No more notifications will arrive after this one. The
	 * daemon is still running.
	 *
	 * @param e Exception causing the process to terminate.
	 */
	void requestTerminated(Exception e);

	/**
	 * User-specific progress information, such as how many percent done, or that a grid-engine id was assigned.
	 *
	 * @param progressInfo User-specific progress information.
	 */
	void userProgressInformation(ProgressInfo progressInfo);
}
