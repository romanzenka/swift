package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.qa.RAWDumpResult;
import edu.mayo.mprc.qa.RAWDumpWorkPacket;
import edu.mayo.mprc.searchdb.RawFileMetaData;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;

/**
 * Dumping data from a .RAW file.
 */
public final class RAWDumpTask extends AsyncTaskBase {
	private File rawFile;
	private File rawInfoFile;
	private File rawSpectraFile;
	private File chromatogramFile;
	private File tuneMethodFile;
	private File instrumentMethodFile;
	private File sampleInformationFile;
	private File errorLogFile;
	private File outputFolder;

	public static final String RAW_INFO_FILE_SUFFIX = ".info.tsv";
	public static final String RAW_SPECTRA_FILE_SUFFIX = ".spectra.tsv";
	public static final String CHROMATOGRAM_FILE_SUFFIX = ".chroma.gif";
	public static final String TUNE_METHOD_FILE_SUFFIX = ".tune.tsv";
	public static final String INSTRUMENT_METHOD_FILE_SUFFIX = ".instrument.tsv";
	public static final String SAMPLE_INFORMATION_FILE_SUFFIX = ".sample.tsv";
	public static final String ERROR_LOG_FILE_SUFFIX = ".error.tsv";

	public RAWDumpTask(final WorkflowEngine engine, final File rawFile, final File outputFolder, final DaemonConnection daemonConnection, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(engine, daemonConnection, fileTokenFactory, fromScratch);

		this.rawFile = rawFile;
		this.outputFolder = outputFolder;
		rawInfoFile = getExpectedRawInfoFile();
		rawSpectraFile = getExpectedRawSpectraFile();
		chromatogramFile = getExpectedChromatogramFile();
		tuneMethodFile = getExpectedTuneMethodFile();
		instrumentMethodFile = getExpectedInstrumentMethodFile();
		sampleInformationFile = getExpectedSampleInformationFile();
		errorLogFile = getExpectedErrorLogFile();

		setName("RAW Dump");
		updateDescription();
	}

	private void updateDescription() {
		setDescription("RAW Dump info file, " + getFileTokenFactory().fileToTaggedDatabaseToken(rawInfoFile)
				+ ", spectra file, " + getFileTokenFactory().fileToTaggedDatabaseToken(rawSpectraFile) + ".");
	}

	@Override
	public WorkPacket createWorkPacket() {
		return new RAWDumpWorkPacket(rawFile,
				rawInfoFile, rawSpectraFile, chromatogramFile,
				tuneMethodFile, instrumentMethodFile, sampleInformationFile, errorLogFile,
				getFullId(), isFromScratch());
	}

	public File getOutputFolder() {
		return outputFolder;
	}

	public File getExpectedRawInfoFile() {
		return new File(outputFolder, rawFile.getName() + RAW_INFO_FILE_SUFFIX);
	}

	public File getExpectedRawSpectraFile() {
		return new File(outputFolder, rawFile.getName() + RAW_SPECTRA_FILE_SUFFIX);
	}

	public File getExpectedChromatogramFile() {
		return new File(outputFolder, rawFile.getName() + CHROMATOGRAM_FILE_SUFFIX);
	}

	public File getExpectedTuneMethodFile() {
		return new File(outputFolder, rawFile.getName() + TUNE_METHOD_FILE_SUFFIX);
	}

	public File getExpectedInstrumentMethodFile() {
		return new File(outputFolder, rawFile.getName() + INSTRUMENT_METHOD_FILE_SUFFIX);
	}

	public File getExpectedSampleInformationFile() {
		return new File(outputFolder, rawFile.getName() + SAMPLE_INFORMATION_FILE_SUFFIX);
	}

	public File getExpectedErrorLogFile() {
		return new File(outputFolder, rawFile.getName() + ERROR_LOG_FILE_SUFFIX);
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

	public RawFileMetaData getRawFileMetadata() {
		return new RawFileMetaData(rawFile, rawInfoFile, tuneMethodFile, instrumentMethodFile, sampleInformationFile, errorLogFile);
	}

	@Override
	public void onSuccess() {
		//Do nothing
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
		if (progressInfo instanceof RAWDumpResult) {
			final RAWDumpResult dumpResult = (RAWDumpResult) progressInfo;
			rawInfoFile = dumpResult.getRawInfoFile();
			rawSpectraFile = dumpResult.getRawSpectraFile();
			updateDescription();
			chromatogramFile = dumpResult.getChromatogramFile();
			tuneMethodFile = dumpResult.getTuneMethodFile();
			instrumentMethodFile = dumpResult.getInstrumentMethodFile();
			sampleInformationFile = dumpResult.getSampleInformationFile();
			errorLogFile = dumpResult.getErrorLogFile();
		}
	}
}
