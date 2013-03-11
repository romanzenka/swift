package edu.mayo.mprc.raw2mgf;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * provides a container for calling extract msn, with or without the use of wine
 */
public final class ExtractMsnWrapper {
	private static final Logger LOGGER = Logger.getLogger(ExtractMsnWrapper.class);

	private File fileToExecute;
	private String sParams;
	private String sRAWFileName;
	private File outputdir;
	private String wrapperScript; // A wrapper script for Linux. No wrapper == windows!
	private String xvfbWrapperScript;

	/**
	 * This sets up the information for making a call to the executable with or without wine
	 *
	 * @param fileToExec - the executable to call (so can support wine)
	 * @param params     - the parameter string
	 * @param rawfile    - the full path to the raw file
	 */
	public ExtractMsnWrapper(final File fileToExec, final String params, final File rawfile, final String wrapperScript, final String xvfbWrapperScript) {
		this.fileToExecute = fileToExec;
		this.sParams = params;
		this.sRAWFileName = rawfile.getAbsolutePath();
		this.wrapperScript = wrapperScript;
		this.xvfbWrapperScript = xvfbWrapperScript;
	}

	/**
	 * Convert unix path to wine path. This means flipping the slashes to backslashes and prepending with Z: (wine
	 * maps Z: to root unix directory)
	 *
	 * @param path Path to map
	 * @return Wine-ized path.
	 */
	private String toWinePathIfWine(final String path) {
		if (isWine()) {
			return "Z:" + path.replaceAll("\\/", "\\\\");
		} else {
			return path;
		}
	}

	/**
	 * call is exe + params + outputfilename
	 *
	 * @return the call
	 */
	private String[] getCall() {
		final List<String> result = new ArrayList<String>();

		if (isWine()) {
			if (xvfbWrapperScript != null) {
				result.add(xvfbWrapperScript);
			}

			result.add(wrapperScript);
		}

		result.add(this.fileToExecute.getAbsolutePath());
		result.addAll(Arrays.asList(this.sParams.split(" ")));
		result.add(toWinePathIfWine(this.sRAWFileName));

		final String[] array = new String[result.size()];
		return result.toArray(array);
	}

	/**
	 * call the executable to make the conversion from a raw file to dta files
	 */
	public void run() {
		final File folder = outputdir;
		LOGGER.debug("Extract_msn output files will go to " + folder.getAbsolutePath() + " which " + (folder.exists() ? "exists" : "does not exist."));
		if (!folder.exists()) {
			LOGGER.debug("Making output folder " + folder.getAbsolutePath());
			try {
				FileUtilities.ensureFolderExists(folder);
			} catch (Exception t) {
				throw new MprcException("Cannot create output folder for extract_msn: " + folder.getAbsolutePath(), t);
			}
		}

		final String[] theCall = getCall();
		final ProcessBuilder builder = new ProcessBuilder(theCall).directory(folder);
		final ProcessCaller caller = new ProcessCaller(builder);
		caller.runAndCheck("extract_msn");
		if (folder.listFiles().length == 0) {
			throw new MprcException("The folder with .dta files is empty: " + folder.getAbsolutePath() + "\nextract_msn call:\n"
					+ caller.getFailedCallDescription());
		}
	}

	/**
	 * set folder where the dta files will be placed
	 *
	 * @param sOutputdir - the folder to place dta files  in
	 */
	public void setOutputDir(final File sOutputdir) {
		this.outputdir = sOutputdir;
	}

	/**
	 * We are running wine iff there is a wrapper script.
	 */
	private boolean isWine() {
		return wrapperScript != null && wrapperScript.length() > 0 && !FileUtilities.isWindowsPlatform();
	}
}
