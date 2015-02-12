package edu.mayo.mprc.utilities;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class FileMonitorTest {

	@Test
	public void shouldNotModifyConcurrently() throws InterruptedException {
		final FileMonitor monitor = new FileMonitor(10);

		final MyFileListener listener = new MyFileListener();

		for (int i = 0; i < 10; i++) {
			final File nonExistant = new File("/this/file/does/not/exist" + i);
			monitor.filesToExist(Arrays.asList(nonExistant), listener, 20);
		}

		Thread.sleep(100);

		monitor.stop();

		Assert.assertEquals(listener.timeoutCount.get(), 10);
	}

	private static class MyFileListener implements FileListener {
		AtomicInteger timeoutCount = new AtomicInteger();

		@Override
		public void fileChanged(Collection<File> files, boolean timeout) {
			// Expiration should happen
			if (timeout) {
				timeoutCount.incrementAndGet();
			}
		}
	}
}