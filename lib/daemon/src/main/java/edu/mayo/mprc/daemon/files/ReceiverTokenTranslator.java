package edu.mayo.mprc.daemon.files;

import java.io.File;

/**
 * After a {@link FileTokenHolder} was sent over the wire, this translator allows you to turn {@link FileToken} back
 * to {@code File} objects.
 */
public interface ReceiverTokenTranslator {
	/**
	 * Translates FileToken object into locally accessible file.
	 *
	 * @param fileToken
	 * @return
	 */
	File getFile(FileToken fileToken);
}
