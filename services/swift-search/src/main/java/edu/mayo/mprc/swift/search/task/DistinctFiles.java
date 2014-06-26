package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Helps ensuring that files that should be distinct do not end up identical.
 * See {@link #getDistinctFile} for more information.
 */
final class DistinctFiles {
	/**
	 * Key: Absolute path to a file (typically output file for a search engine or raw2mgf conversion.
	 * Value: How many times was this path requested in the past.
	 */
	private final Map<String/*Absolute to file*/, /*How many times was this name requested*/Integer> fileNameDisambiguation = new HashMap<String, Integer>();

	/**
	 * Files with meaning remember where they should go.
	 */
	private final Map<FileMeaning, File> meaningFileMap = new HashMap<FileMeaning, File>();

	/**
	 * Helps ensuring that two files that should have distinct names are not accidentally named the same.
	 * <p/>
	 * The function never returns same file twice within a given {@link DistinctFiles} object.
	 * <p/>
	 * Filesystem is not checked. The function relies only on the historical calls.
	 * <p/>
	 * Example:
	 * <table border="1"><tr><th>Call #</th><th>Request</th><th>Response</th></tr>
	 * <tr><td>1</td><td>test.txt</td><td>test.txt</td></tr>
	 * <tr><td>2</td><td>hello.txt</td><td>hello.txt</td></tr>
	 * <tr><td>3</td><td>test.txt</td><td>test_2.txt</td></tr>
	 * <tr><td>4</td><td>test_2.txt</td><td>test_2_2.txt</td></tr>
	 * <tr><td>5</td><td>hello.txt</td><td>hello_2.txt</td></tr>
	 * <tr><td>6</td><td>dir/hello.txt</td><td>dir/hello.txt</td></tr>
	 * <tr><td>7</td><td>dir/hello.tar.gz</td><td>dir/hello.tar.gz</td></tr>
	 * <tr><td>8</td><td>dir/hello.tar.gz</td><td>dir/hello_2.tar.gz</td></tr>
	 * </table>
	 * <p/>
	 * This is useful for the searches that search the same file multiple times, so different output file names have to be generated.
	 */
	public File getDistinctFile(final File file) {
		return getDistinctFile(file, null);
	}

	/**
	 * Same as {@link #getDistinctFile(File)} but with added "meaning".
	 * <p/>
	 * If you ask two times for the same file name with the same "meaning", you obtain the same resulting
	 * file name. A meaning is simply an object that will be checked for equality - two meanings are the same
	 * if the two objects are equal.
	 * <p/>
	 * A meaning of 'null' is never equal to another file with meaning of 'null'.
	 *
	 * @param file    File to distinguish.
	 * @param meaning Meaning of the file, or <c>null</c> when the meaning is unique.
	 * @return Distinct file.
	 */
	public synchronized File getDistinctFile(final File file, final Object meaning) {
		String extension = FileUtilities.getGzippedExtension(file.getName());
		if (!extension.isEmpty()) {
			extension = "." + extension;
		}

		return getDistinctFile(file, meaning, extension);
	}

	/**
	 * Same as {@link #getDistinctFile(File, Object)} but allows the user to explicitly specify
	 * the file extension instead of "guessing" it using {@link FileUtilities#getGzippedExtension}.
	 * The extension provided should contain the '.'
	 *
	 * @param file      File to distinguish.
	 * @param meaning   Meaning of the file, or <c>null</c> when the meaning is unique.
	 * @param extension Extension of the file we are distinguishing. Should contain the .
	 * @return Distinct file.
	 */
	public synchronized File getDistinctFile(final File file, final Object meaning, final String extension) {
		String resultingPath = file.getAbsolutePath();

		final FileMeaning fileMeaning;
		// Short-circuit if we already have a file with the same meaning
		if (meaning != null) {
			fileMeaning = new FileMeaning(resultingPath, meaning);
			final File result = meaningFileMap.get(fileMeaning);
			if (result != null) {
				return result;
			}
		} else {
			fileMeaning = null;
		}

		while (fileNameDisambiguation.containsKey(resultingPath)) {
			// There already was a file of given name issued in the past.
			final int previouslyIssuedCount = fileNameDisambiguation.get(resultingPath);
			final int newCount = previouslyIssuedCount + 1;
			// Store information about the collision
			fileNameDisambiguation.put(resultingPath, newCount);

			// Form a hypothesis - this new path should be okay. But we still need to test it in the next loop.
			if (!resultingPath.endsWith(extension)) {
				throw new MprcException(String.format("File [%s] does not have expected extension [%s]", resultingPath, extension));
			}

			final String basePath = resultingPath.substring(0, resultingPath.length() - extension.length());
			resultingPath = basePath + "_" + newCount + extension;
		}
		// The freshly created name has a count 1
		fileNameDisambiguation.put(resultingPath, 1);
		final File result = new File(resultingPath);

		// If we have a file with meaning, remember where we put it
		if (fileMeaning != null) {
			meaningFileMap.put(fileMeaning, result);
		}
		return result;
	}


	private class FileMeaning {
		/**
		 * Absolute path to the file.
		 */
		private final String file;
		private final Object meaning;

		private FileMeaning(final String file, final Object meaning) {
			Preconditions.checkNotNull(meaning, "This class is not designed to store files with unique meanings");
			this.file = file;
			this.meaning = meaning;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(file, meaning);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			final FileMeaning other = (FileMeaning) obj;
			return Objects.equal(this.file, other.file) && Objects.equal(this.meaning, other.meaning);
		}
	}
}
