package edu.mayo.mprc.quameterdb;

import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;

import java.util.Map;

/**
 * @author Roman Zenka
 */
final class SemitrypticRatioScaffoldReader extends ScaffoldReportReader {
	private int semiTrypticSpectra;
	private int totalSpectra;
	private int numberOfEnzymaticTerminiiColumn;
	private int msmsSampleNameColumn;
	private final String msmsSampleName;

	SemitrypticRatioScaffoldReader(final String msmsSampleName) {
		this.msmsSampleName = msmsSampleName;
	}

	@Override
	public boolean processMetadata(final String key, final String value) {
		return true;
	}

	@Override
	public boolean processHeader(final String line) {
		final Map<String, Integer> map = buildColumnMap(line);
		initializeCurrentLine(map);

		// Store the column numbers for faster parsing
		msmsSampleNameColumn = getColumn(map, ScaffoldReportReader.MS_MS_SAMPLE_NAME);
		numberOfEnzymaticTerminiiColumn = getColumn(map, ScaffoldReportReader.NUMBER_OF_ENZYMATIC_TERMINII);
		return true;
	}

	@Override
	public boolean processRow(final String line) {
		fillCurrentLine(line);

		if (msmsSampleName.equals(currentLine[msmsSampleNameColumn])) {
			totalSpectra++;
			if ("1".equals(currentLine[numberOfEnzymaticTerminiiColumn])) {
				semiTrypticSpectra++;
			}
		}

		return true;
	}

	public double getSemiTrypticRatio() {
		return (totalSpectra > 0) ? ((double) semiTrypticSpectra / (double) totalSpectra) : 0.0;
	}
}
