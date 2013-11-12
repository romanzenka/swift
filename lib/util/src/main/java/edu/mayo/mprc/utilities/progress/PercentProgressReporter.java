package edu.mayo.mprc.utilities.progress;

/**
 * @author Roman Zenka
 */
public interface PercentProgressReporter {
	/**
	 * @param percent How many percent are done of given activity.
	 */
	void reportProgress(float percent);
}
