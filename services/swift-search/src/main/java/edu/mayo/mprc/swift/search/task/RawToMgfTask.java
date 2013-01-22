package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.raw2mgf.RawToMgfResult;
import edu.mayo.mprc.raw2mgf.RawToMgfWorkPacket;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import org.apache.log4j.Logger;

import java.io.File;

final class RawToMgfTask extends AsyncTaskBase implements FileProducingTask {
	private static final Logger LOGGER = Logger.getLogger(RawToMgfTask.class);

	private File inputFile;
	private File mgfFile = null;
	private String extractMsnParameters;
	private boolean publicAccess;

	/**
	 * @param publicAccess When true, the task requests the cache to give the user access to the .mgf file from the user space.
	 */
	public RawToMgfTask(
			final File inputFile,
			final File mgfFile,
			final String extractMsnParams,
			final boolean publicAccess, final DaemonConnection raw2mgfDaemon, final FileTokenFactory fileTokenFactory, final boolean fromScratch

	) {
		super(raw2mgfDaemon, fileTokenFactory, fromScratch);
		this.inputFile = inputFile;
		this.mgfFile = mgfFile;
		this.extractMsnParameters = extractMsnParams;
		this.publicAccess = publicAccess;
		setName("RAW2mgf");

		updateDescription();
	}

	private void updateDescription() {
		setDescription(
				"Converting "
						+ getFileTokenFactory().fileToTaggedDatabaseToken(this.inputFile)
						+ " to " + getFileTokenFactory().fileToTaggedDatabaseToken(this.mgfFile)
						+ " (" + this.extractMsnParameters + ")");
	}

	private static String getFileReference(final File rawFile) {
		return rawFile.getAbsolutePath();
	}

	public String getFileReference() {
		return getFileReference(this.inputFile);
	}

	public File getResultingFile() {
		return mgfFile;
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 *         to send a work packet.
	 */
	public WorkPacket createWorkPacket() {
		if (inputFile.getName().endsWith(".mgf")) {
			LOGGER.info("Skipping Raw2MGF for an mgf file " + inputFile.getAbsolutePath());
			mgfFile = inputFile;
			// Nothing to do, signalize success
			return null;
		} else {
			// We always send the conversion packet even if the .mgf exists at the destination.
			// We need to get its cached location in order for the subsequent caching mechanisms
			// to work properly.
			return new RawToMgfWorkPacket(
					extractMsnParameters,
					mgfFile,
					true,
					inputFile,
					getFullId(),
					isFromScratch(),
					publicAccess);
		}
	}

	public void onSuccess() {
		completeWhenFilesAppear(mgfFile);
	}

	public void onProgress(final ProgressInfo progressInfo) {
		if (progressInfo instanceof RawToMgfResult) {
			final RawToMgfResult result = (RawToMgfResult) progressInfo;
			result.synchronizeFileTokensOnReceiver();
			mgfFile = result.getMgf();
			updateDescription();
		}
	}
}
