package edu.mayo.mprc.config;

/**
 * @author Roman Zenka
 */
public interface Checkable {
	/**
	 * Throw an exception if something is wrong with this object.
	 */
	void check();
}
