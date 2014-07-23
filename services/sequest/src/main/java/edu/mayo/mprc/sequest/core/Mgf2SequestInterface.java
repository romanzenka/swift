package edu.mayo.mprc.sequest.core;

import edu.mayo.mprc.utilities.progress.UserProgressReporter;

import java.io.File;

/**
 * This interface is used to provide dependency injection in the sequest caller
 */
interface Mgf2SequestInterface {
	void callSequest(File outputFile, File paramsFile, File mgfFile, long startTimeOut, long watchDogTimeOut, File hdrFile,
	                 UserProgressReporter progressReporter);

	void setSequestExe(String sequestExe);

	void setHostsFile(File hostsFile);

	void setMaxCommandLineLength(int commandLineLength);
}
