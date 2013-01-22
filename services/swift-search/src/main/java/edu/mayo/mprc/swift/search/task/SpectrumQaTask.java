package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.msmseval.MSMSEvalWorkPacket;
import edu.mayo.mprc.msmseval.MSMSEvalWorker;
import edu.mayo.mprc.msmseval.MsmsEvalResult;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import org.apache.log4j.Logger;

import java.io.File;

final class SpectrumQaTask extends AsyncTaskBase {

	private static final Logger LOGGER = Logger.getLogger(SpectrumQaTask.class);

	private FileProducingTask sourceMGFFile;
	private File msmsEvalParamFile;
	private File outputDirectory;
	private File outputFile;
	private File emFile;

	public static final String TASK_NAME = "MSMSEval Filter";

	public SpectrumQaTask(final DaemonConnection daemon, final FileProducingTask sourceMGFFile, final File msmsEvalParamFile, final File outputDirectory, final FileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(daemon, fileTokenFactory, fromScratch);
		this.outputDirectory = outputDirectory;
		this.sourceMGFFile = sourceMGFFile;
		this.msmsEvalParamFile = msmsEvalParamFile;
		outputFile = MSMSEvalWorker.getExpectedResultFileName(sourceMGFFile.getResultingFile(), outputDirectory);
		emFile = MSMSEvalWorker.getExpectedEmOutputFileName(sourceMGFFile.getResultingFile(), outputDirectory);

		setName(TASK_NAME);

		updateDescription();
	}

	private void updateDescription() {
		setDescription("Analyzing mgf file: "
				+ getFileTokenFactory().fileToTaggedDatabaseToken(this.sourceMGFFile.getResultingFile())
				+ " using msmsEval parameter file: "
				+ getFileTokenFactory().fileToTaggedDatabaseToken(this.msmsEvalParamFile));
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 *         to send a work packet.
	 */
	public WorkPacket createWorkPacket() {
		final File msmsEvalOutputFile = getMsmsEvalOutputFile();
		if (!isFromScratch() && msmsEvalOutputFile.exists() && msmsEvalOutputFile.length() > 0) {
			LOGGER.info("Skipping msmsEval spectrum analysis because output file, " + msmsEvalOutputFile.getAbsolutePath() + ", already exists.");
			return null;
		}

		return new MSMSEvalWorkPacket(sourceMGFFile.getResultingFile(), msmsEvalParamFile, outputDirectory, getFullId());
	}

	public void onSuccess() {
		//Do nothing
	}

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
