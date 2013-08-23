package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.files.FileTokenHolder;
import edu.mayo.mprc.messaging.ResponseListener;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import java.io.Serializable;

/**
 * Listens to response returned by the daemon and translates it to calls on a given {@link edu.mayo.mprc.utilities.progress.ProgressListener}.
 * TODO: This should get better explanation.
 */
class DaemonResponseListener implements ResponseListener {
	private static final Logger LOGGER = Logger.getLogger(DaemonResponseListener.class);

	private final ProgressListener progressListener;

	private DaemonConnection daemonConnection;

	/**
	 * Reason why the request terminated.
	 */
	private String terminateCause = null;
	private String contextInfo = "(unknown)";

	public DaemonResponseListener(final ProgressListener progressListener, final String contextInfo, final DaemonConnection daemonConnection) {
		this.progressListener = progressListener;
		this.contextInfo = contextInfo;
		this.daemonConnection = daemonConnection;
	}

	@Override
	public void responseReceived(final Serializable response, final boolean isLast) {
		try {
			NDC.push(contextInfo);
			processResponse(response);
		} finally {
			NDC.pop();
		}
	}

	/**
	 * Parse the progress message and feed the listener.
	 *
	 * @param response Daemon response to be parsed and fed to the listener. We assume the listener is set.
	 */
	private void processResponse(final Serializable response) {
		if (null == progressListener) {
			return;
		}
		if (response instanceof DaemonProgressMessage) {
			processProgressMessage((DaemonProgressMessage) response);
		} else if (response instanceof DaemonException) {
			processException((DaemonException) response);
		} else if (response instanceof Throwable) {
			processException(new DaemonException((Throwable) response));
		} else {
			LOGGER.error("Unknown response received: " + response.toString());
		}
	}

	private void processProgressMessage(final DaemonProgressMessage msg) {
		switch (msg.getProgress()) {
			case RequestCompleted:
				requestCompleted(msg);
				break;
			case RequestEnqueued:
				requestEnqueued(msg);
				break;
			case RequestProcessingStarted:
				requestProcessingStarted(msg);
				break;
			case UserSpecificProgressInfo:
				userSpecificProgressInfo(msg);
				break;
			default:
				throw new MprcException("Unsupported daemon progress message type: " + msg.getProgress().name());
		}
	}

	private void requestCompleted(final DaemonProgressMessage msg) {
		logProgress(msg, "completed");
		if (terminateCause == null) {
			terminateCause = "REQUEST_COMPLETED " + (msg.getProgressData() == null ? "null" : msg.getProgressData().toString());
			progressListener.requestProcessingFinished();
		} else {
			throw new MprcException("Internal daemon system failure: Daemon response 'REQUEST_COMPLETED' received, after the request was already terminated by '" + terminateCause + "'");
		}
	}

	private void requestEnqueued(final DaemonProgressMessage msg) {
		logProgress(msg, "enqueued");
		progressListener.requestEnqueued(msg.getHostString());
	}

	private void requestProcessingStarted(final DaemonProgressMessage msg) {
		logProgress(msg, "processing started");
		progressListener.requestProcessingStarted(msg.getHostString());
	}

	private void userSpecificProgressInfo(final DaemonProgressMessage msg) {
		logProgress(msg, "progress info");

		//If response is a FileTokenHolder, translate its tokens to files
		if (msg.getProgressData() instanceof FileTokenHolder) {
			final FileTokenHolder fileTokenHolder = (FileTokenHolder) msg.getProgressData();
			fileTokenHolder.translateOnReceiver(daemonConnection.getFileTokenFactory(), daemonConnection.getFileTokenFactory(), null);
		}

		progressListener.userProgressInformation(msg.getProgressData());
	}

	private void processException(final DaemonException response) {
		LOGGER.debug("Request " + contextInfo + " progress info failed with daemon exception ", response);
		if (terminateCause == null) {
			terminateCause = "Exception " + MprcException.getDetailedMessage(response);
			progressListener.requestTerminated(response);
		} else {
			throw new MprcException("Internal daemon system failure: Daemon response containing exception '" + MprcException.getDetailedMessage((DaemonException) response) + "' received, but the request was already terminated by  '" + terminateCause + "'.");
		}
	}

	private void logProgress(final DaemonProgressMessage msg, final String status) {
		LOGGER.debug("Request " + contextInfo + " " + status + ": " + (msg.getProgressData() == null ? "null" : msg.getProgressData().toString()));
	}
}
