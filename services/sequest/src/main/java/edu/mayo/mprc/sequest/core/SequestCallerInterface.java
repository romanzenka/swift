package edu.mayo.mprc.sequest.core;

import edu.mayo.mprc.utilities.progress.UserProgressReporter;

import java.io.File;
import java.util.List;

/**
 * Interface for sequest callers. Was added to stubbing for testing easier
 */
interface SequestCallerInterface {

	SequestCallerInterface createInstance(File workingDir, File paramsFile, List<File> sequestDtaFiles, File hostsFile,
	                                      UserProgressReporter progressReporter);

	String getCall();

	String getCommand();

	void run();

	String getSequestExe();

	void setSequestExe(String sequestExe);

	String getSearchResultsFolder();

	void setSearchResultsFolder(String folder);

	File getWorkingDir();


	long getWatchDogTimeOut();

	void setWatchDogTimeOut(long timeOut);

	long getStartTimeOut();

	void setStartTimeOut(long timeOut);


}
