package edu.mayo.mprc.daemon;

import edu.mayo.mprc.utilities.progress.ProgressReporter;

public interface Worker {

	/**
	 * Processes given request data.
	 * The is responsible for a call {@link edu.mayo.mprc.utilities.progress.ProgressReporter#reportSuccess()} or {@link edu.mayo.mprc.utilities.progress.ProgressReporter#reportFailure(Throwable)}
	 * to signalize whether it succeeded or failed. This call can be performed after the method is done executing,
	 * e.g. be scheduled for later time. You can also report failure or success and keep executing, as long as you do not
	 * report success or failure twice in a row.
	 * <p/>
	 * <b>IMPORTANT:</b> Never throw an exception from a worker. Always catch them and use {@link edu.mayo.mprc.utilities.progress.ProgressReporter#reportFailure}
	 * to report them.
	 *
	 * @param workPacket       Work packet to be processed.
	 * @param progressReporter To report progress, success or failures.
	 */
	void processRequest(WorkPacket workPacket, ProgressReporter progressReporter);

	/**
	 * After the worker was fully configured, this method makes sure that all pieces are in place.
	 * If they are not, it will throw an exception.
	 * This way we can fail early on startup.
	 */
	void check();
}
