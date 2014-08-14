package edu.mayo.mprc.sqt;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Cleans an .SQT file provided by Comet
 *
 * @author Roman Zenka
 */
public final class CleanupSqt {
	private File inputSqt;
	private File outputSqt;
	private File fastaFile;

	private static final Pattern PEPTIDE_SEQUENCE = Pattern.compile("[^a-zA-Z]");

	public CleanupSqt(File inputSqt, File outputSqt, File fastaFile) {
		this.inputSqt = inputSqt;
		this.outputSqt = outputSqt;
		this.fastaFile = fastaFile;
	}

	public void run() {
		BufferedReader reader = FileUtilities.getReader(inputSqt);
		try {
			while (true) {
				final String line = reader.readLine();
				if(line==null) {
					break;
				}
				if(line.startsWith("M")) {
					String PSM = line.split("\t")[9];
				}
			}
		} catch (IOException e) {
			throw new MprcException("Error reading the input SQT file from " + inputSqt.getAbsolutePath(), e);
		} finally {
			FileUtilities.closeQuietly(reader);
		}
	}
}
