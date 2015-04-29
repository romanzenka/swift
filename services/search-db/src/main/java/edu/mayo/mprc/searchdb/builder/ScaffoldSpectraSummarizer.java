package edu.mayo.mprc.searchdb.builder;

import com.google.common.base.Strings;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.fastadb.ProteinSequenceTranslator;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldSpectraVersion;
import edu.mayo.mprc.utilities.FileUtilities;
import org.joda.time.DateTime;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

/**
 * Summarizes Scaffold spectra report into a collection of objects suitable to be loaded into the database.
 * The objects that should be identical are allocated just once - e.g. two peptides with identical modifications will
 * be stored as a single object. Our goal is to limit the database space needed as these tables can grow very large
 * with many experiments.
 * <p/>
 * We assume the report was generated with Scaffold Batch version 3. We make this true upstream by rerunning Scaffold if the
 * versions do not match.
 *
 * @author Roman Zenka
 */
public class ScaffoldSpectraSummarizer extends ScaffoldReportReader {
	private static final String REPORT_DATE_PREFIX_KEY = "Spectrum report created on ";
	private static final String SCAFFOLD_VERSION_KEY = ScaffoldSpectraVersion.SCAFFOLD_VERSION_KEY;
	private static final String SCAFFOLD_4_VERSION_KEY = ScaffoldSpectraVersion.SCAFFOLD_4_VERSION_KEY;
	private static final String SCAFFOLD_4_VERSION_VALUE = ScaffoldSpectraVersion.SCAFFOLD_4_VERSION_VALUE;
	private static final double HUNDRED_PERCENT = 100.0;

	private AnalysisBuilder analysis;
	// Name of the database from the file header
	private String databaseName;

	// Column indices for different fields.
	private int biologicalSampleName;
	private int biologicalSampleCategory;
	private int msmsSampleName;
	private int proteinAccessionNumbers;
	private int databaseSources;
	private int numberOfTotalSpectra;
	private int numberOfUniqueSpectra;
	private int numberOfUniquePeptides;
	private int percentageOfTotalSpectra;
	private int percentageSequenceCoverage;
	private int proteinIdentificationProbability;

	private String databaseSourcesValue;

	/**
	 * @param translator            Can translate accession number + database name into a protein sequence.
	 * @param massSpecDataExtractor Can obtain metadata about the .RAW files.
	 */
	public ScaffoldSpectraSummarizer(final ProteinSequenceTranslator translator,
	                                 final MassSpecDataExtractor massSpecDataExtractor) {
		analysis = new AnalysisBuilder(translator, massSpecDataExtractor);
	}

	/**
	 * @return Result of the parse process.
	 */
	public AnalysisBuilder getAnalysisBuilder() {
		return analysis;
	}

	@Override
	public boolean processMetadata(final String key, final String value) {
		if (key == null) {
			final int datePos = value.indexOf(REPORT_DATE_PREFIX_KEY);
			if (datePos >= 0) {
				final String date = value.substring(datePos + REPORT_DATE_PREFIX_KEY.length());
				final DateTime reportDate = parseAnalysisDate(date);
				analysis.setAnalysisDate(reportDate);
			}
		} else if (SCAFFOLD_VERSION_KEY.equalsIgnoreCase(key)) {
			analysis.setScaffoldVersion(value);
		} else if (SCAFFOLD_4_VERSION_KEY.equalsIgnoreCase(key) && value != null && value.startsWith(SCAFFOLD_4_VERSION_VALUE)) {
			analysis.setScaffoldVersion(value.substring(SCAFFOLD_4_VERSION_VALUE.length()).trim());
		} else if (DATABASE_NAME_KEY.equalsIgnoreCase(key)) {
			databaseName = extractDatabaseName(value);
		}
		return true;
	}

	/**
	 * Saves positions of all the columns of interest, so we can later access the data for a particular column
	 * just by using its index.
	 *
	 * @param line Scaffold spectra file header, defining all the data columns. The header is tab-separated.
	 */
	@Override
	public boolean processHeader(final String line) {
		final Map<String, Integer> map = buildColumnMap(line);
		initializeCurrentLine(map);

		// Store the column numbers for faster parsing
		biologicalSampleName = getColumn(map, ScaffoldReportReader.BIOLOGICAL_SAMPLE_NAME);
		biologicalSampleCategory = getColumn(map, ScaffoldReportReader.BIOLOGICAL_SAMPLE_CATEGORY);
		msmsSampleName = getColumn(map, ScaffoldReportReader.MS_MS_SAMPLE_NAME);
		proteinAccessionNumbers = getColumn(map, ScaffoldReportReader.PROTEIN_ACCESSION_NUMBERS);
		databaseSources = getColumn(map, ScaffoldReportReader.DATABASE_SOURCES);
		numberOfTotalSpectra = getColumn(map, ScaffoldReportReader.NUMBER_OF_TOTAL_SPECTRA);
		numberOfUniqueSpectra = getColumn(map, ScaffoldReportReader.NUMBER_OF_UNIQUE_SPECTRA);
		numberOfUniquePeptides = getColumn(map, ScaffoldReportReader.NUMBER_OF_UNIQUE_PEPTIDES);
		percentageOfTotalSpectra = getColumn(map, ScaffoldReportReader.PERCENTAGE_OF_TOTAL_SPECTRA);
		percentageSequenceCoverage = getColumn(map, ScaffoldReportReader.PERCENTAGE_SEQUENCE_COVERAGE);
		proteinIdentificationProbability = getColumn(map, ScaffoldReportReader.PROTEIN_ID_PROBABILITY);
		return true;
	}

	@Override
	public boolean processRow(final String line) {
		fillCurrentLine(line);
		final BiologicalSampleBuilder biologicalSample = analysis.getBiologicalSamples().getBiologicalSample(new BiologicalSampleId(currentLine[biologicalSampleName], currentLine[biologicalSampleCategory]));
		final SearchResultBuilder searchResult = biologicalSample.getSearchResults().getTandemMassSpecResult(FileUtilities.stripGzippedExtension(currentLine[msmsSampleName]));
		String databaseSourcesValue = Strings.isNullOrEmpty(currentLine[this.databaseSources]) ? databaseName : currentLine[this.databaseSources];
		if (this.databaseSourcesValue == null) {
			this.databaseSourcesValue = databaseSourcesValue;
			analysis.setDatabaseSources(databaseSourcesValue);
		} else {
			if (!this.databaseSourcesValue.equals(databaseSourcesValue)) {
				throw new MprcException("Only single-database Scaffold experiments are supported, saw two different database names: " + this.databaseSourcesValue + ", " + databaseSourcesValue);
			}
		}
		searchResult.getProteinGroups().getProteinGroup(
				currentLine[proteinAccessionNumbers],
				parseInt(currentLine[numberOfTotalSpectra]),
				parseInt(currentLine[numberOfUniquePeptides]),
				parseInt(currentLine[numberOfUniqueSpectra]),
				parseDouble(currentLine[percentageOfTotalSpectra]) / HUNDRED_PERCENT,
				parseDouble(currentLine[percentageSequenceCoverage]) / HUNDRED_PERCENT,
				parseDouble(currentLine[proteinIdentificationProbability]) / HUNDRED_PERCENT);

		return true;
	}

	/**
	 * Parse a double number. If the number is missing, {@link Double#NaN} is returned
	 *
	 * @param s String representation of the number.
	 * @return The number parsed. If the number is missing. {@link Double#NaN} is returned. Commas separating thousands
	 * are handled as if they were not present. Trailing percent sign is removed if present.
	 */
	static Double parseDouble(final String s) {
		if ("".equals(s)) {
			return Double.NaN;
		}
		try {
			return Double.parseDouble(fixCommaSeparatedThousands(cutPercentSign(s)));
		} catch (NumberFormatException e) {
			throw new MprcException("Cannot parse number [" + s + "] as real number.", e);
		}
	}

	private static String cutPercentSign(final String s) {
		return s.endsWith("%") ? s.substring(0, s.length() - 1) : s;
	}

	/**
	 * Try multiple formats to parse the date.
	 *
	 * @param date Date from Scaffold.
	 * @return Parsed date.
	 */
	private static DateTime parseAnalysisDate(final String date) {
		DateTime parsedDate = tryParse(date, DateFormat.getDateTimeInstance(), null);
		parsedDate = tryParse(date, DateFormat.getDateInstance(), parsedDate);
		parsedDate = tryParse(date, DateFormat.getDateInstance(DateFormat.SHORT, Locale.US), parsedDate);
		return parsedDate;
	}

	private static DateTime tryParse(final String date, final DateFormat format, DateTime parsedDate) {
		if (parsedDate == null) {
			try {
				parsedDate = new DateTime(format.parse(date));
			} catch (ParseException ignore) {
				// SWALLOWED - try another option
			}
		}
		return parsedDate;
	}
}
