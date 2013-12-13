package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Zenka
 */
public final class LauncherTest {
	private File configFile;
	private SwiftEnvironment environment;

	@BeforeClass
	public void setup() throws IOException {
		configFile = TestingUtilities.getTempFileFromResource(LauncherTest.class, "/edu/mayo/mprc/launcher/test.conf", true, null);
	}


	@AfterClass
	public void teardown() {
		FileUtilities.cleanupTempFile(configFile);
	}

	@Test
	public void shouldGetDefaultTemp() {
		environment = mock(SwiftEnvironment.class);
		Assert.assertTrue(Launcher.getTempFolder(environment, false).isDirectory());
	}

	@Test
	public void shouldGetTempFromConfig() {
		environment = mock(SwiftEnvironment.class);
		DaemonConfig daemonConfig = new DaemonConfig();
		daemonConfig.setTempFolderPath("/tmp/main");
		when(environment.getDaemonConfig()).thenReturn(daemonConfig);

		Assert.assertEquals(Launcher.getTempFolder(environment, false), new File("/tmp/main"));
	}

	@Test
	public void shouldGetPortFromCommandLine() {
		environment = mock(SwiftEnvironment.class);
		when(environment.getParameters()).thenReturn(Arrays.asList("port", "8100"));

		Assert.assertEquals(Launcher.getPortNumber(environment, false), 8100);
	}

	@Test
	public void shouldGetPortFromConfig() {
		environment = mock(SwiftEnvironment.class);
		final DaemonConfig daemonConfig = new DaemonConfig();
		daemonConfig.addResource(new WebUi.Config(null, "8123", "", "", "", null, null, null));
		when(environment.getDaemonConfig()).thenReturn(daemonConfig);
		when(environment.getParameters()).thenReturn(new ArrayList<String>(0));

		Assert.assertEquals(Launcher.getPortNumber(environment, false), 8123);
	}

	@Test
	public void shouldGetDefaultPort() {
		environment = mock(SwiftEnvironment.class);
		Assert.assertEquals(Launcher.getPortNumber(environment, false), 8080);
	}

}
