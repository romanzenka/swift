package edu.mayo.mprc.swift.ui.server;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares two file names. Directories go before files, otherwise they are sorted alphabetically, ignoring case.
 *
 * @author Roman Zenka
 */
class FilenameComparator implements Comparator<File>, Serializable {
	private static final long serialVersionUID = 20100312L;

	@Override
	public int compare(final File o1, final File o2) {
		if (o1.isDirectory() && !o2.isDirectory()) {
			return -1;
		} else if (!o1.isDirectory() && o2.isDirectory()) {
			return 1;
		} else {
			return o1.getName().compareToIgnoreCase(o2.getName());
		}
	}
}