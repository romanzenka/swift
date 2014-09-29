package edu.mayo.mprc.qa;

import edu.mayo.mprc.daemon.CachableWorkPacket;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Class contains information for RAW file dump request.
 */
public final class RAWDumpWorkPacket extends WorkPacketBase implements CachableWorkPacket {

	private static final long serialVersionUID = 200220L;

	private File rawFile;
	private File rawInfoFile;
	private File rawSpectraFile;
	private File chromatogramFile;
	private File tuneMethodFile;
	private File instrumentMethodFile;
	private File sampleInformationFile;
	private File errorLogFile;
	private File uvDataFile;

	public static final String RAW_INFO_FILE_SUFFIX = ".info.tsv";
	public static final String RAW_SPECTRA_FILE_SUFFIX = ".spectra.tsv";
	public static final String CHROMATOGRAM_FILE_SUFFIX = ".chroma.gif";
	public static final String TUNE_METHOD_FILE_SUFFIX = ".tune.tsv";
	public static final String INSTRUMENT_METHOD_FILE_SUFFIX = ".instrument.tsv";
	public static final String SAMPLE_INFORMATION_FILE_SUFFIX = ".sample.tsv";
	public static final String ERROR_LOG_FILE_SUFFIX = ".error.tsv";
	public static final String UV_DATA_FILE_SUFFIX = ".uv.tsv";

	public RAWDumpWorkPacket(final boolean fromScratch) {
		super(fromScratch);
	}

	public RAWDumpWorkPacket(final File rawFile, final File rawInfoFile, final File rawSpectraFile, final File chromatogramFile,
	                         final File tuneMethodFile, final File instrumentMethodFile, final File sampleInformationFile, final File errorLogFile,
	                         final File uvDataFile,
	                         final boolean fromScratch) {
		super(fromScratch);

		assert rawFile != null : "Raw input file can not be null.";
		assert rawInfoFile != null : "Info output file must be defined.";
		assert rawSpectraFile != null : "Spectra output file must be defined.";

		this.rawFile = rawFile;
		this.rawInfoFile = rawInfoFile;
		this.rawSpectraFile = rawSpectraFile;
		this.chromatogramFile = chromatogramFile;
		this.tuneMethodFile = tuneMethodFile;
		this.instrumentMethodFile = instrumentMethodFile;
		this.sampleInformationFile = sampleInformationFile;
		this.errorLogFile = errorLogFile;
		this.uvDataFile = uvDataFile;
	}

	public File getRawFile() {
		return rawFile;
	}

	public File getRawInfoFile() {
		return rawInfoFile;
	}

	public File getRawSpectraFile() {
		return rawSpectraFile;
	}

	public File getChromatogramFile() {
		return chromatogramFile;
	}

	public File getTuneMethodFile() {
		return tuneMethodFile;
	}

	public File getInstrumentMethodFile() {
		return instrumentMethodFile;
	}

	public File getSampleInformationFile() {
		return sampleInformationFile;
	}

	public File getErrorLogFile() {
		return errorLogFile;
	}

	public File getUvDataFile() {
		return uvDataFile;
	}

	@Override
	public boolean isPublishResultFiles() {
		// We never publish these intermediate files
		return false;
	}

	@Override
	public File getOutputFile() {
		return null;
	}

	@Override
	public String getStringDescriptionOfTask() {
		final StringBuilder description = new StringBuilder();
		description
				.append("Input:")
				.append(getRawFile().getAbsolutePath())
				.append("\n")
				.append("Chromatogram:")
				.append("true")
				.append("\n");
		return description.toString();
	}

	public static File getExpectedRawInfoFile(final File outputFolder, final File rawFile) {
		return new File(outputFolder, rawFile.getName() + RAW_INFO_FILE_SUFFIX);
	}

	public static File getExpectedRawSpectraFile(final File outputFolder, final File rawFile) {
		return new File(outputFolder, rawFile.getName() + RAW_SPECTRA_FILE_SUFFIX);
	}

	public static File getExpectedChromatogramFile(final File outputFolder, final File rawFile) {
		return new File(outputFolder, rawFile.getName() + CHROMATOGRAM_FILE_SUFFIX);
	}

	public static File getExpectedTuneMethodFile(final File outputFolder, final File rawFile) {
		return new File(outputFolder, rawFile.getName() + TUNE_METHOD_FILE_SUFFIX);
	}

	public static File getExpectedInstrumentMethodFile(final File outputFolder, final File rawFile) {
		return new File(outputFolder, rawFile.getName() + INSTRUMENT_METHOD_FILE_SUFFIX);
	}

	public static File getExpectedSampleInformationFile(final File outputFolder, final File rawFile) {
		return new File(outputFolder, rawFile.getName() + SAMPLE_INFORMATION_FILE_SUFFIX);
	}

	public static File getExpectedErrorLogFile(final File outputFolder, final File rawFile) {
		return new File(outputFolder, rawFile.getName() + ERROR_LOG_FILE_SUFFIX);
	}

	public static File getExpectedUvDataFile(final File outputFolder, final File rawFile) {
		return new File(outputFolder, rawFile.getName() + UV_DATA_FILE_SUFFIX);
	}

	@Override
	public WorkPacket translateToCachePacket(final File cacheFolder) {
		return new RAWDumpWorkPacket(
				getRawFile(),
				getExpectedRawInfoFile(cacheFolder, getRawFile()),
				getExpectedRawSpectraFile(cacheFolder, getRawFile()),
				getExpectedChromatogramFile(cacheFolder, getRawFile()),
				getExpectedTuneMethodFile(cacheFolder, getRawFile()),
				getExpectedInstrumentMethodFile(cacheFolder, getRawFile()),
				getExpectedSampleInformationFile(cacheFolder, getRawFile()),
				getExpectedErrorLogFile(cacheFolder, getRawFile()),
				getExpectedUvDataFile(cacheFolder, getUvDataFile()),
				isFromScratch()
		);
	}

	@Override
	public List<String> getOutputFiles() {
		return Arrays.asList(
				getRawInfoFile().getName(),
				getRawSpectraFile().getName(),
				getChromatogramFile().getName(),
				getTuneMethodFile().getName(),
				getInstrumentMethodFile().getName(),
				getSampleInformationFile().getName(),
				getErrorLogFile().getName(),
				getUvDataFile().getName());
	}

	@Override
	public boolean cacheIsStale(final File subFolder, final List<String> outputFiles) {
		final long inputFileModified = getRawFile().lastModified();
		for (final String file : outputFiles) {
			if (inputFileModified > new File(subFolder, file).lastModified()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void reportCachedResult(final ProgressReporter reporter, final File targetFolder, final List<String> outputFiles) {
		final File rawInfo = new File(targetFolder, outputFiles.get(0));
		final File rawSpectra = new File(targetFolder, outputFiles.get(1));
		final File chromatogram = new File(targetFolder, outputFiles.get(2));
		final File tuneMethod = new File(targetFolder, outputFiles.get(3));
		final File instrumentMethod = new File(targetFolder, outputFiles.get(4));
		final File sampleInformation = new File(targetFolder, outputFiles.get(5));
		final File errorLog = new File(targetFolder, outputFiles.get(6));
		reporter.reportProgress(
				new RAWDumpResult(rawInfo, rawSpectra, chromatogram,
						tuneMethod, instrumentMethod, sampleInformation, errorLog, uvDataFile));
	}

}
