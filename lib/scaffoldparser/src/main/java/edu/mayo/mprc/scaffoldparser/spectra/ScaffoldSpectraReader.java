package edu.mayo.mprc.scaffoldparser.spectra;

import com.google.common.io.CountingInputStream;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.BufferedEofReader;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.PercentDoneReporter;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;

import java.io.*;
import java.util.regex.Pattern;

/**
 * An abstract class for reading Scaffold's spectrum report. Calls abstract methods that provide the actual functionality.
 * This class is supposed to be used to load a file only once - after the {@link #load} method was called, you should
 * retrieve results and dispose of the class.
 *
 * @author Roman Zenka
 */
public abstract class ScaffoldSpectraReader {
	/**
	 * Default extension - as Scaffold produces it.
	 */
	public static final String EXTENSION = ".spectra.txt";

	/**
	 * Report progress every X lines.
	 */
	public static final int REPORT_FREQUENCY = 10;

	/**
	 * Version of Scaffold that produced the report (Currently "2" for Scaffold 2 or "3" for Scaffold 3).
	 */
	private String scaffoldVersion;

	/**
	 * Name of the Scaffold spectra data source being loaded for exception handling (typically the filename).
	 */
	private String dataSourceName;

	/**
	 * Current line number for exception handling.
	 */
	private int lineNumber;

	/**
	 * Total size of the input in bytes, <0 if not known.
	 */
	private long totalBytesToRead;

	/**
	 * A counting input stream wrapping the provided data source so we can report progress.
	 */
	private CountingInputStream countingInputStream;

	/**
	 * We use this to report percent done.
	 */
	private PercentDoneReporter percentDoneReporter;

	// Scaffold files are terminated with this marker
	private static final String END_OF_FILE = "END OF FILE";

	public static final String EXPERIMENT_NAME = "Experiment name";
	public static final String BIOLOGICAL_SAMPLE_CATEGORY = "Biological sample category";
	public static final String BIOLOGICAL_SAMPLE_NAME = "Biological sample name";
	public static final String MS_MS_SAMPLE_NAME = "MS/MS sample name";
	public static final String PROTEIN_NAME = "Protein name";
	public static final String PROTEIN_ACCESSION_NUMBERS = "Protein accession numbers";
	public static final String DATABASE_SOURCES = "Database sources";
	public static final String PROTEIN_MOLECULAR_WEIGHT_DA = "Protein molecular weight (Da)";
	public static final String PROTEIN_ID_PROBABILITY = "Protein identification probability";
	public static final String NUMBER_OF_UNIQUE_PEPTIDES = "Number of unique peptides";
	public static final String NUMBER_OF_UNIQUE_SPECTRA = "Number of unique spectra";
	public static final String NUMBER_OF_TOTAL_SPECTRA = "Number of total spectra";
	public static final String PERCENTAGE_OF_TOTAL_SPECTRA = "Percentage of total spectra";
	public static final String PERCENTAGE_SEQUENCE_COVERAGE = "Percentage sequence coverage";
	public static final String MANUAL_VALIDATION = "Manual validation";
	public static final String ASSIGNED = "Assigned";
	public static final String SPECTRUM_NAME = "Spectrum name";
	public static final String PEPTIDE_SEQUENCE = "Peptide sequence";
	public static final String PREVIOUS_AMINO_ACID = "Previous amino acid";
	public static final String NEXT_AMINO_ACID = "Next amino acid";
	public static final String PEPTIDE_ID_PROBABILITY = "Peptide identification probability";

	public static final String SEQUEST_XCORR_SCORE = "Sequest XCorr";
	public static final String SEQUEST_DCN_SCORE = "Sequest deltaCn";
	public static final String SEQUEST_SP = "Sequest Sp";
	public static final String SEQUEST_SP_RANK = "Sequest SpRank";
	public static final String SEQUEST_PEPTIDES_MATCHED = "Sequest Peptides Matched";
	public static final String MASCOT_ION_SCORE = "Mascot Ion score";
	public static final String MASCOT_IDENTITY_SCORE = "Mascot Identity score";
	public static final String MASCOT_HOMOLOGY_SCORE = "Mascot Homology Score";
	public static final String MASCOT_DELTA_ION_SCORE = "Mascot Delta Ion Score";
	public static final String X_TANDEM_HYPER_SCORE = "X! Tandem Hyper Score";
	public static final String X_TANDEM_LADDER_SCORE = "X! Tandem Ladder Score";


	public static final String NUMBER_OF_ENZYMATIC_TERMINII = "Number of enzymatic termini";
	public static final String FIXED_MODIFICATIONS = "Fixed modifications identified by spectrum";
	public static final String VARIABLE_MODIFICATIONS = "Variable modifications identified by spectrum";
	public static final String OBSERVED_MZ = "Observed m/z";
	public static final String ACTUAL_PEPTIDE_MASS_AMU = "Actual peptide mass (AMU)";
	public static final String CALCULATED_1H_PEPTIDE_MASS_AMU = "Calculated +1H Peptide Mass (AMU)";
	public static final String SPECTRUM_CHARGE = "Spectrum charge";
	public static final String PEPTIDE_DELTA_AMU = "Actual minus calculated peptide mass (AMU)";
	public static final String PEPTIDE_DELTA_PPM = "Actual minus calculated peptide mass (PPM)";
	public static final String PEPTIDE_START_INDEX = "Peptide start index";
	public static final String PEPTIDE_STOP_INDEX = "Peptide stop index";
	public static final String EXCLUSIVE = "Exclusive";
	public static final String OTHER_PROTEINS = "Other Proteins";
	public static final String STARRED = "Starred";

	/**
	 * How to tell the header of the file.
	 */
	private static final String FIRST_HEADER_COLUMN = EXPERIMENT_NAME;
	private static final Pattern THOUSANDS_REGEX = Pattern.compile(",(\\d\\d\\d)");

	/**
	 * Initializes the reader.
	 */
	protected ScaffoldSpectraReader() {
	}

	/**
	 * Start loading scaffold spectra file.
	 *
	 * @param scaffoldSpectraFile Spectrum file to load.
	 * @param scaffoldVersion     {@link #scaffoldVersion}
	 */
	public void load(final File scaffoldSpectraFile, final String scaffoldVersion, final UserProgressReporter reporter) {
		dataSourceName = scaffoldSpectraFile.getAbsolutePath();
		this.scaffoldVersion = scaffoldVersion;
		try {
			totalBytesToRead = scaffoldSpectraFile.length();
			// The processing method will close the stream
			processStream(new FileInputStream(scaffoldSpectraFile), reporter);
		} catch (Exception t) {
			throw new MprcException("Cannot parse Scaffold spectra file [" + dataSourceName + "], error at line " + lineNumber, t);
		}
	}

	/**
	 * Start loading scaffold spectra file from a given reader.
	 *
	 * @param stream          Stream to load from. Will be closed upon load.
	 * @param inputSize       The total size of the data in the input stream (in bytes).
	 * @param dataSourceName  Information about where the spectra data came from - displayed when throwing exceptions.
	 * @param scaffoldVersion {@link #scaffoldVersion}
	 * @param reporter        To report the progress. Can be null.
	 */
	public void load(final InputStream stream, final long inputSize, final String dataSourceName, final String scaffoldVersion, final ProgressReporter reporter) {
		this.dataSourceName = dataSourceName;
		this.scaffoldVersion = scaffoldVersion;
		totalBytesToRead = inputSize;
		try {
			processStream(stream, reporter);
		} catch (Exception t) {
			throw new MprcException("Cannot parse Scaffold spectra file [" + dataSourceName + "], error at line " + lineNumber, t);
		}
	}

	private void processStream(final InputStream stream, final UserProgressReporter reporter) throws IOException {
		final Reader reader;
		if (totalBytesToRead > 0 && reporter != null) {
			countingInputStream = new CountingInputStream(stream);
			reader = new InputStreamReader(countingInputStream);
			percentDoneReporter = new PercentDoneReporter(reporter, "Parsing Scaffold spectra file: ");
		} else {
			reader = new InputStreamReader(stream);
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(reader);
			// Skip the header portion of the file, process the header line
			String line;
			while (true) {
				line = br.readLine();
				lineNumber++;
				if (line == null) {
					throw new MprcException("End of file reached before we could find the header line");
				}

				final int colonPos = line.indexOf(':');
				if (colonPos >= 0) {
					final String key = line.substring(0, colonPos);
					final String value = line.substring(colonPos + 1);
					if (!processMetadata(key.trim(), value.trim())) {
						break;
					}
				} else {
					if (!processMetadata(null, line.trim())) {
						break;
					}
				}

				if (line.startsWith(FIRST_HEADER_COLUMN + "\t")) {
					break;
				}
			}

			if (!processHeader(line)) {
				return;
			}
			loadContents(br);
		} finally {
			FileUtilities.closeQuietly(br);
		}
	}

	/**
	 * Returns a parsed metadata value from the header of the file.
	 *
	 * @param key   The key (before colon). Null if no colon present.
	 * @param value Value (after colon). Entire line if no colon present.
	 * @return Whether to keep processing. False stops.
	 */
	public abstract boolean processMetadata(String key, String value);

	/**
	 * Process the Scaffold spectra file header.
	 *
	 * @param line Scaffold spectra file header, defining all the data columns. The header is tab-separated.
	 * @return Whether to keep processing. False stops.
	 */
	public abstract boolean processHeader(String line);

	/**
	 * Process one row from the spectra file.
	 *
	 * @param line Scaffold spectra row, tab-separated, format matches the header supplied by {@link #processHeader(String)}.
	 * @return Whether to keep processing. False stops.
	 */
	public abstract boolean processRow(String line);

	private void loadContents(final BufferedReader reader) throws IOException {
		final BufferedEofReader eofReader = new BufferedEofReader(reader);
		while (true) {
			final String line = eofReader.readLine();
			lineNumber++;
			if (END_OF_FILE.equals(line)) {
				break;
			}
			if (line == null || eofReader.isEof()) {
				// We are at the end of file, but have not reach the end of file mark!
				throw new MprcException("End of file reached before finding Scaffold's " + END_OF_FILE + " marker.");
			}
			if (!processRow(line)) {
				break;
			}
			if (lineNumber % REPORT_FREQUENCY == 0 && percentDoneReporter != null) {
				percentDoneReporter.reportProgress((float) ((double) countingInputStream.getCount() / (double) totalBytesToRead));
			}
		}
	}

	/**
	 * Allows a parser to change the reported scaffold version.
	 *
	 * @param scaffoldVersion Detected scaffold version.
	 */
	public void setScaffoldVersion(final String scaffoldVersion) {
		this.scaffoldVersion = scaffoldVersion;
	}

	/**
	 * @return Version of Scaffold used to generate information for these spectra.
	 */
	public String getScaffoldVersion() {
		return scaffoldVersion;
	}

	/**
	 * @param s String with commas denoting thousands.
	 * @return String without the commas.
	 */
	public static String fixCommaSeparatedThousands(final String s) {
		return THOUSANDS_REGEX.matcher(s).replaceAll("$1");
	}
}
