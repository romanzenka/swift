package edu.mayo.mprc.qa;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.config.ui.WrapperScriptSwitcher;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerBase;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Worker extracts data from given raw file.
 */
public final class RAWDumpWorker extends WorkerBase {

	private static final Logger LOGGER = Logger.getLogger(RAWDumpWorker.class);

	public static final String RAW_FILE_CMD = "--raw";
	public static final String INFO_FILE_CMD = "--info";
	public static final String SPECTRA_FILE_CMD = "--spectra";
	public static final String CHROMATOGRAM_FILE_CMD = "--chromatogram";
	public static final String TUNE_FILE_CMD = "--tune";
	public static final String INSTRUMENT_METHOD_FILE_CMD = "--instrument";
	public static final String SAMPLE_INFORMATION_FILE_CMD = "--sample";
	public static final String ERROR_LOG_FILE_CMD = "--errorlog";
	public static final String PARAM_FILE_CMD = "--params";

	public static final String TYPE = "rawdump";
	public static final String NAME = "RAW Dump";
	public static final String DESC = "Extracts information about experiment and spectra from RAW files.";

	private File wrapperScript;
	private String windowsExecWrapperScript;

	private File rawDumpExecutable;
	private String commandLineOptions;

	private File tempParamFile;
	// If the raw file path is longer than this, we will attempt to shorten it
	private static final int MAX_UNSHORTENED_PATH_LENGTH = 100;

	protected RAWDumpWorker(final Config config) {
		setWrapperScript(config.getWrapperScript() != null && config.getWrapperScript().length() > 0 ? new File(config.getWrapperScript()) : null);
		setWindowsExecWrapperScript(config.getWindowsExecWrapperScript());
	}

	@Override
	public void process(final WorkPacket workPacket, UserProgressReporter progressReporter) {
		try {
			final RAWDumpWorkPacket rawDumpWorkPacket = (RAWDumpWorkPacket) workPacket;

			final File rawInfo = rawDumpWorkPacket.getRawInfoFile();
			final File rawSpectra = rawDumpWorkPacket.getRawSpectraFile();
			final File rawFile = rawDumpWorkPacket.getRawFile();
			final File chromatogramFile = rawDumpWorkPacket.getChromatogramFile();
			final File tuneFile = rawDumpWorkPacket.getTuneMethodFile();
			final File instrumentMethodFile = rawDumpWorkPacket.getInstrumentMethodFile();
			final File sampleInformationFile = rawDumpWorkPacket.getSampleInformationFile();
			final File errorLogFile = rawDumpWorkPacket.getErrorLogFile();

			FileUtilities.ensureFolderExists(rawInfo.getParentFile());
			FileUtilities.ensureFolderExists(rawSpectra.getParentFile());

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
					rawInfo, rawSpectra, chromatogramFile, tuneFile, instrumentMethodFile, sampleInformationFile, errorLogFile);
			final ProcessCaller caller = process(commandLine, true/*windows executable*/, wrapperScript, windowsExecWrapperScript);

			if (shortenedRawFile != null) {
				FileUtilities.cleanupShortenedPath(shortenedRawFile);
			}

			if (!isFileOk(rawInfo)) {
				throw new MprcException("Raw dump has failed to create raw info file: " + rawInfo.getAbsolutePath() + "\n" + caller.getFailedCallDescription());
			}
			if (!isFileOk(rawSpectra)) {
				throw new MprcException("Raw dump has failed to create raw spectra file: " + rawSpectra.getAbsolutePath() + "\n" + caller.getFailedCallDescription());
			}
		} finally {
			FileUtilities.deleteNow(tempParamFile);
		}
	}

	private static boolean isFileOk(final File file) {
		return file.exists() && file.isFile() && file.length() > 0;
	}

	private List<String> getCommandLine(final File rawFile, final File rawInfo, final File rawSpectra, final File chromatogramFile,
	                                    final File tuneFile, final File instrumentMethodFile, final File sampleInformationFile, final File errorLogFile) {

		createParamFile(rawFile, rawInfo, rawSpectra, chromatogramFile, tuneFile, instrumentMethodFile, sampleInformationFile, errorLogFile);

		final List<String> commandLineParams = new LinkedList<String>();
		commandLineParams.add(rawDumpExecutable.getAbsolutePath());
		commandLineParams.add(PARAM_FILE_CMD);
		commandLineParams.add(tempParamFile.getAbsolutePath());

		return commandLineParams;
	}

	private void createParamFile(final File rawFile, final File rawInfo, final File rawSpectra, final File chromatogramFile,
	                             final File tuneFile, final File instrumentMethodFile, final File sampleInformationFile, final File errorLogFile) {
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

		} catch (IOException e) {
			throw new MprcException("Failed to created param file: " + tempParamFile.getAbsolutePath() + ".", e);
		} finally {
			FileUtilities.closeObjectQuietly(bufferedWriter);
		}
	}

	private void addFile(final BufferedWriter bufferedWriter, final String commandLine, final File file) throws IOException {
		bufferedWriter.write(commandLine);
		bufferedWriter.write("\n");
		bufferedWriter.write(file.getAbsolutePath());
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

	/**
	 * Generic method that can execute a given command line, wrapping it properly on windows etc.
	 * TODO: This is coupled to how we process packets on Windows - simplify, clean up.
	 *
	 * @param wrapperScript        The outer script to wrap the command line call into.
	 * @param windowsWrapperScript In case our executable is a windows executable and we are not on a windows
	 *                             platform, this wrapper will turn the executable into something that would run.
	 *                             Typically this wrapper is a script that executes <c>wine</c> or <c>wineconsole</c>.
	 */
	static ProcessCaller process(final List<String> commandLine, final boolean isWindowsExecutable, final File wrapperScript, final String windowsWrapperScript) {
		final List<String> parameters = new ArrayList<String>();

		if (wrapperScript != null) {
			parameters.add(wrapperScript.getAbsolutePath());
		}

		if (isWindowsExecutable && windowsWrapperScript != null && !FileUtilities.isWindowsPlatform() && windowsWrapperScript.length() > 0) {
			parameters.add(windowsWrapperScript);
		}

		parameters.addAll(commandLine);

		LOGGER.info("Running command from the following parameters " + parameters.toString());

		final ProcessBuilder builder = new ProcessBuilder(parameters.toArray(new String[parameters.size()]));
		final ProcessCaller caller = new ProcessCaller(builder);
		caller.runAndCheck("rawdump");

		return caller;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("rawDumpWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final RAWDumpWorker worker = new RAWDumpWorker(config);

			//Raw dump values
			worker.setRawDumpExecutable(FileUtilities.getAbsoluteFileForExecutables(new File(config.getRawDumpExecutable())));
			worker.setCommandLineOptions(config.getCommandLineOptions());

			return worker;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private String wrapperScript;
		private String windowsExecWrapperScript;

		private String rawDumpExecutable;
		private String commandLineOptions;

		public Config() {
		}

		public Config(final String rawDumpExecutable, final String commandLineOptions) {
			this.rawDumpExecutable = rawDumpExecutable;
			this.commandLineOptions = commandLineOptions;
		}

		public String getWrapperScript() {
			return wrapperScript;
		}

		public String getWindowsExecWrapperScript() {
			return windowsExecWrapperScript;
		}

		public String getRawDumpExecutable() {
			return rawDumpExecutable;
		}

		public String getCommandLineOptions() {
			return commandLineOptions;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put("wrapperScript", wrapperScript);
			writer.put("windowsExecWrapperScript", windowsExecWrapperScript);
			writer.put("rawDumpExecutable", rawDumpExecutable);
			writer.put("commandLineOptions", commandLineOptions);
		}

		@Override
		public void load(final ConfigReader reader) {
			wrapperScript = reader.get("wrapperScript");
			windowsExecWrapperScript = reader.get("windowsExecWrapperScript");
			rawDumpExecutable = reader.get("rawDumpExecutable");
			commandLineOptions = reader.get("commandLineOptions");
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String WINDOWS_EXEC_WRAPPER_SCRIPT = "windowsExecWrapperScript";
		private static final String WRAPPER_SCRIPT = "wrapperScript";

		private static final String DEFAULT_RAWDUMP_EXEC = "bin/rawExtract/MprcExtractRaw.exe";
		private static final String DEFAULT_CMDS = "--data";

		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property("rawDumpExecutable", "Executable Path", "RAW Dump executable path."
					+ "<br/>The RAW Dump executable has been inplemented in house and is included with the Swift installation. "
					+ "<br/>Executable can be found in the Swift installation directory: "
					+ "<br/><tt>" + DEFAULT_RAWDUMP_EXEC + "</tt>").executable(Arrays.asList("-v"))
					.required()
					.defaultValue(DEFAULT_RAWDUMP_EXEC)

					.property("commandLineOptions", "Command Line Options",
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
		}

	}

}
