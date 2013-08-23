package edu.mayo.mprc.sequest.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This is used to stub out the SequestCaller
 * It replaces the call to the sequest executable with a call to 'echo'
 * It also creates a '.out' file for each '.dta' file to support sequest caller container testing
 * It satisfies the interface of SequestCaller so that the dta file preprocessing and
 * result post processing (tarring); can be tested without sequest
 */
public final class SequestRunnerStub extends SequestRunner {
	public SequestRunnerStub(final File workingDir, final File paramsFile, final List<File> sequestDtaFiles, final File hostsFile) {
		super(workingDir, paramsFile, sequestDtaFiles, hostsFile);
		setCommand("echo");
		final List<String> someArgs = new ArrayList<String>();
		someArgs.add("hi");
		setArgs(someArgs);
	}

	/**
	 * want no watchdog with this stub
	 */
	public void setupWatchDog() {

	}

	public void validatePvmUp() {

		// do nothing
	}


	@Override
	public String getSearchResultsFolder() {
		return super.getSearchResultsFolder();
	}

	@Override
	public void setSearchResultsFolder(final String folder) {
		super.setSearchResultsFolder(folder);
	}

}
