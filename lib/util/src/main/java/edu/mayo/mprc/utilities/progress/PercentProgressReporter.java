package edu.mayo.mprc.utilities.progress;

/**
 * @author Roman Zenka
 */
public interface PercentProgressReporter {
	/**
	 * @param percent How many percent are done of given activity. Reported as a number in 0..1
	 */
	void reportProgress(float percent);
}
