package edu.mayo.mprc.io.mgf;

import com.google.common.base.Splitter;
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
public final class MsconvertMgfCleanup {
	private static final Logger LOGGER = Logger.getLogger(MsconvertMgfCleanup.class);
	private static final int BUFFER_SIZE = 100 * 1024;
	private static final Pattern CHARGE = Pattern.compile("CHARGE=\\s*(.*?)\\s*$");
	private static final Pattern SUB_CHARGE = Pattern.compile("(\\d+)");

	private File inputMgf;

	public MsconvertMgfCleanup(final File inputMgf) {
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
			LOGGER.debug("Cleaning up after msconvert " + inputMgf.getAbsolutePath() + " into " + output.getAbsolutePath());
			FileUtilities.ensureFileExists(output);
			writer = new BufferedWriter(new FileWriter(output));
			final String prefix = FileUtilities.stripExtension(inputMgf.getName());
			performCleanup(reader, writer, prefix);
			LOGGER.debug("Cleaning up after msconvert finished");
		} catch (Exception t) {
			throw new MprcException(t);
		} finally {
			FileUtilities.closeQuietly(reader);
			FileUtilities.closeQuietly(writer);
		}
		return cleanupNeeded;
	}

	static void performCleanup(final BufferedReader reader, final BufferedWriter writer, final String prefix) throws IOException {
		String title = null;
		String charge = "";
		final StringBuilder contentBuilder = new StringBuilder(500);
		boolean run = true;

		while (run) {
			String line = reader.readLine();
			if (line == null) { // Simulate a new section to force flush at the end
				line = "BEGIN IONS";
				run = false; // We will quit on the next round
			}

			if (line.startsWith("BEGIN IONS")) {
				if (contentBuilder.length() > 0) {
					// Dump the previous
					if (charge.contains(" and ")) { // We have multiple charge states reported. Write the spectrum out multiple times
						final Iterable<String> split = Splitter.on(" and ").trimResults().omitEmptyStrings().split(charge);
						for (final String subCharge : split) {
							final String subChargeNum = parseSubCharge(subCharge);
							// We expect to see title in form TITLE=blah (something.2.2..dta) - the two dots signalize place where charge is missing
							final String replacedTitle;
							if (title != null) {
								replacedTitle = title.replaceFirst("(TITLE=.*\\.)(\\.dta\\))\\s*$", "$1" + Matcher.quoteReplacement(subChargeNum) + "$2");
							} else {
								replacedTitle = null;
							}

							dumpSpectrum(writer, replacedTitle, subCharge, contentBuilder);
						}
					} else {
						dumpSpectrum(writer, title, charge, contentBuilder);
					}
				}

				title = null;
				charge = "";
				contentBuilder.setLength(0);
			} else {
				if (line.contains("=")) {
					// We remember title to output it before charge
					if (title == null && line.startsWith("TITLE=")) {
						title = line;
						line = null;
					} else if (charge.isEmpty() && line.startsWith("CHARGE=")) {
						charge = parseCharge(line);
						line = null;
					}
				}
				if (line != null) {
					contentBuilder.append(line).append('\n');
				}
			}
		}
		writer.flush();
	}

	private static void dumpSpectrum(BufferedWriter writer, String title, String charge, StringBuilder contentBuilder) throws IOException {
		writer.append("BEGIN IONS\n");
		if (title == null) {
			writer.append("TITLE=missing title\n");
		} else {
			writer.append(title).append('\n');
		}
		writer.append("CHARGE=").append(charge).append('\n');
		writer.append(contentBuilder.toString());
	}

	static String parseCharge(final String line) {
		final Matcher matcher = CHARGE.matcher(line);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}

	static String parseSubCharge(final String line) {
		final Matcher matcher = SUB_CHARGE.matcher(line);
		if (matcher.find()) {
			return matcher.group(1).intern();
		}
		return "0";
	}
}
