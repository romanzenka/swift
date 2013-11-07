package edu.mayo.mprc.config;

import java.util.Map;

/**
 * A marker of a resource that can be installed.
 * <p/>
 * Installation is a one-time operation to be performed on a clean machine.
 * Installation is not needed after it was done.
 *
 * @author Roman Zenka
 */
public interface Installable {
	/**
	 * Perform installation steps on the resource so it can start to get used.
	 *
	 * @param params
	 */
	void install(Map<String, String> params);
}
