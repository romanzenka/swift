package edu.mayo.mprc.qa;

import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * Make sure the UV data reader works as expected.
 *
 * @author Roman Zenka
 */
public final class UvDataReaderTest {
	private File testFile;
	private UvDataReader reader;

	@BeforeTest
	public void setup() throws IOException {
		testFile = TestingUtilities.getNamedFileFromResource("/edu/mayo/mprc/qa/uv.tsv", null);
		reader = new UvDataReader(testFile);
	}

	@AfterTest
	public void teardown() {
		FileUtilities.cleanupTempFile(testFile);
	}

	@Test
	public void shouldReturnFirstLine() {
		Assert.assertEquals(
				reader.getLineForKey("0.001300"),
				"0.001300\t10.000\t3.000\t825\t0.500\t1583.670\t1_2\t35.000\t0.000\tDispenseWaste\tRA2\t20.000");
	}

	@Test
	public void shouldReturnLastLine() {
		Assert.assertEquals(
				reader.getLineForKey("50.998283"),
				"50.998283\t1.000\t3.000\t1\t0.500\t1679.100\t10_1\t35.000\t0.000\tIdle\tRA2\t20.000");
	}

	@Test
	public void shouldReturnBeforeFirstLine() {
		Assert.assertEquals(
				reader.getLineForKey("0.0"),
				"0.001300\t10.000\t3.000\t825\t0.500\t1583.670\t1_2\t35.000\t0.000\tDispenseWaste\tRA2\t20.000");
	}

	@Test
	public void shouldReturnAfterLastLine() {
		Assert.assertEquals(
				reader.getLineForKey("100.0"),
				"50.998283\t1.000\t3.000\t1\t0.500\t1679.100\t10_1\t35.000\t0.000\tIdle\tRA2\t20.000");
	}

	@Test
	public void shouldReturnBetweenLines() {
		Assert.assertEquals(
				reader.getLineForKey("0.014299"),
				"0.007017\t10.000\t3.000\t825\t0.500\t1583.520\t1_2\t35.000\t0.000\tDispenseWaste\tRA2\t20.000");
	}

	@Test
	public void shouldSupportNullReader() {
		reader = new UvDataReader(null);
	}
}
