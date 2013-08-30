package edu.mayo.mprc.sge;


import com.google.common.base.Strings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Convert a daemon with a certain work packet on Sun Grid Engine into a command line.
 */
public final class GridScriptFactory {

	private String javaCommand = "java";

	private String swiftLibDirectory;
	private static final String LOG4J_CONFIGURATION = "log4j.configuration";
	private static final String SWIFT_HOME = "swift.home";

	public String getJavaCommand() {
		return javaCommand;
	}

	public void setJavaCommand(final String javaCommand) {
		this.javaCommand = javaCommand;
	}

	public String getSwiftLibDirectory() {
		return swiftLibDirectory;
	}

	public void setSwiftLibDirectory(final String swiftLibDirectory) {
		this.swiftLibDirectory = swiftLibDirectory;
	}

	private static boolean isWrapper(final String wrapper) {
		return !Strings.isNullOrEmpty(wrapper);
	}

	public String getApplicationName(final String wrapper) {
		if (isWrapper(wrapper)) {
			return wrapper;
		}
		return javaCommand;
	}


	// We need to pass certain system properties along to make sure logging will keep working

	public List<String> getParameters(final String wrapper, final File serializedWorkPacket) {
		final List<String> params = new ArrayList<String>(6);

		if (isWrapper(wrapper)) {
			params.add(getJavaCommand());
		}

		if (System.getProperty(LOG4J_CONFIGURATION) != null) {
			params.add("-D" + LOG4J_CONFIGURATION + "=" + System.getProperty(LOG4J_CONFIGURATION));
		}
		if (System.getProperty(SWIFT_HOME) != null) {
			params.add("-D" + SWIFT_HOME + "=" + System.getProperty(SWIFT_HOME));
		}

		params.add("-cp");
		params.add(new File(swiftLibDirectory).getAbsolutePath() + "/*");

		params.add("edu.mayo.mprc.swift.Swift");

		params.add("--sge");
		params.add(serializedWorkPacket.getAbsolutePath());

		return params;
	}

}

