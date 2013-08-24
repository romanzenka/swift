package edu.mayo.mprc.daemon;

import edu.mayo.mprc.daemon.monitor.MonitorUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;

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
			process(workPacket, progressReporter);
			workPacket.synchronizeFileTokensOnReceiver();
			progressReporter.reportSuccess();
		} catch (Exception t) {
			progressReporter.reportFailure(t);
		}
	}

	@Override
	public void check() {
		LOGGER.debug("No check implemented for this worker: " + getClass().getSimpleName());
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
	protected abstract void process(WorkPacket workPacket, UserProgressReporter progressReporter);
}
