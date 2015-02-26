package edu.mayo.mprc.utilities;

import org.apache.log4j.Logger;

import java.io.File;

/**
 * Shortens a file path if necessary (running through Wine on Linux).
 * Remember how the path was shortened to clean up properly when done.
 *
 * @author Roman Zenka
 */
public final class FilePathShortener {
	private static final Logger LOGGER = Logger.getLogger(FilePathShortener.class);
	private boolean temporaryLinkMade;
	private File temporaryFile;

	/**
	 * Shorten given path.
	 *
	 * @param toShorten File whose path should be shortened
	 */
	public FilePathShortener(final File toShorten, final int maxPath) {
		temporaryFile = toShorten;
		if (FileUtilities.isLinuxPlatform() && toShorten.getAbsolutePath().length() > maxPath) {
			// We are on Linux, therefore most likely running wine
			// Make a temporary link to our original raw file, to shorten the path
			try {
				temporaryFile = FileUtilities.shortenFilePath(toShorten);
				LOGGER.info("Shortening path (over " + maxPath + " characters) by linking: " + toShorten.getAbsolutePath() + "->" + temporaryFile.getAbsolutePath());
				temporaryLinkMade = true;
			} catch (Exception ignore) {
				// SWALLOWED: If we fail to make a link, we use the original file. But we must NOT delete it afterwards!
				temporaryLinkMade = false;
			}
		}
	}

	/**
	 * @return The file with shortened path
	 */
	public File getShortenedFile() {
		return temporaryFile;
	}

	/**
	 * Clean up the shortened file (only if shortening was actually performed)
	 */
	public void cleanup() {
		if (temporaryLinkMade) {
			FileUtilities.cleanupShortenedPath(temporaryFile);
		}
	}
}
