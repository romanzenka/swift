package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.config.Lifecycle;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerFactory;
import edu.mayo.mprc.messaging.ActiveMQConnectionPool;
import edu.mayo.mprc.messaging.ResponseDispatcher;
import edu.mayo.mprc.messaging.Service;
import edu.mayo.mprc.messaging.ServiceFactory;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test harness for {@link edu.mayo.mprc.daemon.worker.Worker} classes. Wrap a daemon worker
 */
public final class DaemonWorkerTester implements Lifecycle {
	private static final Logger LOGGER = Logger.getLogger(DaemonWorkerTester.class);
	private SimpleRunner runner;
	private Service service;
	private DaemonConnection daemonConnection;
	private ServiceFactory serviceFactory;
	private ResponseDispatcher responseDispatcher;
	private static AtomicInteger testId = new AtomicInteger(0);

	private DaemonWorkerTester() {
		serviceFactory = new ServiceFactory();
		serviceFactory.setConnectionPool(new ActiveMQConnectionPool());
		try {
			serviceFactory.setBrokerUri(new URI("vm://broker1?broker.useJmx=false&broker.persistent=false"));
		} catch (URISyntaxException e) {
			throw new MprcException(e);
		}
	}

	/**
	 * Creates a test runner that runs given worker in a single thread. Automatically starts a thread.
	 *
	 * @param worker Worker to run
	 */
	public DaemonWorkerTester(final Worker worker) {
		this();
		runner = new SimpleRunner();
		runner.setFactory(new TestWorkerFactory(worker));
		runner.setExecutorService(getSingleThreadExecutor(worker));
		final Daemon daemon = new Daemon();
		daemon.setLogOutputFolder(FileUtilities.createTempFolder());
		runner.setDaemon(daemon);
		runner.setEnabled(true);
	}

	private static ExecutorService getSingleThreadExecutor(Worker worker) {
		return new SimpleThreadPoolExecutor(1, worker.getClass().getSimpleName() + "-runner", true);
	}

	private void initializeFromQueueName(final String queueName) {
		service = serviceFactory.createService(queueName, responseDispatcher);
		final FileTokenFactory fileTokenFactory = new FileTokenFactory();
		fileTokenFactory.setDaemonConfigInfo(new DaemonConfigInfo("daemon1", "shared"));
		daemonConnection = new DirectDaemonConnection(service, fileTokenFactory);
		runner.setDaemonConnection(daemonConnection);
	}


	/**
	 * Creates a test runner that runs given worker. Automatically starts running.
	 *
	 * @param workerFactory    Creates workers to run
	 * @param numWorkerThreads How many threads does the worker run in
	 */
	public DaemonWorkerTester(final WorkerFactory workerFactory, final int numWorkerThreads) {
		this();
		runner = new SimpleRunner();
		runner.setFactory(workerFactory);
		runner.setExecutorService(new SimpleThreadPoolExecutor(numWorkerThreads, "test", true));
		final Daemon daemon = new Daemon();
		daemon.setLogOutputFolder(FileUtilities.createTempFolder());
		runner.setDaemon(daemon);
		runner.setEnabled(true);
	}

	/**
	 * Sends a work packet to the tested worker.
	 *
	 * @param workPacket Data to be processed.
	 * @return Token for this work packet. The token is an opaque object to be used by {@link #isDone}, {@link #isSuccess} and {@link #getLastError}
	 *         methods.
	 */
	public Object sendWork(final WorkPacket workPacket) {
		if (!isRunning()) {
			start();
		}
		final TestProgressListener listener = new TestProgressListener();
		daemonConnection.sendWork(workPacket, listener);
		return listener;
	}

	/**
	 * Sends a work packet to the tested worker.
	 *
	 * @param workPacket   Data to be processed.
	 * @param userListener Listener to be called as the worker progresses.
	 * @return Token for this work packet. The token is an opaque object to be used by {@link #isDone}, {@link #isSuccess} and {@link #getLastError}
	 *         methods.
	 */
	public Object sendWork(final WorkPacket workPacket, final ProgressListener userListener) {
		if (!isRunning()) {
			start();
		}
		final TestProgressListener listener = new TestProgressListener(userListener);
		daemonConnection.sendWork(workPacket, listener);
		return listener;
	}

	@Override
	public boolean isRunning() {
		return runner.isRunning();
	}

	@Override
	public void start() {
		if (!isRunning()) {
			if (serviceFactory != null) {
				serviceFactory.start();
			}
			responseDispatcher = new ResponseDispatcher(serviceFactory, "test-daemon");
			responseDispatcher.start();
			final String queueName = "test_" + testId.incrementAndGet();
			initializeFromQueueName(queueName);
			runner.start();
			waitUntilReady(runner);
			daemonConnection.start();
		}
	}

	/**
	 * Stop the execution of the daemon as soon as possible (the thread does not quit immediatelly, so
	 * there may still be some progress info being sent back).
	 */
	public void stop() {
		if (isRunning()) {
			runner.stop();
			daemonConnection.stop();
			responseDispatcher.stop();
			if (serviceFactory != null) {
				serviceFactory.stop();
			}
		}
	}

	/**
	 * @param workToken Token for work sent through {@link #sendWork}.
	 * @return true if there was a search running that is done now.
	 */
	public boolean isDone(final Object workToken) {
		final TestProgressListener listener = (TestProgressListener) workToken;
		return listener != null && listener.isDone();
	}

	/**
	 * @param workToken Token for work sent through {@link #sendWork}.
	 * @return true if there was a search running which succeeded
	 */
	public boolean isSuccess(final Object workToken) {
		final TestProgressListener listener = (TestProgressListener) workToken;
		return listener != null && listener.isSuccess();
	}

	/**
	 * @param workToken Token for work sent through {@link #sendWork}.
	 * @return The last error in case there was any, null otherwise.
	 */
	public Throwable getLastError(final Object workToken) {
		final TestProgressListener listener = (TestProgressListener) workToken;
		return listener.getLastError();
	}

	public DaemonConnection getDaemonConnection() {
		return daemonConnection;
	}

	/**
	 * Wait until the daemon signalizes it has started up. This is important for unit tests.
	 *
	 * @param runner
	 */
	public static void waitUntilReady(final AbstractRunner runner) {
		if (!runner.isEnabled()) {
			return;
		}
		while (!runner.isOperational()) {
			try {
				// Yield
				Thread.sleep(0);
			} catch (InterruptedException e) {
				throw new MprcException("Daemon worker startup interrupted", e);
			}
		}
		LOGGER.info("The runner for " + runner.toString() + " is up and ready.");
	}

	private static final class TestProgressListener implements ProgressListener {
		private boolean success = false;
		private boolean done = false;
		private Throwable lastError = null;
		private ProgressListener userListener = null;

		TestProgressListener() {
		}

		private TestProgressListener(final ProgressListener userListener) {
			this.userListener = userListener;
		}

		public boolean isSuccess() {
			return success;
		}

		public boolean isDone() {
			return done;
		}

		public Throwable getLastError() {
			return lastError;
		}

		@Override
		public void requestEnqueued(final String hostString) {
			if (null != userListener) {
				userListener.requestEnqueued(hostString);
			}
		}

		@Override
		public void requestProcessingStarted(final String hostString) {
			LOGGER.debug("Starting work");
			if (null != userListener) {
				userListener.requestProcessingStarted(hostString);
			}
		}

		@Override
		public void requestProcessingFinished() {
			success = true;
			done = true;
			LOGGER.debug("Work successful");
			if (null != userListener) {
				userListener.requestProcessingFinished();
			}
		}

		@Override
		public void requestTerminated(final Exception e) {
			LOGGER.error("Work failed", e);
			lastError = e;
			done = true;
			if (null != userListener) {
				userListener.requestTerminated(e);
			}
		}

		@Override
		public void userProgressInformation(final ProgressInfo progressInfo) {
			LOGGER.debug("Work progress: " + progressInfo.toString());
			if (null != userListener) {
				userListener.userProgressInformation(progressInfo);
			}
		}
	}

}
