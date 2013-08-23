package edu.mayo.mprc.sequest.core;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This keeps the model of an ions section
 */
final class MgfIonsModeller implements IonsModellerInterface {


	private static final Logger LOGGER = Logger.getLogger(MgfIonsModeller.class);

	private static final double PROTON_MASS = 1.00727646;

	public MgfIonsModeller() {
		this.scans = new HashMap<String, Integer>();
		this.dtaHistory = new HashMap<String, String>();
		ionsSection = 1;
	}

	/**
	 * the submitter of dta files to sequest
	 */
	private SequestSubmitterInterface sequestSubmitter;


	public String getCharge() {
		return charge;
	}

	public String getMz() {
		return mz;
	}

	/**
	 * m over z
	 */
	private String mz;
	/**
	 * charge on precursor mass
	 */
	private String charge;

	/**
	 * in a section
	 */
	public boolean active;

	/**
	 * section done
	 */
	public boolean done;

	/**
	 * dta file name
	 */
	private String dtaFileName;

	/**
	 * the cycle (from title if found)
	 */
	private int cycle;

	/**
	 * temporary working directory (tempoutdir is a child of this)
	 */
	private String workingDir;

	/**
	 * mgf file name
	 */
	private String mgfFileName;

	/**
	 * output filename without extension
	 */
	private String outFilePrefix;

	/**
	 * line number
	 */
	private int lineNumber;

	/**
	 * ions section number
	 */
	private int ionsSection;

	/**
	 * the scan history
	 * key is '_' + number
	 * value is number
	 * this exists across multiple ions sections
	 */
	public Map<String, Integer> scans;

	/**
	 * the dta filename history
	 * key is the name of the dta file
	 */
	private Map<String, String> dtaHistory;


	@Override
	public void setMgfFileName(final String mgfFileName) {
		this.mgfFileName = mgfFileName;
	}

	/**
	 * clear the model so can reuse it
	 */
	public void clear() {

		this.mz = null;
		this.charge = null;

		this.active = false;
		this.done = false;
		this.cycle = -1;
		this.dtaFileName = null;
		this.outFilePrefix = null;
	}

	public static String matchPattern(final Pattern p, final String seq) {
		final Matcher k = p.matcher(seq);
		if (k.matches()) {
			return k.group(1);
		}
		return null;
	}

	public static String findPattern(final Pattern p, final String seq) {
		final Matcher k = p.matcher(seq);
		if (k.find()) {
			return k.group(1);
		}
		return null;
	}

	/**
	 * Returns a cycle number from given IONS section title.
	 *
	 * @param title The TITLE= line from the .mgf file.
	 * @return Cycle number if one is present, -1 otherwise.
	 */
	public static int getCycleFromTitle(final String title) {
		final String cycleString = findPattern(CYCLE_PATTERN, title);
		if (cycleString != null) {
			return Integer.parseInt(cycleString);
		}
		return -1;
	}

	public static boolean matches(final Pattern p, final String seq) {
		final Matcher k = p.matcher(seq);
		return k.matches();
	}

	public static boolean find(final Pattern p, final String seq) {
		final Matcher k = p.matcher(seq);
		return k.find();
	}

	/**
	 * process a single line from the mgf file
	 * This is a slow version of {@link #processLine(char[], int, int)} provided for convenience only.
	 */
	@Override
	public void processLine(final String line) {
		final char[] chars = line.trim().toCharArray();
		processLine(chars, 0, chars.length);
	}

	/**
	 * process a single line from the mgf file
	 */

	// for optimization
	private char[] left = new char[500];
	private StringBuilder sIONS = new StringBuilder();
	private StringBuilder sTITLE = new StringBuilder();

	private static final String PEPMASS = "PEPMASS=";

	private static final String CHARGE = "CHARGE=";
	private static final String TITLE = "TITLE=";
	private static final String BEGIN_IONS = "BEGIN IONS";
	private static final String END_IONS = "END IONS";

	public static int findPepMass(final char[] buffer, final int top) {
		return findMatch(PEPMASS, buffer, top);
	}

	public static int findCharge(final char[] buffer, final int top) {
		return findMatch(CHARGE, buffer, top);
	}

	public static int findTitle(final char[] buffer, final int top) {
		return findMatch(TITLE, buffer, top);
	}

	static int findBeginIons(final char[] buffer, final int top) {
		return findMatch(BEGIN_IONS, buffer, top);
	}

	static int findEndIons(final char[] buffer, final int top) {
		return findMatch(END_IONS, buffer, top);
	}


	static boolean isIonsLine(final char[] buffer, final int top) {
		return (top > 0 && (buffer[0] >= '0' && buffer[0] <= '9'));

	}

	public static int findMatch(final String what, final char[] buffer, final int top) {
		final int len = what.length();
		int match = -1;
		for (int i = 0; i < top - len + 1; i++) {
			match = i;
			int j = 0;
			for (j = i; (j - i) < len; j++) {
				if (buffer[j] != what.charAt(j - i)) {
					match = -1;
					break;
				}
			}
			if (match != -1 && j - i == len) {
				break;
			}
		}
		return match;
	}


	// modify to
	// return number chars put in to

	static int put(final char[] to, final char[] from, final int pos, final int len) {
		int j = 0;
		int start = pos;
		// skip initial blanks
		for (int i = pos; i < pos + len; i++) {
			if (from[i] == ' ' || from[i] == '\t' || from[i] == '\n') {
				start++;
			} else {
				break;
			}

		}
		for (int k = start; k < pos + len; k++) {
			if (from[k] == '\n' || from[k] == '\r') {
				break;
			}
			to[j] = from[k];
			j++;
		}
		return j;
	}

	/**
	 * @param lineLen is up to but not including the NEWLINE
	 * @buffer the buffer
	 * @linelen length of the line not including new line terminator
	 */

	@Override
	public void processLine(final char[] buffer, final int pos, final int lineLen) {
		lineNumber++;

		// rem is size of remaining buffer
		final int rem = put(left, buffer, pos, lineLen);


		int loc = findPepMass(left, rem);

		if (loc != -1) {
			mz = new String(left, loc + PEPMASS.length(), rem - loc - PEPMASS.length()).trim();
			final int firstSpace = mz.indexOf(' ');
			if (firstSpace > 0) {
				mz = mz.substring(0, firstSpace);
			}
		} else {
			loc = findCharge(left, rem);
			if (loc != -1) {
				charge = new String(left, loc + CHARGE.length(), rem - loc - CHARGE.length());
			} else {
				if (rem == 0) {
					// blank line
					return;
				}
				// ion
				if (isIonsLine(left, rem)) {
					sIONS.append(left, 0, rem);
					sIONS.append("\n");
					return;
				}
				loc = findBeginIons(left, rem);
				if (loc != -1) {
					// new section so call clear

					// clear the ions
					sIONS.setLength(0);
					this.clear();
					this.active = true;
					return;
				}
				loc = findEndIons(left, rem);
				if (loc != -1) {
					this.createDtaFilePrototype();
					this.done = true;
					return;
				}
				loc = findTitle(left, rem);
				if (loc != -1) {
					this.sTITLE.setLength(0);
					this.sTITLE.append(left, loc + TITLE.length(), rem - loc - TITLE.length());
					this.sTITLE.setLength(rem - loc - TITLE.length());
					this.parsetitle(sTITLE.toString());
					return;
				}
			}
		}

	}

	@Override
	public void forceSubmit() {
		sequestSubmitter.forceSubmit();
	}

	public static final Pattern CYCLE_PATTERN = Pattern.compile("Cycle\\(s\\): (\\d+)");
	private static final Pattern DTA_PATTERN = Pattern.compile(".*\\((.*\\.dta)\\).*");
	private static final Pattern FILE_PATTERN = Pattern.compile("File: ((\\w|\\.)+),");

	/**
	 * If the dta filename is in the title then use its bottommost folder + name
	 * and append this to the temporary working directory
	 */
	public void parsetitle(final String titleright) {
		String item = matchPattern(DTA_PATTERN, titleright);
		if (item != null) {
			// found a dta filename
			final File dta = new File(item);
			item = dta.getName();
			this.dtaFileName = this.workingDir + File.separator + item;
		} else {
			this.cycle = getCycleFromTitle(titleright);
			if (this.cycle != -1) {
				// in this case have to build the dta filename
				//  <MGF file prefix>.<scan>.<scan><charge>.dta
				item = findPattern(FILE_PATTERN, titleright);
				if (item != null) {
					outFilePrefix = item;
					outFilePrefix = FileUtilities.stripExtension(outFilePrefix);
					// since charge not known yet need to wait to build the full dta file name
				}
			} else {
				LOGGER.warn("cannot determine scan number at " + mgfFileName + ":" + lineNumber + titleright);
			}
		}
	}


	private void createDtaFilePrototype() {
		final String scan = (((this.cycle < 0 ? "_" + ionsSection : "" + this.cycle)));
		this.scans.put(scan, this.ionsSection);
		if (this.dtaFileName == null) {
			final String shortname = outFilePrefix + "." + scan + "." + scan + "." + charge + ".dta";
			this.dtaFileName = this.workingDir + File.separator + shortname;
		}
		if (dtaHistory.get(this.dtaFileName) != null) {
			LOGGER.warn("duplicate DTA file name " + dtaFileName + " at " + mgfFileName + ":" + lineNumber + " previous occurrence at " + dtaHistory.get(dtaFileName));
		} else {
			// charge could contain a + sign at the end
			String charge = this.charge;
			if (charge == null || charge.trim().isEmpty()) {
				// Charge was not specified, skip this .dta file
				LOGGER.warn("Unspecified charge, skipping spectrum " + dtaFileName);
				return;
			}
			if (mz.trim().isEmpty()) {
				LOGGER.warn("Unspecified M/Z, skipping spectrum " + dtaFileName);
				return;
			}

			charge = charge.replace("+", "");
			charge = charge.replace("-", "");
			final double mh = (new Double(mz) * new Double(charge) - ((new Double(charge)) - 1.0) * PROTON_MASS);

			this.dtaHistory.put(this.dtaFileName, "" + mgfFileName + ":" + lineNumber);
			final File dta = new File(this.dtaFileName);
			FileUtilities.ensureFileExists(dta);

			FileWriter w = null;
			try {
				w = new FileWriter(dta);
				w.append(String.valueOf(mh))
						.append(" ")
						.append(charge)
						.append("\n")
						.append(this.sIONS);
			} catch (IOException ioe) {
				throw new MprcException("could not write to file", ioe);
			} finally {
				FileUtilities.closeQuietly(w);
			}
			//LOGGER.debug("created file="+dta.getAbsolutePath());


			this.sequestSubmitter.addDtaFile(new File(this.dtaFileName), false);
			this.ionsSection++;
		}
	}

	@Override
	public void setSequestSubmitter(final SequestSubmitterInterface submitter) {
		this.sequestSubmitter = submitter;
	}


	@Override
	public void setWorkingDir(final String name) {
		this.workingDir = name;
	}

	@Override
	public void setOutFilePrefix(final String name) {
		this.outFilePrefix = name;
	}
}
