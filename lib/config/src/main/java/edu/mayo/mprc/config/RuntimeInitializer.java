package edu.mayo.mprc.config;

import java.util.Map;

/**
 * Initializes a module before it is being run.
 * The initialization consists of a check whether the work needs to be done. If that is the case,
 * the initialization itself is invoked.
 */
public interface RuntimeInitializer extends Checkable {
	/**
	 * Ensures that all the work needed for a module to run is done. This is called only if {@link #check()}
	 * returns that work is needed.
	 * This is usually done when initializing the database, initial directory layout, etc.
	 *
	 * @param params
	 */
	void initialize(Map<String, String> params);
}
