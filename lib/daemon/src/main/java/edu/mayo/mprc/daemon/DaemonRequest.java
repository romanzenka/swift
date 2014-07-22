package edu.mayo.mprc.daemon;

import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.utilities.log.ParentLog;

import java.io.Serializable;

public interface DaemonRequest {
	/**
	 * @return Information about the work to be done.
	 */
	WorkPacket getWorkPacket();

	/**
	 * @return The log as set up on the sender side.
	 * <p/>
	 * The obtained {@link ParentLog} object should be set up in such way that when its {@link edu.mayo.mprc.utilities.log.ParentLog#createChildLog()}
	 * method is run, the sender of the daemon request will be notified about this.
	 */
	ParentLog getParentLog();

	/**
	 * Sends an object response to the sender of the request.
	 * There can be more than one response to a request, the last one has to have the isLast parameter
	 * set to true.
	 *
	 * @param response Data to send back to the original request sender.
	 * @param isLast   True if this is the last response to return.
	 */
	void sendResponse(Serializable response, boolean isLast);

	/**
	 * Notifies that request has been processed.
	 */
	void processed();
}
