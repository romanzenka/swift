package edu.mayo.mprc.io.mgf;

import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class MgfTitlesTest {
	@Test
	public void testGetTitles() throws Exception {
		final File testMgf = TestingUtilities.getTempFileFromResource(MgfTitlesTest.class, "test.mgf", null);

		final List<String> list = MgfTitles.getTitles(testMgf);
		Assert.assertEquals(list.size(), 2);
		Assert.assertEquals(list.get(0), "ch261_042208_AO_check1 scan 1 1 (ch261_042208_AO_check1.1.1.3.dta)");
		Assert.assertEquals(list.get(1), "ch261_042208_AO_check1 scan 2 2 (ch261_042208_AO_check1.2.2.3.dta)");

		FileUtilities.cleanupTempFile(testMgf);
	}
}
