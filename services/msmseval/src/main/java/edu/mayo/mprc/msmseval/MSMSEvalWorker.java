package edu.mayo.mprc.msmseval;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.WorkerConfig;
import edu.mayo.mprc.config.ui.ExecutableSwitching;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerBase;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.io.mgf.MGF2MzXMLConverter;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public final class MSMSEvalWorker extends WorkerBase {

	public static final String SCORE_FILE_SUFFIX = "_eval.csv";
	public static final String EM_FILE_SUFFIX = "_em.csv";
	public static final String OUTPUT_FILE_SUFFIX = "_eval.mod.csv";
	public static final String MZXML_OUTPUT_FILE_EXTENTION = ".mzxml";

	private static final String MSMS_EVAL_EXECUTABLE = "msmsEvalExecutable";
	private static final String PARAM_FILES = "paramFiles";

	private static final Logger LOGGER = Logger.getLogger(MSMSEvalWorker.class);
	public static final String TYPE = "msmsEval";
	public static final String NAME = "MsmsEval";
	public static final String DESC = "Evaluates the quality of spectra. See <a href=\"http://proteomics.ucd.ie/msmseval/\">http://proteomics.ucd.ie/msmseval/</a> for more information.";

	private File msmsEvalExecutable;

	//Flag is set to true is the execution of this worker is skipped.
	private boolean skippedExecution;

	public MSMSEvalWorker() {
		super();

		skippedExecution = false;
	}

	@Override
	public void process(final WorkPacket workPacket, final UserProgressReporter reporter) {
		if (!MSMSEvalWorkPacket.class.isInstance(workPacket)) {
			throw new DaemonException("Unknown request type [" + workPacket.getClass().getName() + "] expecting [" + MSMSEvalWorkPacket.class.getName() + "]");
		}

		final MSMSEvalWorkPacket msmsEvalWorkPacket = (MSMSEvalWorkPacket) workPacket;

		/**
		 * MGF source file.
		 */
		final File sourceMGFFile = msmsEvalWorkPacket.getSourceMGFFile();
		checkFile(sourceMGFFile, false, "The source mgf file");

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
		final File outputMzXMLFile = getExpectedMzXMLOutputFileName(sourceMGFFile, outputDirectory);
		final File msmsEvalFormattedOuputFile = getExpectedResultFileName(sourceMGFFile, outputDirectory);
		final File msmsEvalOuputFile = getExpectedMsmsEvalOutputFileName(sourceMGFFile, outputDirectory); // Temporary

		//If msmsEval has been executed, skip operation.
		if (!msmsEvalWorkPacket.isFromScratch() && hasMSMSEvalFilterWorkerRun(msmsEvalFormattedOuputFile)) {
			skippedExecution = true;
			return;
		}

		MSMSEval msmsEval = null;

		try {
			LOGGER.info("Converting mgf to mzxml.");

			final Map<Integer, String> mzXMLScanToMGFTitle = MGF2MzXMLConverter.convert(sourceMGFFile, outputMzXMLFile, true);

			LOGGER.info("Convertion mgf to mzxml completed.");
			LOGGER.info("Created mzxml file: " + outputMzXMLFile.getAbsolutePath());

			LOGGER.info("Running msmsEval on the mzxml file.");

			msmsEval = new MSMSEval(outputMzXMLFile, msmsEvalParamFile, msmsEvalExecutable);

			msmsEval.execute(true);

			LOGGER.info("Command msmsEval execution completed.");

			LOGGER.info("Formatting msmsEval output file with mgf scan numbers.");

			MSMSEvalOutputFileFormatter.replaceMzXMLScanIdsWithMgfNumbers(msmsEvalOuputFile, msmsEvalFormattedOuputFile, mzXMLScanToMGFTitle);

			LOGGER.info("Formatted msmsEval output file " + msmsEvalFormattedOuputFile.getAbsolutePath() + " created.");
		} catch (Exception e) {
			throw new DaemonException(e);
		} finally {
			//Clean up.
			LOGGER.info("Deleting files: [" + msmsEvalOuputFile.getAbsolutePath() + ", " + outputMzXMLFile.getAbsolutePath() + "]");
			FileUtilities.deleteNow(msmsEvalOuputFile);
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

	private static File getExpectedMzXMLOutputFileName(final File sourceMGFFile, final File outputDirectory) {
		return new File(outputDirectory, FileUtilities.getFileNameWithoutExtension(sourceMGFFile) + MZXML_OUTPUT_FILE_EXTENTION);
	}

	private static File getExpectedMsmsEvalOutputFileName(final File sourceMGFFile, final File outputDirectory) {
		return new File(outputDirectory, getExpectedMzXMLOutputFileName(sourceMGFFile, outputDirectory).getName() + SCORE_FILE_SUFFIX);
	}

	/**
	 * File with information about expectation maximization parameters.
	 */
	public static File getExpectedEmOutputFileName(final File sourceMGFFile, final File outputDirectory) {
		return new File(outputDirectory, getExpectedMzXMLOutputFileName(sourceMGFFile, outputDirectory).getName() + EM_FILE_SUFFIX);
	}

	/**
	 * File with list of spectra (original spectrum numbers) + their msmsEval information.
	 */
	public static File getExpectedResultFileName(final File sourceMGFFile, final File outputDirectory) {
		return new File(outputDirectory, getExpectedMzXMLOutputFileName(sourceMGFFile, outputDirectory).getName() + OUTPUT_FILE_SUFFIX);
	}

	public File getMsmsEvalExecutable() {
		return msmsEvalExecutable;
	}

	public void setMsmsEvalExecutable(final File msmsEvalExecutable) {
		this.msmsEvalExecutable = msmsEvalExecutable;
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


	/**
	 * A factory capable of creating the worker
	 */
	@Component("msmsEvalWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final MSMSEvalWorker worker = new MSMSEvalWorker();
			worker.setMsmsEvalExecutable(FileUtilities.getAbsoluteFileForExecutables(new File(config.getMsmsEvalExecutable())));
			return worker;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config extends WorkerConfig {
		private String msmsEvalExecutable;
		private String paramFiles;

		public Config() {
		}

		public Config(final String msmsEvalExecutable, final String paramFiles) {
			this.msmsEvalExecutable = msmsEvalExecutable;
			this.paramFiles = paramFiles;
		}

		public String getMsmsEvalExecutable() {
			return msmsEvalExecutable;
		}

		public void setMsmsEvalExecutable(final String msmsEvalExecutable) {
			this.msmsEvalExecutable = msmsEvalExecutable;
		}

		public String getParamFiles() {
			return paramFiles;
		}

		public void setParamFiles(final String paramFiles) {
			this.paramFiles = paramFiles;
		}

		public Map<String, String> save(final DependencyResolver resolver) {
			final Map<String, String> map = new TreeMap<String, String>();
			map.put(MSMS_EVAL_EXECUTABLE, msmsEvalExecutable);
			map.put(PARAM_FILES, paramFiles);
			return map;
		}

		public void load(final Map<String, String> values, final DependencyResolver resolver) {
			msmsEvalExecutable = values.get(MSMS_EVAL_EXECUTABLE);
			paramFiles = values.get(PARAM_FILES);
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {

		private static final String WINDOWS = "bin/msmseval/win/msmsEval.exe";
		private static final String LINUX = "bin/msmseval/linux_x86_64/msmsEval";
		private static final String LINUX_IA = "bin/msmseval/linux_i686/msmsEval";

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
