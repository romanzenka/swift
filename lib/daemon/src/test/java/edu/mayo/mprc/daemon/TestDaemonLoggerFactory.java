package edu.mayo.mprc.daemon;

import com.google.common.base.Charsets;
import edu.mayo.mprc.daemon.worker.log.NewLogFiles;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ResourceUtilities;
import edu.mayo.mprc.utilities.log.ChildLog;
import edu.mayo.mprc.utilities.log.ParentLog;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.mockito.ArgumentCaptor;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static org.mockito.Mockito.*;

/**
 * @author Roman Zenka
 */
public final class TestDaemonLoggerFactory {
	private static final Logger LOGGER = Logger.getLogger(TestDaemonLoggerFactory.class);

	private List<Appender> appenders;

	private File logFolder;
	private DaemonLoggerFactory factory;
	private UUID id;
	private UserProgressReporter reporter;

	private ParentLog log;
	private ChildLog childLog;
	private ArgumentCaptor<NewLogFiles> logFiles;

	@BeforeTest
	public void setup() throws IOException {
		clearLog4j();

		// Configure log4j using a property file
		Reader reader = ResourceUtilities.getReader("classpath:edu/mayo/mprc/daemon/log4j.properties", TestDaemonLoggerFactory.class);
		Properties props = new Properties();
		props.load(reader);
		PropertyConfigurator.configure(props);

		logFolder = FileUtilities.createTempFolder();
		factory = new DaemonLoggerFactory(logFolder);
		id = UUID.randomUUID();
		reporter = mock(UserProgressReporter.class);

		log = factory.createLog(id, reporter);
		childLog = log.createChildLog();
		logFiles = ArgumentCaptor.forClass(NewLogFiles.class);
		verify(reporter, times(1)).reportProgress(logFiles.capture());
	}

	@AfterTest
	public void teardown() {
		FileUtilities.cleanupTempFile(logFolder);
		restoreLog4j();
	}

	@Test
	public void shouldReportLogCreation() {
		Assert.assertEquals(logFiles.getValue().getParentLogId(), id);
		Assert.assertEquals(logFiles.getValue().getLogId(), childLog.getLogId());
	}

	@Test
	public void shouldStartStopLogging() {
		childLog.startLogging();
		LOGGER.info("A sample log message");
		LOGGER.error("A sample error message");

		childLog.stopLogging();

		final String out = getOutputLogContents();
		Assert.assertTrue(out.contains("A sample log message"));

		final String err = getErrorLogContents();
		Assert.assertTrue(err.contains("A sample error message"));
	}

	@Test
	public void shouldProvideLogger() {
		childLog.getOutputLogger().info("--- direct through logger ---");
		childLog.getErrorLogger().error("--- error direct through logger ---");

		final String out = getOutputLogContents();
		Assert.assertTrue(out.contains("--- direct through logger ---"));

		final String err = getErrorLogContents();
		Assert.assertTrue(err.contains("--- error direct through logger ---"));
	}

	private String getErrorLogContents() {
		final File errorLog = logFiles.getValue().getErrorLogFile();
		return FileUtilities.toString(errorLog, Charsets.UTF_8, 10000);
	}

	private String getOutputLogContents() {
		final File outputLog = logFiles.getValue().getOutputLogFile();
		return FileUtilities.toString(outputLog, Charsets.UTF_8, 10000);
	}

	private void clearLog4j() {
		Enumeration allAppenders = Logger.getRootLogger().getAllAppenders();
		appenders = new ArrayList<Appender>(10);
		while (allAppenders.hasMoreElements()) {
			appenders.add((Appender) allAppenders.nextElement());
		}

		Logger.getRootLogger().removeAllAppenders();
	}

	private void restoreLog4j() {
		Logger.getRootLogger().removeAllAppenders();
		for (final Appender appender : appenders) {
			Logger.getRootLogger().addAppender(appender);
		}
	}
}
