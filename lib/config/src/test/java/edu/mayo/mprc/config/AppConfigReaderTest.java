package edu.mayo.mprc.config;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.StringReader;

/**
 * @author Roman Zenka
 */
public final class AppConfigReaderTest {

	public static final TestMultiFactory MULTI_FACTORY = new TestMultiFactory();

	@Test
	public void testUnescapeLine() throws Exception {
		Assert.assertEquals(AppConfigReader.unescapeLine(null), null);
		Assert.assertEquals(AppConfigReader.unescapeLine("   "), "");
		Assert.assertEquals(AppConfigReader.unescapeLine("hello world"), "hello world");
		Assert.assertEquals(AppConfigReader.unescapeLine("    hello world    "), "hello world");
		Assert.assertEquals(AppConfigReader.unescapeLine("hello\\nworld"), "hello\nworld");
		Assert.assertEquals(AppConfigReader.unescapeLine("hello\\rworld"), "hello\rworld");
		Assert.assertEquals(AppConfigReader.unescapeLine("hello\\\\world"), "hello\\world");
		Assert.assertEquals(AppConfigReader.unescapeLine("hello\\\\\\nworld"), "hello\\\nworld");
		Assert.assertEquals(AppConfigReader.unescapeLine(" hello world    #  Comment"), "hello world");
		Assert.assertEquals(AppConfigReader.unescapeLine(" hello world \\# Comment \\## Comment2"), "hello world # Comment #");
	}

	@Test
	public void shouldLoadEmptyApp() {
		AppConfigReader reader = new AppConfigReader(new StringReader(AppConfigWriterTest.getHeader()), MULTI_FACTORY);
		final ApplicationConfig config = reader.load();
		Assert.assertEquals(config.getDaemons().size(), 0);
	}

	@Test
	public void shouldLoadSimpleApp() {
		AppConfigReader reader = new AppConfigReader(new StringReader(AppConfigWriterTest.getHeader() + "\n" + AppConfigWriterTest.getBody()), MULTI_FACTORY);
		final ApplicationConfig config = reader.load();
		Assert.assertEquals(config.getDaemons().size(), 1);
		final DaemonConfig daemon = config.getDaemons().get(0);
		Assert.assertEquals(daemon.getName(), "daemon1");
		Assert.assertEquals(daemon.getServices().size(), 1);
		final ServiceConfig service = daemon.getServices().get(0);
		Assert.assertEquals(service.getName(), "service1");
		Assert.assertNotNull(service.getRunner());
		Assert.assertTrue(service.getRunner().getWorkerConfiguration() instanceof TestResource);
	}
}
