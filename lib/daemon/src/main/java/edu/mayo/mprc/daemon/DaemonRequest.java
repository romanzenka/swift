package edu.mayo.mprc.daemon;

import edu.mayo.mprc.daemon.worker.WorkPacket;

import java.io.Serializable;

public interface DaemonRequest {
	WorkPacket getWorkPacket();

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
