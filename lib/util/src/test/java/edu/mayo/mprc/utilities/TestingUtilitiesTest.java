package edu.mayo.mprc.utilities;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

@Test(sequential = true)
public final class TestingUtilitiesTest {
	private static final Logger LOGGER = Logger.getLogger(TestingUtilitiesTest.class);

	@Test(groups = {"fast"})
	public void test_getTempFileFromResource() {
		try {
			final File tempFile = TestingUtilities.getTempFileFromResource(getClass(), "/SimpleFile.txt", true, null);
			Assert.assertTrue(tempFile.exists(), "The file wasn't created.");
			if (tempFile.exists()) {
				LOGGER.debug("Temp file created: " + tempFile.getAbsolutePath());
			}
		} catch (IOException e) {
			Assert.fail("Could not create a temporary file from a local resource.", e);
		}
	}

	@Test(groups = {"fast", "unit"})
	public void FileComparisonTest() throws IOException {
		final File f1 = TestingUtilities.getTempFileFromResource(getClass(), "/edu/mayo/mprc/utilities/testing/simpleFileA.txt", true, null);
		final File f2 = TestingUtilities.getTempFileFromResource(getClass(), "/edu/mayo/mprc/utilities/testing/simpleFileACopy.txt", true, null);
		LOGGER.debug(f2.getAbsolutePath());
		Assert.assertTrue(f2.exists());
		Assert.assertEquals(TestingUtilities.compareFilesByLine(f1, f2), null);
	}

	@Test(groups = {"fast", "unit"})
	public void DifferentFileComparisonTest() throws IOException {
		final File f1 = TestingUtilities.getTempFileFromResource(getClass(), "/edu/mayo/mprc/utilities/testing/simpleFileA.txt", true, null);
		final File f2 = TestingUtilities.getTempFileFromResource(getClass(), "/edu/mayo/mprc/utilities/testing/simpleFileB.txt", true, null);
		Assert.assertEquals(TestingUtilities.compareFilesByLine(f1, f2), "Difference in line #1:\n" +
				"[A simple file to compare.]\n" +
				"[A second simple file to compare]");
	}

	/**
	 * Check that we can compare given string to a resource file.
	 */
	@Test
	public void shouldCompareStringToResource() {
		Assert.assertNull(TestingUtilities.compareStringToResourceByLine("hello\nworld", "edu/mayo/mprc/utilities/testing/hello_world.txt"));
		Assert.assertEquals(TestingUtilities.compareStringToResourceByLine("hello\nworld2", "edu/mayo/mprc/utilities/testing/hello_world.txt"), "Difference in line #2:\n" +
				"[world2]\n" +
				"[world]");
	}


}
