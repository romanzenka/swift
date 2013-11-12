package edu.mayo.mprc.daemon;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.Checkable;
import edu.mayo.mprc.config.Installable;
import edu.mayo.mprc.config.Lifecycle;
import edu.mayo.mprc.daemon.files.FileTokenHolder;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A daemon thread. When a request arrives, the daemon processes it. Responses to requests
 * are sent when the request starts and when it finishes being processed.
 * <p/>
 * Daemon thread runs a {@link edu.mayo.mprc.daemon.worker.Worker}. The worker has {@link edu.mayo.mprc.daemon.worker.Worker#processRequest} method,
 * that gets executed. Then the daemon starts processing the next request.
 *
 * @author Roman Zenka
 */
public abstract class AbstractRunner implements Checkable, Installable, Lifecycle {

	private static final Logger LOGGER = Logger.getLogger(AbstractRunner.class);

	/**
	 * Daemon this runner belongs to. Daemon is accessed to get some configuration information.
	 */
	private Daemon daemon;
	private ExecutorService executorService;
	private SynchronousRequestReceiver receiver;

	public abstract boolean isEnabled();

	public abstract void setEnabled(boolean enabled);

	/**
	 * @return true if this runner got past its initialization and now is ready for requests.
	 *         Disabled workers are operational immediately, they just do nothing.
	 */
	public abstract boolean isOperational();

	public abstract void setOperational(boolean operational);

	public abstract DaemonConnection getDaemonConnection();

	public abstract void setDaemonConnection(DaemonConnection daemonConnection);

	public abstract String toString();

	private final AtomicInteger requestCount = new AtomicInteger(0);

	protected AbstractRunner() {
	}

	@Override
	public boolean isRunning() {
		return isOperational();
	}

	/**
	 * Starts processing the input.
	 */
	@Override
	public void start() {
		if (!isEnabled()) {
			return;
		}
		LOGGER.info("Starting " + toString());

		executorService = Executors.newFixedThreadPool(1,
				new ThreadFactoryBuilder()
						.setDaemon(false)
						.setNameFormat(getDaemonConnection().getConnectionName() + "-abstract-runner-%d")
						.build());
		receiver = new SynchronousRequestReceiver();
		executorService.execute(receiver);

		setOperational(true);
	}

	@Override
	public void stop() {
		if (receiver != null) {
			receiver.cleanShutdown();
		}
		if (executorService != null) {
			executorService.shutdown();
		}
	}

	public void awaitTermination() {
		try {
			executorService.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException ignore) {
			LOGGER.warn("Termination of daemon was interrupted");
		}
	}

	protected abstract void processRequest(DaemonRequest request);

	/**
	 * Called synchronously when the daemon receives a new request.
	 *
	 * @param request
	 */
	private void requestReceived(final DaemonRequest request) {
		if (!isEnabled()) {
			return;
		}
		try {
			NDC.push(request.getWorkPacket().getTaskId());
			requestCount.incrementAndGet();

			sendResponse(request, new DaemonProgressMessage(DaemonProgress.RequestEnqueued), false);
			processRequest(request);
		} finally {
			NDC.pop();
		}
	}

	/**
	 * Send response to the client. Ignore exceptions.
	 *
	 * @param response Response to be sent.
	 */
	protected void sendResponse(final DaemonRequest request, final Serializable response, final boolean isLast) {
		try {
			if (isLast) {
				requestCount.decrementAndGet();

				synchronized (requestCount) {
					requestCount.notifyAll();
				}
			}

			if (response instanceof DaemonProgressMessage) {
				final DaemonProgressMessage daemonProgressMessage = (DaemonProgressMessage) response;
				//If response if a FileTokenHolder, set FileTokenFactory and force regenerating FileTokens.
				if (daemonProgressMessage.getProgressData() instanceof FileTokenHolder) {
					((FileTokenHolder) daemonProgressMessage.getProgressData()).translateOnSender(getDaemonConnection().getFileTokenFactory());
				}
			}

			// This is the last response we will send - error occurred
			request.sendResponse(response, isLast);
		} catch (MprcException e1) {
			// SWALLOWED: We try to keep running even if we cannot report we failed translating the message.
			LOGGER.error("Ignored error: failed following response to client: '" + response.toString() + "'", e1);
		}
	}

	/**
	 * Check the associate worker, return null if ok, message when something is wrong.
	 */
	@Override
	public abstract String check();

	class SynchronousRequestReceiver implements Runnable {
		private volatile boolean keepRunning = true;

		@Override
		public void run() {
			while (keepRunning) {
				final DaemonRequest request = getDaemonConnection().receiveDaemonRequest(5000);
				if (request != null) {
					requestReceived(request);
				}
			}

			getDaemonConnection().close();

			synchronized (requestCount) {
				while (requestCount.get() != 0) {
					try {
						requestCount.wait();
					} catch (InterruptedException ignore) {
						//SWALLOWED
					}
				}
			}
		}

		public void cleanShutdown() {
			keepRunning = false;
		}
	}

	public File getLogDirectory() {
		return getDaemon().getLogOutputFolder();
	}

	public Daemon getDaemon() {
		return daemon;
	}

	public void setDaemon(final Daemon daemon) {
		this.daemon = daemon;
	}
}
