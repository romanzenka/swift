package edu.mayo.mprc.config;

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

	public static final String HEADER = "# Application configuration\n" +
			"# Supported types:\n" +
			"#     application    APPLICATION\n" +
			"#     daemon         DAEMON\n" +
			"#     runner         RUNNER\n" +
			"#     service        SERVICE\n" +
			"#     testResource   TESTRESOURCE\n" +
			"#     testResource2  TESTRESOURCE2\n";

	public static final String BODY = "<testResource2 _testResource2_1>\n" +
			"        dummy  dummyVal  # dummyComment\n" +
			"</testResource2>\n" +
			"\n" +
			"<testResource2 _testResource2_2>\n" +
			"        dummy  dummyVal  # dummyComment\n" +
			"</testResource2>\n" +
			"\n" +
			"<service service1>\n" +
			"        runner.type        runner                              # Type of the runner (localRunner/sgeRunner)\n" +
			"        runner.workerType  testResource                        # Type of the worker\n" +
			"        # Test resource\n" +
			"        boolean            true                                \n" +
			"        integer            123                                 # Integer\n" +
			"        key                value                               \n" +
			"        key2               value2                              # Comment\n" +
			"        resource           _testResource2_1                    \n" +
			"        resources          _testResource2_1, _testResource2_2  \n" +
			"</service>\n" +
			"\n" +
			"<daemon daemon1>\n" +
			"        hostName                       # Host the daemon runs on\n" +
			"        osName                         # Host system operating system name: e.g. Windows or Linux.\n" +
			"        osArch                         # Host system architecture: x86, x86_64\n" +
			"        sharedFileSpacePath            # Directory on a shared file system can be accessed from all the daemons\n" +
			"        tempFolderPath                 # Temporary folder that can be used for caching. Transferred files from other daemons with no shared file system with this daemon are cached to this folder.\n" +
			"        dumpErrors           false     # Not implemented yet\n" +
			"        dumpFolderPath                 # Not implemented yet\n" +
			"        resources                      # Comma separated list of provided resources\n" +
			"        services             service1  # Comma separated list of provided services\n" +
			"</daemon>\n";

	private StringWriter stringWriter;
	private AppConfigWriter writer;
	private ApplicationConfig config;

	@BeforeMethod
	private void setup() {
		stringWriter = new StringWriter();
		writer = new AppConfigWriter(stringWriter, new TestMultiFactory());
		config = new ApplicationConfig();
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
		Assert.assertEquals(result, HEADER);
	}

	@Test
	public void shouldWriteOneDaemon() {
		final DaemonConfig daemon = new DaemonConfig();
		daemon.setName("daemon1");
		daemon.addResource(new ServiceConfig("service1", new TestRunnerConfig(new TestResource())));

		config.addDaemon(daemon);
		writer.save(config);
		final String result = stringWriter.toString();
		Assert.assertEquals(result, HEADER + "\n" + BODY);
	}


}
