package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.msmseval.MSMSEvalWorkPacket;
import edu.mayo.mprc.msmseval.MsmsEvalResult;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;
import org.apache.log4j.Logger;

import java.io.File;

final class SpectrumQaTask extends AsyncTaskBase {
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SpectrumQaTask)) return false;

		SpectrumQaTask that = (SpectrumQaTask) o;

		if (msmsEvalParamFile != null ? !msmsEvalParamFile.equals(that.msmsEvalParamFile) : that.msmsEvalParamFile != null)
			return false;
		if (outputDirectory != null ? !outputDirectory.equals(that.outputDirectory) : that.outputDirectory != null)
			return false;
		if (sourceMGFFile != null ? !sourceMGFFile.equals(that.sourceMGFFile) : that.sourceMGFFile != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = sourceMGFFile != null ? sourceMGFFile.hashCode() : 0;
		result = 31 * result + (msmsEvalParamFile != null ? msmsEvalParamFile.hashCode() : 0);
		result = 31 * result + (outputDirectory != null ? outputDirectory.hashCode() : 0);
		return result;
	}

	private static final Logger LOGGER = Logger.getLogger(SpectrumQaTask.class);

	private final FileProducingTask sourceMGFFile;
	private final File msmsEvalParamFile;
	private final File outputDirectory;
	private File outputFile;
	private File emFile;

	public static final String TASK_NAME = "MSMSEval Filter";

	SpectrumQaTask(final WorkflowEngine engine, final DaemonConnection daemon, final FileProducingTask sourceMGFFile, final File msmsEvalParamFile,
	               final File outputDirectory, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(engine, daemon, fileTokenFactory, fromScratch);
		this.outputDirectory = outputDirectory;
		this.sourceMGFFile = sourceMGFFile;
		this.msmsEvalParamFile = msmsEvalParamFile;
		outputFile = MSMSEvalWorkPacket.getExpectedResultFileName(sourceMGFFile.getResultingFile(), outputDirectory);
		emFile = MSMSEvalWorkPacket.getExpectedEmOutputFileName(sourceMGFFile.getResultingFile(), outputDirectory);

		setName(TASK_NAME);

		updateDescription();
	}

	private void updateDescription() {
		setDescription("Analyzing input file: "
				+ getFileTokenFactory().fileToTaggedDatabaseToken(sourceMGFFile.getResultingFile())
				+ " using msmsEval parameter file: "
				+ getFileTokenFactory().fileToTaggedDatabaseToken(msmsEvalParamFile));
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 * to send a work packet.
	 */
	@Override
	public WorkPacket createWorkPacket() {
		final File msmsEvalOutputFile = getMsmsEvalOutputFile();
		if (!isFromScratch() && msmsEvalOutputFile.exists() && msmsEvalOutputFile.length() > 0) {
			LOGGER.info("Skipping msmsEval spectrum analysis because output file, " + msmsEvalOutputFile.getAbsolutePath() + ", already exists.");
			return null;
		}

		return new MSMSEvalWorkPacket(sourceMGFFile.getResultingFile(), msmsEvalParamFile, outputFile, emFile, false);
	}

	@Override
	public void onSuccess() {
		//Do nothing
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
		if (progressInfo instanceof MsmsEvalResult) {
			final MsmsEvalResult evalResult = (MsmsEvalResult) progressInfo;
			outputFile = evalResult.getOutputFile();
			emFile = evalResult.getEmFile();
			updateDescription();
		}
	}

	public File getEmOutputFile() {
		return emFile;
	}

	public File getMsmsEvalOutputFile() {
		return outputFile;
	}
}
