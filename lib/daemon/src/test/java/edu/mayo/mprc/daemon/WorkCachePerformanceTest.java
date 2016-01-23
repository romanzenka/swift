package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.messaging.ActiveMQConnectionPool;
import edu.mayo.mprc.messaging.ResponseDispatcher;
import edu.mayo.mprc.messaging.Service;
import edu.mayo.mprc.messaging.ServiceFactoryImpl;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test that the work cache boosts performance - running 10 identical tasks should take roughly the same time as
 * running just one of them.
 */
public final class WorkCachePerformanceTest {
	private static final Logger LOGGER = Logger.getLogger(WorkCachePerformanceTest.class);
	// How long for the entire run
	private static final int RUN_TIME = 500000;

	// How much will each task take
	private static final int TASK_RUNTIME = 500;
	private static final int TOTAL_MESSAGES = 10;
	private final Object workSuccess = new Object();
	private int workSuccessCount = 0;
	private final ActiveMQConnectionPool connectionPool = new ActiveMQConnectionPool();
	private final ServiceFactoryImpl serviceFactory = new ServiceFactoryImpl();
	private ResponseDispatcher responseDispatcher;

	@BeforeClass
	public void init() {
		serviceFactory.setConnectionPool(connectionPool);
		try {
			serviceFactory.setBrokerUri(new URI("vm://test?broker.useJmx=false&broker.persistent=false"));
		} catch (final URISyntaxException e) {
			throw new MprcException(e);
		}
		serviceFactory.start();
		responseDispatcher = new ResponseDispatcher(serviceFactory, "test-daemon");
		responseDispatcher.start();
	}

	@AfterClass
	public void shutdown() {
		serviceFactory.stop();
		responseDispatcher.stop();
		connectionPool.close();
	}

	@Test
	public void shouldAccelerateWithCache() throws URISyntaxException, InterruptedException {
		final File logFolder = FileUtilities.createTempFolder();
		final File cacheFolder = FileUtilities.createTempFolder();
		final File cacheLogFolder = new File(cacheFolder, "cache_log");
		FileUtilities.ensureFolderExists(cacheLogFolder);

		final String tempFolder = FileUtilities.getDefaultTempDirectory().getAbsolutePath();
		final DaemonConfigInfo daemonConfigInfo = new DaemonConfigInfo("test", tempFolder, tempFolder);
		final FileTokenFactory fileTokenFactory = new FileTokenFactory(daemonConfigInfo);

		final TestWorker worker = new TestWorker();

		final SimpleRunner runner = wrapWithRunner(worker, "simpleTestQueue", logFolder, fileTokenFactory);
		runner.start();

		final TestWorkCache cache = new TestWorkCache();
		cache.setCacheFolder(cacheFolder);

		cache.setDaemon(runner.getDaemonConnection());

		final SimpleRunner cacheRunner = wrapWithRunner(cache, "simpleTestCache", cacheLogFolder, fileTokenFactory);
		cacheRunner.start();

		final DaemonConnection connection = cacheRunner.getDaemonConnection();

		final MyProgressListener listener = new MyProgressListener();

		for (int i = 0; i < TOTAL_MESSAGES; i++) {
			final SimpleTestWorkPacket workPacket = new SimpleTestWorkPacket("task" + i + "-test", false);
			workPacket.setResultFile(new File("text.txt"));
			connection.sendWork(workPacket, listener);
		}

		final long currentTime = System.currentTimeMillis();
		while (true) {
			final long time = System.currentTimeMillis();
			synchronized (workSuccess) {
				if (workSuccessCount == TOTAL_MESSAGES || time > currentTime + RUN_TIME) {
					break;
				} else {
					workSuccess.wait(RUN_TIME / 10);
				}
			}
		}

		final long timeElapsed = System.currentTimeMillis() - currentTime;
		runner.stop();
		cacheRunner.stop();
		synchronized (workSuccess) {
			Assert.assertEquals(workSuccessCount, TOTAL_MESSAGES, "Wrong amount of successfully processed work packets");
		}

		Assert.assertEquals(listener.getProgressInfos().size(), TOTAL_MESSAGES + 1); /* One for each cache log + one for the work itself */

		FileUtilities.cleanupTempFile(logFolder);
		FileUtilities.cleanupTempFile(cacheFolder);

		Assert.assertTrue(timeElapsed < 3 * TASK_RUNTIME, "The total running time is expected to be " + TASK_RUNTIME + "ms but was " + timeElapsed + "ms");
	}

	private SimpleRunner wrapWithRunner(final Worker worker, final String queueName, final File logFolder, final FileTokenFactory fileTokenFactory) throws URISyntaxException {
		final Service service = serviceFactory.createService(queueName, responseDispatcher);
		final DirectDaemonConnection directConnection = new DirectDaemonConnection(service, fileTokenFactory, new DaemonLoggerFactory(logFolder));
		final Daemon daemon = new Daemon();
		daemon.setLogOutputFolder(logFolder);
		final SimpleRunner runner = new SimpleRunner();
		runner.setDaemon(daemon);
		runner.setFactory(new TestWorkerFactory(worker));
		runner.setExecutorService(new SimpleThreadPoolExecutor(1, worker.getClass().getSimpleName() + "-runner", true));
		runner.setDaemonConnection(directConnection);
		runner.setDaemonLoggerFactory(new DaemonLoggerFactory(logFolder));
		return runner;
	}

	private class TestWorkCache extends WorkCache<SimpleTestWorkPacket> {
	}

	private class TestWorker implements Worker {
		@Override
		public void processRequest(final WorkPacket workPacket, final ProgressReporter progressReporter) {
			progressReporter.reportStart("localhost");
			try {
				Thread.sleep(TASK_RUNTIME);
				LOGGER.debug("Request completed");
				final SimpleTestWorkPacket testWorkPacket = (SimpleTestWorkPacket) workPacket;
				FileUtilities.ensureFileExists(testWorkPacket.getResultFile());
			} catch (final InterruptedException e) {
				throw new MprcException(e);
			}
			progressReporter.reportSuccess();
		}

		@Override
		public String check() {
			return null;
		}
	}

	private class MyProgressListener implements ProgressListener {
		private final List<ProgressInfo> progressInfos = new ArrayList<ProgressInfo>();

		@Override
		public void requestEnqueued(final String hostString) {
		}

		@Override
		public void requestProcessingStarted(final String hostString) {
		}

		@Override
		public void requestProcessingFinished() {
			synchronized (workSuccess) {
				workSuccessCount++;
				if (workSuccessCount > TOTAL_MESSAGES) {
					Assert.assertEquals(workSuccessCount, TOTAL_MESSAGES, "Too many messages delivered");
				}
				workSuccess.notifyAll();
			}
		}

		@Override
		public void requestTerminated(final Exception e) {
			Assert.fail("Unexpected exception", e);
		}

		@Override
		public void userProgressInformation(final ProgressInfo progressInfo) {
			synchronized (progressInfos) {
				progressInfos.add(progressInfo);
			}
		}

		public List<ProgressInfo> getProgressInfos() {
			synchronized (progressInfos) {
				return progressInfos;
			}
		}
	}
}
