package edu.mayo.mprc.peaklist;

import java.io.File;

/**
 * @author Roman Zenka
 */
public interface PeakListReaderFactory {
	/**
	 * @return File extension supported by this reader.
	 */
	String getExtension();

	/**
	 * Create an instance of actual reader.
	 *
	 * @param file File to read from.
	 * @return The reader.
	 */
	PeakListReader createReader(File file);
}
