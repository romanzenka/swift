package edu.mayo.mprc.raw2mgf;

import edu.mayo.mprc.utilities.ComparisonChain;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;

/**
 * Sorts the .mgf spectra logically by scan ids and charge states.
 */
public class DtaComparator implements Comparator<File>, Serializable {
	private static final long serialVersionUID = -2011114L;

	@Override
	public int compare(final File o1, final File o2) {
		final DtaName name1 = new DtaName(o1);
		final DtaName name2 = new DtaName(o2);

		if (name1.matches() && name2.matches()) {
			return ComparisonChain.start()
					.nullsFirst()
					.compare(name1.getSearchName(), name2.getSearchName())
					.compare(Long.parseLong(name1.getFirstScan()), Long.parseLong(name2.getFirstScan()))
					.compare(Long.parseLong(name1.getSecondScan()), Long.parseLong(name2.getSecondScan()))
					.compare(Integer.parseInt(name1.getCharge()), Integer.parseInt(name2.getCharge()))
					.compare(name1.getExtras(), name2.getExtras())
					.result();
		} else {
			return o1.getName().compareTo(o1.getName());
		}
	}
}
