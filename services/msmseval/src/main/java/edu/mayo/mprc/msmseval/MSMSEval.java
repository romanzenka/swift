/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mayo.mprc.msmseval;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Wrapper around msmsEval external command.
 * <p/>
 * We use msmsEval in the "Finding high-quality unidentified spectra" mode. For each spectrum a probability of being identifiable is
 * calculated.
 * <p/>
 * See the
 * msmsEval manuscript for more information:
 * <p/>
 * <cite>
 * Wong, J.W.H., Sullivan, M.J., Cartwright, H.M. and Cagney, G. (2007) "msmsEval: Tandem mass spectral quality assignment for high-throughput proteomics" BMC Bioinformatics, 8:51
 * </cite>
 * <p/>
 * More resources are at <a href="http://proteomics.ucd.ie/msmseval">msmsEval website</a>.
 */
public final class MSMSEval {
	private static final Logger LOGGER = Logger.getLogger(MSMSEval.class);

	/**
	 * Constants
	 */
	public static final String PARAM_FILE_FLAG = "-d";
	public static final String SOURCE_MZXML_FILE_FLAG = "-s";
	/**
	 * Variables
	 */
	private ProcessBuilder processBuilder;
	private File sourceMzXML;

	/**
	 * Constructor.
	 *
	 * @param sourceMzXML        Input: Source mzxml file.
	 * @param msmsEvalParameter  MSMSEval parameter file - describes how to do the EM and relative importance of various fields.
	 * @param msmsEvalExecutable msmsEval executable file. Can be either absolute or relative file - if the path does not denote an executable, exception is thrown.
	 * @see MSMSEval More information about cutoff probability.
	 */
	public MSMSEval(final File sourceMzXML, final File msmsEvalParameter, final File msmsEvalExecutable) {
		if (msmsEvalExecutable == null) {
			throw new MprcException("The msmsEval executable path was not set");
		}
		if (msmsEvalExecutable.isAbsolute() && !msmsEvalExecutable.isFile()) {
			throw new MprcException("The msmsEval executable path does not denote a file: " + msmsEvalExecutable);
		}
		this.sourceMzXML = sourceMzXML;

		final List<String> parameters = new LinkedList<String>();

		parameters.add(msmsEvalExecutable.getPath());
		parameters.add(SOURCE_MZXML_FILE_FLAG);
		parameters.add(sourceMzXML.getAbsolutePath());
		parameters.add(PARAM_FILE_FLAG);
		parameters.add(msmsEvalParameter.getAbsolutePath());

		processBuilder = new ProcessBuilder(parameters);

		LOGGER.debug(processBuilder.command().toString());
	}

	/**
	 * Executes the msmsEval external command.
	 *
	 * @return int value which represents the exit value of the command, By convention, a return value of 0 means command run successfully.
	 */
	public void execute(UserProgressReporter reporter) {
		if (processBuilder != null) {

			ProcessCaller processCaller = null;

			processCaller = new ProcessCaller(processBuilder, reporter.getLog());

			processCaller.runAndCheck("msmseval");
		} else {
			throw new IllegalArgumentException("Process builder object for the msmsEval command is null.");
		}
	}

	/**
	 * Returns comman separated value file with all data calculated and analyzed by the msmsEval process.
	 *
	 * @return
	 */
	public String getMsmsEvalOutputFileName() {
		//MSMSEval outputs the data to a file with the following name.
		return sourceMzXML + "_eval.csv";
	}
}
