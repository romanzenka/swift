package edu.mayo.mprc.integration;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Can install a required environment for testing purposes, using a given folder.
 * <p/>
 * The integration data is downloaded as a .zip file from a given URL to a particular location within the temporary
 * folder. This data is cached and reused as long as the md5 sum of the loaded file matches the expected one.
 * This approach is chosen instead of putting the testing data into the repository since the integration check data
 * is potentially huge and contains entire software packages.
 */
public class Installer {
	private static final String UNKNOWN_INSTALL_ACTION = "Unknown install action {0}";

	/**
	 * Obtain a directory corresponding to given environment variable name. Exception is thrown if the variable
	 * is not set or if the variable points to nonexistent directory.
	 *
	 * @param envVar      Environment variable to read.
	 * @param description Description of what the folder should contain.
	 * @return The matching directory.
	 */
	public static File getDirectory(final String envVar, final String description) {
		final String value = System.getenv(envVar);
		if (value == null) {
			throw new MprcException(MessageFormat.format("Cannot run test. Please set environment variable {0} to a folder with {1}", envVar, description));
		}
		final File directory = new File(value);
		if (!directory.isDirectory()) {
			throw new MprcException(MessageFormat.format("Cannot run test. The environment variable {0} points to {1}, which is not an existing folder", envVar, value));
		}
		if (!directory.canRead()) {
			throw new MprcException(MessageFormat.format("Cannot run test. The environment variable {0} points to {1}, which is not a readable folder", envVar, value));
		}
		return directory;
	}

	/**
	 * Obtain an executable file corresponding to given environment variable name. Exception is thrown if the variable
	 * is not set or if the variable points to nonexistent/non-executable file.
	 *
	 * @param envVar      Environment variable to read.
	 * @param description Description of the executable.
	 * @return The matching executable
	 */
	public static File getExecutable(final String envVar, final String description) {
		final String value = System.getenv(envVar);
		if (value == null) {
			throw new MprcException(MessageFormat.format("Cannot run test. Please set environment variable {0} to a folder with {1}", envVar, description));
		}
		final File directory = new File(value);
		if (!directory.isFile()) {
			throw new MprcException(MessageFormat.format("Cannot run test. The environment variable {0} points to {1}, which is not a file", envVar, value));
		}
		if (!directory.canExecute()) {
			throw new MprcException(MessageFormat.format("Cannot run test. The environment variable {0} points to {1}, which is not executable", envVar, value));
		}
		return directory;
	}


	public enum Action {
		INSTALL,
		UNINSTALL,
	}

	private static final String WRAPPER_SCRIPT = "unixXvfbWrapper.sh";

	private static final List<String> UNIX_XVFB_WRAPPER = Arrays.asList(
			"!/util/" + WRAPPER_SCRIPT
	);

	private static final List<String> FASTA_TEST = Arrays.asList(
			"/test_in.fasta",
			"/test_in.fasta.gz",
			"/test_in_dups.fasta"
	);

	private static final List<String> FASTA_YEAST = Arrays.asList(
			"/SprotYeast.fasta"
	);

	private static final List<String> MGF_TEST = Arrays.asList(
			"/test.mgf"
	);

	private static final List<String> RAW_FILES = Arrays.asList(
			"/test.RAW"
	);

	private static final List<String> PEPXML_FILES = Arrays.asList(
			"/test.pepXML"
	);

	private static final int EXECUTABLE = 0x1ed; // 0755

	/**
	 * Install a given list of files into given folder.
	 *
	 * @param folder        Folder to install to.
	 * @param defaultFolder If folder is null, install into a temporary folder with this name.
	 * @param files         List of files to install. The files prefixed with exclamation mark need to have executable flag set.
	 * @return Folder where the list of files was installed.
	 */
	private static File installList(File folder, final String defaultFolder, final Collection<String> files) {
		folder = folderOrDefault(folder, defaultFolder);

		for (String file : files) {
			try {
				boolean makeExecutable = false;
				if (file.startsWith("!")) {
					file = file.substring(1);
					makeExecutable = true;
				}

				final File resultingFile = TestingUtilities.getNamedFileFromResource(file, folder);

				if (makeExecutable) {
					FileUtilities.chmod(resultingFile, EXECUTABLE, '+', false);
				}
			} catch (Exception e) {
				throw new MprcException("Could not install file: " + file + " to " + folder.getAbsolutePath(), e);
			}
		}
		return folder;
	}

	private static File folderOrDefault(final File folder, final String defaultFolder) {
		if (folder == null) {
			return FileUtilities.createTempFolder(null, defaultFolder, true);
		}
		FileUtilities.ensureFolderExists(folder);
		return folder;
	}

	private static void uninstallList(final File folder, final Collection<String> files) {
		for (final String file : files) {
			final String name = new File(file).getName();
			FileUtilities.cleanupTempFile(new File(folder, name));
		}
		FileUtilities.quietDelete(folder);
		if (folder.exists()) {
			throw new MprcException("Could not uninstall files - the folder " + folder.getAbsolutePath() + " is not empty.");
		}
	}

	private static File processList(final File folder, final String defaultFolder, final Collection<String> files, final Action action) {
		switch (action) {
			case INSTALL:
				return installList(folder, defaultFolder, files);
			case UNINSTALL:
				uninstallList(folder, files);
				return folder;
			default:
				throw new MprcException(MessageFormat.format(UNKNOWN_INSTALL_ACTION, action.name()));
		}
	}

	public static File xvfbWrapper(final File folder, final Action action) {
		return processSingleFile(folder, "util", UNIX_XVFB_WRAPPER, WRAPPER_SCRIPT, action);
	}

	public static File testFastaFiles(final File folder, final Action action) {
		return processList(folder, "fasta", FASTA_TEST, action);
	}

	public static File yeastFastaFiles(final File folder, final Action action) {
		return processList(folder, "fasta", FASTA_YEAST, action);
	}

	public static File mgfFiles(final File folder, final Action action) {
		return processList(folder, "mgf", MGF_TEST, action);
	}

	public static File rawFiles(final File folder, final Action action) {
		return processList(folder, "raw", RAW_FILES, action);
	}

	public static File pepXmlFiles(final File folder, final Action action) {
		return processList(folder, "pepxml", PEPXML_FILES, action);
	}

	private static File processSingleFile(final File folder, final String defaultFolder, final List<String> files, final String mainFile, final Action action) {
		switch (action) {
			case INSTALL:
				final File output = processList(folder, defaultFolder, files, action);
				return new File(output, mainFile);
			case UNINSTALL:
				processList(folder.getParentFile(), defaultFolder, files, action);
				return folder.getParentFile();
			default:
				throw new MprcException(MessageFormat.format(UNKNOWN_INSTALL_ACTION, action.name()));
		}
	}
}
