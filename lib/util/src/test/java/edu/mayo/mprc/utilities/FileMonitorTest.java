package edu.mayo.mprc.utilities;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class FileMonitorTest {
	private final FileMonitor monitor = new FileMonitor(10);
	private final static int TOTAL_FILES = 3;

	@Test
	public void shouldNotModifyConcurrently() throws InterruptedException {

		final MyFileListener listener = new MyFileListener();

		// Submit a file. Upon the file expiring, we submit another files from within the listener
		final File nonExistant = new File("/this/file/does/not/exist" + 0);
		monitor.filesToExist(Arrays.asList(nonExistant), listener, 20);

		Thread.sleep(100);

		monitor.stop();

		Assert.assertEquals(listener.timeoutCount.get(), TOTAL_FILES);
	}

	private class MyFileListener implements FileListener {
		AtomicInteger timeoutCount = new AtomicInteger();
		AtomicInteger numFilesWatched = new AtomicInteger(0);

		@Override
		public void fileChanged(Collection<File> files, boolean timeout) {
			// Expiration should happen
			if (timeout) {
				timeoutCount.incrementAndGet();
			}

			int n = numFilesWatched.incrementAndGet();
			if (n < TOTAL_FILES) {
				final File nonExistant = new File("/this/file/does/not/exist" + n);
				monitor.filesToExist(Arrays.asList(nonExistant), this, 20);
			}
		}
	}
}