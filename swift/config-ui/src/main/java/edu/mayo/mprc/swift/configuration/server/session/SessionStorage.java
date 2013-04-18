package edu.mayo.mprc.swift.configuration.server.session;

/**
 * Abstracting away access to a session storage (e.g. in servlet context).
 *
 * @author Roman Zenka
 */
public interface SessionStorage {
	/**
	 * @param key Name of the object.
	 * @return Object associated with the key. Null if no such object exists.
	 */
	Object get(final String key);

	/**
	 * @param key    Key for the object.
	 * @param object Object to store.
	 */
	void put(final String key, final Object object);
}
