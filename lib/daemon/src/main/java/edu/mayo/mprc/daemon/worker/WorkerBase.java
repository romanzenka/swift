package edu.mayo.mprc.daemon.worker;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.MonitorUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;
import java.text.MessageFormat;

/**
 * Base implementation of {@link Worker} interface.
 *
 * @author Roman Zenka
 */
public abstract class WorkerBase implements Worker {
	private static final Logger LOGGER = Logger.getLogger(WorkerBase.class);

	/**
	 * Default implementation for processing requests.
	 * <ul>
	 * <li>Report processing start</li>
	 * <li>Do all the work</li>
	 * <li>Upload the generated files (if any) to sender of the request</li>
	 * <li>Report success (or failure, if any exception was thrown)</li>
	 * </ul>
	 *
	 * @param workPacket       Work packet to be processed.
	 * @param progressReporter To report progress, success or failures.
	 */
	@Override
	public void processRequest(final WorkPacket workPacket, final ProgressReporter progressReporter) {
		try {
			progressReporter.reportStart(MonitorUtilities.getHostInformation());
			final File tempWorkFolder = createTempWorkFolder();
			if (tempWorkFolder != null) {
				LOGGER.debug("Temporary work folder at: " + tempWorkFolder.getAbsolutePath());
			}
			process(workPacket, tempWorkFolder, progressReporter);
			cleanTempWorkFolder(tempWorkFolder);
			progressReporter.reportSuccess();
		} catch (final Exception t) {
			// SWALLOWED: We report the exception over network
			LOGGER.error(String.format("Processing failed: %s", MprcException.getDetailedMessage(t)));
			progressReporter.reportFailure(t);
		}
	}

	@Override
	public String check() {
		LOGGER.debug("No check implemented for this worker: " + getClass().getSimpleName());
		return null;
	}

	/**
	 * Do the actual work the work packet asked for.
	 * In case of failure, throw an exception.
	 * The progress reporter passed in is limited to reporting only the additional {@link ProgressInfo}.
	 * The main reporting is done in {@link #processRequest}.
	 *
	 * @param workPacket       Packet to process.
	 * @param progressReporter Reporter for additional progress information.
	 */
	protected abstract void process(WorkPacket workPacket, File tempWorkFolder, UserProgressReporter progressReporter);

	/**
	 * Get a temporary working folder to process the work packet in.
	 * All output files generated will be put into this folder instead of their desired target place.
	 * That allows us to complete all work and then publish a consistent set of results at once.
	 *
	 * @return Folder where to process the work packet.
	 */
	public File createTempWorkFolder() {
		return FileUtilities.createTempFolder();
	}

	/**
	 * Delete given temporary work folder and all files in it.
	 *
	 * @param tempWorkFolder
	 */
	public void cleanTempWorkFolder(final File tempWorkFolder) {
		if (tempWorkFolder != null) {
			FileUtilities.deleteNow(tempWorkFolder);
		}
	}

	/**
	 * For given temporary folder and a file to be produced, obtain path to
	 * the file within the temporary folder.
	 *
	 * @param tempFolder Temporary folder.
	 * @param file       File to be output.
	 * @return Temporary version of the file to be output.
	 */
	public File getTempOutputFile(final File tempFolder, final File file) {
		return new File(tempFolder, file.getName());
	}

	/**
	 * Publishes a given file from its temporary location to its desired target location.
	 * <p/>
	 * Publishing moves the file to the target location and then it renames it in place.
	 *
	 * @param from From where to publish the file.
	 * @param to   To where to publish the file.
	 */
	public void publish(final File from, final File to) {
		LOGGER.debug(MessageFormat.format("Publishing file {0} as {1}", from.getAbsolutePath(), to.getAbsolutePath()));
		final File published = new File(to.getParentFile(), to.getName() + ".publish~");
		try {
			// Make sure we have the parent folder
			FileUtilities.ensureFolderExists(published.getParentFile());
			// Copy to an intermediate file
			FileUtilities.copyFile(from, published, true);
			// We restore umask just in case our software did not honor it for some reason
			FileUtilities.restoreUmaskRights(published, false);
			// Now we rename in place. This marks the file as published safely.
			FileUtilities.rename(published, to);
			// And finally delete the source file
			FileUtilities.quietDelete(from);
		} finally {
			if (published.exists()) {
				FileUtilities.quietDelete(published);
			}
		}
	}
}
