package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class RawFilesSupported {
	public static boolean isRawFile(final File file) {
		final String extension = FileUtilities.getExtension(file.getName()).toLowerCase();
		return "raw".equals(extension) || "d".equals(extension);
	}
}
