package edu.mayo.mprc.qa;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.io.KeyedTsvReader;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.StringUtilities;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Loads data from the UV controller. Organizes them by retention time, so for each spectrum we can quickly look
 * up the closest RT from the controller. This is important because the UV controller uses different timing intervals
 * and the R code (and the user) benefit from having a single, collated, output document
 *
 * @author Roman Zenka
 */
public final class UvDataReader implements KeyedTsvReader {
	private static final Logger LOGGER = Logger.getLogger(UvDataReader.class);

	private TreeMap<Double/* Retention time */, String/*The full line without id and RT*/> lines;
	private String[] header;
	private static final String ID_HEADER = "id";
	private static final String RT_HEADER = "rt";
	private static final String[] DEFAULT_HEADER = new String[]{
			"UV.RT", // Retention time as reported by the UV module
			"PumpModule.LoadingPump.Flow",
			"PumpModule.NC_Pump.%B",
			"PumpModule.LoadingPump.Pressure",
			"PumpModule.NC_Pump.Flow",
			"PumpModule.NC_Pump.Pressure",
			"ColumnOven.ValveRight",
			"ColumnOven.Temperature",
			"ColumnOven.ColumnOven_Temp.Signal",
			"Sampler.Status",
			"Sampler.Position",
			"Sampler.Volume"
	};
	private static final String EMPTY_LINE;
	private static final Pattern TAB_SPLIT = Pattern.compile("\t");

	static {
		// Empty line has a tab for each header item (minus one)
		EMPTY_LINE = StringUtilities.repeat('\t', DEFAULT_HEADER.length - 1);
	}

	/**
	 * Prepare the reader. In this implementation, the entire file is loaded at once and cached in memory (we
	 * expect the file to have around 10000 lines).
	 *
	 * @param uvDataFile rawDump file to process
	 */
	public UvDataReader(final File uvDataFile) {
		if (uvDataFile == null) {
			// Null files are honored - they will act as if there was no input information
			// Use default header (otherwise we use header obtained from the file).
			header = DEFAULT_HEADER;
		} else {
			lines = new TreeMap<Double, String>();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(uvDataFile));
				parse(reader, lines);
			} catch (Exception t) {
				throw new MprcException("Cannot parse uv data file [" + uvDataFile.getAbsolutePath() + "]", t);
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
	 * @param key Retention time to get data for, in string format.
	 * @return Entire line for the closest earlier entry for given retention time.
	 */
	@Override
	public String getLineForKey(final String key) {
		if (lines == null) {
			return EMPTY_LINE;
		}
		Map.Entry<Double, String> entry = lines.floorEntry(Double.parseDouble(key));
		if(entry==null) {
			// Try ceiling if nothing below
			entry = lines.ceilingEntry(Double.parseDouble(key));
		}
		final String line = entry.getValue();
		if (line == null) {
			return EMPTY_LINE;
		}
		return line;
	}

	private void parse(final BufferedReader br, final Map<Double, String> lines) {
		try {
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
					final int secondTab = line.indexOf('\t', firstTab + 1);
					// We have data
					final String scanNumStr = line.substring(firstTab + 1, secondTab);
					final double retentionTime = Double.parseDouble(scanNumStr);

					lines.put(retentionTime, line.substring(firstTab + 1));
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

	private static String[] readHeader(final BufferedReader br) throws IOException {
		final String line = br.readLine();
		if (line == null) {
			throw new MprcException("The rawDump output has no header");
		}
		final String[] tmpHeader = TAB_SPLIT.split(line);
		if (tmpHeader.length < 2) {
			throw new MprcException(String.format("Unknown rawDump output format - we expect at least %d columns, got %d", DEFAULT_HEADER.length + 2, tmpHeader.length));
		}
		if (!ID_HEADER.equals(tmpHeader[0])) {
			throw new MprcException(String.format("Unknown rawDump output format (first column should be '%s', was '%s'.", ID_HEADER, tmpHeader[0]));
		}
		if (!RT_HEADER.equals(tmpHeader[1])) {
			throw new MprcException(String.format("Unknown rawDump output format (first column should be '%s', was '%s'.", RT_HEADER, tmpHeader[1]));
		}
		final String[] parsedHeader = new String[tmpHeader.length - 1];
		System.arraycopy(tmpHeader, 2, parsedHeader, 1, tmpHeader.length - 2);
		parsedHeader[0] = "UV.RT";
		return parsedHeader;
	}
}
