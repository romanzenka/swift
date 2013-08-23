package edu.mayo.mprc.dbundeploy;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class that calls database undeployer given a database undeployer connection and a database identifier.
 */
public final class DatabaseUndeployerCaller {

	private static final Logger LOGGER = Logger.getLogger(DatabaseUndeployerCaller.class);

	private static AtomicLong taskIdPostFix = new AtomicLong(0);
	private static final String taskIdPrefix = "DatabaseUndeployment_";

	private static Map<Long, LinkedBlockingQueue<DatabaseUndeployerProgress>> taskIdMessagesPair = new HashMap<Long, LinkedBlockingQueue<DatabaseUndeployerProgress>>();

	private DatabaseUndeployerCaller() {
	}

	/**
	 * Sends a DatabaseUndeployerWorkPacket object to given DaemonConnection,
	 * and monitors (though a ProgressListener) progress on database undeployment
	 * processing.
	 *
	 * @param databaseUndeployerConnection
	 * @param dbToUndeploy
	 * @return
	 */
	public static DatabaseUndeployerProgress callDatabaseUndeployer(final DaemonConnection databaseUndeployerConnection, final Curation dbToUndeploy) {
		final long taskId = taskIdPostFix.incrementAndGet();

		addMessageToQueue(taskId, "Sending database undeployment request for " + dbToUndeploy.getShortName(), false);

		databaseUndeployerConnection.sendWork(new DatabaseUndeployerWorkPacket(dbToUndeploy, taskIdPrefix + taskId), new ProgressListener() {

			@Override
			public void requestEnqueued(final String hostString) {
				addMessageToQueue(taskId, "Enqueued at " + hostString, false);
			}

			@Override
			public void requestProcessingStarted(final String hostString) {
				addMessageToQueue(taskId, "Processing request at " + hostString, false);
			}

			@Override
			public void requestProcessingFinished() {
				addMessageToQueue(taskId, "Undeployment completed.", true);
			}

			@Override
			public void requestTerminated(final Exception e) {
				addMessageToQueue(taskId, "Undeployment request terminated due:\n" + MprcException.getDetailedMessage(e), true);
			}

			@Override
			public void userProgressInformation(final ProgressInfo progressInfo) {
				if (progressInfo instanceof DatabaseUndeployerResult) {
					final StringBuilder builder = new StringBuilder();

					for (final Map.Entry<String, UndeploymentTaskResult> me : ((DatabaseUndeployerResult) progressInfo).getDatabaseUndeployerResults().entrySet()) {
						builder.append("-----------------------------------------------------------------------------------------")
								.append("\n").append("Database undeployment task for ").append(me.getKey())
								.append("\n").append(me.getValue().wasSuccessful() ? "Completed Successfully" : "Failed");

						/**
						 * If there is an error or any message in the undeployment task result object, report it.
						 */
						if (me.getValue().getExecutionError() != null || !me.getValue().getMessages().isEmpty()) {
							builder.append("\n").append(me.getValue().toString());
						}

						builder.append("\n");
					}

					addMessageToQueue(taskId, builder.toString(), false);
				}
			}
		});

		return getMessageFromQueue(taskId);
	}

	private static void addMessageToQueue(final Long taskId, final String message, final boolean isLast) {
		LinkedBlockingQueue<DatabaseUndeployerProgress> messageQueue = null;

		if ((messageQueue = taskIdMessagesPair.get(taskId)) == null) {
			taskIdMessagesPair.put(taskId, messageQueue = new LinkedBlockingQueue<DatabaseUndeployerProgress>());
		}

		try {
			messageQueue.put(new DatabaseUndeployerProgress(taskId, message, isLast));
		} catch (InterruptedException e) {
			LOGGER.warn("Error occurred while trying to enqueue message for task id " + taskId, e);
		}
	}

	/**
	 * Gets mext message report for the given task id.
	 *
	 * @param taskId
	 * @return
	 */
	public static DatabaseUndeployerProgress getMessageFromQueue(final Long taskId) {
		final LinkedBlockingQueue<DatabaseUndeployerProgress> messageQueue = taskIdMessagesPair.get(taskId);

		if (messageQueue == null) {
			return new DatabaseUndeployerProgress(taskId, "Queue message for task id " + taskId + " not found.", true);
		} else {
			DatabaseUndeployerProgress reportMessage = null;

			try {
				reportMessage = messageQueue.take();
			} catch (InterruptedException e) {
				LOGGER.warn("Error occurred while trying to dequeue message for task id " + taskId, e);
			} finally {
				/**
				 * If last message, remove queue from map. If there is a failure to get a message from
				 * the queue, notify of failure and remove queue from map.
				 */
				if (reportMessage == null || reportMessage.getProgressMessage() == null) {
					taskIdMessagesPair.remove(taskId);

					return new DatabaseUndeployerProgress(taskId, "Error occurred while waiting to message from task id " + taskId + " queue.", true);
				} else if (reportMessage.isLast()) {
					taskIdMessagesPair.remove(taskId);
				}

				return reportMessage;
			}
		}
	}
}
