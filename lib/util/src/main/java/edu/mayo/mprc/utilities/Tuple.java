package edu.mayo.mprc.utilities;

import java.io.Serializable;

public final class Tuple<S extends Comparable<S> & Serializable, T extends Comparable<T> & Serializable> implements Comparable<Tuple<S, T>>, Serializable {
	private static final long serialVersionUID = 20100912L;
	private final S first;
	private final T second;

	public Tuple(final S first, final T second) {
		this.first = first;
		this.second = second;
	}

	public S getFirst() {
		return first;
	}

	public T getSecond() {
		return second;
	}

	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Tuple)) {
			return false;
		}

		final Tuple tuple = (Tuple) o;

		if (first != null ? !first.equals(tuple.first) : tuple.first != null) {
			return false;
		}
		return !(second != null ? !second.equals(tuple.second) : tuple.second != null);
	}

	public int hashCode() {
		int result;
		result = (first != null ? first.hashCode() : 0);
		result = 31 * result + (second != null ? second.hashCode() : 0);
		return result;
	}


	/**
	 * Compare two objects, handle nulls correctly.
	 */
	public int compareWithNulls(final Comparable o1, final Object o2) {
		if (o1 == null) {
			if (o2 == null) {
				return 0;
			} else {
				return -1;
			}
		}
		if (o2 == null) {
			return 1;
		}
		// o1 and o2 are not null
		return o1.compareTo(o2);
	}

	@Override
	public int compareTo(final Tuple<S, T> o) {
		final int c1 = compareWithNulls(first, o.first);
		if (c1 == 0) {
			return compareWithNulls(second, o.second);
		}
		return c1;
	}
}
