package edu.mayo.mprc.io;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.StringUtilities;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads tab separated files in a stream fashion - never going back.
 */
public final class TsvStreamReader implements Closeable {

	private static final int BUFFER_SIZE = 10240;
	private static final char DELIMITER = '\t';
	private BufferedReader reader;
	private String lastLine;

	public TsvStreamReader(final File tsvFile) {
		try {
			open(new FileReader(tsvFile));
		} catch (IOException e) {
			throw new MprcException("Cannot open tsv file [" + tsvFile.getAbsolutePath() + "]", e);
		}
	}

	public TsvStreamReader(final Reader reader) {
		open(reader);
	}

	private void open(final Reader reader) {
		try {
			this.reader = new BufferedReader(reader, BUFFER_SIZE);
			lastLine = this.reader.readLine();
		} catch (IOException t) {
			close();
			throw new MprcException(t);
		}
	}

	/**
	 * @return True if there are any lines left to read.
	 */
	public boolean hasLine() {
		return lastLine != null;
	}


	/**
	 * @return Current line as a single string. Null if we are at the last line.
	 */
	public String getCurrentLine() {
		return lastLine;
	}

	/**
	 * @return Next line in the input file or null if we reached the end.
	 */
	public String nextLine() {
		if (lastLine == null) {
			return null;
		}
		try {
			final String line = lastLine;
			lastLine = reader.readLine();
			return line;
		} catch (IOException t) {
			close();
			throw new MprcException(t);
		}
	}


	/**
	 * Like nextLine, only parses columns into particular data types.
	 */
	public boolean nextValues(final int[] columnIndices, final char[] columnTypes, final float[] floatValues, final int[] intValues, final String[] stringValues) {
		if (!hasLine()) {
			return false;
		}
		final ArrayList<String> tokens = new ArrayList<String>(columnIndices.length);
		StringUtilities.split(lastLine, DELIMITER, tokens);
		int floatPos = 0;
		int intPos = 0;
		int stringPos = 0;
		for (int i = 0; i < columnIndices.length; i++) {
			final String value = tokens.get(columnIndices[i]);
			switch (columnTypes[i]) {
				case 'f':
					floatValues[floatPos++] = Float.parseFloat(value);
					break;
				case 'i':
					intValues[intPos++] = Integer.parseInt(value);
					break;
				case 's':
					stringValues[stringPos++] = value;
					break;
				default:
					throw new MprcException("Column type '" + columnTypes[i] + "' not supported.");
			}
		}
		nextLine();
		return true;
	}

	/**
	 * Like nextLine, assumes all columns are strings, splits input into columns and retains only the requested column indices.
	 * Fills a given array to prevent array allocation. Assumes the string array has the same length as the array of column indices.
	 */
	public boolean nextValues(final int[] columnIndices, final List<String> stringValues) {
		if (!hasLine()) {
			return false;
		}

		final ArrayList<String> tokens = new ArrayList<String>(columnIndices.length);
		StringUtilities.split(lastLine, '\t', tokens);

		stringValues.clear();
		for (final int index : columnIndices) {
			if (index >= tokens.size()) {
				// We could not fulfill the contract - we produce only a partial result
				break;
			}
			stringValues.add(tokens.get(index));
		}
		nextLine();
		return true;
	}


	@Override
	public void close() {
		FileUtilities.closeQuietly(reader);
	}
}
