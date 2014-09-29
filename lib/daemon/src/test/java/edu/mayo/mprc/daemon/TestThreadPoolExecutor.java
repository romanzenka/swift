package edu.mayo.mprc.daemon;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
@Test(sequential = true)
public final class TestThreadPoolExecutor {
	@Test
	public void shouldBlock() throws InterruptedException {
		final SimpleThreadPoolExecutor executor = new SimpleThreadPoolExecutor(1, "test thread", true);
		final long start = System.currentTimeMillis();
		final int threadCount = Thread.activeCount();
		executor.execute(new MyRunnable());
		Thread.sleep(10);
		final int threadCount2 = Thread.activeCount();
		executor.execute(new MyRunnable());
		Thread.sleep(10);
		final int threadCount3 = Thread.activeCount();
		executor.execute(new MyRunnable());
		final long end = System.currentTimeMillis();
		Assert.assertTrue(end - start > 200, "Two of the tasks must have blocked, the execution takes at least 2*100 ms");
		Assert.assertTrue((threadCount3 - threadCount) <= 1, "Only one thread created");
		Assert.assertTrue((threadCount2 - threadCount) <= 1, "Only one thread created");
	}

	@Test
	public void shouldNotBlock() throws InterruptedException {
		final SimpleThreadPoolExecutor executor = new SimpleThreadPoolExecutor(1, "test thread", false);
		final long start = System.currentTimeMillis();
		final int threadCount = Thread.activeCount();
		executor.execute(new MyRunnable());
		Thread.sleep(10);
		final int threadCount2 = Thread.activeCount();
		executor.execute(new MyRunnable());
		Thread.sleep(10);
		final int threadCount3 = Thread.activeCount();
		executor.execute(new MyRunnable());
		final long end = System.currentTimeMillis();
		Assert.assertTrue(end - start < 100, "No blocking, should execute fast");
		Assert.assertTrue((threadCount3 - threadCount) <= 1, "Only one thread created");
		Assert.assertTrue((threadCount2 - threadCount) <= 1, "Only one thread created");
	}


	private static class MyRunnable implements Runnable {
		@Override
		public void run() {
			try {
				Thread.sleep(100);
			} catch (final InterruptedException e) {
				// SWALLOWED
			}
		}
	}
}
