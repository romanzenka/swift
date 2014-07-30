package edu.mayo.mprc.quameterdb;

import com.google.common.collect.ImmutableMap;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.DaoTest;
import edu.mayo.mprc.fastadb.FastaDbDaoHibernate;
import edu.mayo.mprc.quameterdb.dao.QuameterDaoHibernate;
import edu.mayo.mprc.quameterdb.dao.QuameterResult;
import edu.mayo.mprc.searchdb.dao.SearchDbDaoHibernate;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.db.SwiftDaoHibernate;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.ParamsDaoHibernate;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDaoHibernate;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
public final class QuameterUiTest extends DaoTest {
	QuameterDaoHibernate quameterDao;
	SearchDbDaoHibernate searchDbDao;
	SwiftDaoHibernate swiftDao;
	FastaDbDaoHibernate fastaDbDao;
	ParamsDaoHibernate paramsDao;
	WorkspaceDaoHibernate workspaceDao;

	QuameterUi quameterUi;

	TandemMassSpectrometrySample sample1;
	TandemMassSpectrometrySample sample2;
	FileSearch fileSearch1;
	FileSearch fileSearch2;

	@BeforeMethod
	public void setup() {
		swiftDao = new SwiftDaoHibernate();
		fastaDbDao = new FastaDbDaoHibernate();
		searchDbDao = new SearchDbDaoHibernate(swiftDao, fastaDbDao, getDatabase());
		paramsDao = new ParamsDaoHibernate();
		quameterDao = new QuameterDaoHibernate(swiftDao, searchDbDao);
		workspaceDao = new WorkspaceDaoHibernate();
		initializeDatabase(Arrays.asList(swiftDao, fastaDbDao, quameterDao, searchDbDao, paramsDao, workspaceDao));

		searchDbDao.begin();
		sample1 = searchDbDao.addTandemMassSpectrometrySample(
				new TandemMassSpectrometrySample(
						new File("test.RAW"),
						new DateTime(2014, 1, 10, 9, 10, 11, 0),
						100,
						1000,
						0,
						"instrument",
						"Orbi123",
						new DateTime(2014, 1, 10, 10, 20, 30, 0),
						60.0,
						"comment",
						"sample Information")
		);

		sample2 = searchDbDao.addTandemMassSpectrometrySample(
				new TandemMassSpectrometrySample(
						new File("test2.RAW"),
						new DateTime(2014, 2, 12, 11, 20, 30, 40),
						200,
						2000,
						0,
						"instrument",
						"Orbi123",
						new DateTime(2014, 2, 12, 12, 21, 22, 23),
						120.0,
						"comment 2",
						"sample Information 2")
		);


		fileSearch1 = swiftDao.addFileSearch(new FileSearch(new File("test.RAW"), "bioSample", "category", "experiment", null));
		fileSearch2 = swiftDao.addFileSearch(new FileSearch(new File("test2.RAW"), "bioSample2", "category2", "experiment2", null));
		final List<FileSearch> fileSearches = Arrays.asList(fileSearch1, fileSearch2);
		final Map<String, String> metadata = new HashMap<String, String>(1);
		metadata.put("quameter.category", "AL-Kappa");

		User user = workspaceDao.addNewUser("Tester", "Testin", "test@test.tst", new Change("testing", new DateTime()));

		final SwiftSearchDefinition definition = swiftDao.addSwiftSearchDefinition(
				new SwiftSearchDefinition("test", user, null, null, null, null, fileSearches,
						false, false, false, metadata)
		);

		final SearchRun searchRun = swiftDao.fillSearchRun(definition);
		searchRun.setHidden(0);
		searchRun.setErrorCode(0);
		searchRun.setNumTasks(10);
		searchRun.setTasksCompleted(10);
		searchRun.setEndTimestamp(new Date());

		searchDbDao.commit();

		final QuameterUi.Config config = new QuameterUi.Config();
		config.setSearchFilter(".*");
		final QuameterUi.Factory factory = new QuameterUi.Factory();
		factory.setQuameterDao(quameterDao);
		quameterUi = factory.create(config, new DependencyResolver(null));
	}

	@AfterMethod
	public void teardown() {
		teardownDatabase();
	}

	@Test
	public void shouldAddResult() {
		quameterDao.begin();

		final QuameterResult quameterResult = addResult1();

		nextTransaction();

		Assert.assertEquals(quameterResult.getMs2_4a(), 1.22);

		final List<QuameterResult> quameterResults = quameterDao.listAllResults(Pattern.compile(".*"));
		Assert.assertEquals(quameterResults.size(), 1);

		quameterDao.commit();
	}

	@Test
	public void shouldFilterOutputs() {
		quameterDao.begin();

		addResult1();

		nextTransaction();

		addResult2();

		nextTransaction();

		final List<QuameterResult> quameterResults = quameterDao.listAllResults(Pattern.compile(".*"));
		Assert.assertEquals(quameterResults.size(), 2);

		final List<QuameterResult> quameterResults2 = quameterDao.listAllResults(Pattern.compile("ment2$"));
		Assert.assertEquals(quameterResults2.size(), 1);

		final List<QuameterResult> quameterResults3 = quameterDao.listAllResults(Pattern.compile("^blah$"));
		Assert.assertEquals(quameterResults3.size(), 0);

		quameterDao.commit();
	}

	@Test
	public void shouldProduceDataTable() {
		quameterUi.begin();

		addResult1();
		addResult2();

		nextTransaction();

		final StringWriter writer = new StringWriter(1000);
		try {
			quameterUi.dataTableJson(writer);
		} finally {
			FileUtilities.closeQuietly(writer);
		}

		Assert.assertTrue(writer.toString().length() > 100);

		quameterUi.commit();
	}


	private QuameterResult addResult1() {
		return quameterDao.addQuameterScores(sample1.getId(), fileSearch1.getId(), new ImmutableMap.Builder<String, Double>()
				.put("MS2-4A", 1.22)
				.put("C-1A", 0.0)
				.build(), 0);
	}

	private QuameterResult addResult2() {
		return quameterDao.addQuameterScores(sample2.getId(), fileSearch2.getId(), new ImmutableMap.Builder<String, Double>()
				.put("MS2-4A", 2.33)
				.put("C-1A", 1.2)
				.build(), 0);
	}


}
