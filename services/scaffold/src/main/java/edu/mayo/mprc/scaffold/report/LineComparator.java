package edu.mayo.mprc.scaffold.report;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

final class LineComparator implements Comparator<String[]>, Serializable {
	private static final long serialVersionUID = 20101221L;
	private static final String ASCENDING = "a";
	private static final String INTEGER = "i";

	private int[] positions;
	private String[] directions;

	/**
	 * Initialize the comparator to compare given items of the list in given order.
	 *
	 * @param columns         List of column names to be compared to each other.
	 * @param directions      Direction of the comparison, 'a' stands for ascending, 'd' stands for descending.
	 * @param columnPositions Map from column names to column positions
	 */
	public LineComparator(final String[] columns, final String[] directions, final Map<String, Integer> columnPositions) {
		if (columns.length != directions.length) {
			throw new MprcException("The list comparator is not set up correctly.");
		}

		this.directions = directions.clone();
		positions = new int[columns.length];
		for (int i = 0; i < columns.length; i++) {
			positions[i] = ScaffoldReportReader.getColumn(columnPositions, columns[i]);
		}
	}

	@Override
	public int compare(final String[] o1, final String[] o2) {
		for (int i = 0; i < positions.length; i++) {
			final int index = positions[i];
			final int comparison;
			if (directions[i].endsWith(INTEGER)) {
				// Numeric
				comparison = Integer.valueOf(o1[index]).compareTo(Integer.valueOf(o2[index]));
			} else {
				comparison = o1[index].compareTo(o2[index]);
			}
			if (comparison != 0) {
				return directions[i].startsWith(ASCENDING) ? comparison : -comparison;
			}
		}
		return 0;
	}
}
