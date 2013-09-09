package edu.mayo.mprc.heme.dao;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.DaoTest;
import edu.mayo.mprc.swift.db.SwiftDaoHibernate;
import edu.mayo.mprc.swift.params2.ParamsDaoHibernate;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class HemeDaoHibernateTest extends DaoTest {
	HemeDaoHibernate hemeDao;
	SwiftDaoHibernate swiftDao;
	ParamsDaoHibernate paramsDao;

	@BeforeMethod
	public void setup() {
		hemeDao = new HemeDaoHibernate();
		swiftDao = new SwiftDaoHibernate();
		paramsDao = new ParamsDaoHibernate();
		initializeDatabase(Arrays.asList(hemeDao, swiftDao, paramsDao));
	}

	@AfterMethod
	public void teardown() {
		teardownDatabase();
	}

	private HemeTest sampleTest() {
		return new HemeTest("patient1", new DateTime(2013, 8, 22, 0, 0, 0, 0).toDate(), "20130822/patient1", 40.0, 0.5);
	}

	private HemeTest addSampleTest() {
		hemeDao.begin();
		HemeTest test = sampleTest();
		test = hemeDao.addTest(test);
		nextTransaction();
		return test;
	}

	@Test
	public void testGetAllTests() {
		final HemeTest test = addSampleTest();
		final List<HemeTest> allTests = hemeDao.getAllTests();
		Assert.assertEquals(allTests.size(), 1);
		Assert.assertEquals(allTests.get(0), test);
		hemeDao.commit();
	}

	@Test
	public void shouldAdd() {
		hemeDao.begin();
		HemeTest test = sampleTest();
		test = hemeDao.addTest(test);
		hemeDao.commit();
		Assert.assertNotNull(test.getId());
	}

	@Test(expectedExceptions = MprcException.class)
	public void shouldNotAddNull() {
		hemeDao.begin();
		hemeDao.addTest(null);
		hemeDao.commit();
	}

	@Test(expectedExceptions = MprcException.class)
	public void shouldNotDoubleAdd() {
		final HemeTest test = addSampleTest();
		HemeTest test2 = sampleTest();
		test2.setMassDelta(1234.0);
		hemeDao.addTest(test2);
		hemeDao.commit();
	}


	@Test(expectedExceptions = MprcException.class)
	public void shouldNotAddMissingDate() {
		hemeDao.begin();
		HemeTest test = sampleTest();
		test.setDate(null);
		hemeDao.addTest(test);
		hemeDao.commit();
	}

	@Test(expectedExceptions = MprcException.class)
	public void shouldNotAddMissingPath() {
		hemeDao.begin();
		HemeTest test = sampleTest();
		test.setPath(null);
		hemeDao.addTest(test);
		hemeDao.commit();
	}

	@Test(expectedExceptions = MprcException.class)
	public void shouldNotAddMissingName() {
		hemeDao.begin();
		HemeTest test = sampleTest();
		test.setName(null);
		hemeDao.addTest(test);
		hemeDao.commit();
	}

	@Test
	public void shouldRemoveExisting() {
		addSampleTest();
		Assert.assertEquals(hemeDao.countTests(), 1);
		nextTransaction();
		hemeDao.removeTest(sampleTest());
		nextTransaction();
		Assert.assertEquals(hemeDao.countTests(), 0);
		hemeDao.commit();
	}

	@Test
	public void shouldNotFailRemoveNonexisting() {
		final HemeTest test = addSampleTest();
		Assert.assertEquals(hemeDao.countTests(), 1);
		nextTransaction();
		final HemeTest different = sampleTest();
		different.setPath("different");
		hemeDao.removeTest(different);
		nextTransaction();
		Assert.assertEquals(hemeDao.countTests(), 1);
		hemeDao.commit();
	}

	@Test
	public void shouldGetById() {
		final HemeTest test = addSampleTest();
		final HemeTest testForId = hemeDao.getTestForId(test.getId());
		Assert.assertEquals(testForId, test);
		hemeDao.commit();
	}

	@Test(expectedExceptions = MprcException.class)
	public void shouldFailUnknownId() {
		final HemeTest test = addSampleTest();
		try {
			hemeDao.getTestForId(test.getId() + 1);
		} finally {
			hemeDao.rollback();
		}
	}


}
