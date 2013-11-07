package edu.mayo.mprc.config;

/**
 * @author Roman Zenka
 */
public interface Checkable {
	/**
	 * Return a string if something is wrong, null if everything is ok.
	 */
	String check();
}
