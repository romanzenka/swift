package edu.mayo.mprc.daemon;

import com.google.common.base.Charsets;
import edu.mayo.mprc.daemon.worker.log.NewLogFiles;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.log.ChildLog;
import edu.mayo.mprc.utilities.log.ParentLog;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.mockito.ArgumentCaptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * @author Roman Zenka
 */
public final class TestDaemonLoggerFactory {
	private static final Logger LOGGER = Logger.getLogger(TestDaemonLoggerFactory.class);

	@Test
	public void shouldLog() {
		final File logFolder = FileUtilities.createTempFolder();
		try {
			final DaemonLoggerFactory factory = new DaemonLoggerFactory(logFolder);
			final UUID id = UUID.randomUUID();
			final UserProgressReporter reporter = mock(UserProgressReporter.class);

			final ParentLog log = factory.createLog(id, reporter);
			final ChildLog childLog = log.createChildLog();

			// We must report to the caller
			final ArgumentCaptor<NewLogFiles> logFiles = ArgumentCaptor.forClass(NewLogFiles.class);
			verify(reporter, times(1))
					.reportProgress(logFiles.capture());

			Assert.assertEquals(logFiles.getValue().getParentLogId(), id);
			Assert.assertEquals(logFiles.getValue().getLogId(), childLog.getLogId());

			childLog.startLogging();
			LOGGER.info("A sample log message");
			LOGGER.error("A sample error message");

			childLog.getOutputLogger().info("--- direct through logger ---");
			childLog.getErrorLogger().error("--- error direct through logger ---");

			childLog.stopLogging();

			final File outputLog = logFiles.getValue().getOutputLogFile();
			final String out = FileUtilities.toString(outputLog, Charsets.UTF_8, 10000);
			Assert.assertTrue(out.contains("A sample log message"));
			Assert.assertTrue(out.contains("--- direct through logger ---"));

			final File errorLog = logFiles.getValue().getErrorLogFile();
			final String err = FileUtilities.toString(outputLog, Charsets.UTF_8, 10000);
			Assert.assertTrue(err.contains("A sample error message"));
			Assert.assertTrue(err.contains("--- error direct through logger ---"));

		} finally {
			FileUtilities.cleanupTempFile(logFolder);
		}
	}
}
