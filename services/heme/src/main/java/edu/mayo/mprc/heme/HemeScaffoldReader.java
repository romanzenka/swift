package edu.mayo.mprc.heme;

import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;

import java.util.*;
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
	private FastaDbDao fastaDao;
	private Curation database;

	public HemeScaffoldReader(FastaDbDao fastaDao, Curation database) {
		this.fastaDao = fastaDao;
		this.database = database;
	}

	private Map<String, HemeReportEntry> entries = new HashMap<String, HemeReportEntry>(100);

	public Collection<HemeReportEntry> getEntries() {
		return entries.values();
	}

	public boolean processMetadata(final String key, final String value) {
		return true;
	}

	@Override
	public boolean processHeader(final String line) {
		final Map<String, Integer> map = buildColumnMap(line);
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
		final List<ProteinId> ids = new ArrayList<ProteinId>();
		String accnumString = currentLine[proteinAccessionNumbers];
		final Iterable<String> accNums = PROTEIN_ACCESSION_SPLITTER.split(accnumString);
		for (final String accNum : accNums) {
			final String description = getDescription(accNum);
			final ProteinId id = new ProteinId(accNum, description, getMassDelta(description));
			ids.add(id);
		}
		final int totalSpectra = parseInt(currentLine[numberOfTotalSpectra]);
		final int uniquePeptides = parseInt(currentLine[numberOfUniquePeptides]);

		HemeReportEntry entry = entries.get(accnumString);
		if (entry == null) {
			entry = new HemeReportEntry(ids, totalSpectra);
			entries.put(accnumString, entry);
		} else {
			entry.checkSpectra(totalSpectra);
		}

		return true;
	}

	/**
	 * Get description for a protein of given accession number
	 */
	private String getDescription(final String accNum) {
		return fastaDao.getProteinDescription(database, accNum);
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
