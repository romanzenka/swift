package edu.mayo.mprc.searchdb;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.chem.AminoAcidSet;
import edu.mayo.mprc.fastadb.ProteinSequenceTranslator;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldSpectraReader;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldSpectraVersion;
import edu.mayo.mprc.searchdb.builder.*;
import edu.mayo.mprc.searchdb.dao.Analysis;
import edu.mayo.mprc.unimod.IndexedModSet;
import edu.mayo.mprc.utilities.FileUtilities;
import org.joda.time.DateTime;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class ScaffoldSpectraSummarizer extends ScaffoldSpectraReader {
	private static final String REPORT_DATE_PREFIX_KEY = "Spectrum report created on ";
	private static final String SCAFFOLD_VERSION_KEY = ScaffoldSpectraVersion.SCAFFOLD_VERSION_KEY;
	private static final String SCAFFOLD_4_VERSION_KEY = ScaffoldSpectraVersion.SCAFFOLD_4_VERSION_KEY;
	private static final String SCAFFOLD_4_VERSION_VALUE = ScaffoldSpectraVersion.SCAFFOLD_4_VERSION_VALUE;
	private static final String DATABASE_NAME_KEY = "Database Name";
	private static final Splitter SPLITTER = Splitter.on('\t').trimResults();
	private static final double HUNDRED_PERCENT = 100.0;
	private static final AminoAcidSet SUPPORTED_AMINO_ACIDS = AminoAcidSet.DEFAULT;
	private static final Pattern DATABASE_REGEX = Pattern.compile("the (.*) database");

	/**
	 * To parse Scaffold-reported mods.
	 */
	private ScaffoldModificationFormat format;
	private AnalysisBuilder analysis;
	// Name of the database from the file header
	private String databaseName;

	// Current line parsed into columns, with data in fields trimmed
	private String[] currentLine;

	// Column indices for different fields.
	private int biologicalSampleName;
	private int biologicalSampleCategory;
	private int numColumns;
	private int msmsSampleName;
	private int proteinAccessionNumbers;
	private int databaseSources;
	private int numberOfTotalSpectra;
	private int numberOfUniqueSpectra;
	private int numberOfUniquePeptides;
	private int percentageOfTotalSpectra;
	private int percentageSequenceCoverage;
	private int proteinIdentificationProbability;
	private int peptideSequence;
	private int previousAminoAcid;
	private int nextAminoAcid;
	private int numberOfEnzymaticTerminii;
	private int fixedModifications;
	private int variableModifications;
	private int spectrumName;
	private int spectrumCharge;
	private int peptideIdentificationProbability;

	/**
	 * @param modSet                List of modifications as stored in our database.
	 * @param scaffoldModSet        List of modifications as configured within Scaffold.
	 * @param translator            Can translate accession number + database name into a protein sequence.
	 * @param massSpecDataExtractor Can obtain metadata about the .RAW files.
	 */
	public ScaffoldSpectraSummarizer(final IndexedModSet modSet, final IndexedModSet scaffoldModSet, final ProteinSequenceTranslator translator,
	                                 final MassSpecDataExtractor massSpecDataExtractor) {
		format = new ScaffoldModificationFormat(modSet, scaffoldModSet);
		analysis = new AnalysisBuilder(format, translator, massSpecDataExtractor);
	}

	/**
	 * @return Result of the parse process.
	 */
	public Analysis getAnalysis() {
		return analysis.build();
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
	 * Extract database name from Scaffold export. The database is specified as "the WHATEVER database"
	 *
	 * @param value String to extract the name from.
	 * @return Extracted database name.
	 */
	static String extractDatabaseName(final String value) {
		final Matcher matcher = DATABASE_REGEX.matcher(value);
		if (matcher.matches()) {
			return addFastaSuffix(matcher.group(1));
		}
		return addFastaSuffix(value);
	}

	/**
	 * Add a .fasta suffix to a string if it does not have it already.
	 *
	 * @param value Value to add suffix to.
	 * @return Value with suffix added.
	 */
	static String addFastaSuffix(final String value) {
		if (value.endsWith(".fasta")) {
			return value;
		}
		return value + ".fasta";
	}

	/**
	 * Saves positions of all the columns of interest, so we can later access the data for a particular column
	 * just by using its index.
	 *
	 * @param line Scaffold spectra file header, defining all the data columns. The header is tab-separated.
	 */
	@Override
	public boolean processHeader(final String line) {
		final HashMap<String, Integer> map = buildColumnMap(line);
		numColumns = map.size();
		currentLine = new String[numColumns];

		// Store the column numbers for faster parsing
		biologicalSampleName = getColumn(map, ScaffoldSpectraReader.BIOLOGICAL_SAMPLE_NAME);
		biologicalSampleCategory = getColumn(map, ScaffoldSpectraReader.BIOLOGICAL_SAMPLE_CATEGORY);
		msmsSampleName = getColumn(map, ScaffoldSpectraReader.MS_MS_SAMPLE_NAME);
		proteinAccessionNumbers = getColumn(map, ScaffoldSpectraReader.PROTEIN_ACCESSION_NUMBERS);
		databaseSources = getColumn(map, ScaffoldSpectraReader.DATABASE_SOURCES);
		numberOfTotalSpectra = getColumn(map, ScaffoldSpectraReader.NUMBER_OF_TOTAL_SPECTRA);
		numberOfUniqueSpectra = getColumn(map, ScaffoldSpectraReader.NUMBER_OF_UNIQUE_SPECTRA);
		numberOfUniquePeptides = getColumn(map, ScaffoldSpectraReader.NUMBER_OF_UNIQUE_PEPTIDES);
		percentageOfTotalSpectra = getColumn(map, ScaffoldSpectraReader.PERCENTAGE_OF_TOTAL_SPECTRA);
		percentageSequenceCoverage = getColumn(map, ScaffoldSpectraReader.PERCENTAGE_SEQUENCE_COVERAGE);
		proteinIdentificationProbability = getColumn(map, ScaffoldSpectraReader.PROTEIN_ID_PROBABILITY);
		peptideSequence = getColumn(map, ScaffoldSpectraReader.PEPTIDE_SEQUENCE);
		previousAminoAcid = getColumn(map, ScaffoldSpectraReader.PREVIOUS_AMINO_ACID);
		nextAminoAcid = getColumn(map, ScaffoldSpectraReader.NEXT_AMINO_ACID);
		numberOfEnzymaticTerminii = getColumn(map, ScaffoldSpectraReader.NUMBER_OF_ENZYMATIC_TERMINII);
		fixedModifications = getColumn(map, ScaffoldSpectraReader.FIXED_MODIFICATIONS);
		variableModifications = getColumn(map, ScaffoldSpectraReader.VARIABLE_MODIFICATIONS);
		spectrumName = getColumn(map, ScaffoldSpectraReader.SPECTRUM_NAME);
		spectrumCharge = getColumn(map, ScaffoldSpectraReader.SPECTRUM_CHARGE);
		peptideIdentificationProbability = getColumn(map, ScaffoldSpectraReader.PEPTIDE_ID_PROBABILITY);
		return true;
	}

	/**
	 * @param columnPositions Column positions.
	 * @param columnName      Name of the column to find.
	 * @return Index of the column. If a matching column not found, throws an exception.
	 */
	private int getColumn(final HashMap<String, Integer> columnPositions, final String columnName) {
		final Integer column = getColumnNumber(columnPositions, columnName);
		if (null == column) {
			throw new MprcException("Missing column [" + columnName + "]");
		} else {
			return column;
		}
	}

	/**
	 * @return -1 if the column name is not found, column number otherwise.
	 */
	private int getColumnOptional(final HashMap<String, Integer> columnPositions, final String columnName) {
		final Integer column = getColumnNumber(columnPositions, columnName);
		return column == null ? -1 : column;
	}

	private Integer getColumnNumber(final HashMap<String, Integer> columnPositions, final String columnName) {
		return columnPositions.get(columnName.toUpperCase(Locale.US));
	}

	@Override
	public boolean processRow(final String line) {
		fillCurrentLine(line);
		final BiologicalSampleBuilder biologicalSample = analysis.getBiologicalSamples().getBiologicalSample(currentLine[biologicalSampleName], currentLine[biologicalSampleCategory]);
		final SearchResultBuilder searchResult = biologicalSample.getSearchResults().getTandemMassSpecResult(FileUtilities.stripGzippedExtension(currentLine[msmsSampleName]));
		final ProteinGroupBuilder proteinGroup = searchResult.getProteinGroups().getProteinGroup(
				currentLine[proteinAccessionNumbers],
				Strings.isNullOrEmpty(currentLine[databaseSources]) ? databaseName : currentLine[databaseSources],

				parseInt(currentLine[numberOfTotalSpectra]),
				parseInt(currentLine[numberOfUniquePeptides]),
				parseInt(currentLine[numberOfUniqueSpectra]),
				parseDouble(currentLine[percentageOfTotalSpectra]) / HUNDRED_PERCENT,
				parseDouble(currentLine[percentageSequenceCoverage]) / HUNDRED_PERCENT,
				parseDouble(currentLine[proteinIdentificationProbability]) / HUNDRED_PERCENT);


		final PeptideSpectrumMatchBuilder peptideSpectrumMatch = proteinGroup.getPeptideSpectrumMatches().getPeptideSpectrumMatch(
				currentLine[peptideSequence],
				currentLine[fixedModifications],
				currentLine[variableModifications],

				parseAminoAcid(currentLine[previousAminoAcid]),
				parseAminoAcid(currentLine[nextAminoAcid]),
				parseInt(currentLine[numberOfEnzymaticTerminii]));

		peptideSpectrumMatch.recordSpectrum(
				currentLine[spectrumName],
				parseInt(currentLine[spectrumCharge]),
				parseDouble(currentLine[peptideIdentificationProbability]) / HUNDRED_PERCENT
		);
		return true;
	}

	/**
	 * Check that the string corresponds to a single known amino acid.
	 *
	 * @param s String to check.
	 * @return One-char amino acid code.
	 */
	private char parseAminoAcid(final String s) {
		if (s == null || s.length() != 1) {
			throw new MprcException("Wrong single-letter format for an amino acid: [" + s + "]");
		}
		final char aminoAcid = s.charAt(0);
		if (aminoAcid != '-' && SUPPORTED_AMINO_ACIDS.getForSingleLetterCode(s) == null) {
			throw new MprcException("Unsupported amino acid code [" + aminoAcid + "]");
		}
		return Character.toUpperCase(aminoAcid);
	}

	private int parseInt(final String s) {
		try {
			return Integer.parseInt(fixCommaSeparatedThousands(s));
		} catch (NumberFormatException e) {
			throw new MprcException("Cannot parse number [" + s + "] as integer.", e);
		}
	}

	/**
	 * Parse a double number. If the number is missing, {@link Double#NaN} is returned
	 *
	 * @param s String representation of the number.
	 * @return The number parsed. If the number is missing. {@link Double#NaN} is returned. Commas separating thousands
	 *         are handled as if they were not present. Trailing percent sign is removed if present.
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

	private void fillCurrentLine(final String line) {
		final Iterator<String> iterator = SPLITTER.split(line).iterator();
		for (int i = 0; i < currentLine.length; i++) {
			if (iterator.hasNext()) {
				currentLine[i] = iterator.next();
			} else {
				currentLine[i] = "";
			}
		}
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

	private static HashMap<String, Integer> buildColumnMap(final String line) {
		final HashMap<String, Integer> columnPositions = new HashMap<String, Integer>(30);
		int position = 0;
		for (final String column : SPLITTER.split(line)) {
			columnPositions.put(column.toUpperCase(Locale.US), position);
			position++;
		}
		return columnPositions;
	}

}
