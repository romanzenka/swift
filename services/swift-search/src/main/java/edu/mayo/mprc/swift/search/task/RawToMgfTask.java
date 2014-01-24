package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.raw2mgf.RawToMgfResult;
import edu.mayo.mprc.raw2mgf.RawToMgfWorkPacket;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;
import org.apache.log4j.Logger;

import java.io.File;

final class RawToMgfTask extends AsyncTaskBase implements FileProducingTask {
	private static final Logger LOGGER = Logger.getLogger(RawToMgfTask.class);

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (!(o instanceof RawToMgfTask)) return false;

		final RawToMgfTask that = (RawToMgfTask) o;

		if (publicAccess != that.publicAccess) return false;
		if (extractMsnParameters != null ? !extractMsnParameters.equals(that.extractMsnParameters) : that.extractMsnParameters != null)
			return false;
		if (inputFile != null ? !inputFile.equals(that.inputFile) : that.inputFile != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = inputFile != null ? inputFile.hashCode() : 0;
		result = 31 * result + (extractMsnParameters != null ? extractMsnParameters.hashCode() : 0);
		result = 31 * result + (publicAccess ? 1 : 0);
		return result;
	}

	private final File inputFile;
	private final String extractMsnParameters;
	private final boolean publicAccess;
	private File outputFile = null;

	/**
	 * @param publicAccess When true, the task requests the cache to give the user access to the .mgf file from the user space.
	 */
	RawToMgfTask(
			final WorkflowEngine engine,
			final File inputFile,
			final File outputFile,
			final String extractMsnParams,
			final boolean publicAccess,
			final DaemonConnection raw2mgfDaemon, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch

	) {
		super(engine, raw2mgfDaemon, fileTokenFactory, fromScratch);
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		extractMsnParameters = extractMsnParams;
		this.publicAccess = publicAccess;
		setName("RAW2mgf");

		updateDescription();
	}

	void setOutputFile(final File outputFile) {
		this.outputFile = outputFile;
		updateDescription();
	}

	private void updateDescription() {
		setDescription(
				"Converting "
						+ getFileTokenFactory().fileToTaggedDatabaseToken(inputFile)
						+ " to " + getFileTokenFactory().fileToTaggedDatabaseToken(outputFile)
						+ " (" + extractMsnParameters + ")");
	}

	private static String getFileReference(final File rawFile) {
		return rawFile.getAbsolutePath();
	}

	public String getFileReference() {
		return getFileReference(inputFile);
	}

	@Override
	public File getResultingFile() {
		return outputFile;
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 *         to send a work packet.
	 */
	@Override
	public WorkPacket createWorkPacket() {
		if (!RawFilesSupported.isRawFile(inputFile)) {
			LOGGER.info("Skipping Raw2MGF for an mgf file " + inputFile.getAbsolutePath());
			outputFile = inputFile;
			// Nothing to do, signalize success
			return null;
		} else {
			// We always send the conversion packet even if the .mgf exists at the destination.
			// We need to get its cached location in order for the subsequent caching mechanisms
			// to work properly.
			return new RawToMgfWorkPacket(
					extractMsnParameters,
					outputFile,
					true,
					inputFile,
					getFullId(),
					isFromScratch(),
					publicAccess);
		}
	}

	@Override
	public void onSuccess() {
		completeWhenFilesAppear(outputFile);
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
		if (progressInfo instanceof RawToMgfResult) {
			final RawToMgfResult result = (RawToMgfResult) progressInfo;
			outputFile = result.getMgf();
			updateDescription();
		}
	}
}
