package edu.mayo.mprc.swift.report;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.QueryCallback;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.utilities.CsvWriter;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.Tuple;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.*;

/**
 * Provides data for {@link SampleReport}
 *
 * @author Roman Zenka
 */
public final class SampleReportData {
	private static final int TYPICAL_SAMPLE_HEADERS = 30;
	private static final List<String> FIXED_HEADER = Arrays.asList(
			"File", "Last Modified",
			"Ms1 Spectra", "Ms2 Spectra", "Ms3+ Spectra", "Instrument Name",
			"Instrument Serial #", "Start Time", "Run Time (seconds)", "Comment", "Error Log");
	public static final int DYNAMIC_HEADER_OFFSET = FIXED_HEADER.size();

	/**
	 * Write the CSV data to a provided writer.
	 *
	 * @param writer Writer to write data to.
	 */
	static void writeCsv(Writer writer, SearchDbDao dao) {
		CsvWriter csvWriter = new CsvWriter(writer);
		final SampleHeaderCollector headerCollector = new SampleHeaderCollector();

		dao.getTandemMassSpectrometrySamples(headerCollector);

		final Collection<String> combinedHeaders = headerCollector.getHeaders();
		List<String> headerList = new ArrayList<String>(DYNAMIC_HEADER_OFFSET + combinedHeaders.size());
		headerList.addAll(FIXED_HEADER);
		headerList.addAll(combinedHeaders);
		final int headersLength = headerList.size();
		String[] headers = new String[headersLength];
		headerList.toArray(headers);
		csvWriter.writeNext(headers);

		dao.getTandemMassSpectrometrySamples(new SamplePrinter(headers, csvWriter));
	}

	/**
	 * Provide a combined list of all headers present in a set of samples.
	 */
	static final class SampleHeaderCollector implements QueryCallback {
		private final LinkedHashSet<String> headers = new LinkedHashSet<String>(100);

		@Override
		public void process(Object[] data) {
			if (data[0] instanceof TandemMassSpectrometrySample) {
				TandemMassSpectrometrySample sample = (TandemMassSpectrometrySample) data[0];
				headers.addAll(parseSampleHeaders(sample.getSampleInformation()));
			} else {
				ExceptionUtilities.throwCastException(data[0], String.class);
				return;
			}
		}

		public LinkedHashSet<String> getHeaders() {
			return headers;
		}
	}

	/**
	 * Prints a given sample to provided writer.
	 */
	static final class SamplePrinter implements QueryCallback {
		public static final int MAX_ERROR_LOG_LENGTH = 1024;
		private final String[] headers;
		private final CsvWriter writer;
		private final String[] row;
		private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss");

		SamplePrinter(String[] headers, CsvWriter writer) {
			this.headers = headers;
			this.writer = writer;
			row = new String[headers.length];
		}

		@Override
		public void process(Object[] data) {
			if (data[0] instanceof TandemMassSpectrometrySample) {
				TandemMassSpectrometrySample sample = (TandemMassSpectrometrySample) data[0];
				// Loop over all the data and write values ordered according to the header
				final Map<String, String> values = parseSampleValues(sample.getSampleInformation());

				// Copy fixed data
				row[0] = sample.getFile() == null ? null : sample.getFile().getAbsolutePath();
				row[1] = sample.getLastModified() == null ? null : DATE_FORMAT.print(sample.getLastModified());
				row[2] = String.valueOf(sample.getMs1Spectra());
				row[3] = String.valueOf(sample.getMs2Spectra());
				row[4] = String.valueOf(sample.getMs3PlusSpectra());
				row[5] = sample.getInstrumentName();
				row[6] = sample.getInstrumentSerialNumber();
				row[7] = DATE_FORMAT.print(sample.getStartTime());
				row[8] = String.valueOf(sample.getRunTimeInSeconds());
				row[9] = sample.getComment();
				row[10] = sample.getErrorLog();
				if (row[10].length() > MAX_ERROR_LOG_LENGTH) {
					row[10] = row[10].substring(0, MAX_ERROR_LOG_LENGTH - 3) + "...";
				}

				for (int i = DYNAMIC_HEADER_OFFSET; i < row.length; i++) {
					final String header = headers[i];
					row[i] = values.get(header);
				}
				writer.writeNext(row);
			} else {
				ExceptionUtilities.throwCastException(data, TandemMassSpectrometrySample.class);
				return;
			}
		}
	}

	/**
	 * Take a sampleInfo record and parse the headers out.
	 *
	 * @param sampleInfo Information about the sample.
	 * @return A list of headers defined in the sample info field.
	 */
	static List<String> parseSampleHeaders(final String sampleInfo) {
		if (sampleInfo == null) {
			return Collections.emptyList();
		}
		final List<String> headers = new ArrayList<String>(TYPICAL_SAMPLE_HEADERS);
		final BufferedReader reader = new BufferedReader(new StringReader(sampleInfo));
		try {
			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				}
				final Tuple<String, String> entry = getEntry(line);
				final String header = getHeaderLabel(entry);
				if (header != null) {
					headers.add(header);
				}
			}
		} catch (IOException e) {
			throw new MprcException("Failed to parse sample headers", e);
		} finally {
			FileUtilities.closeQuietly(reader);
		}
		return headers;
	}

	private static String getHeaderLabel(Tuple<String, String> entry) {
		if (entry == null) {
			return null;
		}
		String header;
		if (isUserLabel(entry)) {
			header = "User " + entry.getSecond();
		} else if (isUserText(entry)) {
			return null;
		} else {
			header = entry.getFirst();
		}
		return header;
	}

	private static boolean isUserLabel(Tuple<String, String> entry) {
		return entry.getFirst().startsWith("User Label");
	}

	private static boolean isUserText(Tuple<String, String> entry) {
		return entry.getFirst().startsWith("User Text");
	}

	static Map<String, String> parseSampleValues(final String sampleInfo) {
		if (sampleInfo == null) {
			return Collections.emptyMap();
		}
		final Map<String, String> values = new HashMap<String, String>(TYPICAL_SAMPLE_HEADERS);

		// For user labels, remember the previous user header so it can be associated with the user value
		String nextUserHeader = null;
		final BufferedReader reader = new BufferedReader(new StringReader(sampleInfo));
		try {
			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				}
				final Tuple<String, String> entry = getEntry(line);
				if (entry != null) {
					if (isUserLabel(entry)) {
						nextUserHeader = getHeaderLabel(entry);
					} else {
						if (nextUserHeader != null) {
							values.put(nextUserHeader, entry.getSecond());
							nextUserHeader = null;
						} else {
							values.put(entry.getFirst(), entry.getSecond());
						}
					}
				}
			}
		} catch (IOException e) {
			throw new MprcException("Failed to parse sample headers", e);
		} finally {
			FileUtilities.closeQuietly(reader);
		}
		return values;
	}

	/**
	 * @param line Line from sample info
	 * @return The header (if present) for the line
	 */
	private static String getHeader(final String line) {
		final int semicolonPos = line.indexOf(':');
		if (semicolonPos >= 0) {
			return line.substring(0, semicolonPos).trim();
		} else {
			return null;
		}
	}

	/**
	 * @param line Line from sample info.
	 * @return The key-value entry the line represents
	 */
	private static Tuple<String, String> getEntry(final String line) {
		final int semicolonPos = line.indexOf(':');
		if (semicolonPos >= 0) {
			return new Tuple<String, String>(line.substring(0, semicolonPos).trim(), line.substring(semicolonPos + 1).trim());
		} else {
			return null;
		}
	}
}
