package edu.mayo.mprc.common.server;

/**
 * When invoked, stops the execution of the web application by sending
 * a signal to the container to shut down.
 *
 * @author Roman Zenka
 */
public interface WebApplicationStopper {
	/**
	 * When invoked, sends a signal to the container to stop the web application.
	 */
	void stopWebApplication();
}
