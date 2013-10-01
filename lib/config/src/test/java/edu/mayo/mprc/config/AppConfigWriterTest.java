package edu.mayo.mprc.config;

import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author Roman Zenka
 */
public final class AppConfigWriterTest {

	private StringWriter stringWriter;
	private AppConfigWriter writer;
	private ApplicationConfig config;
	private String header;
	private String body;

	@BeforeMethod
	private void setup() {
		stringWriter = new StringWriter();
		writer = new AppConfigWriter(stringWriter, new TestMultiFactory());
		config = new ApplicationConfig(null);
		header = getHeader();
		body = getBody();
	}

	public static String getBody() {
		return TestingUtilities.resourceToString("edu/mayo/mprc/config/body.conf");
	}

	public static String getHeader() {
		return TestingUtilities.resourceToString("edu/mayo/mprc/config/header.conf");
	}

	@AfterMethod
	private void teardown() throws IOException {
		writer.close();
		stringWriter.close();
	}

	@Test
	public void shouldWriteEmptyApp() {
		writer.save(config);
		final String result = stringWriter.toString();
		Assert.assertEquals(result, header);
	}

	@Test
	public void shouldWriteOneDaemon() {
		final DaemonConfig daemon = new DaemonConfig();
		daemon.setName("daemon1");
		daemon.addResource(new ServiceConfig("service1", new TestRunnerConfig(new TestResource())));

		config.addDaemon(daemon);
		writer.save(config);
		final String result = stringWriter.toString();
		Assert.assertEquals(result, header + "\n" + body);
	}


}
