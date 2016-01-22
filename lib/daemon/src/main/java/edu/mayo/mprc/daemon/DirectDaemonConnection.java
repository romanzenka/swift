package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.daemon.files.FileTokenHolder;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.messaging.Request;
import edu.mayo.mprc.messaging.Service;
import edu.mayo.mprc.utilities.progress.ProgressListener;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wrapper around a Service that represents a connection to a daemon.
 * Takes care of request numbering which aids logging.
 * <p/>
 * After you send work request, you get automatically notified when the work starts being processed, finishes successfully or
 * when an exception occurs.
 * <p/>
 * Daemon connection takes care of proper file transfers. It is equipped with a file token factory that can
 * translate file references to URLs and then transfer files when needed.
 * <p/>
 * The direct daemon connection means there is only one daemon this connection points to. There could be
 * round robin connections or failover connections implemented as well.
 */
final class DirectDaemonConnection implements DaemonConnection {
	public static final int NORMAL_PRIORITY = 4;

	private Service service = null;
	private static AtomicInteger listenerNumber = new AtomicInteger(0);
	private FileTokenFactory fileTokenFactory;
	/**
	 * We will use this parent log to create child loggers for each separate request as they arrive.
	 * This parent logger needs to be set up in such way that it will notify the sender of a request
	 */
	private DaemonLoggerFactory daemonLoggerFactory;

	DirectDaemonConnection(final Service service, final FileTokenFactory fileTokenFactory, final DaemonLoggerFactory daemonLoggerFactory) {
		if (service == null) {
			throw new MprcException("The service must not be null");
		}
		this.service = service;
		this.fileTokenFactory = fileTokenFactory;
		this.daemonLoggerFactory = daemonLoggerFactory;
	}

	@Override
	public FileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	@Override
	public String getConnectionName() {
		return service.getName();
	}

	@Override
	public void sendWork(final WorkPacket workPacket, final ProgressListener listener) {
		sendWork(workPacket, NORMAL_PRIORITY, listener);
	}

	@Override
	public void sendWork(final WorkPacket workPacket, final int priority, final ProgressListener listener) {
		if (!isRunning()) {
			start();
		}

		workPacket.translateOnSender(fileTokenFactory);

		try {
			final int number = listenerNumber.incrementAndGet();
			service.sendRequest(workPacket, priority, new DaemonResponseListener(listener, "R#" + number, this));
		} catch (final MprcException e) {
			// SWALLOWED: The exception is reported directly to the listener
			listener.requestTerminated(new DaemonException(e));
		}
	}

	@Override
	public DaemonRequest receiveDaemonRequest(final long timeout) {
		if (!isRunning()) {
			start();
		}

		final Request request = service.receiveRequest(timeout);
		if (request != null) {
			try {
				return new MyDaemonRequest(request, fileTokenFactory);
			} catch (MprcException e) {
				request.sendResponse(new MprcException("Failed to process the request on the receiver", e), true);
			}
		}
		return null;
	}

	@Override
	public boolean isRunning() {
		return service.isRunning();
	}

	@Override
	public void start() {
		if (!isRunning()) {
			service.start();
		}
	}

	@Override
	public void stop() {
		if (!isRunning()) {
			service.stop();
		}
	}

	private static final class MyDaemonRequest implements DaemonRequest {
		private Request request;

		private MyDaemonRequest(final Request request, final FileTokenFactory fileTokenFactory) {
			this.request = request;

			if (request.getMessageData() instanceof FileTokenHolder) {
				final FileTokenHolder fileTokenHolder = (FileTokenHolder) request.getMessageData();
				fileTokenHolder.translateOnReceiver(fileTokenFactory, null);
			}
		}

		@Override
		public WorkPacket getWorkPacket() {
			return (WorkPacket) request.getMessageData();
		}

		@Override
		public void sendResponse(final Serializable response, final boolean isLast) {
			request.sendResponse(response, isLast);

			if (isLast) {
				processed();
			}
		}

		@Override
		public void processed() {
			request.processed();
		}
	}
}
