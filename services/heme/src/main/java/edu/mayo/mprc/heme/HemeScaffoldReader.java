package edu.mayo.mprc.heme;

import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
public class HemeScaffoldReader extends ScaffoldReportReader {
	private int proteinAccessionNumbers;
	private int proteinName;
	private int numberOfTotalSpectra;
	private int numberOfUniquePeptides;
	private static final Pattern DELTA_PATTERN = Pattern.compile(".*#DeltaMass:([^#]+)#.*");

	private Map<String, HemeReportEntry> entries = new HashMap<String, HemeReportEntry>(100);

	public Collection<HemeReportEntry> getEntries() {
		return entries.values();
	}

	public boolean processMetadata(final String key, final String value) {
		return true;
	}

	@Override
	public boolean processHeader(final String line) {
		final HashMap<String, Integer> map = buildColumnMap(line);
		initializeCurrentLine(map);

		// Store the column numbers for faster parsing
		proteinAccessionNumbers = getColumn(map, ScaffoldReportReader.PROTEIN_ACCESSION_NUMBERS);
		proteinName = getColumn(map, ScaffoldReportReader.PROTEIN_NAME);
		numberOfTotalSpectra = getColumn(map, ScaffoldReportReader.NUMBER_OF_TOTAL_SPECTRA);
		numberOfUniquePeptides = getColumn(map, ScaffoldReportReader.NUMBER_OF_UNIQUE_PEPTIDES);
		return true;
	}

	@Override
	public boolean processRow(final String line) {
		fillCurrentLine(line);
		final String accNums = currentLine[proteinAccessionNumbers];
		final String accNum = PROTEIN_ACCESSION_SPLITTER.split(accNums).iterator().next();
		final String desc = currentLine[proteinName];
		final int totalSpectra = parseInt(currentLine[numberOfTotalSpectra]);
		final int uniquePeptides = parseInt(currentLine[numberOfUniquePeptides]);
		final Double massDelta = getMassDelta(desc);

		if (massDelta == null) {
			return true;
		}

		HemeReportEntry entry = entries.get(accNum);
		if (entry == null) {
			entry = new HemeReportEntry(accNum, desc, totalSpectra, massDelta);
			entries.put(accNum, entry);
		} else {
			entry.addSpectra(totalSpectra);
		}

		return true;
	}

	private static Double getMassDelta(final CharSequence description) {
		final Matcher matcher = DELTA_PATTERN.matcher(description);
		if (matcher.matches()) {
			final String delta = matcher.group(1);
			try {
				return Double.parseDouble(delta);
			} catch (NumberFormatException e) {
				// SWALLOWED: this is expected
				return null;
			}
		}
		return null;
	}
}
