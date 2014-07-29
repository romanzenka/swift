package edu.mayo.mprc.heme;

import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
public class HemeScaffoldReader extends ScaffoldReportReader {
    private static final Logger LOGGER = Logger.getLogger(ScaffoldReportReader.class);

    private int proteinAccessionNumbers;
	private int numberOfTotalSpectra;
//    private static final Pattern DELTA_PATTERN = Pattern.compile(".*#DeltaMass:([^#]+)#.*");
    private static final Pattern MASS_PATTERN = Pattern.compile("(\\d+\\.\\d+)?"); //last double on line
	private HashMap<String, String> databaseCache;

	public HemeScaffoldReader(File cache) {


        this.databaseCache = SerializeFastaDB.load(cache.getAbsolutePath());
        //TODO - add code to check if cache exists? and if not create it? or somewhere else?
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
		numberOfTotalSpectra = getColumn(map, ScaffoldReportReader.NUMBER_OF_TOTAL_SPECTRA);
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
            if(description == null){
                LOGGER.warn(String.format("Database does not contain entry for accession number [%s]", accNum));
                continue;
            }
			final ProteinId id = new ProteinId(accNum, description, getMassIsotopic(description)); //missing desc -> null results in error chain
			ids.add(id);
		}
		final int totalSpectra = parseInt(currentLine[numberOfTotalSpectra]);

		HemeReportEntry entry = entries.get(accnumString);
		if (entry == null) {
            if(!ids.isEmpty()) {
		    	entry = new HemeReportEntry(ids, totalSpectra);
			    entries.put(accnumString, entry);
            }
		} else {
			entry.checkSpectra(totalSpectra);
		}

		return true;
	}

	/**
	 * Get description for a protein of given accession number
	 */
	private String getDescription(final String accNum) {
        return databaseCache.get(accNum);
	}

	private static Double getMassIsotopic(final CharSequence description) {
        final Matcher matcher = MASS_PATTERN.matcher(description);
		if (matcher.matches()) {
			final String isomass = matcher.group(1);
            System.out.println(isomass);
			try {
				return Double.parseDouble(isomass);
			} catch (NumberFormatException e) {
				// SWALLOWED: this is expected
				return null;
			}
		}
		return null;
	}
}
