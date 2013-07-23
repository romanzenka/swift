package edu.mayo.mprc.utilities;

import edu.mayo.mprc.MprcException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Semaphore;

/**
 * @author Roman Zenka
 */
public final class TestFileMonitor {
	private Semaphore semaphore;
	File tempFolder;
	File file;

	@BeforeMethod
	public void setup() {
		tempFolder = FileUtilities.createTempFolder();
		file = new File(tempFolder, "test.txt");
	}

	@AfterMethod
	public void teardown() {
		FileUtilities.quietDelete(file);
		FileUtilities.quietDelete(tempFolder);
	}

	@Test(timeOut = 1000)
	public void shouldHandleEmptyFiles() {
		FileUtilities.waitForFilesBlocking(new ArrayList<File>(0));
	}

	@Test(timeOut = 2000)
	public void shouldHandleCreatedFile() throws IOException, InterruptedException {
		startWaiting(file, 0);
		Assert.assertTrue(file.createNewFile());
		endWaiting();
	}

	@Test(timeOut = 2000)
	public void shouldTimeout() {
		startWaiting(file, 100);
		endWaiting();
	}

	@Test(timeOut = 3000)
	public void shouldHandleModifiedFile() throws IOException, InterruptedException {
		Assert.assertTrue(file.createNewFile());
		startWaiting(file, 2000);
		Thread.sleep(1000);
		file.setLastModified(new Date().getTime());
		endWaiting();
	}

	private void endWaiting() {
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			throw new MprcException(e);
		}
	}

	private void startWaiting(final File file, final int msTimeout) {
		semaphore = new Semaphore(0);
		FileUtilities.waitForFile(file, msTimeout, new FileListener() {
			@Override
			public void fileChanged(Collection<File> files, boolean timeout) {
				semaphore.release();
			}
		});
	}
}
