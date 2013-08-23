package edu.mayo.mprc.scaffold.report;

import edu.mayo.mprc.MprcException;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

final class ListComparator implements Comparator<List<String>>, Serializable {
	private static final long serialVersionUID = 20101221L;
	private static final String ASCENDING = "a";
	private static final String INTEGER = "i";

	private int[] indices;
	private String[] directions;

	/**
	 * Initialize the comparator to compare given items of the list in given order.
	 *
	 * @param indices    Indices of list items to be compared to each other.
	 * @param directions Direction of the comparison, 'a' stands for ascending, 'd' stands for descending.
	 */
	public ListComparator(final int[] indices, final String[] directions) {
		if (indices.length != directions.length) {
			throw new MprcException("The list comparator is not set up correctly.");
		}

		this.indices = indices.clone();
		this.directions = directions.clone();
	}

	@Override
	public int compare(final List<String> o1, final List<String> o2) {
		for (int i = 0; i < indices.length; i++) {
			final int index = indices[i];
			final int comparison;
			if (directions[i].endsWith(INTEGER)) {
				// Numeric
				comparison = Integer.valueOf(o1.get(index)).compareTo(Integer.valueOf(o2.get(index)));
			} else {
				comparison = o1.get(index).compareTo(o2.get(index));
			}
			if (comparison != 0) {
				return directions[i].startsWith(ASCENDING) ? comparison : -comparison;
			}
		}
		return 0;
	}
}
