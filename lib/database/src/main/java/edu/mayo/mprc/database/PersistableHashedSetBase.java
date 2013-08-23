package edu.mayo.mprc.database;

import java.util.Collection;

/**
 * Same as {@link PersistableSetBase} only with a hash code for optimizing equality checks.
 *
 * @author Roman Zenka
 */
public class PersistableHashedSetBase<T extends PersistableBase> extends PersistableSetBase<T> implements HashedCollection<T> {
	private long hash;

	public PersistableHashedSetBase() {
	}

	public PersistableHashedSetBase(int initialCapacity) {
		super(initialCapacity);
	}

	public PersistableHashedSetBase(Collection<T> items) {
		super(items);
	}

	@Override
	public long getHash() {
		return hash;
	}

	@Override
	public void setHash(long hash) {
		this.hash = hash;
	}

	@Override
	public int hashCode() {
		if (this.getId() != null) {
			// We are serialized, that means immutable.
			// We can use the saved hash value
			return (int) getHash();
		} else {
			return super.hashCode();
		}
	}
}
