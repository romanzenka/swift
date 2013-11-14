package edu.mayo.mprc.messaging;

import edu.mayo.mprc.config.Lifecycle;

import java.io.Serializable;

/**
 * A service is a named entity that allows requests to be sent, delivered, executed and
 * responses returned back. There are two users of a service - those who send requests, and those who receive them and send
 * back responses.
 * <p/>
 * How exactly are the requests sent and queued is dependent on implementation - JMS, GridEngine, sockets or other technologies can be used.
 * A service can be one-to-one, one-to-many, load balanced, etc. You create services through
 * central service factory {@link ServiceFactory}. Which service you obtain is specified by the URI.
 * <p/>
 * The Service allows multi-threaded access. You can share one service and send requests from multiple threads,
 * or you can receive requests from multiple threads. Internally, the service might create thread-local variables
 * to cope with such usage patterns in case the underlying technology is not multi-treaded.
 * <p/>
 * See {@link ServiceFactory}.
 */
public interface Service extends Lifecycle {
	/**
	 * Sends an object request to given target with given priority. 5 is default, 0..5 low, 6..9 high.
	 *
	 * @param request  Message to be sent.
	 * @param priority Priority of the request.
	 * @param listener The object to be notified about the request sending progress.
	 */
	void sendRequest(Serializable request, int priority, ResponseListener listener);

	/**
	 * This methods returns the next request sent through this service object. The call to this method
	 * blocks until a request is received.
	 * <p/>
	 * Once you are done receiving, do not forget to call {@link #stop()}. If somebody else wants to
	 * start receiving requests later on and you do not indicate you are no longer receiving, the requests
	 * would bounce round-robin style between the two receivers and the messaging would block.
	 *
	 * @param timeout Timeout in milliseconds. If no message arrives within timeout, null is returned as request.
	 * @return Received request, or null if nothing arrived within the timeout.
	 */
	Request receiveRequest(long timeout);

	/**
	 * Gets service name.
	 *
	 * @return Name of the service.
	 */
	String getName();
}
