package edu.mayo.mprc.utilities;

import edu.mayo.mprc.MprcException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Downloads a given URL asynchronously and writes it into a provided file.
 * While the download is running, progress is being logged in yet another thread.
 */
public final class AsyncFileWriter implements Runnable, Future<File> {
	private static final Logger LOGGER = Logger.getLogger(AsyncFileWriter.class);
	private final URL source;
	private final File outFile;
	private final AtomicBoolean cancelled = new AtomicBoolean(false);
	private final BlockingQueue<File> results = new ArrayBlockingQueue<File>(1);
	private final Integer sourceSize;
	private static final int PROGRESS_UPDATE_GRANULARITY = 2500;

	private AsyncFileWriter(final URL source, final File toWriteTo) throws IOException {
		this.source = source;
		final URLConnection connection = source.openConnection();
		connection.connect();
		sourceSize = connection.getContentLength();
		this.outFile = toWriteTo;
	}

	@Override
	public void run() {
		InputStream istream = null;
		try {
			final URLConnection connection = source.openConnection();
			istream = connection.getInputStream();
			FileUtilities.writeStreamToFile(istream, outFile);
			synchronized (this) {
				this.results.add(outFile);
				this.notifyAll();
			}
		} catch (Exception e) {
			throw new MprcException("Error writing file.", e);
		} finally {
			FileUtilities.closeQuietly(istream);
		}
	}

	/**
	 * Spawns two threads. One does the asynchronous transfer from given url to given file. The other one
	 * produces progress message.
	 *
	 * @param progressMessage Message prepended to % of progress.
	 */
	public static AsyncFileWriter writeURLToFile(final URL source, final File file, final String progressMessage) throws IOException {
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final AsyncFileWriter writer = new AsyncFileWriter(source, file);
		executor.execute(writer);
		LOGGER.debug("Downloading " + source + " to " + file.getAbsolutePath()
				+ ".  The size is " + (writer.getDownloadTotalSize() == -1 ? "undetermined" : writer.getDownloadTotalSize()));
		if (progressMessage != null) {
			writer.monitorProgress(progressMessage);
		}
		return writer;
	}

	public float getProgress() {
		return 100f * outFile.length() / (float) sourceSize;
	}

	public long getDownloadTotalSize() {
		return sourceSize;
	}

	@Override
	public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
		if (isDone() || mayInterruptIfRunning) {
			this.cancelled.set(true);
			notifyAll();
			return true;
		}
		return false;
	}

	@Override
	public synchronized boolean isCancelled() {
		return this.cancelled.get();
	}

	@Override
	public synchronized boolean isDone() {
		return !results.isEmpty();
	}

	@Override
	public synchronized File get() throws InterruptedException {
		return results.take();
	}

	@Override
	public synchronized File get(final long timeout, final TimeUnit unit) throws InterruptedException {
		return results.poll(timeout, unit);
	}

	private void monitorProgress(final String progressMessage) {
		new Thread(this).start();
		while (!this.isDone() && !this.isCancelled()) {
			if (sourceSize < 0) {
				LOGGER.debug(progressMessage + "(progress undetermined)");
			} else {
				LOGGER.debug(progressMessage + this.getProgress() + "%");
			}
			try {
				synchronized (this) {
					this.wait(PROGRESS_UPDATE_GRANULARITY);
				}
			} catch (InterruptedException e) {
				throw new MprcException("Download interrupted", e);
			}
		}
	}
}
