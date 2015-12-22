package edu.mayo.mprc.qa;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerBase;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.msmseval.MSMSEvalOutputReader;
import edu.mayo.mprc.myrimatch.MyriMatchPepXmlReader;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldQaSpectraReader;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.PercentDone;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Generates data files and image files representing QA data.
 */
public final class QaWorker extends WorkerBase {

	private static final Logger LOGGER = Logger.getLogger(QaWorker.class);
	public static final String TYPE = "qa";
	public static final String NAME = "Quality Assurance";
	// How many output files we produce per .mgf
	private static final int INITIAL_OUTPUT_FILES = 10;
	// Generating input files for the R script is considered 50% of all work(R script takes another 50%)
	private static final float PERCENT_GENERATING_FILES = 50.0f;
	// 100% - work complete
	private static final float COMPLETE = 100.0f;
	public static final String DESC = "Generates statistical information for analysis of the data adquisition process and the data search process.";

	private String rExecutable;
	private File rScript;
	private File xvfbWrapperScript;
	private String rtcMzOrder;
	private SpectrumInfoJoiner spectrumInfoJoiner;

	private static final String XVFB_WRAPPER_SCRIPT = "xvfbWrapperScript";
	private static final String R_SCRIPT = "rScript";
	private static final String R_EXECUTABLE = "rExecutable";
	private static final String RAWDUMP = "rawdump";

	@Override
	public void process(final WorkPacket workPacket, final File tempWorkFolder, final UserProgressReporter progressReporter) {
		final QaWorkPacket qaWorkPacket = (QaWorkPacket) workPacket;

		final File reportFile = qaWorkPacket.getReportFile();
		final File qaReportFolder = qaWorkPacket.getQaReportFolder();
		final String decoyRegex = qaWorkPacket.getDecoyRegex();
		FileUtilities.ensureFolderExists(qaReportFolder);

		final File rScriptInputFile = new File(qaReportFolder, "rInputData.tsv");

		FileWriter fileWriter = null;

		final LinkedList<File> generatedFileList = new LinkedList<File>();

		boolean atLeastOneFileMissing = !reportFile.exists();
		boolean writerIsClosed = false;

		try {
			fileWriter = new FileWriter(rScriptInputFile);

			fileWriter.write("Data File\tId File\tMz File\tIdVsMz File\tSource Current File\tmsmsEval Discriminant File\tGenerate Files\tRaw File\tmsmsEval Output\tRaw Info File\tRaw Spectra File\tPeptide Tolerance File\tTIC File\tChromatogram File\tUV Data File\tRTC Input File\tRTC Picture File");
			fileWriter.write("\n");

			final List<ExperimentQa> experimentQas = qaWorkPacket.getExperimentQas();

			int numFilesDone = 0;
			final int numFilesTotal = countTotalFiles(experimentQas);

			for (final ExperimentQa experimentQa : experimentQas) {
				final List<QaFiles> entries = experimentQa.getQaFiles();
				for (final QaFiles me : entries) {
					atLeastOneFileMissing |= addRScriptInputLine(fileWriter, qaReportFolder, experimentQa, generatedFileList, me);
					numFilesDone++;
					reportProgress(numFilesDone * PERCENT_GENERATING_FILES / numFilesTotal, progressReporter);
				}
			}

			FileUtilities.closeObjectQuietly(fileWriter);
			writerIsClosed = true;

			if (atLeastOneFileMissing) {
				LOGGER.info("Running R script [" + getRScript().getAbsolutePath() + "] for output file [" + reportFile + "]");
				runRScript(rScriptInputFile, reportFile, decoyRegex, getRtcMzOrder(), progressReporter);
			}

			for (final File file : generatedFileList) {
				if (!file.exists()) {
					throw new MprcException("Some of the output files for the QA have not been created, example: [" + file.getAbsolutePath() + "]");
				}
			}

			reportProgress(COMPLETE, progressReporter);
		} catch (final Exception e) {
			throw new MprcException("Processing of QA work packet failed.", e);
		} finally {
			if (!writerIsClosed) {
				FileUtilities.closeObjectQuietly(fileWriter);
			}
		}
	}

	/**
	 * TODO: We do not publish QA results properly just yet
	 */
	@Override
	public File createTempWorkFolder() {
		return null;
	}

	private boolean addRScriptInputLine(final FileWriter fileWriter, final File qaReportFolder, final ExperimentQa experimentQa,
	                                    final LinkedList<File> generatedFiles,
	                                    final QaFiles qaFiles) throws IOException {

		final File msmsEvalDiscriminantFile;
		final File ticFile;
		final File mgfFile = qaFiles.getInputFile();

		// The name of the analysis output file is the original .mgf name combined with Scaffold version (we used to support running two versions of Scaffold simultaneously, not anymore)
		final String uniqueMgfAnalysisName = getAnalysisName(mgfFile);
		final File outputFile = getSfsFileName(qaReportFolder, uniqueMgfAnalysisName);

		final List<File> rScriptOutputFilesSet = new ArrayList<File>(INITIAL_OUTPUT_FILES);

		final File massCalibrationRtFile = new File(qaReportFolder, uniqueMgfAnalysisName + ".calRt.png");
		final File massCalibrationMzFile = new File(qaReportFolder, uniqueMgfAnalysisName + ".calMz.png");
		final File mzRtFile = new File(qaReportFolder, uniqueMgfAnalysisName + ".mzRt.png");
		final File sourceCurrentFile = new File(qaReportFolder, uniqueMgfAnalysisName + ".current.png");
		final File pepTolFile = new File(qaReportFolder, uniqueMgfAnalysisName + ".pepTol.png");
		final File uvDataFile = new File(qaReportFolder, uniqueMgfAnalysisName + ".uv.png");
		final File rtcFile = new File(qaReportFolder, uniqueMgfAnalysisName + ".rtc.png");

		rScriptOutputFilesSet.add(massCalibrationRtFile);
		rScriptOutputFilesSet.add(massCalibrationMzFile);
		rScriptOutputFilesSet.add(mzRtFile);
		rScriptOutputFilesSet.add(sourceCurrentFile);
		rScriptOutputFilesSet.add(pepTolFile);
		rScriptOutputFilesSet.add(uvDataFile);

		//Do not add msmsEval file to list before checking if msmsEval is enabled.
		msmsEvalDiscriminantFile = new File(qaReportFolder, uniqueMgfAnalysisName + ".msmsEval.png");

		// Do not add anything that depends on RawDump unless we are sure we can provide the data
		ticFile = new File(qaReportFolder, uniqueMgfAnalysisName + ".tic.png");

		final MyriMatchPepXmlReader myrimatchReader = getMyriMatchReader(qaFiles.getAdditionalSearchResults());

		final long newestInputTime = getNewestInputTime(experimentQa, qaFiles);

		boolean atLeastOneFileMissing = false;
		boolean generate = false;

		if (!outputFile.exists() || outputFile.length() == 0 || outputFile.lastModified() < newestInputTime) {

			LOGGER.info("Generating output file [" + outputFile.getAbsolutePath() + "]");

			final ScaffoldQaSpectraReader scaffoldParser = new ScaffoldQaSpectraReader();
			scaffoldParser.load(experimentQa.getScaffoldSpectraFile(), experimentQa.getScaffoldVersion(), null);
			final RawDumpReader rawDumpReader = new RawDumpReader(qaFiles.getRawSpectraFile());
			final MSMSEvalOutputReader msmsEvalReader = new MSMSEvalOutputReader(qaFiles.getMsmsEvalOutputFile());
			final UvDataReader uvDataReader = new UvDataReader(qaFiles.getUvDataFile());
			final String rawInputFile = qaFiles.getRawInputFile() != null ? qaFiles.getRawInputFile().getAbsolutePath() : null;
			final FileSpectrumInfoSink sink = new FileSpectrumInfoSink(outputFile);
			generate = spectrumInfoJoiner.joinSpectrumData(
					mgfFile,
					scaffoldParser,
					rawDumpReader,
					msmsEvalReader,
					myrimatchReader,
					sink,
					uvDataReader,
					rawInputFile) > 0;

			if (generate) {
				atLeastOneFileMissing = true;
			}
		} else {
			LOGGER.info("Skipping creation of output file [" + outputFile.getAbsolutePath() + "] because it already exists.");

			//Check msmsEval files.
			if (qaFiles.getMsmsEvalOutputFile() != null && (!msmsEvalDiscriminantFile.exists() || msmsEvalDiscriminantFile.length() == 0)) {
				atLeastOneFileMissing = true;
				generate = true;
			}

			if (!generate) {
				for (final File file : rScriptOutputFilesSet) {
					if (!file.exists() || file.length() == 0) {
						atLeastOneFileMissing = true;
						generate = true;
						break;
					}
				}
			}
		}

		if (generate) {
			//If msmsEval is enabled, add it to the output file list.
			if (qaFiles.getMsmsEvalOutputFile() != null) {
				rScriptOutputFilesSet.add(msmsEvalDiscriminantFile);
			}

			// TIC file and others need rawDump output
			if (qaFiles.getRawInfoFile() != null && qaFiles.getRawSpectraFile() != null) {
				rScriptOutputFilesSet.add(ticFile);
			}

			if (qaFiles.getRtcFile() != null) {
				rScriptOutputFilesSet.add(rtcFile);
			}

			generatedFiles.addAll(rScriptOutputFilesSet);
		}

		final File chromatogramFile = qaFiles.getChromatogramFile();
		writeInputLine(fileWriter, outputFile, massCalibrationRtFile, massCalibrationMzFile, mzRtFile, sourceCurrentFile, msmsEvalDiscriminantFile, generate, qaFiles, pepTolFile, ticFile, chromatogramFile, uvDataFile, rtcFile);
		return atLeastOneFileMissing;
	}

	/**
	 * @param mgfFile File being visualized
	 * @return The root name for the output files, based on the name of the mgf file
	 */
	public static String getAnalysisName(final File mgfFile) {
		return FileUtilities.getFileNameWithoutExtension(mgfFile) + ".sf3";
	}

	/**
	 * We expose this function so the {@code QaTask} can determine if outputs already exist.
	 *
	 * @param qaReportFolder        Folder with outputs
	 * @param uniqueMgfAnalysisName Result of {@link #getAnalysisName}.
	 * @return Name of the .sfs file to be generated.
	 */
	public static File getSfsFileName(final File qaReportFolder, final String uniqueMgfAnalysisName) {
		return new File(qaReportFolder, uniqueMgfAnalysisName + ".sfs");
	}

	/**
	 * Go through all QA input files, find the modification time that is the newest.
	 *
	 * @param experimentQa Info from Scaffold
	 * @param qaFiles      Info about QA files for this particular input set
	 * @return Timestamp of the newest existing input file
	 */
	private long getNewestInputTime(final ExperimentQa experimentQa, final QaFiles qaFiles) {
		long newestInputTime = qaFiles.getNewestModificationDate();
		final long l = experimentQa.getScaffoldSpectraFile().lastModified();
		newestInputTime = Math.max(newestInputTime, l);
		return newestInputTime;
	}

	/**
	 * Find a myrimatch search engine results in the list and create a reader from them.
	 */
	private MyriMatchPepXmlReader getMyriMatchReader(final HashMap<String, File> additionalSearchResults) {
		for (final Map.Entry<String, File> entry : additionalSearchResults.entrySet()) {
			final String searchEngineCode = entry.getKey();
			if ("MYRIMATCH".equals(searchEngineCode)) {
				final MyriMatchPepXmlReader reader = new MyriMatchPepXmlReader();
				final File searchResult = entry.getValue();
				reader.load(FileUtilities.getInputStream(searchResult));
				return reader;
			}
		}
		return null;
	}

	private void writeInputLine(final FileWriter fileWriter, final File outputFile, final File idVsPpmFile, final File mzVsPpmFile, final File idVsMzFile, final File sourceCurrentFile, final File msmsEvalDiscriminantFile, final boolean generate, final QaFiles qaFiles, final File pepTolFile, final File ticFile, final File chromatogramFile, final File uvDataFile, final File rtcFile) throws IOException {
		fileWriter.write(outputFile.getAbsolutePath());
		fileWriter.write("\t");
		fileWriter.write(idVsPpmFile.getAbsolutePath());
		fileWriter.write("\t");
		fileWriter.write(mzVsPpmFile.getAbsolutePath());
		fileWriter.write("\t");
		fileWriter.write(idVsMzFile.getAbsolutePath());
		fileWriter.write("\t");
		fileWriter.write(sourceCurrentFile.getAbsolutePath());
		fileWriter.write("\t");
		fileWriter.write(msmsEvalDiscriminantFile.getAbsolutePath());
		fileWriter.write("\t");
		fileWriter.write(Boolean.toString(generate));
		fileWriter.write("\t");
		fileWriter.write(isDataFileValid(qaFiles.getRawInputFile()) ? qaFiles.getRawInputFile().getAbsolutePath() : "");
		fileWriter.write("\t");
		fileWriter.write(isDataFileValid(qaFiles.getMsmsEvalOutputFile()) ? qaFiles.getMsmsEvalOutputFile().getAbsolutePath() : "");
		fileWriter.write("\t");
		fileWriter.write(isDataFileValid(qaFiles.getRawInfoFile()) ? qaFiles.getRawInfoFile().getAbsolutePath() : "");
		fileWriter.write("\t");
		fileWriter.write(isDataFileValid(qaFiles.getRawSpectraFile()) ? qaFiles.getRawSpectraFile().getAbsolutePath() : "");
		fileWriter.write("\t");
		fileWriter.write(pepTolFile.getAbsolutePath());
		fileWriter.write("\t");
		fileWriter.write((isDataFileValid(qaFiles.getRawInfoFile()) && isDataFileValid(qaFiles.getRawSpectraFile())) ? ticFile.getAbsolutePath() : "");
		fileWriter.write("\t");
		fileWriter.write(chromatogramFile != null ? chromatogramFile.getAbsolutePath() : "");
		fileWriter.write("\t");
		fileWriter.write(uvDataFile != null ? uvDataFile.getAbsolutePath() : "");
		fileWriter.write("\t");
		fileWriter.write(isDataFileValid(qaFiles.getRtcFile()) ? qaFiles.getRtcFile().getAbsolutePath() : "");
		fileWriter.write("\t");
		fileWriter.write(rtcFile != null ? rtcFile.getAbsolutePath() : "");
		fileWriter.write("\n");
	}

	private int countTotalFiles(final List<ExperimentQa> experimentQas) {
		int numFilesTotal = 0;
		for (final ExperimentQa experimentQa : experimentQas) {
			numFilesTotal += experimentQa.getQaFiles().size();
		}
		return numFilesTotal;
	}

	private void reportProgress(final float percentDone, final UserProgressReporter progressReporter) {
		progressReporter.reportProgress(new PercentDone(percentDone));
	}

	/**
	 * @param file
	 * @return true if file exists and is not empty.
	 */
	private boolean isDataFileValid(final File file) {
		return file != null && file.exists() && file.length() > 0;
	}

	private void runRScript(final File inputFile, final File reportFile, final String decoyRegex, final String rtcMzOrder, final UserProgressReporter reporter) {

		final List<String> result = new ArrayList<String>();

		if (getXvfbWrapperScript() != null && FileUtilities.isLinuxPlatform()) {
			result.add(getXvfbWrapperScript().getAbsolutePath());
		}

		result.add(getRExecutable());
		result.add(getRScript().getAbsolutePath());
		result.add(inputFile.getAbsolutePath());
		result.add(reportFile.getAbsolutePath());
		result.add(decoyRegex);
		result.add(rtcMzOrder);

		final ProcessBuilder builder = new ProcessBuilder(result.toArray(new String[result.size()]));
		final ProcessCaller caller = new ProcessCaller(builder, reporter.getLog());
		caller.runAndCheck("QA R script");
	}

	public String getRExecutable() {
		return rExecutable;
	}

	public void setRExecutable(final String rExecutable) {
		this.rExecutable = rExecutable;
	}

	public File getRScript() {
		return rScript;
	}

	public void setRScript(final File rScript) {
		this.rScript = rScript;
	}

	public File getXvfbWrapperScript() {
		return xvfbWrapperScript;
	}

	public void setXvfbWrapperScript(final File xvfbWrapperScript) {
		this.xvfbWrapperScript = xvfbWrapperScript;
	}

	public String getRtcMzOrder() {
		return rtcMzOrder;
	}

	public void setRtcMzOrder(String rtcMzOrder) {
		this.rtcMzOrder = rtcMzOrder;
	}

	public SpectrumInfoJoiner getSpectrumInfoJoiner() {
		return spectrumInfoJoiner;
	}

	public void setSpectrumInfoJoiner(final SpectrumInfoJoiner spectrumInfoJoiner) {
		this.spectrumInfoJoiner = spectrumInfoJoiner;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("qaWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		private SpectrumInfoJoiner spectrumInfoJoiner;

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final QaWorker qaWorker = new QaWorker();
			qaWorker.setRExecutable(config.getrExecutable());
			qaWorker.setRScript(new File(config.getrScript()));
			final String xvfbWrapperScript = config.getXvfbWrapperScript();
			final String rtcOrder = ((RAWDumpWorker.Config) config.getRawDump().getRunner().getWorkerConfiguration()).getRtcPrecursorMzs();
			qaWorker.setXvfbWrapperScript(xvfbWrapperScript != null && !xvfbWrapperScript.isEmpty() ? new File(xvfbWrapperScript) : null);
			qaWorker.setSpectrumInfoJoiner(getSpectrumInfoJoiner());
			qaWorker.setRtcMzOrder(rtcOrder);
			return qaWorker;
		}

		public SpectrumInfoJoiner getSpectrumInfoJoiner() {
			return spectrumInfoJoiner;
		}

		@Resource(name = "spectrumInfoJoiner")
		public void setSpectrumInfoJoiner(final SpectrumInfoJoiner spectrumInfoJoiner) {
			this.spectrumInfoJoiner = spectrumInfoJoiner;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private String xvfbWrapperScript;
		private String rScript;
		private String rExecutable;
		private ServiceConfig rawDump;

		public Config() {
		}

		@Override
		public void save(ConfigWriter writer) {
			writer.put(R_EXECUTABLE, rExecutable);
			writer.put(XVFB_WRAPPER_SCRIPT, xvfbWrapperScript);
			writer.put(R_SCRIPT, rScript);
			writer.put(RAWDUMP, writer.save(rawDump));
		}

		@Override
		public void load(ConfigReader reader) {
			rExecutable = reader.get(R_EXECUTABLE);
			xvfbWrapperScript = reader.get(XVFB_WRAPPER_SCRIPT);
			rScript = reader.get(R_SCRIPT);
			rawDump = (ServiceConfig) reader.getObject(RAWDUMP);
		}

		@Override
		public int getPriority() {
			return 0;
		}

		public ServiceConfig getRawDump() {
			return rawDump;
		}

		public void setRawDump(ServiceConfig rawDump) {
			this.rawDump = rawDump;
		}

		public String getrExecutable() {
			return rExecutable;
		}

		public void setrExecutable(String rExecutable) {
			this.rExecutable = rExecutable;
		}

		public String getrScript() {
			return rScript;
		}

		public void setrScript(String rScript) {
			this.rScript = rScript;
		}

		public String getXvfbWrapperScript() {
			return xvfbWrapperScript;
		}

		public void setXvfbWrapperScript(String xvfbWrapperScript) {
			this.xvfbWrapperScript = xvfbWrapperScript;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String PROVIDED_R_SCRIPT = "bin/util/rPpmPlot.r";
		private static final String R_EXECUTABLE_DEFAULT = "Rscript";

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(R_EXECUTABLE, "<tt>R executable</tt>", "R script executable or interpreter that runs the given R script below. R must be installed in the system. " +
							"R installation packages can be found at <a href=\"http://www.r-project.org\"/>http://www.r-project.org</a>")
					.required()
					.defaultValue(R_EXECUTABLE_DEFAULT)

					.property(R_SCRIPT, "<tt>R script</tt> path", "R script that generates ppm analysis plots. " +
							"<p>For your convenience, a copy is in <tt>" + PROVIDED_R_SCRIPT + "</tt></p>")
					.required()
					.defaultValue(PROVIDED_R_SCRIPT)

					.property(XVFB_WRAPPER_SCRIPT, "X Window Wrapper Script",
							"<p>This is needed only for Linux. On Windows, leave this field blank.</p>"
									+ "<p>This wrapper script makes sure there is X window system set up and ready to be used by the <tt>R script</tt> (see above).</p>"
									+ "<p>We provide a script <tt>" + DaemonConfig.XVFB_CMD + "</tt> that does just that - feel free to modify it to suit your needs. "
									+ " The script uses <tt>Xvfb</tt> - X virtual frame buffer, so <tt>Xvfb</tt>"
									+ " has to be functional on the host system.</p>"
									+ "<p>If you do not require this functionality, leave the field blank.</p>")
					.executable(Arrays.asList("-v"))
					.defaultValue(daemon.getXvfbWrapperScript())

					.property(RAWDUMP, RAWDumpWorker.NAME, "RawDump that extracts the retention time calibration data")
					.reference(RAWDumpWorker.TYPE, UiBuilder.NONE_TYPE);

		}
	}
}
