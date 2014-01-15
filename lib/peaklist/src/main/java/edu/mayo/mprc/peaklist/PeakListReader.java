package edu.mayo.mprc.peaklist;

import java.io.Closeable;

/**
 * @author Roman Zenka
 */
public interface PeakListReader extends Closeable {
	/**
	 * Read the next peak list from the file.
	 *
	 * @return PeakList just read or null if we are at the end of the file.
	 */
	PeakList nextPeakList();

	/**
	 * @return True if this reader actually reads the peak information (otherwise it reads just the headers)
	 */
	boolean isReadPeaks();

	/**
	 * @param readPeaks When true, the reader will also read the actual peaks.
	 */
	void setReadPeaks(boolean readPeaks);
}
