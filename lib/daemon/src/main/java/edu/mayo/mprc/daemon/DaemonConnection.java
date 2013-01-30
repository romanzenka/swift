package edu.mayo.mprc.daemon;

import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.daemon.files.FileTokenHolder;
import edu.mayo.mprc.messaging.Request;
import edu.mayo.mprc.messaging.ResponseListener;
import edu.mayo.mprc.messaging.Service;
import edu.mayo.mprc.utilities.progress.ProgressListener;

/**
 * A wrapper around {@link Service} that adds the capability to translate {@link FileTokenHolder}
 * object as they go onto and off the wire. This requires the received {@link Request} to be
 * wrapped into {@link DaemonRequest} in order to ensure the translation also happens as responses are sent back.
 * <p/>
 * The responses going on the wire add additional semantics. There are separate messages sent when the request gets enqueued,
 * starts running, finishes running successfully or finishes running with a failure. All these responses are
 * being reported using a {@link ProgressListener} which wraps {@link ResponseListener}.
 */
public interface DaemonConnection {
	/**
	 * @return The file token factory that to translate work packets with files to be sent over the net.
	 */
	FileTokenFactory getFileTokenFactory();

	/**
	 * @return Unique name of this connection.
	 */
	String getConnectionName();

	/**
	 * Sends work to daemon. The progress is monitored by supplied listener. The listener must not be null.
	 *
	 * @param workPacket Any data that represents work for the daemon.
	 * @param listener   Gets information about the work progress. Must not be null.
	 */
	void sendWork(WorkPacket workPacket, ProgressListener listener);

	/**
	 * Sends work to daemon. The progress is monitored by supplied listener. The listener must not be null.
	 * The request is sent with a given priority.
	 *
	 * @param workPacket Any data that represents work for the daemon.
	 * @param priority   Priority to send the request with. Priority of 5 is normal, 0..5 low, 6..9 high.
	 * @param listener   Gets information about the work progress. Must not be null.
	 */
	void sendWork(WorkPacket workPacket, int priority, ProgressListener listener);

	/**
	 * Receive work.
	 *
	 * @param timeout The method returns null after the timeout passes without any work arriving.
	 * @return Requested work + means of reporting progress.
	 */
	DaemonRequest receiveDaemonRequest(long timeout);

	/**
	 * When you no longer want to receive messages.
	 */
	void close();
}
