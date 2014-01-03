package edu.mayo.mprc.io.mgf;

import edu.mayo.mprc.peaklist.PeakListReader;
import edu.mayo.mprc.utilities.FileUtilities;
import org.proteomecommons.io.mgf.MascotGenericFormatPeakList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class MgfTitles {
	/**
	 * @param mgf .mgf file
	 * @return List of all spectra titles.
	 */
	public static List<String> getTitles(final File mgf) {
		PeakListReader sourceMgfReader = new MgfPeakListReader(mgf);
		sourceMgfReader.setReadPeaks(false);
		List<String> titles = new ArrayList<String>(1000);
		try {
			MascotGenericFormatPeakList peakList;
			while ((peakList = sourceMgfReader.nextPeakList()) != null) {
				titles.add(peakList.getTitle());
			}
		} finally {
			FileUtilities.closeQuietly(sourceMgfReader);
		}
		return titles;
	}
}
