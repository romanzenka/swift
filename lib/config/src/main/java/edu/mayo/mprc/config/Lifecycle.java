package edu.mayo.mprc.config;

/**
 * Objects with this interface have a lifecycle. Once they are created,
 * they need to be started. Before they get destroyed, they need to be stopped.
 * <p/>
 * This lifecycle needs to be controlled separately, since an object can be created with the purpose of
 * checking its configuration, or running its installation routine, while starting it is not recommended.
 * <p/>
 * This is modelled after Spring's Lifecycle, but is independent on Spring, as we do not want spring to manage
 * the lifecycle of the objects automatically.
 * <p/>
 * Since it is currently very difficult to get the control of the lifecycle right, the implementors could be designed
 * to start themselves up on demand. If that is not possible the object should fail if it is not started.
 *
 * @author Roman Zenka
 */
public interface Lifecycle {
	/**
	 * Check whether this component is currently running.
	 *
	 * @return whether the component is currently running
	 */
	boolean isRunning();

	/**
	 * Start this component. Should not throw an exception if the component is already running.
	 * <p/>
	 * In the case of a container, this will propagate the start signal to all components that apply.
	 */
	void start();

	/**
	 * Stop this component, typically in a synchronous fashion, such that the component is fully stopped upon return of this method.
	 * Should not throw an exception if the component isn't started yet.
	 * <p/>
	 * In the case of a container, this will propagate the stop signal to all components that apply.
	 */
	void stop();
}
