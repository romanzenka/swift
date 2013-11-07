package edu.mayo.mprc.qa;

import edu.mayo.mprc.daemon.CachableWorkPacket;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.WorkPacketBase;
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

	public RAWDumpWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	public RAWDumpWorkPacket(final File rawFile, final File rawInfoFile, final File rawSpectraFile, final File chromatogramFile,
	                         final File tuneMethodFile, final File instrumentMethodFile, final File sampleInformationFile, final File errorLogFile,
	                         final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);

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

	@Override
	public WorkPacket translateToWorkInProgressPacket(final File wipFolder) {
		return new RAWDumpWorkPacket(
				getRawFile(),
				new File(wipFolder, getRawInfoFile().getName()),
				new File(wipFolder, getRawSpectraFile().getName()),
				new File(wipFolder, getChromatogramFile().getName()),
				new File(wipFolder, getTuneMethodFile().getName()),
				new File(wipFolder, getInstrumentMethodFile().getName()),
				new File(wipFolder, getSampleInformationFile().getName()),
				new File(wipFolder, getErrorLogFile().getName()),
				getTaskId(),
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
				getErrorLogFile().getName());
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
						tuneMethod, instrumentMethod, sampleInformation, errorLog));
	}

}
