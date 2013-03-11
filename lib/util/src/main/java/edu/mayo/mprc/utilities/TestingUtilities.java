package edu.mayo.mprc.utilities;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.io.Resources;
import edu.mayo.mprc.MprcException;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Eric Winter
 */
public final class TestingUtilities {
	private static final Logger LOGGER = Logger.getLogger(TestingUtilities.class);
	private static final String TEMP_EXTENSION = ".tmp";

	private TestingUtilities() {
	}

	/**
	 * Creates a temporary file from a given input stream.
	 *
	 * @param instream   the stream we want to write to a temporary file
	 * @param autoDelete
	 * @param suffix
	 * @return the temporary file
	 * @throws IOException if there was a problem using the stream or writing the file.
	 */
	public static File getTempFileFromInputStream(final InputStream instream, final boolean autoDelete, final File parentFolder, final String suffix) throws IOException {
		final File tmpFile = getUniqueTempFile(autoDelete, parentFolder, suffix);
		FileUtilities.writeStreamToFile(instream, tmpFile);
		if (autoDelete) {
			tmpFile.deleteOnExit();
		}
		return tmpFile;
	}

	/**
	 * @param autoDelete   set to true if you want to have the file cleaned up on JVM exit, no guareentee but...
	 * @param parentFolder Where to create the file. When null, uses default temp folder.
	 * @param suffix       Suffix for the file.
	 * @return A temporary file.
	 * @throws IOException The file could not be created.
	 */
	public static File getUniqueTempFile(final boolean autoDelete, final File parentFolder, final String suffix) throws IOException {
		final File tmpFile = File.createTempFile(
				"test_" + new SimpleDateFormat("ddMMMyyyy-HHmmss").format(new Date()),
				suffix,
				parentFolder);
		if (autoDelete) {
			tmpFile.deleteOnExit();
		}
		return tmpFile;
	}

	/**
	 * Gets a temp file in the system temporary space but deletes the file so this will really be a marker
	 *
	 * @return a system temporary File with no actual file in that location
	 * @throws IOException if the file could not be used
	 */
	public static File getTempFileMarker(final File parentFolder) throws IOException {
		final File file = getUniqueTempFile(false, parentFolder, TEMP_EXTENSION);
		FileUtilities.quietDelete(file);
		return file;
	}

	/**
	 * gets a temporary file by looking for a given resource using the ClassLoader.
	 *
	 * @param clazz      the class you want to use to start ClassLoader search from.
	 * @param resource   the resource we will try to find using the ClassLoader
	 * @param autoDelete
	 * @return the temporary file created from that stream
	 * @throws IOException there were any io related issues.
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	public static File getTempFileFromResource(final Class<?> clazz, final String resource, final boolean autoDelete, final File parentFolder) throws IOException {
		return getTempFileFromResource(clazz, resource, autoDelete, parentFolder, TEMP_EXTENSION);
	}

	/**
	 * gets a temporary file by looking for a given resource using the ClassLoader.
	 *
	 * @param clazz      the class you want to use to start ClassLoader search from.
	 * @param resource   the resource we will try to find using the ClassLoader
	 * @param autoDelete
	 * @param suffix     the suffix to be added at the end of the file name
	 * @return the temporary file created from that stream
	 * @throws IOException there were any io related issues.
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	public static File getTempFileFromResource(final Class<?> clazz, final String resource, final boolean autoDelete, final File parentFolder, final String suffix) throws IOException {
		final InputStream is = clazz.getResourceAsStream(resource);
		if (is == null) {
			throw new IOException("Resource not found: " + resource);
		}
		return getTempFileFromInputStream(is, autoDelete, parentFolder, suffix);
	}

	/**
	 * creates a file in a particular directory that will be deleted upon vm closing.
	 *
	 * @param clazz       the class to obtain the class loader from
	 * @param resource    the resource to create a file form
	 * @param inDirectory the directory where we want to place the temporary file
	 * @return the File in the given directory
	 * @throws IOException if we couldn't write a file to the given directory.
	 */
	public static File getTempFileFromResource(final Class<?> clazz, final String resource, final File inDirectory) throws IOException {
		InputStream is = null;
		try {
			is = clazz.getResourceAsStream(resource);
			return getTempFileFromInputStream(is, true, inDirectory, TEMP_EXTENSION);
		} finally {
			FileUtilities.closeQuietly(is);
		}
	}

	/**
	 * gets a temporary file by looking for a given resource using the ClassLoader.
	 * except uses this (TestingUtilities) as the class to start search.
	 * <p/>
	 *
	 * @param resource   the resource we will try to find using the ClassLoader
	 * @param autoDelete
	 * @return the temporary file created from that stream
	 * @throws IOException there were any io related issues.
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	public static File getTempFileFromResource(final String resource, final boolean autoDelete, final File parentFolder) throws IOException {
		return getTempFileFromResource(resource, autoDelete, parentFolder, TEMP_EXTENSION);
	}

	/**
	 * gets a temporary file by looking for a given resource using the ClassLoader.
	 * except uses this (TestingUtilities) as the class to start search.
	 * <p/>
	 *
	 * @param resource   the resource we will try to find using the ClassLoader
	 * @param autoDelete
	 * @param suffix     The suffix to add to the file name.
	 * @return the temporary file created from that stream
	 * @throws IOException there were any io related issues.
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	public static File getTempFileFromResource(final String resource, final boolean autoDelete, final File parentFolder, final String suffix) throws IOException {
		final InputStream is = TestingUtilities.class.getResourceAsStream(resource);
		try {
			if (is == null) {
				throw new IOException("Resource not found: " + resource);
			}
			return getTempFileFromInputStream(is, autoDelete, parentFolder, suffix);
		} finally {
			FileUtilities.closeQuietly(is);
		}
	}

	/**
	 * Creates a file from given resource that has the same file name and is located in specified parent folder.
	 */
	public static File getNamedFileFromResource(final String resource, final File parentFolder) throws IOException {
		final InputStream is = TestingUtilities.class.getResourceAsStream(resource);
		try {
			final String name = new File(resource).getName();
			if (is == null) {
				throw new IOException("Resource not found: " + resource);
			}
			final File outputFile = new File(parentFolder, name);
			FileUtilities.writeStreamToFile(is, outputFile);
			return outputFile;
		} finally {
			FileUtilities.closeQuietly(is);
		}
	}

	/**
	 * @param firstFile  the first file to compare
	 * @param secondFile the file to compare {@code firstFile} to
	 * @param trim       If true lines are trimmed before being compared.
	 * @return returns null if files are equal; otherwise, returns first different lines found.
	 */
	public static String compareFilesByLine(final File firstFile, final File secondFile, final boolean trim) {

		Preconditions.checkNotNull(firstFile, "Cannot compare a null file");
		Preconditions.checkNotNull(secondFile, "Cannot compare a null file");

		BufferedReader br1 = null;
		BufferedReader br2 = null;
		try {
			br1 = new BufferedReader(new FileReader(firstFile));
			br2 = new BufferedReader(new FileReader(secondFile));

			return compareByLine(trim, br1, br2);

		} catch (FileNotFoundException e) {
			throw new MprcException("Could not compare files", e);
		} finally {
			FileUtilities.closeQuietly(br1);
			FileUtilities.closeQuietly(br2);
		}
	}

	/**
	 * Compares two strings by separating each into lines.
	 *
	 * @param s1
	 * @param s2
	 * @param trim
	 * @return null if files are equal; otherwise, returns first different lines found.
	 * @throws IOException
	 */
	public static String compareStringsByLine(final String s1, final String s2, final boolean trim) {
		BufferedReader br1 = null;
		BufferedReader br2 = null;
		try {

			br1 = new BufferedReader(new StringReader(s1));
			br2 = new BufferedReader(new StringReader(s2));

			return compareByLine(trim, br1, br2);

		} finally {
			FileUtilities.closeQuietly(br1);
			FileUtilities.closeQuietly(br2);
		}
	}

	/**
	 * Performs the actual comparison.
	 *
	 * @param trim If true, lines are trimmed before compare runs.
	 * @param br1  First reader.
	 * @param br2  Second reader.
	 * @return First line that differs or null if files are the same.
	 */
	private static String compareByLine(final boolean trim, final BufferedReader br1, final BufferedReader br2) {
		String line1 = null;
		String line2 = null;
		boolean different = false;
		int lineNumber = 0;

		while (true) {
			try {
				line1 = br1.readLine();
				line2 = br2.readLine();
			} catch (IOException e) {
				throw new MprcException("Read for comparison failed", e);
			}
			lineNumber++;
			if (line1 == null || line2 == null) {
				break;
			}
			if (trim) {
				line1 = line1.trim();
				line2 = line2.trim();
			}
			if (!line1.equals(line2)) {
			different = true;
				break;
			}
		}

		if (different || (line1 != null || line2 != null)) {
			return "Difference in line #" + lineNumber + ":\n[" + line1 + "]\n[" + line2 + "]";
		} else {
			return null;
		}
	}

	/**
	 * Compares lines without trimming tailing white spaces.
	 *
	 * @param firstFile
	 * @param secondFile
	 * @return null if files are equal.
	 */
	public static String compareFilesByLine(final File firstFile, final File secondFile) {
		return compareFilesByLine(firstFile, secondFile, false);
	}

	public static void quietDelete(final String name) {
		if (name != null) {
			final File file = new File(name);
			if (file.exists() && !file.delete()) {
				LOGGER.debug("Failed to delete file " + name);
			}
		}
	}

	/**
	 * Check that given string matches what is in a given resource file.
	 * <p/>
	 * Check is done line by line, each line gets trimmed before comparison.
	 *
	 * @param actual           What we got.
	 * @param expectedResource Name of the resource containing what we expect to see.
	 * @return String describing the first difference, or null if identical. See {@link #compareStringsByLine}
	 */
	public static String compareStringToResourceByLine(final String actual, final String expectedResource) {
		return compareStringsByLine(actual, resourceToString(expectedResource), true);
	}

	/**
	 * Load given resource URL into a string.
	 * @param url URL to load.
	 * @return Resulting string.
	 */
	public static String resourceToString(String url) {
		try {
			return Resources.toString(Resources.getResource(url), Charsets.UTF_8);
		} catch (IOException e) {
			throw new MprcException("Could not compare to resource [" + url + "]", e);
		}
	}

	/**
	 * Split the string into lines, trim those lines, join them with proper newline characters.
	 * @param s String to canonicalize.
	 * @return Canonicalized string.
	 */
	public static String canonicalizeNewLines(final CharSequence s) {
		return Joiner.on('\n').join(Splitter.on('\n').trimResults().split(s));
	}
}
