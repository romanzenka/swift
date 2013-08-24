package edu.mayo.mprc.database;

import java.util.*;

/**
 * Base for classes that are nothing but a set to be persisted.
 * We do not care about the order of the items in the database. However, we use {@link LinkedHashSet} simply
 * because we want reproducibility for testing purposes - the items should come out in the same order as they
 * came in.
 * <p/>
 * The set should be reasonably small for this to work well.
 *
 * @author Roman Zenka
 */
public abstract class PersistableSetBase<T extends PersistableBase> extends PersistableBase implements Collection<T> {
	private Collection<T> list;

	public PersistableSetBase() {
		list = new LinkedHashSet();
	}

	public PersistableSetBase(final int initialCapacity) {
		list = new LinkedHashSet(initialCapacity);
	}

	@Override
	public boolean remove(final Object o) {
		return list.remove(o);
	}

	@Override
	public boolean addAll(final Collection<? extends T> ts) {
		return list.addAll(ts);
	}

	/**
	 * Create a list prefilled with a given collection.
	 *
	 * @param items Items to add to this list.
	 */
	public PersistableSetBase(final Collection<T> items) {
		this(items.size());
		list.addAll(items);
	}

	public Collection<T> getList() {
		return list;
	}

	public void setList(final Collection<T> list) {
		this.list = list;
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return list.containsAll(c);
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		return list.removeAll(c);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		return list.retainAll(c);
	}

	@Override
	public Iterator<T> iterator() {
		return list.iterator();
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public boolean contains(final Object o) {
		return list.contains(o);
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		return list.toArray(a);
	}

	@Override
	public boolean add(final T t) {
		return list.add(t);
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PersistableSetBase)) {
			return false;
		}

		final PersistableSetBase that = (PersistableSetBase) o;

		final Set<T> me = makeSet(getList());
		final Set<T> other = makeSet(that.getList());
		return !(me != null ? !me.equals(other) : other != null);

	}

	private Set<T> makeSet(final Collection collection) {
		if (collection == null) {
			return null;
		}
		if (collection instanceof HashSet) {
			return (Set<T>) collection;
		}
		return new HashSet(collection);
	}

	@Override
	public int hashCode() {
		return getList() != null ? makeSet(getList()).hashCode() : 0;
	}
}
