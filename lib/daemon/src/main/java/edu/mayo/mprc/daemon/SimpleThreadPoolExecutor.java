package edu.mayo.mprc.daemon;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A thread pool executor that will potentially block if too much work is asked of it.
 */
public final class SimpleThreadPoolExecutor extends ThreadPoolExecutor {

	public SimpleThreadPoolExecutor(final int numThreads, final String threadName, boolean blockIfFull) {
		super(numThreads, numThreads, 1, TimeUnit.SECONDS, blockIfFull ? new BlockingSynchronousQueue<Runnable> () : new LinkedBlockingQueue<Runnable>());

		final String name = threadName == null ? "worker" : threadName;

		setThreadFactory(new ThreadFactoryBuilder()
				.setDaemon(false)
				.setNameFormat(name + "-%d")
				.build());
	}

	/**
	 * A synchronous queue that blocks when elements are offered to it.
	 * @param <T>
	 */
	private static final class BlockingSynchronousQueue<T> extends SynchronousQueue<T> {
		private static final long serialVersionUID = -5525953574329882688L;

		private BlockingSynchronousQueue() {
		}

		private BlockingSynchronousQueue(boolean fair) {
			super(fair);
		}

		@Override
		public boolean offer(T o, long timeout, TimeUnit unit) throws InterruptedException {
			put(o);
			return true;
		}

		@Override
		public boolean offer(T t) {
			try {
				put(t);
			} catch (InterruptedException e) {
				// SWALLOWED: We can report lack of success without throwing an exception
				return false;
			}
			return true;
		}
	}
}
