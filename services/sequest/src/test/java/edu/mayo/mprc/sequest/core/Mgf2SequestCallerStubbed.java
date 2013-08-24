package edu.mayo.mprc.sequest.core;


import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * this is used to provide a stub for testing of the Mgf2SequestCaller class without having a
 * sequest executable available.
 */
class Mgf2SequestCallerStubbed implements Mgf2SequestInterface {

	private static final Logger LOGGER = Logger.getLogger(Mgf2SequestCallerStubbed.class);
	private String sequestExe;
	private File hostsFile;
	private int maxCommandLineLength;

	@Override
	public void callSequest(final File tarFile, final File paramsFile, final File mgfFile, final long startTimeOut, final long watchDogTimeOut, final File hdrFile) {

		// validate that mgf file exists
		final boolean havemgf = mgfFile.isFile();
		if (!havemgf) {
			throw new MprcException(mgfFile.getAbsolutePath() + " not found");
		}

		// validate that the params file exists
		final boolean haveparams = paramsFile.isFile();
		if (!haveparams) {
			throw new MprcException(paramsFile.getAbsolutePath() + " not found");
		}

		final File outputDir = tarFile.getParentFile();

		if (!outputDir.isDirectory()) {
			throw new MprcException("Output directory " + outputDir + " not found");
		}

		// create a temporary folder for the dta files
		// and .out files
		final File tempfolder = FileUtilities.createTempFolder();
		final String tempFolderName = tempfolder.getAbsolutePath();

		int maxCommandLength = maxCommandLineLength;
		if (maxCommandLength == 0) {
			maxCommandLength = 100;
		}

		final SequestSubmitterInterface s = new SequestSubmit(100, paramsFile, outputDir, new File(outputDir, "mytar.tar"), hostsFile);

		final SequestRunnerStub sc = new SequestRunnerStub(tempfolder, null, new ArrayList<File>(), hostsFile);

		sc.setSequestExe(sequestExe);

		s.setSequestCaller(sc);
		sc.setWatchDogTimeOut(watchDogTimeOut);
		sc.setStartTimeOut(startTimeOut);


		final IonsModellerInterface i = new MgfIonsModeller();
		i.setWorkingDir(tempFolderName);


		final MgfToDtaFileParser parser = new MgfToDtaFileParser(s, i, tempFolderName);

		parser.setMgfFileName(mgfFile.getAbsolutePath());

		//InputStream is = new InputStream(r)
		try {
			final BufferedReader br = new BufferedReader(new FileReader(mgfFile));
			LOGGER.debug("starting to process mgf");
			parser.getDTAsFromFile(br);
			FileUtilities.closeQuietly(br);

		} catch (Exception t) {
			LOGGER.error("parser failed", t);
			throw new MprcException(t);
		}

	}

	@Override
	public void setSequestExe(final String sequestexe) {
		sequestExe = sequestexe;
	}

	@Override
	public void setHostsFile(final File hostsFile) {
		this.hostsFile = hostsFile;
	}

	@Override
	public void setMaxCommandLineLength(final int commandlinelength) {
		maxCommandLineLength = commandlinelength;
	}

}



