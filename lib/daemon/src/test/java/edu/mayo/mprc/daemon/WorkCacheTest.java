package edu.mayo.mprc.daemon;

import com.google.common.collect.Lists;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.log.SimpleParentLog;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public final class WorkCacheTest {
	private File cacheFolder;
	private boolean cacheIsStale;

	@Test
	public void shouldCacheWork() {
		final TestConnection connection = new TestConnection();
		connection.start();

		final ProgressReporter reporter = mock(ProgressReporter.class);
		when(reporter.getLog()).thenReturn(new SimpleParentLog());
		cacheFolder = FileUtilities.createTempFolder();

		final TestWorkCache workCache = new TestWorkCache();
		workCache.setCacheFolder(cacheFolder);
		workCache.setDaemon(connection);


		workCache.processRequest(new TestWorkPacket("task1", "request1", null), reporter);
		connection.enqueue(0);
		connection.start(0); // Start 1

		workCache.processRequest(new TestWorkPacket("task2", "request2", null), reporter);
		connection.enqueue(1);
		connection.start(1); // Start 2
		connection.progress(1); // Progress 1
		connection.success(1); // Success 1, Progress 2 (cache report)

		workCache.processRequest(new TestWorkPacket("task1", "request3", null), reporter);
		connection.progress(0); // Progress 3 + 4 (reported twice for task1)
		connection.success(0); // Success 2 + 3 (reported twice for task 1), Progress 5 + 6 (cache report)

		workCache.processRequest(new TestWorkPacket("error", "request4", null), reporter);
		connection.enqueue(2);
		connection.start(2); // Start 3
		connection.progress(2); // Progress 7
		connection.failure(2); // Failure 1

		workCache.processRequest(new TestWorkPacket("task1", "request5", null), reporter);
		// Start 4 (From cache)
		// Progress 8 (cache report)
		// Success 4 (from cache)

		cacheIsStale = true;
		workCache.processRequest(new TestWorkPacket("task1", "request6", null), reporter);
		connection.enqueue(3);
		connection.start(3); // Start >>> 5 <<<
		connection.progress(3); // Progress 9
		connection.success(3); // Success >>> 5 <<<, Progress >>> 10 <<< (cache report)

		final ArgumentCaptor<TestProgressInfo> argument = ArgumentCaptor.forClass(TestProgressInfo.class);

		verify(reporter, times(5)).reportStart("localhost");
		verify(reporter, times(10)).reportProgress(argument.capture());
		verify(reporter, times(5)).reportSuccess();
		verify(reporter, times(1)).reportFailure(Matchers.<Throwable>any());

		final List<TestProgressInfo> allValues = argument.getAllValues();
		int count = 0;
		Assert.assertEquals(allValues.get(count++).getRequest(), "request2");
		Assert.assertEquals(allValues.get(count++).getRequest(), "cache:request2");
		Assert.assertEquals(allValues.get(count++).getRequest(), "request1");
		Assert.assertEquals(allValues.get(count++).getRequest(), "request1"); // Request 3 reported together with 1
		Assert.assertEquals(allValues.get(count++).getRequest(), "cache:request1");
		Assert.assertEquals(allValues.get(count++).getRequest(), "cache:request1"); // Cache request 3 reported together with 1
		Assert.assertEquals(allValues.get(count++).getRequest(), "request4");
		Assert.assertEquals(allValues.get(count++).getRequest(), "cache:request5");
		Assert.assertEquals(allValues.get(count++).getRequest(), "request6");  // Stale cache caused recalculation of request 6
		Assert.assertEquals(allValues.get(count++).getRequest(), "cache:request6");

		Assert.assertEquals(cacheFolder.listFiles().length, 2, "There should be two result folders (two hashes fold into one)");
		Assert.assertFalse(workCache.isWorkInProgress(), "There should be no work in progress anymore");

		connection.stop();
		FileUtilities.cleanupTempFile(cacheFolder);
	}

	public class TestProgressInfo implements ProgressInfo {
		private static final long serialVersionUID = -6401192874783083247L;

		private String request;

		public TestProgressInfo(final String request) {
			this.request = request;
		}

		public String getRequest() {
			return request;
		}
	}

	public class TestConnection implements DaemonConnection {
		private ArrayList<ProgressListener> listeners = new ArrayList<ProgressListener>();
		private ArrayList<TestWorkPacket> workPackets = new ArrayList<TestWorkPacket>();
		private boolean running;

		@Override
		public FileTokenFactory getFileTokenFactory() {
			return null;
		}

		@Override
		public String getConnectionName() {
			return "test";
		}

		@Override
		public void sendWork(final WorkPacket workPacket, final ProgressListener listener) {
			sendWork(workPacket, 5, listener);
		}

		@Override
		public void sendWork(final WorkPacket workPacket, final int priority, final ProgressListener listener) {
			if (!running) {
				throw new MprcException("The service is not running");
			}
			final TestWorkPacket testPacket = (TestWorkPacket) workPacket;
			listeners.add(listener);
			workPackets.add(testPacket);
		}

		public void enqueue(final int index) {
			listeners.get(index).requestEnqueued("localhost");
		}

		public void start(final int index) {
			listeners.get(index).requestProcessingStarted("localhost");
		}

		public void progress(final int index) {
			listeners.get(index).userProgressInformation(new TestProgressInfo(workPackets.get(index).getRequest()));
		}

		public void success(final int index) {
			final TestWorkPacket packet = workPackets.get(index);
			FileUtilities.ensureFileExists(new File(packet.getFolder(), "file1.txt"));
			FileUtilities.ensureFileExists(new File(packet.getFolder(), "file2.txt"));
			listeners.get(index).requestProcessingFinished();
		}

		public void failure(final int index) {
			listeners.get(index).requestTerminated(new DaemonException("Task failed"));
		}

		@Override
		public DaemonRequest receiveDaemonRequest(final long timeout) {
			return null;
		}

		@Override
		public boolean isRunning() {
			return running;
		}

		@Override
		public void start() {
			running = true;
		}

		@Override
		public void stop() {
			running = false;
		}
	}

	public class TestWorkCache extends WorkCache<TestWorkPacket> {
	}

	private class TestWorkPacket extends WorkPacketBase implements CachableWorkPacket {
		private static final long serialVersionUID = -4723515359638194382L;

		private String name;
		private String request;
		private File folder;

		private TestWorkPacket(final String name, final String request, final File folder) {
			super(false);
			this.name = name;
			this.request = request;
			this.folder = folder;
		}

		public String getRequest() {
			return request;
		}

		public File getFolder() {
			return folder;
		}

		@Override
		public boolean isPublishResultFiles() {
			return false;
		}

		@Override
		public File getOutputFile() {
			return null;
		}

		@Override
		public String getStringDescriptionOfTask() {
			return name;
		}

		@Override
		public WorkPacket translateToCachePacket(final File cacheFolder) {
			return new TestWorkPacket(this.name, getRequest(), cacheFolder);
		}

		@Override
		public List<String> getOutputFiles() {
			return Lists.newArrayList("file1.txt", "file2.txt");
		}

		@Override
		public boolean cacheIsStale(final File subFolder, final List<String> outputFiles) {
			return cacheIsStale;
		}

		@Override
		public void reportCachedResult(final ProgressReporter reporter, final File targetFolder, final List<String> outputFiles) {
			reporter.reportProgress(new TestProgressInfo("cache:" + getRequest()));
		}

	}
}
