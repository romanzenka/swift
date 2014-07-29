package edu.mayo.mprc.heme.dao;

import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.heme.HemeEntry;
import edu.mayo.mprc.heme.HemeTestStatus;
import edu.mayo.mprc.heme.HemeUi;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.swift.search.SwiftSearcherCaller;
import edu.mayo.mprc.utilities.FileUtilities;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * @author Roman Zenka
 */
public final class HemeUiTest {
	private HemeUi hemeUi;
	private File data;
	private File results;
	private HemeDao hemeDao;
	private SwiftDao swiftDao;
	private FastaDbDao fastaDbDao;
	private ParamsDao paramsDao;
	private SwiftSearcherCaller swiftSearcherCaller;

	@BeforeTest
	public void setup() {
		data = FileUtilities.createTempFolder();

		// Valid, but already exists in database
		createValidEntry("PT1", "20130905");
		HemeTest test1 = new HemeTest("PT1", new DateTime(2013, 9, 5, 0, 0, 0, 0).toDate(), "20130905/PT1", 40.0, 0.5);

		createValidEntry("PT2", "20040229");

		// Incorrect date should be skipped
		File wrong = new File(data, "20133355");
		FileUtilities.ensureFolderExists(wrong);

		// Non-date should be skipped
		File nonDate = new File(data, "hello");
		FileUtilities.ensureFolderExists(nonDate);

		results = FileUtilities.createTempFolder();

		hemeDao = mock(HemeDao.class);
		stub(hemeDao.getAllTests()).toReturn(Arrays.asList(test1));
		stub(hemeDao.countTests()).toReturn(1L);

		swiftDao = mock(SwiftDao.class);
		swiftSearcherCaller = mock(SwiftSearcherCaller.class);

		paramsDao = mock(ParamsDao.class);

		fastaDbDao = mock(FastaDbDao.class);
		stub(fastaDbDao.getProteinDescription(any(Curation.class), anyString())).toReturn("Protein description");

		hemeUi = new HemeUi(data, results, hemeDao, swiftDao, fastaDbDao, paramsDao, swiftSearcherCaller, "1", "2", "zenka.roman@mayo.edu", new File("test_cache.obj"));
	}

	private void createValidEntry(String name, String date) {
		File newDate = new File(data, date);
		FileUtilities.ensureFolderExists(newDate);
		File patient1ct = new File(newDate, name + "_CT.d");
		FileUtilities.ensureFolderExists(patient1ct);
		File patient1t = new File(newDate, name + "_T.d");
		FileUtilities.ensureFolderExists(patient1t);
	}

	@AfterTest
	public void teardown() {
		FileUtilities.cleanupTempFile(data);
		FileUtilities.cleanupTempFile(results);
	}

	@Test
	public void shouldScanFiles() {
		hemeUi.begin();
		try {
			hemeUi.scanFileSystem();
			hemeUi.commit();
		} catch (Exception e) {
			hemeUi.rollback();
		}

		verify(hemeDao, times(1)).addTest(new HemeTest("PT2", new DateTime(2004, 2, 29, 0, 0, 0, 0).toDate(), "20040229/PT2", 0.0, 0.0));
	}

	@Test
	public void shouldListEntries() {
		List<HemeEntry> currentEntries = hemeUi.getCurrentEntries();
		Assert.assertEquals(currentEntries.size(), 1);
		HemeEntry hemeEntry = currentEntries.get(0);
		Assert.assertEquals(hemeEntry.getTest().getName(), "PT1");
		Assert.assertEquals(hemeEntry.getTest().getPath(), "20130905/PT1");
		Assert.assertEquals(hemeEntry.getTest().getMass(), 40.0);
		Assert.assertEquals(hemeEntry.getStatus(), HemeTestStatus.NOT_STARTED);
		Assert.assertEquals(hemeEntry.getDuration(), null);
	}
}
