package edu.mayo.mprc.daemon;

import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.worker.*;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests the daemon workers.
 */
public final class DaemonWorkerTest {

	@Test
	public void shouldDoSimpleWork() throws InterruptedException {
		final DaemonWorkerTester tester = new DaemonWorkerTester(createSimpleWorker());
		runTest(tester, 2);
	}

	@Test
	public void shouldDoSimpleWorkInThreadPool() throws InterruptedException {
		final DaemonWorkerTester tester = new DaemonWorkerTester(new WorkerFactory<ResourceConfig, Worker>() {
			@Override
			public Worker create(ResourceConfig config, DependencyResolver dependencies) {
				return createSimpleWorker();
			}

			@Override
			public Worker createSingleton(ResourceConfig config, DependencyResolver dependencies) {
				return create(config, dependencies);
			}

			@Override
			public String getType() {
				return "daemonWorker";
			}

			@Override
			public String getUserName() {
				return "DaemonWorkerTester";
			}

			@Override
			public String getDescription() {
				return "Tester for daemon workers";
			}

			@Override
			public Class<? extends ResourceConfig> getConfigClass() {
				return null;
			}

			@Override
			public ServiceUiFactory getServiceUiFactory() {
				return null;
			}
		}, 3);
		runTest(tester, 6);
	}

	private static final class StringWorkPacket extends WorkPacketBase {
		private static final long serialVersionUID = 20101221L;
		private String value;

		private StringWorkPacket(final String value) {
			super(false);
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	private static Worker createSimpleWorker() {
		return new WorkerBase() {
			private AtomicInteger concurrentRequests = new AtomicInteger(0);

			@Override
			public void process(final WorkPacket wp, final File tempWorkFolder, final UserProgressReporter progressReporter) {
				Assert.assertEquals(concurrentRequests.incrementAndGet(), 1, "The amount of requests must start at 1. The worker calls are not serialized.");
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					throw new DaemonException(e);
				}
				Assert.assertEquals(concurrentRequests.decrementAndGet(), 0, "The amount of requests must end at 0. The worker calls are not serialized.");
			}

			@Override
			public File createTempWorkFolder() {
				return null;
			}

			@Override
			public String check() {
				return null;
			}
		};
	}

	private static void runTest(final DaemonWorkerTester tester, final int iterations) throws InterruptedException {
		try {
			tester.start();
			final Object[] token = new Object[iterations];
			for (int i = 0; i < iterations; i++) {
				token[i] = tester.sendWork(new StringWorkPacket("hello #" + i), null);
			}
			/**
			 * Give the search at most 10 seconds.
			 */
			for (int i = 0; i < 10000; i++) {
				boolean allDone = true;
				for (int j = 0; j < iterations; j++) {
					if (!tester.isDone(token[j])) {
						allDone = false;
						break;
					}
				}
				if (allDone) {
					break;
				}
				Thread.sleep(10);
				i += 10;
			}
			tester.stop();
			for (int i = 0; i < iterations; i++) {
				Assert.assertTrue(tester.isDone(token[i]), "Work is not done");
				if (!tester.isSuccess(token[i])) {
					throw new DaemonException(tester.getLastError(token[i]));
				}
			}
		} finally {
			tester.stop();
		}
	}
}
