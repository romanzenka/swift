package edu.mayo.mprc.dbundeploy;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.AssignedTaskData;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.enginedeployment.DeploymentResult;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Database undeployment generic task. The main difference between this task and
 * the AsyncTaskBase class is that the monitoring of the progress and the processing
 * of progress messages is done by this class. Progress messages and objects are not
 * forward to be stored in the Swift database.
 *
 * @deprecated This is implemented in too complicated fashion
 */
public final class DatabaseUndeploymentTask {
	private AssignedTaskData assignedTaskData;
	private Throwable throwable;

	private boolean isDone;
	private final Object monitor = new Object();

	private final Curation dbToUndeploy;

	private static final Logger LOGGER = Logger.getLogger(DatabaseUndeploymentTask.class);

	private static AtomicLong taskIdPostFix = new AtomicLong(0);
	private static final String TASK_ID_PREFIX = "DatabaseUndeployment_";

	private LinkedList<String> messages;
	private DaemonConnection deploymentDaemon;

	public DatabaseUndeploymentTask(final DaemonConnection deploymentDaemon, final Curation dbToUndeploy, final FileTokenFactory fileTokenFactory) {
		this.deploymentDaemon = deploymentDaemon;
		this.dbToUndeploy = dbToUndeploy;

		messages = new LinkedList<String>();
	}

	public WorkPacket createWorkPacket() {
		final DeploymentRequest workPacket = new DeploymentRequest(
				deploymentDaemon.getConnectionName() + TASK_ID_PREFIX + taskIdPostFix.incrementAndGet(),
				dbToUndeploy.getFastaFile());
		workPacket.setUndeployment(true);

		return workPacket;
	}

	public void run() {
		if (deploymentDaemon == null) {
			throw new MprcException("The daemon for database undeployment task was not set.");
		}

		final WorkPacket workPacket = createWorkPacket();

		deploymentDaemon.sendWork(workPacket, new ProgressListener() {
			@Override
			public void requestEnqueued(final String hostString) {
			}

			@Override
			public void requestProcessingStarted(final String hostString) {
			}

			@Override
			public void requestProcessingFinished() {
				onSuccess();
			}

			@Override
			public void requestTerminated(final Exception e) {
				onFailure(e);
			}

			@Override
			public void userProgressInformation(final ProgressInfo progressInfo) {
				onProgress(progressInfo);
			}
		});
	}

	public UndeploymentTaskResult waitUntilDone() {
		synchronized (monitor) {
			while (!isDone) {
				try {
					monitor.wait();
				} catch (InterruptedException e) {
					LOGGER.warn("Exception occurred while waiting for task completion.", e);
				}
			}
		}

		String outputLog = null;
		String errorLog = null;

		if (assignedTaskData != null) {
			outputLog = assignedTaskData.getOutputLogFile().getAbsolutePath();
			errorLog = assignedTaskData.getErrorLogFile().getAbsolutePath();
		}

		final UndeploymentTaskResult undeploymentTaskResult = new UndeploymentTaskResult(throwable == null, outputLog, errorLog);
		undeploymentTaskResult.setExecutionError(throwable);
		undeploymentTaskResult.addAllMessage(messages);

		return undeploymentTaskResult;
	}

	public void onSuccess() {
		synchronized (monitor) {
			try {
				isDone = true;
			} finally {
				monitor.notifyAll();
			}
		}
	}

	private void onFailure(final Throwable t) {
		synchronized (monitor) {
			try {
				isDone = true;
				this.throwable = t;
			} finally {
				monitor.notifyAll();
			}
		}
	}

	public void onProgress(final ProgressInfo progressInfo) {
		if (progressInfo instanceof AssignedTaskData) {
			assignedTaskData = (AssignedTaskData) progressInfo;
		} else if (progressInfo instanceof Throwable) {
			onFailure((Throwable) progressInfo);
		} else if (progressInfo instanceof DeploymentResult) {
			final DeploymentResult deploymentResult = (DeploymentResult) progressInfo;

			/**
			 * This step must synchronize the deployment folder.
			 */
			deploymentResult.synchronizeFileTokensOnReceiver();

			messages.addAll(deploymentResult.getMessages());
		} else {
			messages.add(progressInfo.toString());
		}
	}
}
