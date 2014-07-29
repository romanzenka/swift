package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.qa.RAWDumpResult;
import edu.mayo.mprc.qa.RAWDumpWorkPacket;
import edu.mayo.mprc.searchdb.builder.RawFileMetaData;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;

/**
 * Dumping data from a .RAW file.
 */
public final class RAWDumpTask extends AsyncTaskBase {
	private final File rawFile;
	private File rawInfoFile;
	private File rawSpectraFile;
	private File chromatogramFile;
	private File tuneMethodFile;
	private File instrumentMethodFile;
	private File sampleInformationFile;
	private File errorLogFile;
	private final File outputFolder;

	public RAWDumpTask(final WorkflowEngine engine, final File rawFile, final File outputFolder, final DaemonConnection daemonConnection, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(engine, daemonConnection, fileTokenFactory, fromScratch);

		this.rawFile = rawFile;
		this.outputFolder = outputFolder;
		rawInfoFile = RAWDumpWorkPacket.getExpectedRawInfoFile(outputFolder, rawFile);
		rawSpectraFile = RAWDumpWorkPacket.getExpectedRawSpectraFile(outputFolder, rawFile);
		chromatogramFile = RAWDumpWorkPacket.getExpectedChromatogramFile(outputFolder, rawFile);
		tuneMethodFile = RAWDumpWorkPacket.getExpectedTuneMethodFile(outputFolder, rawFile);
		instrumentMethodFile = RAWDumpWorkPacket.getExpectedInstrumentMethodFile(outputFolder, rawFile);
		sampleInformationFile = RAWDumpWorkPacket.getExpectedSampleInformationFile(outputFolder, rawFile);
		errorLogFile = RAWDumpWorkPacket.getExpectedErrorLogFile(outputFolder, rawFile);

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
				isFromScratch());
	}

	public File getOutputFolder() {
		return outputFolder;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RAWDumpTask)) return false;

		RAWDumpTask that = (RAWDumpTask) o;

		if (outputFolder != null ? !outputFolder.equals(that.outputFolder) : that.outputFolder != null) return false;
		if (rawFile != null ? !rawFile.equals(that.rawFile) : that.rawFile != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = rawFile != null ? rawFile.hashCode() : 0;
		result = 31 * result + (outputFolder != null ? outputFolder.hashCode() : 0);
		return result;
	}
}
