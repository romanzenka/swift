package edu.mayo.mprc.msconvert;

import com.google.common.collect.Lists;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.WorkerConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerBase;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.utilities.FilePathShortener;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Calls <tt>msaccess.exe</tt> to determine whether peak picking should be enabled.
 * Then calls <tt>msconvert.exe</tt>.
 */
public final class MsconvertWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(MsconvertWorker.class);
	public static final String TYPE = "msconvert";
	public static final String NAME = "Msconvert";
	public static final String DESC = "<p>Converts Thermo's .RAW files to Mascot Generic Format (.mgf) using ProteoWizard's <tt>msconvert</tt>. " +
			"Without this module, Swift cannot process <tt>.RAW</tt> files.</p>" +
			"<p>You need to supply scripts capable of executing <tt>msconvert.exe</tt> and <tt>msaccess.exe</tt>, which is not trivial on Linux.</p>" +
			"<p>See <a href=\"https://github.com/jmchilton/proteomics-wine-env\">https://github.com/jmchilton/proteomics-wine-env</a> for information how to run ProteoWizard under <tt>wine</tt>.</p>";
	/**
	 * Maximum size of msconvert/msaccess .RAW file path.
	 */
	public static final int MAX_MSACCESS_PATH_SIZE = 120;
	public static final String MSACCESS_SUFFIX = ".metadata.txt";

	private File msconvertExecutable;
	private File msaccessExecutable;

	public static final String MSCONVERT_EXECUTABLE = "msconvertExecutable";
	public static final String MSACCESS_EXECUTABLE = "msaccessExecutable";

	// Format in which we expect the .mgf titles to be done
	private static final String TITLE_FILTER = "titleMaker %FILENAME% scan <ScanNumber> <ScanNumber> (%FILENAME%.<ScanNumber>.<ScanNumber>.<ChargeState>.dta)";

	@Override
	public void process(final WorkPacket workPacket, final UserProgressReporter progressReporter) {
		if (!(workPacket instanceof MsconvertWorkPacket)) {
			ExceptionUtilities.throwCastException(workPacket, MsconvertWorkPacket.class);
			return;
		}

		final MsconvertWorkPacket batchWorkPacket = (MsconvertWorkPacket) workPacket;

		//	steps are
		//	call msaccess and determine whether the MS2 data is in profile mode
		//	call msconvert with one or the other parameter set, based on what kind of MS2 data we have

		final File mgfFile = batchWorkPacket.getOutputFile().getAbsoluteFile();

		LOGGER.debug("msconvert: starting conversion " + batchWorkPacket.getInputFile() + " -> " + mgfFile);

		File rawFile = getRawFile(batchWorkPacket);

		//  check if already exists (skip condition)
		if (isConversionDone(batchWorkPacket, rawFile)) {
			return;
		}

		final FilePathShortener shortener = new FilePathShortener(rawFile, MAX_MSACCESS_PATH_SIZE);
		rawFile = shortener.getShortenedFile();

		final File tempFolder = FileUtilities.createTempFolder();
		try {
			final boolean ms2Profile = ms2SpectraInProfileMode(rawFile, tempFolder);

			ProcessBuilder builder = new ProcessBuilder(getMsconvertCall(rawFile, mgfFile, ms2Profile));
			builder.directory(rawFile.getParentFile());
			ProcessCaller caller = new ProcessCaller(builder);
			caller.runAndCheck("msconvert");
			if (!mgfFile.exists() || !mgfFile.isFile() || !mgfFile.canRead()) {
				throw new MprcException("msconvert failed to create file: " + mgfFile.getAbsolutePath());
			}
		} finally {
			FileUtilities.deleteNow(tempFolder);
			shortener.cleanup();
		}
	}

	/**
	 * Return the command line to execute msconvert.
	 *
	 * @param rawFile    Raw file to convert.
	 * @param mgfFile    The resulting mgf file.
	 * @param ms2Profile True if the MS2 data are in profile mode.
	 * @return Command to execute
	 */
	private List<String> getMsconvertCall(File rawFile, File mgfFile, boolean ms2Profile) {
		List<String> command = new ArrayList<String>();
		// /mnt/mprc/instruments/QE1/Z10_qe1_2012october/qe1_2012oct8_02_100_yeast_t10.raw --mgf --filter "chargeStatePredictor false 4 2 0.9"
		// --filter "peakPicking true 2-" --filter "threshold absolute 0.1 most-intense"   --outfile qe1_2012oct8_02_100_yeast_t10.mgf --outdir ~
		command.add(msconvertExecutable.getPath());
		command.add(rawFile.getName()); // .raw file to convert
		command.add("--mgf"); // We want to convert to .mgf

		command.add("--filter"); // Charge state predictor
		command.add("chargeStatePredictor false 4 2 0.9");

		if(rawFile.getName().endsWith(".d")) {
			command.add("--filter");
			command.add("peakPicking true 1-");
		}

		if (ms2Profile) {
			command.add("--filter");
			command.add("peakPicking true 2-"); // Do peak picking on MS2 and higher

			command.add("--filter");
			command.add("threshold absolute 0.1 most-intense"); // The peak-picked data have a lot of 0-intensity peaks. toss those
		}

		// Make proper .mgf titles that Swift needs
		final String filename = FileUtilities.getFileNameWithoutExtension(rawFile);
		command.add("--filter");
		command.add("titleMaker " + filename + " scan <ScanNumber> <ScanNumber> (" + filename + ".<ScanNumber>.<ScanNumber>.<ChargeState>.dta)");

		command.add("--outfile");
		command.add(mgfFile.getName());

		command.add("--outdir");
		command.add(mgfFile.getParent());

		return command;
	}

	/**
	 * Determine whether the given raw file has ms2 spectar in profile mode.
	 *
	 * @param rawFile    Which file to check.
	 * @param tempFolder Where to put temporary data.
	 */
	private boolean ms2SpectraInProfileMode(final File rawFile, final File tempFolder) {
		final ProcessBuilder builder = new ProcessBuilder(msaccessExecutable.getPath(), "-x", "metadata", "-o", tempFolder.getAbsolutePath(), rawFile.getName());
		builder.directory(rawFile.getParentFile().getAbsoluteFile());
		final ProcessCaller caller = new ProcessCaller(builder);
		caller.runAndCheck("msaccess");

		final File expectedResultFile = new File(tempFolder, rawFile.getName() + MSACCESS_SUFFIX);
		if (!expectedResultFile.exists() || !expectedResultFile.isFile() || !expectedResultFile.canRead()) {
			throw new MprcException("msaccess failed to create file: " + expectedResultFile.getAbsolutePath());
		}
		try {
			final MsaccessMetadataParser parser = new MsaccessMetadataParser(expectedResultFile);
			parser.process();
			return parser.isOrbitrapForMs2();
		} finally {
			FileUtilities.quietDelete(expectedResultFile);
		}
	}

	private static boolean isConversionDone(final MsconvertWorkPacket batchWorkPacket, final File rawFile) {
		//  check if already exists (skip condition)
		if (batchWorkPacket.isSkipIfExists()) {
			final File mgfFile = batchWorkPacket.getOutputFile();
			if (mgfFile.exists() && mgfFile.lastModified() >= rawFile.lastModified()) {
				LOGGER.info(rawFile.getAbsolutePath() + " conversion already done.");
				return true;
			}
		}
		return false;
	}

	private static File getRawFile(final MsconvertWorkPacket batchWorkPacket) {
		final File rawFile = batchWorkPacket.getInputFile();

		// check that we got real raw file to work with
		checkRawFile(rawFile);

		return rawFile;
	}

	private static void checkRawFile(final File pFile) {
		if (pFile.exists()) {
			if (pFile.isDirectory() && !pFile.getName().endsWith(".d")) {
				throw new DaemonException("Raw to MGF convertor cannot convert a directory");
			}
		} else {
			throw new DaemonException("The file " + pFile.getAbsolutePath() + " cannot be found.");
		}
	}

	public String toString() {
		return MessageFormat.format("Batch conversion:\n\tmsconvert={0}\n\tnsaccess={1}", msconvertExecutable.getPath(), msaccessExecutable.getPath());
	}

	public File getMsconvertExecutable() {
		return msconvertExecutable;
	}

	public void setMsconvertExecutable(final File msconvertExecutable) {
		this.msconvertExecutable = msconvertExecutable;
	}

	public File getMsaccessExecutable() {
		return msaccessExecutable;
	}

	public void setMsaccessExecutable(final File msaccessExecutable) {
		this.msaccessExecutable = msaccessExecutable;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("msconvertWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final MsconvertWorker worker = new MsconvertWorker();
			worker.setMsaccessExecutable(new File(config.getMsaccessExecutable()));
			worker.setMsconvertExecutable(new File(config.getMsconvertExecutable()));
			return worker;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config extends WorkerConfig {

		private String msconvertExecutable;
		private String msaccessExecutable;

		public Config() {
		}

		public Config(final String msconvertExecutable, final String msaccessExecutable) {
			this.msconvertExecutable = msconvertExecutable;
			this.msaccessExecutable = msaccessExecutable;
		}

		public String getMsconvertExecutable() {
			return msconvertExecutable;
		}

		public void setMsconvertExecutable(final String msconvertExecutable) {
			this.msconvertExecutable = msconvertExecutable;
		}

		public String getMsaccessExecutable() {
			return msaccessExecutable;
		}

		public void setMsaccessExecutable(final String msaccessExecutable) {
			this.msaccessExecutable = msaccessExecutable;
		}

		public Map<String, String> save(final DependencyResolver resolver) {
			final Map<String, String> map = new TreeMap<String, String>();
			map.put(MSCONVERT_EXECUTABLE, getMsconvertExecutable());
			map.put(MSACCESS_EXECUTABLE, getMsaccessExecutable());
			return map;
		}

		public void load(final Map<String, String> values, final DependencyResolver resolver) {
			setMsconvertExecutable(values.get(MSCONVERT_EXECUTABLE));
			setMsaccessExecutable(msaccessExecutable = values.get(MSACCESS_EXECUTABLE));
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder

					.property(MSCONVERT_EXECUTABLE, "<tt>msconvert.exe</tt> path", "Location of ProteoWizard's <tt>msconvert.exe</tt>."
							+ "<p>Use a wrapper script when running on Linux that takes care of calling Wine.</p>")
					.required()
					.executable(Lists.<String>newArrayList())
					.defaultValue("msconvert.exe")

					.property(MSACCESS_EXECUTABLE, "<tt>msaccess.exe</tt> path", "Location of ProteoWizard's <tt>msaccess.exe</tt>."
							+ "<p><tt>msaccess</tt> is used to determine whether peak picking should be enabled.</p>" +
							"<p>Use a wrapper script when running on Linux that takes care of calling Wine.</p>")
					.required()
					.executable(Lists.<String>newArrayList())
					.defaultValue("msaccess.exe");
		}
	}
}
