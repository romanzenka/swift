package edu.mayo.mprc.swift.search.task;

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
	private Map<String/*Absolute to file*/, /*How many times was this name requested*/Integer> fileNameDisambiguation = new HashMap<String, Integer>();

	/**
	 * Helps ensuring that two files that should have distinct names are not accidentally named the same.
	 * <p/>
	 * The function returns same file twice for a single {@link DistinctFiles} object.
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
	public synchronized File getDistinctFile(final File file) {
		String resultingPath = file.getAbsolutePath();
		while (fileNameDisambiguation.containsKey(resultingPath)) {
			// There already was a file of given name issued in the past.
			final int previouslyIssuedCount = fileNameDisambiguation.get(resultingPath);
			final int newCount = previouslyIssuedCount + 1;
			// Store information about the collision
			fileNameDisambiguation.put(resultingPath, newCount);

			// Form a hypothesis - this new path should be okay. But we still need to test it in the next loop.			
			String extension = FileUtilities.getGzippedExtension(new File(resultingPath).getName());
			if (!extension.isEmpty()) {
				extension = "." + extension;
			}

			final String basePath = resultingPath.substring(0, resultingPath.length() - extension.length());
			resultingPath = basePath + "_" + newCount + extension;
		}
		// The freshly created name has a count 1
		fileNameDisambiguation.put(resultingPath, 1);
		return new File(resultingPath);
	}
}
