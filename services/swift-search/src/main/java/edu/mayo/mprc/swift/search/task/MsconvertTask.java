package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.msconvert.MsconvertResult;
import edu.mayo.mprc.msconvert.MsconvertWorkPacket;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;
import org.apache.log4j.Logger;

import java.io.File;

final class MsconvertTask extends AsyncTaskBase implements FileProducingTask {
	private static final Logger LOGGER = Logger.getLogger(MsconvertTask.class);

	private File inputFile;
	private File outputFile = null;
	private boolean publicAccess;

	/**
	 * @param publicAccess When true, the task requests the cache to give the user access to the .mgf file from the user space.
	 */
	public MsconvertTask(
			final WorkflowEngine engine,
			final File inputFile,
			final File outputFile,
			final boolean publicAccess, final DaemonConnection raw2mgfDaemon, final FileTokenFactory fileTokenFactory, final boolean fromScratch

	) {
		super(engine, raw2mgfDaemon, fileTokenFactory, fromScratch);
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.publicAccess = publicAccess;
		setName("msconvert");

		updateDescription();
	}

	private void updateDescription() {
		setDescription(
				"Converting "
						+ getFileTokenFactory().fileToTaggedDatabaseToken(inputFile)
						+ " to " + getFileTokenFactory().fileToTaggedDatabaseToken(outputFile));
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
		if (inputFile.getName().endsWith(".mgf")) {
			LOGGER.info("Skipping msconvert for an mgf file " + inputFile.getAbsolutePath());
			outputFile = inputFile;
			// Nothing to do, signalize success
			return null;
		} else {
			// We always send the conversion packet even if the .mgf exists at the destination.
			// We need to get its cached location in order for the subsequent caching mechanisms
			// to work properly.
			return new MsconvertWorkPacket(
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
		if (progressInfo instanceof MsconvertResult) {
			final MsconvertResult result = (MsconvertResult) progressInfo;
			result.synchronizeFileTokensOnReceiver();
			outputFile = result.getMgf();
			updateDescription();
		}
	}
}
