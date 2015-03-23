package edu.mayo.mprc.daemon.worker;

/**
 * This interface is used for marking work packets that can send core # as a part of the request.
 * <p/>
 * This helps the grid runner to announce the amount of cores needed to the grid engine.
 *
 * @author Roman Zenka
 */
public interface CoreRequirements {
	/**
	 * @return How many cores are to be used for doing this job.
	 */
	int getNumRequiredCores();
}
