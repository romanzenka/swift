package edu.mayo.mprc.swift.db;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.DaoTest;
import edu.mayo.mprc.dbcurator.model.CurationContext;
import edu.mayo.mprc.dbcurator.model.impl.CurationDaoHibernate;
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
public final class TestSwiftDaoHibernate extends DaoTest {

	private File tempFolder;
	private WorkspaceDaoHibernate workspaceDao;
	private CurationDaoHibernate curationDao;
	private ParamsDaoHibernate paramsDao;
	private UnimodDaoHibernate unimodDao;
	private SwiftDaoHibernate swiftDao;

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

		initializeDatabase(Arrays.asList(workspaceDao, curationDao, paramsDao, unimodDao, swiftDao));

		swiftDao.begin();
		try {
			final Map<String, String> testMap = Maps.newHashMap();
			testMap.put("test", "true");
			swiftDao.install(testMap);
			swiftDao.commit();
		} catch (final Exception e) {
			swiftDao.rollback();
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

			List<SearchRun> searchRunList = swiftDao.getSearchRunList(runFilter(), true);
			swiftDao.commit();

			Assert.assertEquals(searchRunList.size(), 1, "We have 1 search run by default");
			Assert.assertEquals(searchRunList.get(0).getReports().size(), 0, "There are no reports");
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
	public void shouldGetNumberOfRunningTasks() {
		swiftDao.begin();
		try {
			SearchRun searchRun = swiftDao.getSearchRunList(runFilter(), true).get(0);
			Assert.assertEquals(swiftDao.getNumberRunningTasksForSearchRun(searchRun), 0, "The run has nothing running at the moment");
			swiftDao.commit();
		} catch (Exception e) {
			swiftDao.rollback();
			throw new MprcException(e);
		}
	}
}
