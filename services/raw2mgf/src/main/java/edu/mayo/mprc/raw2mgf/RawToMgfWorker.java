package edu.mayo.mprc.raw2mgf;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.config.ui.WrapperScriptSwitcher;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerBase;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.utilities.FilePathShortener;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static java.util.Arrays.sort;

/**
 * When running extract_msn, we must not extract more than about 10000 spectra at a time
 * (the Windows filesystem fails to have more than 65535 directory entries, long file names can break this
 * limit easily).
 * <p/>
 * We filter out the -F and -L parameters out in {@link #cleanupFromToParams} (they specify first and last spectrum) and we substitute our
 * own limits, running extract_msn in multiple passes. We combine the resulting spectra into a .mgf file.
 */
public final class RawToMgfWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(RawToMgfWorker.class);
	public static final String TYPE = "raw2mgf";
	public static final String NAME = "Extract_msn";
	public static final String DESC = "<p>Converts Thermo's .RAW files to Mascot Generic Format (.mgf). Without this module, Swift cannot process <tt>.RAW</tt> files.</p><p>We are using <tt>extract_msn</tt> from XCalibur, which is a windows program. On Linux we execute extract_msn using <tt>wine</tt>, which has to be installed prior to using the convertor.</p>";
	private File tempFolder = new File(".");
	private File extractMsnExecutable;
	private String wrapperScript;
	private File xvfbWrapperScript;
	private int spectrumBatchSize = 8000;

	public static final String TEMP_FOLDER = "tempFolder";
	public static final String EXTRACT_MSN_EXECUTABLE = "extractMsnExecutable";
	private static final String WRAPPER_SCRIPT = "wrapperScript";
	private static final String XVFB_WRAPPER_SCRIPT = "xvfbWrapperScript";

	private static final int MAX_RAW_PATH_LENGTH = 100;

	private static final Pattern FIRST_LAST_SPECTRUM = Pattern.compile("^-[FfLl]\\d*$");

	public static File[] getDtaFiles(final File dtaFolder) {
		if (!dtaFolder.isDirectory()) {
			throw new MprcException("Dat file location is not a directory: " + dtaFolder.getAbsolutePath());
		}

		final File[] files = dtaFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return name.endsWith(".dta");
			}
		});
		// sort them to aid unit testing
		sort(files, new DtaComparator());
		return files;
	}

	@Override
	public void process(final WorkPacket workPacket, final UserProgressReporter progressReporter) {
		if (!(workPacket instanceof RawToMgfWorkPacket)) {
			throw new DaemonException("Unknown request type: " + workPacket.getClass().getName());
		}

		final RawToMgfWorkPacket batchWorkPacket = (RawToMgfWorkPacket) workPacket;

		//	steps are
		//	call the extract_msn utility to convert from raw to dta
		//	call dta to mgf converter to make the mgf file

		final File mgfFile = batchWorkPacket.getOutputFile();

		final String origParams = batchWorkPacket.getParams();
		final Long firstSpectrum = getParamValue("-F", origParams);
		final Long lastSpectrum = getParamValue("-L", origParams);
		final String params = cleanupFromToParams(origParams);

		LOGGER.info("Raw2mgf: starting conversion " + batchWorkPacket.getInputFile() + " -> " + mgfFile + " (params: " + params + ")");

		File rawFile = getRawFile(batchWorkPacket);

		//  check if already exists (skip condition)
		if (isConversionDone(batchWorkPacket, rawFile)) {
			return;
		}

		// will use the temporary folder to get dta files then make mgf file in the
		// output_dir
		FileUtilities.ensureFolderExists(tempFolder);
		final File fulltempfolder = getMirrorFolderonTemp(tempFolder);
		FileUtilities.ensureFolderExists(fulltempfolder);

		FilePathShortener shortener = new FilePathShortener(rawFile, MAX_RAW_PATH_LENGTH);
		rawFile = shortener.getShortenedFile();

		long currentSpectrum = firstSpectrum == null ? 1 : firstSpectrum;
		long lastSpectrumInBatch = currentSpectrum + spectrumBatchSize - 1;

		try {
			final File executable = getExtractMsnExecutable();

			FileUtilities.ensureFolderExists(mgfFile.getParentFile());

			long totalSpectraExtracted = 0;
			while (currentSpectrum <= lastSpectrumInBatch) {
				runExtractMsnJob(executable, fulltempfolder, params, rawFile, currentSpectrum, lastSpectrumInBatch, wrapperScript, xvfbWrapperScript == null ? null : xvfbWrapperScript.getAbsolutePath());

				// Extract .dta files
				File[] dtaFiles = getDtaFiles(fulltempfolder);

				// Terminate if we could not find any .dta anymore
				if (dtaFiles.length == 0) {
					if (totalSpectraExtracted == 0) {
						throw new MprcException("There were no .dta files present in " + rawFile.getAbsolutePath());
					}
					break;
				}

				// Combine .dta to .mgf
				final boolean wine = wrapperScript != null && wrapperScript.length() > 0;
				dtaToMgf(fulltempfolder, dtaFiles, mgfFile, wine && !FileUtilities.isWindowsPlatform());

				// Determine next batch position
				totalSpectraExtracted += dtaFiles.length;
				currentSpectrum = lastSpectrumInBatch + 1;
				lastSpectrumInBatch = currentSpectrum + spectrumBatchSize - 1;
				if (lastSpectrum != null) {
					lastSpectrumInBatch = Math.min(lastSpectrumInBatch, lastSpectrum);
				}
			}
		} catch (Exception we) {
			throw new DaemonException("Error extracting dta files from " + batchWorkPacket.getInputFile(), we);
		} finally {
			shortener.cleanup();
		}
		LOGGER.info("Raw2mgf: conversion ended: " + batchWorkPacket.getInputFile() + " -> " + mgfFile + " (params: " + params + ")");
	}

	static Long getParamValue(final String paramName, final String params) {
		final String lcParamName = paramName.toLowerCase();
		for (final String parameter : Splitter.on(" ").omitEmptyStrings().trimResults().split(params)) {
			if (parameter.toLowerCase().startsWith(lcParamName)) {
				final String value = parameter.substring(paramName.length()).trim();
				if (value.length() != 0 && value.charAt(0) >= '0' && value.charAt(0) <= '9') {
					try {
						return Long.valueOf(value);
					} catch (NumberFormatException e) {
						// SWALLOWED: We keep going - maybe there is another instance on the command line where the parameter is defined
						LOGGER.warn("Could not parse parameter " + paramName + " - expected numeric value, got " + parameter + ". " + e.getMessage());
					}
				}
			}
		}
		return null;
	}

	/**
	 * Omit all parameters defining the first and last spectrum.
	 *
	 * @param params Parameter list to clean from -F and -L
	 * @return Cleaned up parameter list.
	 */
	static String cleanupFromToParams(final String params) {
		final StringBuilder result = new StringBuilder(params.length());
		for (final String parameter : Splitter.on(" ").omitEmptyStrings().trimResults().split(params)) {
			if (!FIRST_LAST_SPECTRUM.matcher(parameter).matches()) {
				if (result.length() > 0) {
					result.append(' ');
				}
				result.append(parameter);
			}
		}
		return result.toString();
	}

	private boolean isConversionDone(final RawToMgfWorkPacket batchWorkPacket, final File rawFile) {
		//  check if already exists (skip condition)
		if (batchWorkPacket.isSkipIfExists()) {
			final File mgf_file = batchWorkPacket.getOutputFile();
			if (mgf_file.exists()) {
				LOGGER.info(rawFile.getAbsolutePath() + " conversion already done.");
				return true;
			}
		}
		return false;
	}

	private File getRawFile(final RawToMgfWorkPacket batchWorkPacket) {
		final File rawFile = batchWorkPacket.getInputFile();

		// check that we got real raw file to work with
		checkRawFile(rawFile);

		return rawFile;
	}

	/**
	 * run the extract msn executable
	 *
	 * @param fileToExec      - the executable file
	 * @param thermoOutputDir - output dir where the dta files will be written
	 * @param params          - parameters to run extract_msn with
	 * @param rawfile         - the raw file
	 * @return Number of extracted spectra. If there are genuinely no spectra, returns 0. Throws an exception if things go wrong.
	 */
	synchronized void runExtractMsnJob(final File fileToExec, final File thermoOutputDir, final String params, final File rawfile, final long firstSpectrum, final long lastSpectrum, final String wrapperScript, final String xvfbWrapperScript) {
		final String spectrumRangeParams = (params.length() == 0 ? "" : params + " ") + "-F" + firstSpectrum + " -L" + lastSpectrum;
		final ExtractMsnWrapper extractMsn = new ExtractMsnWrapper(fileToExec, spectrumRangeParams, rawfile, wrapperScript, xvfbWrapperScript);
		extractMsn.setOutputDir(thermoOutputDir);

		try {
			extractMsn.run();
		} catch (Exception we) {
			throw new DaemonException("Error extracting dta files from " + rawfile, we);
		}
	}

	/**
	 * convert dta files to an mgf file
	 *
	 * @param thermoOutputDir - Where .dta files were stored
	 * @param dtaFiles        - the list of dta files to process
	 * @param finalOutputFile - token for mgf output file
	 * @param wine            - if should use wine or not
	 * @throws edu.mayo.mprc.MprcException
	 */
	private void dtaToMgf(final File thermoOutputDir, final File[] dtaFiles, final File finalOutputFile, final boolean wine) {
		// now do mgf file creation...
		final File finalOutputDir = finalOutputFile.getParentFile();

		final DTAToMGFConverter pDTAtoMGF =
				new DTAToMGFConverter(
						dtaFiles,
						finalOutputFile);
		pDTAtoMGF.setWineCleanup(wine);
		try {
			// Run the conversion
			pDTAtoMGF.run();
			// delete the dta files
			deleteDTAFiles(thermoOutputDir);
			// copy remaining files in folder to pBatchInfo.Output_Dir
			copyRemainingFiles(thermoOutputDir, finalOutputDir);
		} catch (Exception t) {
			throw new MprcException("dta to MGF conversion failed", t);
		}
		final File file = pDTAtoMGF.getResultFile();
		if (!file.exists() || !file.isFile()) {
			throw new MprcException("The MGF file does not exist " + file.getAbsolutePath());
		}
		if (file.length() == 0) {
			throw new MprcException(
					MessageFormat.format("Merging of DTAs resulted in mgf of zero length (merge {0} into {1})",
							thermoOutputDir, file.getAbsolutePath()));
		}
	}

	private static void copyRemainingFiles(final File fromfolder, final File tofolder) {
		final String[] fromfiles = fromfolder.list();
		for (final String fromfile : fromfiles) {
			final File to = new File(tofolder, FileUtilities.getLastFolderName(fromfile));
			final File localfile = new File(fromfolder, fromfile);
			try {
				FileUtilities.copyFile(localfile, to, true);
			} catch (Exception e) {
				throw new MprcException("Error moving remaining files in batch converter", e);
			}
			FileUtilities.quietDelete(localfile);
		}
		FileUtilities.quietDelete(fromfolder);
	}

	private static File getMirrorFolderonTemp(final File tempfolder) {
		try {
			return FileUtilities.createTempFolder(tempfolder, "raw2mgf", false);
		} catch (Exception t) {
			throw new DaemonException("Cannot create temporary folder for raw->mgf conversion", t);
		}
	}

	private void deleteDTAFiles(final File dir) {

		final ExtensionFilter filter = new ExtensionFilter("dta");

		final String[] list = dir.list(filter);
		File file;
		if (list.length == 0) {
			return;
		}

		for (final String aList : list) {
			file = new File(dir, aList);
			final boolean isdeleted = file.delete();
			if (!isdeleted) {
				LOGGER.warn("Deletion of dta file failed with name=" + dir.getAbsolutePath() + File.separator + aList);
			}
		}
	}

	private static void checkRawFile(final File pFile) {
		if (pFile.exists()) {
			if (pFile.isDirectory()) {
				throw new DaemonException("Raw to MGF convertor cannot convert a directory");
			}
		} else {
			throw new DaemonException("The file " + pFile.getAbsolutePath() + " cannot be found.");
		}
	}

	public void setExtractMsnExecutable(final File extractMsnExecutable) {
		this.extractMsnExecutable = extractMsnExecutable;
	}

	public File getExtractMsnExecutable() {
		return this.extractMsnExecutable;
	}

	public File getTempFolder() {
		return tempFolder;
	}

	public void setTempFolder(final File tempFolder) {
		this.tempFolder = tempFolder;
	}

	public String getWrapperScript() {
		return wrapperScript;
	}

	public void setWrapperScript(final String wrapperScript) {
		this.wrapperScript = wrapperScript;
	}

	public File getXvfbWrapperScript() {
		return xvfbWrapperScript;
	}

	public void setXvfbWrapperScript(final File xvfbWrapperScript) {
		this.xvfbWrapperScript = xvfbWrapperScript;
	}

	public String toString() {
		return MessageFormat.format("Batch conversion:\n\ttemp={0}\n\textract_msn path={1}\n\twrapper={2}", tempFolder.getPath(), extractMsnExecutable, wrapperScript);
	}

	public void setSpectrumBatchSize(final int spectrumBatchSize) {
		this.spectrumBatchSize = spectrumBatchSize;
	}

	public int getSpectrumBatchSize() {
		return spectrumBatchSize;
	}

	/**
	 * A factory capable of creating the worker
	 */
	public static final class Factory extends WorkerFactoryBase<Config> {

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final RawToMgfWorker worker = new RawToMgfWorker();
			worker.setTempFolder(new File(config.getTempFolder()));
			worker.setWrapperScript(config.getWrapperScript());
			worker.setXvfbWrapperScript(Strings.isNullOrEmpty(config.getXvfbWrapperScript()) ? null : new File(config.getXvfbWrapperScript()));
			worker.setExtractMsnExecutable(new File(config.getExtractMsnExecutable()));
			return worker;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {

		private String tempFolder;
		private String wrapperScript;
		private String xvfbWrapperScript;
		private String extractMsnExecutable;

		public Config() {
		}

		public Config(final String tempFolder, final String wrapperScript, final String xvfbWrapperScript, final String extractMsnExecutable) {
			this.tempFolder = tempFolder;
			this.wrapperScript = wrapperScript;
			this.xvfbWrapperScript = xvfbWrapperScript;
			this.extractMsnExecutable = extractMsnExecutable;
		}

		public String getTempFolder() {
			return tempFolder;
		}

		public void setTempFolder(final String tempFolder) {
			this.tempFolder = tempFolder;
		}

		public String getWrapperScript() {
			return wrapperScript;
		}

		public void setWrapperScript(final String wrapperScript) {
			this.wrapperScript = wrapperScript;
		}

		public String getExtractMsnExecutable() {
			return extractMsnExecutable;
		}

		public void setExtractMsnExecutable(final String extractMsnExecutable) {
			this.extractMsnExecutable = extractMsnExecutable;
		}

		public String getXvfbWrapperScript() {
			return xvfbWrapperScript;
		}

		public void setXvfbWrapperScript(final String xvfbWrapperScript) {
			this.xvfbWrapperScript = xvfbWrapperScript;
		}

		public Map<String, String> save(final DependencyResolver resolver) {
			final Map<String, String> map = new TreeMap<String, String>();
			map.put(TEMP_FOLDER, tempFolder);
			map.put(WRAPPER_SCRIPT, wrapperScript);
			map.put(XVFB_WRAPPER_SCRIPT, xvfbWrapperScript);
			map.put(EXTRACT_MSN_EXECUTABLE, extractMsnExecutable);
			return map;
		}

		public void load(final Map<String, String> values, final DependencyResolver resolver) {
			tempFolder = values.get(TEMP_FOLDER);
			wrapperScript = values.get(WRAPPER_SCRIPT);
			xvfbWrapperScript = values.get(XVFB_WRAPPER_SCRIPT);
			extractMsnExecutable = values.get(EXTRACT_MSN_EXECUTABLE);
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String PROVIDED_EXTRACT_MSN = "bin/extract_msn/extract_msn.exe";

		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(EXTRACT_MSN_EXECUTABLE, "<tt>extract_msn.exe</tt> path", "Location of XCalibur's <tt>extract_msn.exe</tt>."
					+ "<p>Typically installed at <tt>C:\\XCalibur\\System\\Programs\\extract_msn.exe</tt></p>"
					+ "<p>For your convenience, a copy is in <tt>" + PROVIDED_EXTRACT_MSN + "</tt></p>")
					.required()
					.executable(Arrays.asList("-v"))
					.defaultValue(PROVIDED_EXTRACT_MSN)

					.property(TEMP_FOLDER, "Temp folder",
							"extract_msn takes a .RAW file on the input and turns it into " +
									"a lot of <tt>.dta</tt> files (one per spectrum), that are subsequently collected into an <tt>.mgf</tt>. " +
									"The <tt>.dta</tt> files need to be stored to a temporary place that is hopefully very fast. For highest performance," +
									"consider setting up a RAM disk (has to have enough space to contain all .dta files for your largest .RAW files). " +
									"A local temporary folder will work as well. Avoid putting this folder on slow, network drives.")
					.required()
					.existingDirectory()

					.property(WRAPPER_SCRIPT, "Wrapper Script",
							"<p>This is needed only for Linux. On Windows, leave this field blank.</p>" +
									"<p>A wrapper script takes the extract_msn command line as a parameter and executes extract_msn.</p>"
									+ "<p>On linux we suggest using <tt>" + DaemonConfig.WINECONSOLE_CMD + "</tt>. You need to have X Window System installed for <tt>" + DaemonConfig.WINECONSOLE_CMD + "</tt> to work, or use the X virtual frame buffer for headless operation (see below).</p>"
									+ "<p>Alternatively, use <tt>" + DaemonConfig.WINE_CMD + "</tt> without need to run X, but in our experience <tt>" + DaemonConfig.WINE_CMD + "</tt> is less stable.</p>")
					.executable(Arrays.asList("-v"))
					.defaultValue(daemon.getWrapperScript())

					.property(XVFB_WRAPPER_SCRIPT, "X Window Wrapper Script",
							"<p>This is needed only for Linux. On Windows, leave this field blank.</p>"
									+ "<p>This wrapper script makes sure there is X window system set up and ready to be used by <tt>wineconsole</tt> (see above).</p>"
									+ "<p>We provide a script <tt>" + DaemonConfig.XVFB_CMD + "</tt> that does just that - feel free to modify it to suit your needs. "
									+ " The script uses <tt>Xvfb</tt> - X virtual frame buffer, so <tt>Xvfb</tt>"
									+ " has to be functional on the host system.</p>"
									+ "<p>If you do not require this functionality, leave the field blank.</p>")
					.executable(Arrays.asList("-v"))
					.defaultValue(daemon.getXvfbWrapperScript())
					.addDaemonChangeListener(new WrapperScriptSwitcher(resource, daemon, WRAPPER_SCRIPT));
		}
	}

}
