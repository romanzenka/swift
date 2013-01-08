package edu.mayo.mprc.io.mgf;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cleanup process for a single .mgf file.
 * The cleanup ensures that the mgf title is valid (contains scan numbers).
 * If the title is invalid:
 * <ul>
 * <li>if the .mgf was produced from .wiff files, there is a Cycle attribute that can be used as scan number</li>
 * <li>otherwise we produce consecutive numbers and use them as scan numbers</li>
 * </ul>
 * <p/>
 * <b>WARNING:</b> We expect that either all titles are valid or all are invalid. There might be issues for files that have just
 * some spectra invalid.
 */
public final class MgfCleanup {
	private static final Logger LOGGER = Logger.getLogger(MgfCleanup.class);
	private static final int BUFFER_SIZE = 100 * 1024;
	private static final Pattern VALID_TITLE = Pattern.compile("\\([^)]*\\.dta\\s*\\)");
	private static final Pattern CHARGE = Pattern.compile("CHARGE=.*?(\\d+)");
	private static final Pattern SCANS = Pattern.compile("SCANS=.*?(\\d+)");

	private File inputMgf;

	public MgfCleanup(final File inputMgf) {
		this.inputMgf = inputMgf;
	}

	/**
	 * @param output where to put cleaned up mgf
	 * @return true if cleanup was actually needed. If false, the cleaned mgf was not even created.
	 */
	public boolean produceCleanedMgf(final File output) {
		BufferedReader reader = null;
		BufferedWriter writer = null;
		boolean cleanupNeeded = false;
		try {
			reader = new BufferedReader(new FileReader(inputMgf), BUFFER_SIZE);
			cleanupNeeded = cleanupNeeded(reader);
			LOGGER.debug("Cleanup of " + inputMgf.getAbsolutePath() + " is " + (cleanupNeeded ? "needed" : "not needed"));
			if (cleanupNeeded) {
				FileUtilities.closeQuietly(reader);
				reader = new BufferedReader(new FileReader(inputMgf), BUFFER_SIZE);
				LOGGER.debug("Cleaning up " + inputMgf.getAbsolutePath() + " into " + output.getAbsolutePath());
				FileUtilities.ensureFileExists(output);
				writer = new BufferedWriter(new FileWriter(output));
				final String prefix = FileUtilities.stripExtension(inputMgf.getName());
				performCleanup(reader, writer, prefix);
				LOGGER.debug("Cleanup finished");
			}
		} catch (Exception t) {
			throw new MprcException(t);
		} finally {
			FileUtilities.closeQuietly(reader);
			FileUtilities.closeQuietly(writer);
		}
		return cleanupNeeded;
	}

	static boolean cleanupNeeded(final BufferedReader reader) throws IOException {
		boolean insideParamSection = false;
		String title = null;
		// We assume (according to MGF specification), that search parameters must
		// follow BEGIN IONS section. Once the param section is over (queries start),
		// there must be NO parameters until we encounter another BEGIN IONS section.
		while (true) {
			final String line = reader.readLine();
			if (line == null) {
				break;
			}
			if (!insideParamSection) {
				if (line.startsWith("BEGIN IONS")) {
					insideParamSection = true;
					title = null;
				}
			} else { // Inside param section
				if (!line.contains("=")) {
					insideParamSection = false;
					if (title == null) {
						// We found missing title!
						return true;
					}
				} else if (title == null && line.startsWith("TITLE=")) {
					title = line;
					if (!VALID_TITLE.matcher(line).find()) {
						// We found invalid title!
						return true;
					}
				}
			}
		}
		return false;
	}

	static void performCleanup(final BufferedReader reader, final BufferedWriter writer, final String prefix) throws IOException {
		int invalidTitleId = 0;
		boolean insideParamSection = false;
		String title = null;
		final StringBuilder headerBuilder = new StringBuilder(500);
		int charge = 0;
		int scans = 0;
		// We assume (according to MGF specification), that search parameters must
		// follow BEGIN IONS section. Once the param section is over (queries start),
		// there must be NO parameters until we encounter another BEGIN IONS section.
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			if (!insideParamSection) {
				if (line.startsWith("BEGIN IONS")) {
					insideParamSection = true;
					title = null;
					charge = 0;
					scans = -1;
					headerBuilder.setLength(0);
				}
				writer.append(line).append('\n');
			} else { // Inside param section
				// We gobble the params up, will write them out all at once when the entire header is over.
				if (!line.contains("=")) {
					insideParamSection = false;
					// Dump the header we have collected so far
					if (title == null) {
						title = "TITLE=missing title";
					}
					writer.append(title);
					if (!VALID_TITLE.matcher(title).find()) {
						// Title is invalid
						invalidTitleId++;
						writer.append(getTitleDtaIdentification(scans, prefix, charge, invalidTitleId));
					}
					writer.append('\n');
					writer.append(headerBuilder.toString());
					writer.append(line).append('\n');
				} else {
					if (title == null && line.startsWith("TITLE=")) {
						title = line;
						line = null;
					} else if (charge == 0 && line.startsWith("CHARGE=")) {
						charge = parseCharge(line);
					} else if (scans == -1 && line.startsWith("SCANS=")) {
						scans = parseScans(line);
					}
					if (line != null) {
						headerBuilder.append(line).append('\n');
					}
				}
			}
		}
		writer.flush();
	}

	static int parseCharge(final String line) {
		final Matcher matcher = CHARGE.matcher(line);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1).intern());
		}
		return 0;
	}

	static int parseScans(final String line) {
		final Matcher matcher = SCANS.matcher(line);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1).intern());
		}
		return -1;
	}

	static String getTitleDtaIdentification(final int runNumber, final String prefix, final int charge, final int uniqueId) {
		int fixedRunNumber = runNumber;
		if (runNumber == -1) {
			fixedRunNumber = uniqueId;
		} else {
			assert runNumber >= 0 : "The run number has to be non-negative, only -1 is used to represent unknown run number";
		}
		return " (" + prefix + "." + fixedRunNumber + "." + fixedRunNumber + "." + charge + ".dta)";
	}

}
