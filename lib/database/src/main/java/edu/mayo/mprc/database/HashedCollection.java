package edu.mayo.mprc.database;

import java.util.Collection;

/**
 * A collection storing its member hash for faster equality checks.
 *
 * @author Roman Zenka
 */
public interface HashedCollection<S extends PersistableBase> extends Collection<S> {
	long getHash();

	void setHash(long hash);

	/**
	 * Set hash calculated using the standard algorithm.
	 */
	void calculateHash();
}
