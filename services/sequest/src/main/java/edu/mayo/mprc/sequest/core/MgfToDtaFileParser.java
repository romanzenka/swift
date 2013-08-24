package edu.mayo.mprc.sequest.core;


import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;


/**
 * This wrappers the operations on one mgf file for the mgf to sequest calls
 */
final class MgfToDtaFileParser {

	private static final Logger LOGGER = Logger.getLogger(MgfToDtaFileParser.class);

	/**
	 * the mgf file name
	 */
	private String mgfFileName;


	/**
	 * temp working diretory (base)
	 */
	private String tempWorkingDir;

	/**
	 * dta files working directory
	 * is one per mgf file and dta files are kept there until
	 * moved to the tar file
	 */
	private String dtaTempFolder;

	/**
	 * the sequest submitter, passes dtas to sequest
	 */
	private SequestSubmitterInterface sequestSubmitter;

	/**
	 * the ions section modeller (reused across sections)
	 */
	private IonsModellerInterface ionsModeller;

	/**
	 * output filename without extension
	 */
	private String outfilePrefix;


	public MgfToDtaFileParser(final SequestSubmitterInterface s, final IonsModellerInterface i, final String tempBase) {
		sequestSubmitter = s;
		ionsModeller = i;
		tempWorkingDir = tempBase;
	}


	public void setMgfFileName(final String fileName) {
		mgfFileName = fileName;
		outfilePrefix = new File(mgfFileName).getName();
		outfilePrefix = FileUtilities.stripExtension(outfilePrefix);
		dtaTempFolder = tempWorkingDir + File.separator + outfilePrefix;
		// create the output directory
		if (new File(dtaTempFolder).isDirectory()) {
			LOGGER.warn("Output directory for dtas already exists: " + dtaTempFolder);
		} else {
			if (new File(dtaTempFolder).isFile()) {
				LOGGER.error("The file " + dtaTempFolder + " exists and is not a directory");
			}
		}
	}

	/**
	 * parse the mgf file #mgfFileName for the dta files to pass to sequest
	 */
	public void getDTAsFromFile(final BufferedReader br) {


		ionsModeller.setSequestSubmitter(sequestSubmitter);
		ionsModeller.setWorkingDir(tempWorkingDir);
		ionsModeller.setMgfFileName(mgfFileName);
		ionsModeller.setOutFilePrefix(outfilePrefix);

		String line;

		while (true) {

			try {
				line = br.readLine();
			} catch (IOException ioe) {
				throw new MprcException("failure reading the mgf file name=" + mgfFileName, ioe);
			}
			if (line == null) {
				break;
			}
			try {
				ionsModeller.processLine(line);
			} catch (Exception t) {
				throw new MprcException(t);
			}
		}
		// force a cleanup of any left over dta files
		ionsModeller.forceSubmit();
	}

	/**
	 * parse the mgf file #mgfFileName for the dta files to pass to sequest
	 */

	private char[] buffer = new char[8192 + 256];

	public void getDTAsFromFileWithBlockReads(final BufferedReader br) {


		ionsModeller.setSequestSubmitter(sequestSubmitter);
		ionsModeller.setWorkingDir(tempWorkingDir);
		ionsModeller.setMgfFileName(mgfFileName);
		ionsModeller.setOutFilePrefix(outfilePrefix);


		long offset = 0;
		long count = 0;

		//char[] buffer = new char[8192];
		int num = 0;
		int base = 0;

		while (true) {

			try {
				num = br.read(buffer, base, 8192);
				if (num == -1) {
					break;
				}
			} catch (IOException ioe) {
				throw new MprcException("failure reading the mgf file name=" + mgfFileName, ioe);
			}
			// find some lines
			int start = 0;
			int topLine = start;
			for (int i = 0; i < base + num; i++) {
				// find a newline
				if (buffer[i] == '\n') {
					topLine = i;
				} else {
					//if(i == base+num-1){
					//    topLine = base+num;
					//}
					continue;
				}

				try {

					ionsModeller.processLine(buffer, start, topLine - start);
				} catch (Exception t) {
					throw new MprcException(t);
				}
				start = topLine;
				count++;
			}
			// see if something not used from buffer
			if (num + base > topLine) {
				// copy remainder to base of the buffer
				final int length = base + num - (start + 1);
				System.arraycopy(buffer, start + 1, buffer, 0, length);
				base = length;
			}
			offset += num;
		}
		// force a cleanup of any left over dta files
		ionsModeller.forceSubmit();

	}

	//public void processline(char[] buffer, int pos, int linelen, long fileoffset)

}
