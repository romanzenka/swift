package edu.mayo.mprc.peaklist;

import org.proteomecommons.io.PeakList;

import java.io.Closeable;

/**
 * @author Roman Zenka
 */
public interface PeakListWriter extends Closeable {

	/**
	 * @param peakList PeakList to write
	 * @return scan number written
	 */
	int writePeakList(PeakList peakList);
}
