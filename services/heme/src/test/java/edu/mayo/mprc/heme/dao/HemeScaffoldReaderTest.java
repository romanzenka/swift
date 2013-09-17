package edu.mayo.mprc.heme.dao;

import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.heme.HemeReportEntry;
import edu.mayo.mprc.heme.HemeScaffoldReader;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

/**
 * @author Roman Zenka
 */
public final class HemeScaffoldReaderTest {

	@Test
	public static void shouldReadTest() throws IOException {
		final File spectra = TestingUtilities.getTempFileFromResource("/edu/mayo/mprc/heme/PT1.spectra.txt", true, null);
		try {
			Curation database = new Curation();
			database.setShortName("testdb");

			FastaDbDao fastaDbDao = mock(FastaDbDao.class);
			stub(fastaDbDao.getProteinDescription(eq(database), anyString())).toReturn("Description of protein ##DeltaMass:1.0##");

			HemeScaffoldReader reader = new HemeScaffoldReader(fastaDbDao, database);
			reader.load(spectra, "3", null);
			Collection<HemeReportEntry> entries = reader.getEntries();
			Assert.assertEquals(entries.size(), 18);
		} finally {
			FileUtilities.cleanupTempFile(spectra);
		}
	}
}
