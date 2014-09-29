package edu.mayo.mprc.qa;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.io.KeyedTsvReader;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.StringUtilities;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * A class capable of reading rawDump output and then responding to queries. For given scan number produces a tab-separated line
 * with rawDump data. If the input file is null, the class will return empty output for any query.
 * <p/>
 * Right now the class simply loads the full file into memory, in the future (as we scale) we might want to do indexing
 * and serve the data from the disk.
 * <p/>
 * See also: {@link edu.mayo.mprc.msmseval.MSMSEvalOutputReader}.
 */
public final class RawDumpReader implements KeyedTsvReader, Iterable<String> {
	private static final Logger LOGGER = Logger.getLogger(RawDumpReader.class);

	private Map<String/*Scan ID*/, String/*The full line without scan ID*/> lines;
	private String[] header;
	private static final String SCAN_NUM_HEADER = "Scan Id";
	private int firstSpectrum;
	private int lastSpectrum;
	public static final String MS_LEVEL = "MS Level";
	private int rtColumnIndex;

	/**
	 * Header without the first {@link #SCAN_NUM_HEADER} column.
	 */
	private static final String[] DEFAULT_HEADER = new String[]{
			"Parent m/z",
			"TIC", "RT", MS_LEVEL,
			"Parent Scan",
			"Child Scans",
			"Ion Injection Time", "Cycle Time", "Elapsed Time", "Dead Time", "Time To Next Scan",
			"Lock Mass Found", "Lock Mass Shift",
			"Conversion Parameter I", "Conversion Parameter A", "Conversion Parameter B", "Conversion Parameter C", "Conversion Parameter D", "Conversion Parameter E",
			"Dissociation Type",
			"Polymer Segment Size", "Polymer Offset", "Polymer Score", "Polymer p-value"
	};
	private static final String EMPTY_LINE;
	private static final Pattern TAB_SPLIT = Pattern.compile("\t");

	static {
		// One less tab - we produce tabs only in between values
		// Also, we automatically consider all spectra to be MS/MS
		int msLevelPos = -1;
		for (int i = 0; i < DEFAULT_HEADER.length; i++) {
			if (MS_LEVEL.equalsIgnoreCase(DEFAULT_HEADER[i])) {
				msLevelPos = i;
				break;
			}
		}
		if (msLevelPos == -1) {
			EMPTY_LINE = StringUtilities.repeat('\t', DEFAULT_HEADER.length - 1);
		} else {
			EMPTY_LINE = StringUtilities.repeat('\t', msLevelPos) + "2" + StringUtilities.repeat('\t', DEFAULT_HEADER.length - 1 - msLevelPos);
		}
	}

	/**
	 * Prepare the reader. In this implementation, the entire file is loaded at once and cached in memory (we
	 * expect the file to have around 10000 lines).
	 *
	 * @param rawDumpFile rawDump file to process
	 */
	public RawDumpReader(final File rawDumpFile) {
		if (rawDumpFile == null) {
			// Null files are honored - they will act as if there was no input information
			// Use default header (otherwise we use header obtained from the file).
			header = DEFAULT_HEADER;
			rtColumnIndex = findRtColumnIndex(DEFAULT_HEADER);
		} else {
			lines = new HashMap<String, String>();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(rawDumpFile));
				parse(reader, lines);
			} catch (Exception t) {
				throw new MprcException("Cannot parse rawDump file [" + rawDumpFile.getAbsolutePath() + "]", t);
			} finally {
				FileUtilities.closeQuietly(reader);
			}
		}
	}

	/**
	 * @return A tab-separated header line (sans the Scan number column) for all columns in the data in proper format
	 */
	@Override
	public String getHeaderLine() {
		return Joiner.on("\t").join(header);
	}

	@Override
	public String getEmptyLine() {
		return EMPTY_LINE;
	}

	/**
	 * @param key Scan number to obtain data for.
	 * @return Entire line for a given scan, ready to be embedded in a larger tab-separated scan. The line does not
	 * contain the scan id. The columns are as specified by {@link #getHeaderLine()}.
	 */
	@Override
	public String getLineForKey(final String key) {
		if (lines == null) {
			return EMPTY_LINE;
		}
		final String line = lines.get(key);
		if (line == null) {
			return EMPTY_LINE;
		}
		return line;
	}

	/**
	 * Extract retention time field from given line.
	 *
	 * @param line Line to extract RT from
	 * @return Retention time, expressed as string.
	 */
	public String getRtFromLine(final String line) {
		final Iterator<String> iterator = Splitter.on('\t').trimResults().split(line).iterator();
		int i = 0;
		String element = null;
		while (i <= rtColumnIndex) {
			checkIterator(line, iterator, i);
			element = iterator.next();
			i++;
		}
		return element;
	}

	private static void checkIterator(String line, Iterator<String> iterator, int i) {
		if (!iterator.hasNext()) {
			throw new MprcException(String.format("Could not extract retention time from line %s - there is no column #%d (zero-based)", line, i));
		}
	}

	private void parse(final BufferedReader br, final Map<String, String> lines) {
		try {
			initSpectrumMinMax();
			header = readHeader(br);
			String line;
			int ignoredLines = 0;
			while (true) {
				line = br.readLine();
				if (line == null) {
					break;
				}
				final int firstTab = line.indexOf('\t');
				if (firstTab > 0) {
					// We have data
					final String scanNumStr = line.substring(0, firstTab);
					final int scanNum = Integer.parseInt(scanNumStr);

					lines.put(scanNumStr, line.substring(firstTab + 1));
					updateSpectrumMinMax(scanNum);
				} else {
					// Ignore the line
					ignoredLines++;
				}
			}
			if (ignoredLines > 0) {
				LOGGER.info("Ignored lines when parsing rawDump output file: " + ignoredLines);
			}
		} catch (Exception t) {
			throw new MprcException("Failed to parse rawDump output file", t);
		}
	}

	private void initSpectrumMinMax() {
		firstSpectrum = -1;
		lastSpectrum = 0;
	}

	/**
	 * For given scan number, update the {@link #firstSpectrum} and {@link #lastSpectrum}
	 */
	private void updateSpectrumMinMax(final int scanNum) {
		if (scanNum < firstSpectrum || firstSpectrum == -1) {
			firstSpectrum = scanNum;
		}
		if (scanNum > lastSpectrum) {
			lastSpectrum = scanNum;
		}
	}

	private String[] readHeader(final BufferedReader br) throws IOException {
		final String line = br.readLine();
		if (line == null) {
			throw new MprcException("The rawDump output has no header");
		}
		final String[] tmpHeader = TAB_SPLIT.split(line);
		if (!SCAN_NUM_HEADER.equals(tmpHeader[0])) {
			throw new MprcException("Unknown rawDump output format (first column should be '" + SCAN_NUM_HEADER + "', was '" + tmpHeader[0] + "'.");
		}
		rtColumnIndex = findRtColumnIndex(tmpHeader) - 1; /* We remove first column */

		final String[] parsedHeader = new String[tmpHeader.length - 1];
		System.arraycopy(tmpHeader, 1, parsedHeader, 0, tmpHeader.length - 1);
		return parsedHeader;
	}

	private static int findRtColumnIndex(final String[] tmpHeader) {
		for (int i = 0; i < tmpHeader.length; i++) {
			if ("RT".equalsIgnoreCase(tmpHeader[i])) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public Iterator<String> iterator() {
		return new Iterator<String>() {
			private int currentSpectrum = firstSpectrum - 1;

			@Override
			public boolean hasNext() {
				return currentSpectrum < lastSpectrum;
			}

			@Override
			public String next() {
				currentSpectrum++;
				final String spectrumStr = String.valueOf(currentSpectrum);
				if (lines.containsKey(spectrumStr)) {
					return spectrumStr;
				}
				throw new NoSuchElementException("No spectrum #" + currentSpectrum);
			}

			@Override
			public void remove() {
				throw new MprcException("Cannot remove from this collection");
			}
		};
	}

	public boolean emptyFile() {
		return lines == null;
	}
}