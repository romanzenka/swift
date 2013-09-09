package edu.mayo.mprc.heme.dao;

import edu.mayo.mprc.heme.HemeReportEntry;
import edu.mayo.mprc.heme.HemeScaffoldReader;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Roman Zenka
 */
public final class HemeScaffoldReaderTest {

	@Test
	public static void shouldReadTest() throws IOException {
		final File spectra = TestingUtilities.getTempFileFromResource("/edu/mayo/mprc/heme/PT1.spectra.txt", true, null);
		try {
			HemeScaffoldReader reader = new HemeScaffoldReader();
			reader.load(spectra, "3", null);
			Collection<HemeReportEntry> entries = reader.getEntries();
			Assert.assertEquals(entries.size(), 5);
		} finally {
			FileUtilities.cleanupTempFile(spectra);
		}
	}
}
