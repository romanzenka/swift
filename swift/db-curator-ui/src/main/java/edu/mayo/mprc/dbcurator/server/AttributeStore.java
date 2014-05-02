package edu.mayo.mprc.dbcurator.server;

/**
 * Stores attributes
 *
 * @author Roman Zenka
 */
public interface AttributeStore {
	Object getAttribute(String name);

	void setAttribute(String name, Object value);
}
