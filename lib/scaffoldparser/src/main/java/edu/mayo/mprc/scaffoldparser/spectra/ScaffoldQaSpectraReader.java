package edu.mayo.mprc.scaffoldparser.spectra;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.io.KeyedTsvReader;
import edu.mayo.mprc.utilities.StringUtilities;

import java.util.*;

/**
 * A parser for Scaffold spectrum report - tab separated file with a line for each spectrum.
 * Loads all the data into memory so the access can be sped up. This is to be used by the QA module.
 *
 * @author Roman Zenka
 */
public final class ScaffoldQaSpectraReader extends ScaffoldReportReader implements Iterable<String>, KeyedTsvReader {

	private Map<String/*spectrumName*/, String/*entire line about spectrum except spectrumName*/> mapSpectrumNameToScaffoldSpectraInfo = new HashMap<String, String>();
	private String[] header;
	private String emptyLine;
	private int spectrumNameColumn;
	private final StringBuilder sb;

	public ScaffoldQaSpectraReader() {
		sb = new StringBuilder(1000);
	}

	@Override
	public boolean processMetadata(final String key, final String value) {
		// We do not care about metadata. Keep going.
		return true;
	}

	/**
	 * Fills in the spectrum column that is to be skipped.
	 */
	@Override
	public boolean processHeader(final String line) {
		// Extract everything except the spectrum column name
		final String[] tempHeader = line.split("\t");
		// Scaffold 2.06.01 has a bug - one column is added extra before the last starred/hidden. We give this column an explicit name "Blank Column"
		List<String> headerList = new ArrayList<String>(tempHeader.length);
		emptyLine = null;
		spectrumNameColumn = 0;
		int columnOffset = 0;
		for (int i = 0; i < tempHeader.length; i++) {
			if (SPECTRUM_NAME.equals(tempHeader[i])) {
				spectrumNameColumn = i;
				columnOffset++; // We are skipping this column
				continue;
			}
			if (STARRED.equals(tempHeader[i])) {
				headerList.add("Blank Column");
				columnOffset--; // We are inserting a "Blank Column"
			}
			headerList.add(tempHeader[i]);
		}
		if (spectrumNameColumn == 0) {
			throw new MprcException("Wrong Scaffold spectra file format - header column missing [" + SPECTRUM_NAME + "].");
		}
		header = new String[headerList.size()];
		header = headerList.toArray(header);
		return true;
	}

	@Override
	public boolean processRow(final String line) {
		sb.setLength(0);
		int columnNumber = 0;
		int spectrumNameStart = 0;
		String spectrumName = null;
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '\t') {
				columnNumber++;
				if (columnNumber == spectrumNameColumn) {
					// This is the column we are skipping
					// Append everything till now to the output
					sb.append(line, 0, i);
					spectrumNameStart = i + 1;
				} else if (columnNumber == spectrumNameColumn + 1) {
					// We are past the column to skip
					// Spectrum name is in between
					spectrumName = line.substring(spectrumNameStart, i);

					// Append everything from here to the end of the string (including the tab)
					sb.append(line, i, line.length());
					break;
				}
			}
		}
		mapSpectrumNameToScaffoldSpectraInfo.put(spectrumName, fixCommaSeparatedThousands(sb.toString()));
		return true;
	}

	/**
	 * @return Tab-separated header line for all the data provided. The header does not include spectrumName.
	 */
	@Override
	public String getHeaderLine() {
		return Joiner.on("\t").join(header);
	}

	/**
	 * @return A sequence of tabs that matches the length of the header-1. Used to output missing information.
	 */
	@Override
	public String getEmptyLine() {
		if (emptyLine == null) {
			emptyLine = StringUtilities.repeat('\t', header.length - 1);
		}
		return emptyLine;
	}

	/**
	 * @param key Name of the .dta file. This corresponds to Scaffolds 'spectrum' attribute in the PepXML export.
	 * @return Information for given spectrum, tab-separated. The <code>spectrumName</code> itself is not included.
	 */
	@Override
	public String getLineForKey(final String key) {
		return mapSpectrumNameToScaffoldSpectraInfo.get(key);
	}

	@Override
	public Iterator<String> iterator() {
		return mapSpectrumNameToScaffoldSpectraInfo.keySet().iterator();
	}
}
