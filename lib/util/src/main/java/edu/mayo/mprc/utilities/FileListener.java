package edu.mayo.mprc.utilities;

import java.io.File;
import java.util.Collection;

/**
 * Interface for listening to disk file changes.
 *
 * @see FileMonitor
 */
public interface FileListener {
	/**
	 * Called when one of the monitored files is created or modified, or if the file monitoring expires.
	 *
	 * @param files   These files were being monitored for change (all of them were supposed to change).
	 * @param timeout True if this notification was triggered because of a timeout.
	 */
	void fileChanged(Collection<File> files, boolean timeout);
}
