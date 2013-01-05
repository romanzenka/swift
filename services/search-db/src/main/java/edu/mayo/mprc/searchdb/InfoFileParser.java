package edu.mayo.mprc.searchdb;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.io.TsvStreamReader;
import edu.mayo.mprc.utilities.FileUtilities;
import org.joda.time.DateTime;

import java.io.File;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Parses data from the .RAW.info.tsv file.
 *
 * @author Roman Zenka
 */
public class InfoFileParser {
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

	/**
	 * Parse .RAW.info file
	 *
	 * @param infoFile File to parse
	 */
	public InfoFileData parse(final File infoFile) {
		return parse(new TsvStreamReader(infoFile));
	}

	/**
	 * Parse .RAW.info file data coming from a given reader.
	 */
	public InfoFileData parse(final Reader reader) {
		return parse(new TsvStreamReader(reader));
	}

	private InfoFileData parse(final TsvStreamReader reader) {
		final InfoFileData data = new InfoFileData();
		try {
			final int[] indices = {0, 1};
			final ArrayList<String> values = new ArrayList<String>(2);
			while (reader.nextValues(indices, values)) {
				final String key = values.size() > 0 ? values.get(0).trim() : "";
				final String value = values.size() > 1 ? values.get(1).trim() : "";
				if ("Creation Date".equals(key)) {
					try {
						data.setStartTime(new DateTime(dateFormat.parse(value)));
					} catch (ParseException e) {
						throw new MprcException("Malformed .RAW file start date: " + value, e);
					}
				} else if ("MS1 Spectra".equals(key)) {
					data.setMs1Spectra(Integer.parseInt(value));
				} else if ("MS2 Spectra".equals(key)) {
					data.setMs2Spectra(Integer.parseInt(value));
				} else if ("MS3+ Spectra".equals(key)) {
					data.setMs3PlusSpectra(Integer.parseInt(value));
				} else if ("Instrument Serial".equals(key)) {
					data.setInstrumentSerialNumber(value);
				} else if ("Instrument Name".equals(key)) {
					data.setInstrumentName(value);
				} else if ("Run Time (seconds)".equals(key)) {
					data.setRunTimeInSeconds(Double.parseDouble(value));
				} else if ("Comment".equals(key)) {
					data.setComment(value);
				} else if ("Sample Id".equals(key)) {
					data.setSampleId(value);
				}
			}
		} finally {
			FileUtilities.closeQuietly(reader);
		}
		return data;
	}
}
