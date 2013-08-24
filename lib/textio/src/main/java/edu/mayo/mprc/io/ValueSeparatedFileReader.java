package edu.mayo.mprc.io;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class that allows random access of value separated files. The implementation of this class is not thread safe.
 * TODO: Merge this with {@link edu.mayo.mprc.io.TsvStreamReader}
 */
public final class ValueSeparatedFileReader implements Closeable {

	private File file;
	private String columnDelimiter;
	private BufferedReader reader;
	private List<String> currentRowValue;
	private boolean rereadLast = false;
	private String lastLine = null;
	private static final int BUFFER_SIZE = 10 * 1024;

	public ValueSeparatedFileReader(final File file, final String columnDelimiter) throws IOException {
		this.file = file;
		this.columnDelimiter = columnDelimiter;
		reader = null;
		resetCurrentRowPointer();
	}

	public File getFile() {
		return file;
	}

	public String getColumnDelimiter() {
		return columnDelimiter;
	}

	/**
	 * @return True if there are more rows to read.
	 */
	public boolean hasMoreRows() {
		try {
			final List<String> contents = nextRow();
			previousRow();
			return contents != null;
		} catch (IOException t) {
			throw new MprcException(t);
		}
	}

	/**
	 * Read and tokenize next row.
	 * If there are no more rows, return null.
	 */
	public List<String> nextRow() throws IOException {
		if (!rereadLast) {
			lastLine = reader.readLine();
			if (lastLine != null) {
				final String[] split = lastLine.split(columnDelimiter, /*Keep trailing*/-1);
				currentRowValue = Arrays.asList(split);
			} else {
				currentRowValue = null;
			}
		}
		rereadLast = false;
		return currentRowValue;
	}

	/**
	 * Read and tokenize next row.
	 * If there are no more rows, return null.
	 *
	 * @param includeColumnDelimiters If set, the delimiters are returned as a part of the output.
	 * @param columns                 Return only the columns of given indices. The includeColumnDelimiters parameter does not
	 *                                affect this output -you will get the same set of data columns irregardless of whether you required the delimiters or not.
	 *                                Only the values of listed columns are returned. If null, all columns are returned.
	 *                                When there are missing requested columns in the input, null is returned.
	 */
	public List<String> nextRow(final boolean includeColumnDelimiters, final int[] columns) throws IOException {
		final List<String> row = nextRow();
		if (row == null) {
			return null;
		}

		if (columns == null) {
			if (!includeColumnDelimiters) {
				return row;
			} else {
				return interleave(row);
			}
		}
		final List<String> subset = new ArrayList<String>(columns.length + (includeColumnDelimiters ? columns.length - 1 : 0));
		for (int i = 0; i < columns.length; i++) {
			if (columns[i] >= row.size()) {
				return null;
			}
			subset.add(row.get(columns[i]));
			if (includeColumnDelimiters && i < columns.length - 1) {
				subset.add(columnDelimiter);
			}
		}
		return subset;
	}

	private List<String> interleave(final List<String> row) {
		final List<String> interleaved = new ArrayList<String>(row.size() + row.size() - 1);
		int i = 0;
		for (final String s : row) {
			interleaved.add(s);
			i++;
			if (i < row.size()) {
				interleaved.add(columnDelimiter);
			}
		}
		return interleaved;
	}

	/**
	 * Makes the next call to {@link #nextRow} return the same information it returned previously. This is useful when
	 * peeking at the next line before passing the reader elsewhere.
	 */
	private void previousRow() throws IOException {
		rereadLast = true;
	}

	/**
	 * Start reading from the beginning
	 */
	public void resetCurrentRowPointer() throws IOException {
		currentRowValue = null;

		FileUtilities.closeQuietly(reader);

		reader = new BufferedReader(new FileReader(file), BUFFER_SIZE);

		// Initialize by reading the first row
		nextRow();

		// Next call to nextRow will return the line we just read
		previousRow();
	}

	@Override
	public void close() throws IOException {
		FileUtilities.closeQuietly(reader);
	}
}
