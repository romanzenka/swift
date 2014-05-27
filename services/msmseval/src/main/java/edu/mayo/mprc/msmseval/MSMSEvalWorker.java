package edu.mayo.mprc.msmseval;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ExecutableSwitching;
import edu.mayo.mprc.config.ui.ResourceConfigBase;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerBase;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.io.mgf.MzXmlConverter;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Map;

public final class MSMSEvalWorker extends WorkerBase {

	private static final String MSMS_EVAL_EXECUTABLE = "msmsEvalExecutable";
	public static final String PARAM_FILES = "paramFiles";

	private static final Logger LOGGER = Logger.getLogger(MSMSEvalWorker.class);
	public static final String TYPE = "msmsEval";
	public static final String NAME = "MsmsEval";
	public static final String DESC = "Evaluates the quality of spectra. See <a href=\"http://proteomics.ucd.ie/msmseval/\">http://proteomics.ucd.ie/msmseval/</a> for more information.";

	private File msmsEvalExecutable;

	//Flag is set to true is the execution of this worker is skipped.
	private boolean skippedExecution;

	private MzXmlConverter converter;

	public MSMSEvalWorker() {
		super();

		skippedExecution = false;
	}

	@Override
	public void process(final WorkPacket workPacket, final File tempWorkFolder, final UserProgressReporter reporter) {
		if (!MSMSEvalWorkPacket.class.isInstance(workPacket)) {
			throw new DaemonException("Unknown request type [" + workPacket.getClass().getName() + "] expecting [" + MSMSEvalWorkPacket.class.getName() + "]");
		}

		final MSMSEvalWorkPacket msmsEvalWorkPacket = (MSMSEvalWorkPacket) workPacket;

		/**
		 * MGF source file.
		 */
		final File sourceFile = msmsEvalWorkPacket.getSourceFile();
		checkFile(sourceFile, false, "The source file");

		/**
		 * MsmsEval parameter file.
		 */
		final File msmsEvalParamFile = msmsEvalWorkPacket.getMsmsEvalParamFile();
		checkFile(msmsEvalParamFile, false, "The msmsEval parameter file");

		/**
		 * Output directory.
		 */
		final File outputDirectory = msmsEvalWorkPacket.getOutputDirectory();
		FileUtilities.ensureFolderExists(outputDirectory);
		checkFile(outputDirectory, true, "The msmsEval output directory");

		/**
		 * Output files.
		 */
		final File outputMzXMLFile = MSMSEvalWorkPacket.getExpectedMzXMLOutputFileName(sourceFile, tempWorkFolder); // Temporary
		final File msmsEvalOutputFile = MSMSEvalWorkPacket.getExpectedMsmsEvalOutputFileName(sourceFile, tempWorkFolder); // Temporary

		/**
		 * Files to be published.
		 */
		final File finalOutputFile = MSMSEvalWorkPacket.getExpectedResultFileName(sourceFile, outputDirectory);
		final File outputFile = getTempOutputFile(tempWorkFolder, finalOutputFile);

		//If msmsEval has been executed, skip operation.
		if (!msmsEvalWorkPacket.isFromScratch() && hasMSMSEvalFilterWorkerRun(finalOutputFile)) {
			skippedExecution = true;
			return;
		}

		MSMSEval msmsEval = null;

		try {
			LOGGER.info("Converting mgf to mzxml.");

			final Map<Integer, String> mzXMLScanToMGFTitle = getConverter().convert(sourceFile, outputMzXMLFile, true);

			LOGGER.info("Convertion mgf to mzxml completed.");
			LOGGER.info("Created mzxml file: " + outputMzXMLFile.getAbsolutePath());

			LOGGER.info("Running msmsEval on the mzxml file.");

			msmsEval = new MSMSEval(outputMzXMLFile, msmsEvalParamFile, msmsEvalExecutable);

			msmsEval.execute(true);

			LOGGER.info("Command msmsEval execution completed.");

			LOGGER.info("Formatting msmsEval output file with mgf scan numbers.");

			MSMSEvalOutputFileFormatter.replaceMzXMLScanIdsWithMgfNumbers(msmsEvalOutputFile, outputFile, mzXMLScanToMGFTitle);

			LOGGER.info("Formatted msmsEval output file " + outputFile.getAbsolutePath() + " created.");

			publish(msmsEvalOutputFile, finalOutputFile);
		} catch (Exception e) {
			throw new DaemonException(e);
		} finally {
			//Clean up.
			LOGGER.info("Deleting files: [" + msmsEvalOutputFile.getAbsolutePath() + ", " + outputMzXMLFile.getAbsolutePath() + "]");
			FileUtilities.deleteNow(msmsEvalOutputFile);
			FileUtilities.deleteNow(outputMzXMLFile);
		}
	}

	/**
	 * This method is used for debugging purposes.
	 * This method must be called after calling of the processRequest(...) method.
	 *
	 * @return
	 */
	public boolean isSkippedExecution() {
		return skippedExecution;
	}

	public File getMsmsEvalExecutable() {
		return msmsEvalExecutable;
	}

	public void setMsmsEvalExecutable(final File msmsEvalExecutable) {
		this.msmsEvalExecutable = msmsEvalExecutable;
	}

	public MzXmlConverter getConverter() {
		return converter;
	}

	public void setConverter(MzXmlConverter converter) {
		this.converter = converter;
	}

	private void checkFile(final File file, final boolean directory, final String fileDescription) {
		if (file.exists()) {
			if (!file.isDirectory() && directory) {
				throw new DaemonException(fileDescription + " is not a directory: " + file.getAbsolutePath());
			}
			if (file.isDirectory() && !directory) {
				throw new DaemonException(fileDescription + " is a directory: " + file.getAbsolutePath());
			}
		} else {
			throw new DaemonException(fileDescription + " could not be found: " + file.getAbsolutePath());
		}
	}

	private boolean hasMSMSEvalFilterWorkerRun(final File msmsEvalFormattedOuputFileName) {
		if (msmsEvalFormattedOuputFileName.exists() && msmsEvalFormattedOuputFileName.length() > 0) {
			LOGGER.info(MSMSEvalWorker.class.getSimpleName() + " has already run and file [" + msmsEvalFormattedOuputFileName.getAbsolutePath() + "] already exist. MSMSEval execution has been skipped.");
			return true;
		}

		return false;
	}

	@Override
	public String check() {
		LOGGER.info("Checking msmsEval worker");
		if (!msmsEvalExecutable.canExecute()) {
			return "msmsEval is not executable: " + msmsEvalExecutable.getAbsolutePath();
		}
		return null;
	}


	/**
	 * A factory capable of creating the worker
	 */
	@Component("msmsEvalWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		private MzXmlConverter converter;

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final MSMSEvalWorker worker = new MSMSEvalWorker();
			worker.setMsmsEvalExecutable(FileUtilities.getAbsoluteFileForExecutables(new File(config.get(MSMS_EVAL_EXECUTABLE))));
			worker.setConverter(getConverter());
			return worker;
		}

		public MzXmlConverter getConverter() {
			return converter;
		}

		@Resource(name = "mzXmlConverter")
		public void setConverter(final MzXmlConverter converter) {
			this.converter = converter;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config extends ResourceConfigBase {
		public Config() {
		}

		public Config(final String msmsEval, final String paramFiles) {
			put(MSMS_EVAL_EXECUTABLE, msmsEval);
			put(PARAM_FILES, paramFiles);
		}
	}

	public static final class Ui implements ServiceUiFactory {

		private static final String WINDOWS = "bin/msmseval/win/msmsEval.exe";
		private static final String LINUX = "bin/msmseval/linux_x86_64/msmsEval";
		private static final String LINUX_IA = "bin/msmseval/linux_i686/msmsEval";

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(MSMS_EVAL_EXECUTABLE, "Executable Path", "MsmsEval executable path."
							+ "<br/>The msmsEval executable depends on the OS." +
							"<br/>Executables and source code are available at <a href=\"http://proteomics.ucd.ie/msmseval\">http://proteomics.ucd.ie/msmseval</a> or " +
							"<br/>can be found in the Swift installation directory:" +
							"<br/><tt>src/msmseval/</tt>" +
							"<br/>Precompiled executables:" +
							"<br/><table><tr><td>Windows</td><td><tt>" + WINDOWS + "</tt></td></tr>" +
							"<tr><td>Linux x86_64</td><td><tt>" + LINUX + "</tt></td></tr>" +
							"<tr><td>Linux i686</td><td><tt>" + LINUX_IA + "</tt></td></tr>").required().executable(Arrays.asList("-v"))

					.property(PARAM_FILES, "Parameter files for msmsEval",
							"msmsEval uses these parameter files for determining the relative weights of various attributes it calculates. "
									+ "The final score is determined using the configuration. This field defines multiple parameter files in single line. "
									+ "The format is:<p><tt>&lt;name 1&gt;,&lt;config file 1&gt;,&lt;name 2&gt;,&lt;config file 2&gt;,...")
					.required()
					.defaultValue("Orbitrap,conf/msmseval/msmsEval-orbi.params,Default,conf/msmseval/msmsEval.params")
					.addChangeListener(new ExecutableSwitching(resource, MSMS_EVAL_EXECUTABLE, WINDOWS, LINUX));
		}
	}
}
