package edu.mayo.mprc.qa;

import com.google.common.base.*;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ResourceConfigBase;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.config.ui.WrapperScriptSwitcher;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerBase;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * Worker extracts data from given raw file.
 */
public final class RAWDumpWorker extends WorkerBase {

	private static final Logger LOGGER = Logger.getLogger(RAWDumpWorker.class);

	public static final String TYPE = "rawdump";
	public static final String NAME = "RAW Dump";
	public static final String DESC = "Extracts information about experiment and spectra from RAW files.";
	public static final String WRAPPER_SCRIPT = "wrapperScript";
	public static final String WINDOWS_EXEC_WRAPPER_SCRIPT = "windowsExecWrapperScript";
	public static final String RAW_DUMP_EXECUTABLE = "rawDumpExecutable";
	public static final String COMMAND_LINE_OPTIONS = "commandLineOptions";
	public static final String RTC_PRECURSOR_MZS = "rtcPrecursorMzs";
	public static final String RTC_PRECURSOR_RTS = "rtcPrecursorRts";
	public static final String RTC_PPM_MASS_TOL = "rtcPpmMassTol";
	public static final String RTC_MASS_ROUNDING = "rtcMassRounding";

	public static final String RAW_FILE_CMD = "--raw";
	public static final String INFO_FILE_CMD = "--info";
	public static final String SPECTRA_FILE_CMD = "--spectra";
	public static final String CHROMATOGRAM_FILE_CMD = "--chromatogram";
	public static final String TUNE_FILE_CMD = "--tune";
	public static final String INSTRUMENT_METHOD_FILE_CMD = "--instrument";
	public static final String SAMPLE_INFORMATION_FILE_CMD = "--sample";
	public static final String ERROR_LOG_FILE_CMD = "--errorlog";
	public static final String UV_FILE_CMD = "--uv";
	public static final String RTC_FILE_CMD = "--rtc";
	public static final String RTC_PRECURSOR_MZS_CMD = "--" + RTC_PRECURSOR_MZS;
	public static final String RTC_PPM_MASS_TOL_CMD = "--" + RTC_PPM_MASS_TOL;
	public static final String RTC_MASS_ROUNDING_CMD = "--" + RTC_MASS_ROUNDING;

	public static final String PARAM_FILE_CMD = "--params";

	private File wrapperScript;
	private String windowsExecWrapperScript;

	private File rawDumpExecutable;
	private String commandLineOptions;
	private Double[] rtcPrecursorMzs;
	private Double[] rtcPrecursorRts;
	private double rtcPpmMassTol;
	private int rtcMassRounding;

	private File tempParamFile;
	// If the raw file path is longer than this, we will attempt to shorten it
	private static final int MAX_UNSHORTENED_PATH_LENGTH = 100;

	protected RAWDumpWorker(final Config config) {
		final String wrapperScript = config.get(WRAPPER_SCRIPT);
		setWrapperScript(Strings.isNullOrEmpty(wrapperScript) ? null : new File(wrapperScript));
		setWindowsExecWrapperScript(config.get(WINDOWS_EXEC_WRAPPER_SCRIPT));
	}

	@Override
	public void process(final WorkPacket workPacket, final File tempWorkFolder, UserProgressReporter progressReporter) {
		try {
			final RAWDumpWorkPacket rawDumpWorkPacket = (RAWDumpWorkPacket) workPacket;

			final File rawFile = rawDumpWorkPacket.getRawFile();

			final File finalRawInfo = rawDumpWorkPacket.getRawInfoFile();
			final File finalRawSpectra = rawDumpWorkPacket.getRawSpectraFile();
			final File finalChromatogramFile = rawDumpWorkPacket.getChromatogramFile();
			final File finalTuneFile = rawDumpWorkPacket.getTuneMethodFile();
			final File finalInstrumentMethodFile = rawDumpWorkPacket.getInstrumentMethodFile();
			final File finalSampleInformationFile = rawDumpWorkPacket.getSampleInformationFile();
			final File finalErrorLogFile = rawDumpWorkPacket.getErrorLogFile();
			final File finalUvDataFile = rawDumpWorkPacket.getUvDataFile();
			final File finalRtcFile = rawDumpWorkPacket.getRtcFile();

			final File rawInfo = getTempOutputFile(tempWorkFolder, finalRawInfo);
			final File rawSpectra = getTempOutputFile(tempWorkFolder, finalRawSpectra);
			final File chromatogramFile = getTempOutputFile(tempWorkFolder, finalChromatogramFile);
			final File tuneFile = getTempOutputFile(tempWorkFolder, finalTuneFile);
			final File instrumentMethodFile = getTempOutputFile(tempWorkFolder, finalInstrumentMethodFile);
			final File sampleInformationFile = getTempOutputFile(tempWorkFolder, finalSampleInformationFile);
			final File errorLogFile = getTempOutputFile(tempWorkFolder, finalErrorLogFile);
			final File uvDataFile = getTempOutputFile(tempWorkFolder, finalUvDataFile);
			final File rtcFile = getTempOutputFile(tempWorkFolder, finalRtcFile);

			File shortenedRawFile = null;
			if (rawFile.getAbsolutePath().length() > MAX_UNSHORTENED_PATH_LENGTH) {
				try {
					shortenedRawFile = FileUtilities.shortenFilePath(rawFile);
				} catch (Exception ignore) {
					// SWALLOWED: Failed shortening does not necessarily mean a problem
					shortenedRawFile = null;
				}
			}

			final List<String> commandLine = getCommandLine(shortenedRawFile != null ? shortenedRawFile : rawFile,
					rawInfo, rawSpectra, chromatogramFile, tuneFile, instrumentMethodFile, sampleInformationFile,
					errorLogFile, uvDataFile, rtcFile, rtcPrecursorMzs, rtcPpmMassTol, rtcMassRounding);
			final ProcessCaller caller = process(commandLine, true/*windows executable*/, wrapperScript, windowsExecWrapperScript, progressReporter);

			if (rtcPrecursorMzs.length == 0) {
				// Make dummy rtc file since data is missing
				try {
					Files.write("Precursor m/z\tm/z Window\tScan ID\tBasePeakXIC\tTICXIC\n", rtcFile, Charsets.US_ASCII);
				} catch (IOException e) {
					throw new MprcException("Could not write out dummy rtc file " + rtcFile.getAbsolutePath(), e);
				}
			}

			if (shortenedRawFile != null) {
				FileUtilities.cleanupShortenedPath(shortenedRawFile);
			}

			if (!isFileOk(rawInfo)) {
				throw new MprcException("Raw dump has failed to create raw info file: " + rawInfo.getAbsolutePath() + '\n' + caller.getFailedCallDescription());
			}
			if (!isFileOk(rawSpectra)) {
				throw new MprcException("Raw dump has failed to create raw spectra file: " + rawSpectra.getAbsolutePath() + '\n' + caller.getFailedCallDescription());
			}

			publish(rawInfo, finalRawInfo);
			publish(rawSpectra, finalRawSpectra);
			publish(chromatogramFile, finalChromatogramFile);
			publish(tuneFile, finalTuneFile);
			publish(instrumentMethodFile, finalInstrumentMethodFile);
			publish(sampleInformationFile, finalSampleInformationFile);
			publish(errorLogFile, finalErrorLogFile);
			publish(uvDataFile, finalUvDataFile);
			publish(rtcFile, finalRtcFile);

		} finally {
			FileUtilities.deleteNow(tempParamFile);
		}
	}

	private static boolean isFileOk(final File file) {
		return file.exists() && file.isFile() && file.length() > 0;
	}

	private List<String> getCommandLine(final File rawFile, final File rawInfo, final File rawSpectra, final File chromatogramFile,
	                                    final File tuneFile, final File instrumentMethodFile, final File sampleInformationFile,
	                                    final File errorLogFile, final File uvDataFile, final File rtcFile, Double[] rtcPrecursorMzs, double rtcPpmMassTol, int rtcMassRounding) {

		createParamFile(rawFile, rawInfo, rawSpectra,
				chromatogramFile, tuneFile, instrumentMethodFile,
				sampleInformationFile, errorLogFile, uvDataFile, rtcFile, rtcPrecursorMzs, rtcPpmMassTol, rtcMassRounding);

		final List<String> commandLineParams = new LinkedList<String>();
		commandLineParams.add(rawDumpExecutable.getAbsolutePath());
		commandLineParams.add(PARAM_FILE_CMD);
		commandLineParams.add(tempParamFile.getAbsolutePath());

		return commandLineParams;
	}

	private void createParamFile(final File rawFile, final File rawInfo, final File rawSpectra, final File chromatogramFile,
	                             final File tuneFile, final File instrumentMethodFile, final File sampleInformationFile, final File errorLogFile,
	                             final File uvDataFile, final File rtcFile, Double[] rtcPrecursorMzs, double rtcPpmMassTol, int rtcMassRounding) {
		try {
			tempParamFile = File.createTempFile("inputParamFile", null);
		} catch (IOException e) {
			throw new MprcException("Could not create temporary file for RawDump parameters", e);
		}

		LOGGER.info("Creating parameter file: " + tempParamFile.getAbsolutePath() + ".");

		BufferedWriter bufferedWriter = null;

		try {
			bufferedWriter = new BufferedWriter(new FileWriter(tempParamFile));

			final StringTokenizer stringTokenizer = new StringTokenizer(commandLineOptions, ",");

			while (stringTokenizer.hasMoreTokens()) {
				bufferedWriter.write(stringTokenizer.nextToken().trim());
				bufferedWriter.write("\n");
			}

			addFile(bufferedWriter, RAW_FILE_CMD, rawFile);
			addFile(bufferedWriter, INFO_FILE_CMD, rawInfo);
			addFile(bufferedWriter, SPECTRA_FILE_CMD, rawSpectra);
			addFile(bufferedWriter, CHROMATOGRAM_FILE_CMD, chromatogramFile);
			addFile(bufferedWriter, TUNE_FILE_CMD, tuneFile);
			addFile(bufferedWriter, INSTRUMENT_METHOD_FILE_CMD, instrumentMethodFile);
			addFile(bufferedWriter, SAMPLE_INFORMATION_FILE_CMD, sampleInformationFile);
			addFile(bufferedWriter, ERROR_LOG_FILE_CMD, errorLogFile);
			addFile(bufferedWriter, UV_FILE_CMD, uvDataFile);

			if (rtcPrecursorMzs.length > 0) {
				addFile(bufferedWriter, RTC_FILE_CMD, rtcFile);
				addKeyValue(bufferedWriter, RTC_PRECURSOR_MZS_CMD, Joiner.on(':').join(rtcPrecursorMzs));
				addKeyValue(bufferedWriter, RTC_PPM_MASS_TOL_CMD, String.valueOf(rtcPpmMassTol));
				addKeyValue(bufferedWriter, RTC_MASS_ROUNDING_CMD, String.valueOf(String.valueOf(rtcMassRounding)));
			}

		} catch (IOException e) {
			throw new MprcException("Failed to created param file: " + tempParamFile.getAbsolutePath() + ".", e);
		} finally {
			FileUtilities.closeObjectQuietly(bufferedWriter);
		}
	}

	private void addFile(final BufferedWriter bufferedWriter, final String commandLine, final File file) throws IOException {
		addKeyValue(bufferedWriter, commandLine, file.getAbsolutePath());
	}

	private void addKeyValue(BufferedWriter bufferedWriter, String commandLine, String value) throws IOException {
		bufferedWriter.write(commandLine);
		bufferedWriter.write("\n");
		bufferedWriter.write(value);
		bufferedWriter.write("\n");
	}

	public File getWrapperScript() {
		return wrapperScript;
	}

	public void setWrapperScript(final File wrapperScript) {
		this.wrapperScript = wrapperScript;
	}

	public String getWindowsExecWrapperScript() {
		return windowsExecWrapperScript;
	}

	public void setWindowsExecWrapperScript(final String windowsExecWrapperScript) {
		this.windowsExecWrapperScript = windowsExecWrapperScript;
	}

	public File getRawDumpExecutable() {
		return rawDumpExecutable;
	}

	public void setRawDumpExecutable(final File rawDumpExecutable) {
		this.rawDumpExecutable = rawDumpExecutable;
	}

	public String getCommandLineOptions() {
		return commandLineOptions;
	}

	public void setCommandLineOptions(final String commandLineOptions) {
		this.commandLineOptions = commandLineOptions;
	}

	public int getRtcMassRounding() {
		return rtcMassRounding;
	}

	public void setRtcMassRounding(int rtcMassRounding) {
		this.rtcMassRounding = rtcMassRounding;
	}

	public double getRtcPpmMassTol() {
		return rtcPpmMassTol;
	}

	public void setRtcPpmMassTol(double rtcPpmMassTol) {
		this.rtcPpmMassTol = rtcPpmMassTol;
	}

	public Double[] getRtcPrecursorMzs() {
		return rtcPrecursorMzs;
	}

	public void setRtcPrecursorMzs(Double[] rtcPrecursorMzs) {
		this.rtcPrecursorMzs = rtcPrecursorMzs;
	}

	public Double[] getRtcPrecursorRts() {
		return rtcPrecursorRts;
	}

	public void setRtcPrecursorRts(Double[] rtcPrecursorRts) {
		this.rtcPrecursorRts = rtcPrecursorRts;
	}

	/**
	 * Generic method that can execute a given command line, wrapping it properly on windows etc.
	 * TODO: This is coupled to how we process packets on Windows - simplify, clean up.
	 *
	 * @param wrapperScript        The outer script to wrap the command line call into.
	 * @param windowsWrapperScript In case our executable is a windows executable and we are not on a windows
	 *                             platform, this wrapper will turn the executable into something that would run.
	 *                             Typically this wrapper is a script that executes <c>wine</c> or <c>wineconsole</c>.
	 */
	static ProcessCaller process(final List<String> commandLine, final boolean isWindowsExecutable, final File wrapperScript, final String windowsWrapperScript, final UserProgressReporter reporter) {
		final List<String> parameters = new ArrayList<String>();

		if (wrapperScript != null) {
			parameters.add(wrapperScript.getAbsolutePath());
		}

		if (isWindowsExecutable && windowsWrapperScript != null && !FileUtilities.isWindowsPlatform() && !windowsWrapperScript.isEmpty()) {
			parameters.add(windowsWrapperScript);
		}

		parameters.addAll(commandLine);

		LOGGER.info("Running command from the following parameters " + parameters.toString());

		final ProcessBuilder builder = new ProcessBuilder(parameters.toArray(new String[parameters.size()]));
		final ProcessCaller caller = new ProcessCaller(builder, reporter.getLog());
		caller.runAndCheck("rawdump");

		return caller;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("rawDumpWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final RAWDumpWorker worker = new RAWDumpWorker(config);

			//Raw dump values
			worker.setRawDumpExecutable(FileUtilities.getAbsoluteFileForExecutables(new File(config.get(RAW_DUMP_EXECUTABLE))));
			worker.setCommandLineOptions(config.get(COMMAND_LINE_OPTIONS));
			worker.setRtcPpmMassTol(Double.parseDouble(config.get(RTC_PPM_MASS_TOL)));
			worker.setRtcMassRounding(Integer.parseInt(config.get(RTC_MASS_ROUNDING)));

			worker.setRtcPrecursorMzs(stringToDoubleArray(config, RTC_PRECURSOR_MZS));
			worker.setRtcPrecursorRts(stringToDoubleArray(config, RTC_PRECURSOR_RTS));
			if (worker.getRtcPrecursorMzs().length != worker.getRtcPrecursorRts().length) {
				throw new MprcException(MessageFormat.format("The number of m/z values from {0} ({1}) does not match the number of retention times from {2} ({3})",
						RTC_PRECURSOR_MZS, worker.getRtcPrecursorMzs().length,
						RTC_PRECURSOR_RTS, worker.getRtcPrecursorRts().length));
			}

			return worker;
		}

	}

	/**
	 * Convert comma-delimited string of numbers into an array of numbers
	 *
	 * @param config Config to take the values from
	 * @param prop   Config property name
	 * @return
	 */
	public static Double[] stringToDoubleArray(final Config config, final String prop) {
		try {
			final Iterable<String> split = Splitter.on(':').omitEmptyStrings().split(config.get(prop));
			final Iterable<Double> result = Iterables.transform(split, new StringToPositiveDouble());
			return Iterables.toArray(result, Double.class);

		} catch (Exception e) {
			throw new MprcException("Incorrect " + prop + " parameter value", e);
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config extends ResourceConfigBase {
		public Config() {
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String WINDOWS_EXEC_WRAPPER_SCRIPT = RAWDumpWorker.WINDOWS_EXEC_WRAPPER_SCRIPT;
		private static final String WRAPPER_SCRIPT = RAWDumpWorker.WRAPPER_SCRIPT;

		private static final String DEFAULT_RAWDUMP_EXEC = "bin/rawExtract/MprcExtractRaw.exe";
		private static final String DEFAULT_CMDS = "--data";

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(RAW_DUMP_EXECUTABLE, "Executable Path", "RAW Dump executable path."
					+ "<br/>The RAW Dump executable has been inplemented in house and is included with the Swift installation. "
					+ "<br/>Executable can be found in the Swift installation directory: "
					+ "<br/><tt>" + DEFAULT_RAWDUMP_EXEC + "</tt>").executable(Arrays.asList("-v"))
					.required()
					.defaultValue(DEFAULT_RAWDUMP_EXEC)

					.property(COMMAND_LINE_OPTIONS, "Command Line Options",
							"<br/>Command line option --data is required for this application to generate RAW file information related files. Multiple command line options must be separated by commas.")
					.required()
					.defaultValue(DEFAULT_CMDS);

			builder.property(WINDOWS_EXEC_WRAPPER_SCRIPT, "Windows Program Wrapper Script",
					"<p>This is needed only for Linux when running Windows executables. On Windows, leave this field blank.</p>" +
							"<p>A wrapper script takes the Windows command as a parameter and executes on the Linux Platform.</p>"
							+ "<p>On Linux we suggest using <tt>" + DaemonConfig.WINECONSOLE_CMD + "</tt>. You need to have X Window System installed for <tt>" + DaemonConfig.WINECONSOLE_CMD
							+ "</tt> to work, or use the X virtual frame buffer for headless operation (see below).</p>"
							+ "<p>Alternatively, use <tt>" + DaemonConfig.WINE_CMD + "</tt> without need to run X, but in our experience <tt>" + DaemonConfig.WINE_CMD + "</tt> is less stable.</p>")
					.executable(Arrays.asList("-v"))
					.defaultValue(daemon.getWrapperScript())

					.property(WRAPPER_SCRIPT, "Wrapper Script",
							"<p>This an optional wrapper script in case some pre-processing and set up is needed before running command, for example, this is needed for Linux if the command"
									+ " to run is a Windows executable.</p><p>Default values are set up to allowed Windows executables to run in Linux.</p>"
									+ "<p>The default wrapper script makes sure there is X window system set up and ready to be used by <tt>wineconsole</tt> (see above).</p>"
									+ "<p>We provide a script <tt>" + DaemonConfig.XVFB_CMD + "</tt> that does just that - feel free to modify it to suit your needs. "
									+ " The script uses <tt>Xvfb</tt> - X virtual frame buffer, so <tt>Xvfb</tt>"
									+ " has to be functional on the host system.</p>"
									+ "<p>If you do not require this functionality, leave the field blank.</p>")
					.executable(Arrays.asList("-v"))
					.defaultValue(daemon.getXvfbWrapperScript())
					.addDaemonChangeListener(new WrapperScriptSwitcher(resource, daemon, WINDOWS_EXEC_WRAPPER_SCRIPT));

			builder.property(RTC_PRECURSOR_MZS, "Retention Time Calibration precursor m/zs",
					"A colon separated list of precursor m/z values to be extracted for retention time calibration plot")
					.defaultValue("")

					.property(RTC_PRECURSOR_RTS, "Retention Time Calibration retention times (minutes)",
							"A colon separated list of expected retention times. Must match the precursor m/zs list")
					.defaultValue("")

					.property(RTC_PPM_MASS_TOL, "Retention Time Calibration tolerance (ppm)",
							"+/- ppm around the precursor to be used for extracting the chromatogram")
					.defaultValue("10")
					.required()

					.property(RTC_MASS_ROUNDING, "Retention Time Calibration mass rounding",
							"How many decimal places to use when outputting the m/z values")
					.defaultValue("2")
					.required();
		}

	}

	private static class StringToPositiveDouble implements Function<String, Double> {
		@Override
		public Double apply(final String from) {
			final double v = Double.parseDouble(from);
			if (v <= 0.0) {
				throw new MprcException("The values have to be > 0, was " + from);
			}
			return v;
		}
	}
}
