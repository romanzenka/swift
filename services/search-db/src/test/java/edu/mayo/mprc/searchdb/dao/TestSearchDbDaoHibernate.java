package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.DaoTest;
import edu.mayo.mprc.dbcurator.model.CurationContext;
import edu.mayo.mprc.dbcurator.model.impl.CurationDaoHibernate;
import edu.mayo.mprc.fastadb.FastaDbDaoHibernate;
import edu.mayo.mprc.swift.db.SwiftDaoHibernate;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.params2.ParamsDaoHibernate;
import edu.mayo.mprc.unimod.UnimodDaoHibernate;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.workspace.WorkspaceDaoHibernate;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.v6.Maps;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class TestSearchDbDaoHibernate extends DaoTest {

	private File tempFolder;
	private WorkspaceDaoHibernate workspaceDao;
	private CurationDaoHibernate curationDao;
	private ParamsDaoHibernate paramsDao;
	private UnimodDaoHibernate unimodDao;
	private SwiftDaoHibernate swiftDao;
	private FastaDbDaoHibernate fastaDbDao;
	private SearchDbDaoHibernate searchDbDao;

	@BeforeTest
	public void init() {
		tempFolder = FileUtilities.createTempFolder();

		workspaceDao = new WorkspaceDaoHibernate();
		final CurationContext curationContext = new CurationContext();
		curationContext.initialize(
				new File(tempFolder, "fasta"),
				new File(tempFolder, "fastaUpload"),
				new File(tempFolder, "fastaArchive"),
				new File(tempFolder, "localTemp"));

		curationDao = new CurationDaoHibernate(curationContext);
		paramsDao = new ParamsDaoHibernate(workspaceDao, curationDao);
		unimodDao = new UnimodDaoHibernate();
		swiftDao = new SwiftDaoHibernate(workspaceDao, curationDao, paramsDao, unimodDao);
		fastaDbDao = new FastaDbDaoHibernate(curationDao);
		searchDbDao = new SearchDbDaoHibernate(swiftDao, fastaDbDao);

		initializeDatabase(Arrays.asList(workspaceDao, curationDao, paramsDao, unimodDao, swiftDao, searchDbDao, fastaDbDao));

		searchDbDao.begin();
		try {
			final Map<String, String> testMap = Maps.newHashMap();
			testMap.put("test", "true");
			searchDbDao.install(testMap);
			searchDbDao.commit();
		} catch (final Exception e) {
			searchDbDao.rollback();
			throw new MprcException(e);
		}
	}

	@AfterTest
	public void teardown() {
		FileUtilities.cleanupTempFile(tempFolder);
	}

	@Test
	public void shouldCheckExistingSearches() {
		swiftDao.begin();
		try {

			List<SearchRun> searchRunList = searchDbDao.getSearchRunList(runFilter(), true);
			swiftDao.commit();

			Assert.assertEquals(searchRunList.size(), 1, "We have 1 search run by default");
			Assert.assertEquals(searchRunList.get(0).getReports().size(), 1, "There is one report");
		} catch (Exception e) {
			swiftDao.rollback();
			throw new MprcException(e);
		}
	}

	private SearchRunFilter runFilter() {
		SearchRunFilter filter = new SearchRunFilter();
		filter.setCount("1000");
		return filter;
	}

	@Test
	public void shouldFilterByInstrument() {
		swiftDao.begin();
		try {

			final SearchRunFilter filter = runFilter();
			filter.setInstrumentFilter("sort=0;filter=Velos234"); // This instrument does not exist
			List<SearchRun> searchRunList = searchDbDao.getSearchRunList(filter, true);
			swiftDao.commit();

			Assert.assertEquals(searchRunList.size(), 0, "No searches");
		} catch (Exception e) {
			swiftDao.rollback();
			throw new MprcException(e);
		}
	}

	@Test
	public void shouldFilterByInstrument2() {
		swiftDao.begin();
		try {

			final SearchRunFilter filter = runFilter();
			filter.setInstrumentFilter("sort=0;where=Orbi123"); // This instrument does exist
			List<SearchRun> searchRunList = searchDbDao.getSearchRunList(filter, true);
			swiftDao.commit();

			Assert.assertEquals(searchRunList.size(), 1, "One search referencing Orbi123");
		} catch (Exception e) {
			swiftDao.rollback();
			throw new MprcException(e);
		}
	}

	@Test
	public void shouldFillExtraFields() {
		searchDbDao.begin();
		try {
			final List<SearchRun> searchRunList = searchDbDao.getSearchRunList(runFilter(), true);
			SearchRun searchRun = searchRunList.get(0);
			swiftDao.fillExtraFields(searchRunList);
			Assert.assertEquals(searchRun.getRunningTasks(), Integer.valueOf(0), "The run has nothing running at the moment");
			searchDbDao.commit();
		} catch (Exception e) {
			searchDbDao.rollback();
			throw new MprcException(e);
		}
	}

	@Test
	public void shouldCleanupAfterStartup() {
		searchDbDao.begin();
		try {
			List<SearchRun> searchRunList = searchDbDao.getSearchRunList(runFilter(), true);
			Assert.assertNull(searchRunList.get(0).getEndTimestamp());
			swiftDao.cleanupAfterStartup();
			searchDbDao.commit();

			searchDbDao.begin();
			List<SearchRun> searchRunListAfter = searchDbDao.getSearchRunList(runFilter(), true);
			searchDbDao.commit();
			Assert.assertNotNull(searchRunListAfter.get(0).getEndTimestamp());
		} catch (Exception e) {
			searchDbDao.rollback();
			throw new MprcException(e);
		}
	}

	@Test
	public void shouldFilterByReport() {
		searchDbDao.begin();
		try {
			ReportData reportData = (ReportData) searchDbDao.getSession().get(ReportData.class, 1L);
			final Analysis analysis = searchDbDao.getAnalysis(reportData);
			Assert.assertEquals(analysis.getId(), Integer.valueOf(1), "Should be the one and only analysis");
			searchDbDao.commit();
		} catch (Exception e) {
			searchDbDao.rollback();
			throw new MprcException(e);
		}
	}
}
