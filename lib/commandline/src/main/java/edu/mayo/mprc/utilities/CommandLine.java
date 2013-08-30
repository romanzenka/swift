package edu.mayo.mprc.utilities;

import edu.mayo.mprc.MprcException;
import joptsimple.OptionSet;

import java.io.File;

/**
 * Utilities for command line parsing.
 */
public final class CommandLine {

	private CommandLine() {
	}

	/**
	 * Fetches a given file, testing whether it exists. If anything fails, this method
	 * returns null.
	 *
	 * @param options         Command line options.
	 * @param paramName       Name of the command line parameter for this file.
	 * @param fileDescription Description of the file for the error messages.
	 * @param defaultValue    Default name of the file in case it is not specified.
	 * @return The location of the file or null if file does not exist.
	 */
	public static File findFile(final OptionSet options, final String paramName, final String fileDescription, final String defaultValue) {
		File file = null;
		if (options.has(paramName)) {
			file = (File) options.valueOf(paramName);
		} else if (defaultValue != null) {
			file = new File(defaultValue).getAbsoluteFile();
			FileUtilities.err("The " + fileDescription + " parameter not specified, trying default " + file.getAbsolutePath());
			FileUtilities.err("You can set path to " + fileDescription + " using the --" + paramName + " switch.");
		} else {
			throw new MprcException("The " + fileDescription + " parameter not specified.\n" +
					"You can set path to " + fileDescription + " using the --" + paramName + " switch.");
		}

		try {
			FileUtilities.ensureReadableFile(fileDescription, file);
		} catch (MprcException e) {
			// SWALLOWED: we return null on missing file
			FileUtilities.err(e.getMessage());
			return null;
		}
		return file;
	}
}
