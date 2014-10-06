package edu.mayo.mprc.qa;

import com.google.common.base.Strings;
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
				"0.0013\t10\t3\t825\t0.5\t1583.67\t1_2\t35\t0\tDispenseWaste\tRA2\t20");
	}

	@Test
	public void shouldReturnLastLine() {
		Assert.assertEquals(
				reader.getLineForKey("50.998283"),
				"50.998283\t1\t3\t1\t0.5\t1679.100\t10_1\t35\t0\tIdle\tRA2\t20");
	}

	@Test
	public void shouldReturnBeforeFirstLine() {
		Assert.assertEquals(
				reader.getLineForKey("0.0"),
				"0.0013\t10\t3\t825\t0.5\t1583.67\t1_2\t35\t0\tDispenseWaste\tRA2\t20");
	}

	@Test
	public void shouldReturnAfterLastLine() {
		Assert.assertEquals(
				reader.getLineForKey("100.0"),
				"50.998283\t1\t3\t1\t0.5\t1679.100\t10_1\t35\t0\tIdle\tRA2\t20");
	}

	@Test
	public void shouldReturnBetweenLines() {
		Assert.assertEquals(
				reader.getLineForKey("0.014299"),
				"0.007017\t10\t3\t825\t0.5\t1583.52\t1_2\t35\t0\tDispenseWaste\tRA2\t20");
	}

	@Test
	public void shouldSupportNullReader() {
		final UvDataReader reader = new UvDataReader(null);
		Assert.assertEquals(
				reader.getLineForKey("10.0"),
				Strings.repeat("\t", 12 - 1)); /* one less than the column count */
	}
}
