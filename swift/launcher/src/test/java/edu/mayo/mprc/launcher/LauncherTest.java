package edu.mayo.mprc.launcher;

import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Roman Zenka
 */
public final class LauncherTest {
	private File configFile;
	private Launcher launcher;

	@BeforeClass
	public void setup() throws IOException {
		configFile = TestingUtilities.getTempFileFromResource(LauncherTest.class, "/edu/mayo/mprc/launcher/test.conf", true, null);
		launcher = new Launcher();
	}

	@AfterClass
	public void teardown() {
		FileUtilities.cleanupTempFile(configFile);
	}

	@Test
	public void testGetTempFolder() throws Exception {
		Assert.assertEquals(launcher.getTempFolder(null, configFile), new File("/tmp/main"));
	}

	@Test
	public void testGetPortNumber() throws Exception {
		Assert.assertEquals(launcher.getPortNumber(null, configFile), 8080);
	}
}
