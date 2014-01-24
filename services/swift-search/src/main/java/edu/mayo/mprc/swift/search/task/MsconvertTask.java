package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.msconvert.MsconvertResult;
import edu.mayo.mprc.msconvert.MsconvertWorkPacket;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;
import org.apache.log4j.Logger;

import java.io.File;

final class MsconvertTask extends AsyncTaskBase implements FileProducingTask {
	private static final Logger LOGGER = Logger.getLogger(MsconvertTask.class);

	private final File inputFile;
	private final boolean publicAccess;
	private final String outputExtension;
	private File outputFile = null;

	/**
	 * @param publicAccess When true, the task requests the cache to give the user access to the .mgf file from the user space.
	 */
	MsconvertTask(
			final WorkflowEngine engine,
			final File inputFile,
			final File outputFile,
			final boolean publicAccess, final DaemonConnection raw2mgfDaemon, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch

	) {
		super(engine, raw2mgfDaemon, fileTokenFactory, fromScratch);
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.publicAccess = publicAccess;
		outputExtension = FileUtilities.getExtension(outputFile.getName());
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
		if (!RawFilesSupported.isRawFile(inputFile)) {
			LOGGER.info("Skipping msconvert for an input file " + inputFile.getAbsolutePath());
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
			outputFile = result.getMgf();
			updateDescription();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(inputFile, outputExtension, publicAccess);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final MsconvertTask other = (MsconvertTask) obj;
		return Objects.equal(inputFile, other.inputFile)
				&& Objects.equal(outputExtension, other.outputExtension)
				&& Objects.equal(publicAccess, other.publicAccess);
	}

	void setOutputFile(final File outputFile) {
		this.outputFile = outputFile;
		updateDescription();
	}
}
