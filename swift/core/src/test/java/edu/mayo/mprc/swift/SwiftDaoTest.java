package edu.mayo.mprc.swift;

import edu.mayo.mprc.swift.db.SwiftDaoHibernate;
import edu.mayo.mprc.swift.dbmapping.*;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.workflow.persistence.TaskState;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

@Test(sequential = true)
public final class SwiftDaoTest {
	private SwiftDaoHibernate swiftDao;
	private ParamsDao paramsDao;
	private TestApplicationContext context;

	@BeforeClass
	public void setup() {
		context = new TestApplicationContext();
		context.start();
		swiftDao = (SwiftDaoHibernate) context.getSwiftDao();
		paramsDao = context.getParamsDao();
	}

	@AfterClass
	public void shutdown() {
		context.stop();
	}

	@Test
	public void doubleSaveExtractMsn() throws Throwable {
		swiftDao.begin();
		try {

			ExtractMsnSettings settings = new ExtractMsnSettings("-D", ExtractMsnSettings.EXTRACT_MSN);
			settings = paramsDao.addExtractMsnSettings(settings);

			ExtractMsnSettings settings2 = new ExtractMsnSettings("-D", ExtractMsnSettings.EXTRACT_MSN);
			settings2 = paramsDao.addExtractMsnSettings(settings2);

			Assert.assertEquals(settings.getId(), settings2.getId(), "Same objects have to get the same id");
			swiftDao.commit();
		} catch (Exception t) {
			swiftDao.rollback();
			throw t;
		}
	}

	@Test
	public void doubleSaveQa() throws Throwable {
		swiftDao.begin();
		try {

			SpectrumQa settings = new SpectrumQa("qa.txt", "msmsEval");
			settings = swiftDao.addSpectrumQa(settings);

			SpectrumQa settings2 = new SpectrumQa("qa.txt", "msmsEval");
			settings2 = swiftDao.addSpectrumQa(settings2);

			Assert.assertEquals(settings.getId(), settings2.getId(), "Same objects have to get the same id");
			swiftDao.commit();
		} catch (Exception t) {
			swiftDao.rollback();
			throw t;
		}
	}

	@Test
	public void doubleSaveScaffoldSettings() throws Throwable {
		swiftDao.begin();
		try {

			ScaffoldSettings settings = new ScaffoldSettingsBuilder().setSaveOnlyIdentifiedSpectra(false).createScaffoldSettings();
			settings = paramsDao.addScaffoldSettings(settings);

			final List list = swiftDao.getDatabase().getSession().createQuery("from ScaffoldSettings").list();

			ScaffoldSettings settings2 = new ScaffoldSettingsBuilder().setSaveOnlyIdentifiedSpectra(false).createScaffoldSettings();
			settings2 = paramsDao.addScaffoldSettings(settings2);

			Assert.assertEquals(settings.getId(), settings2.getId(), "Same objects have to get the same id");

			swiftDao.commit();
		} catch (Exception t) {
			swiftDao.rollback();
			throw t;
		}
	}

	@Test
	public void doubleSaveScaffoldSettingsWithStarred() throws Throwable {
		swiftDao.begin();
		try {
			StarredProteins starredProteins = new StarredProteins();
			starredProteins.setDelimiter(",");
			starredProteins.setStarred("ALBU_HUMAN,OVAL_CHICK");
			starredProteins.setRegularExpression(false);
			starredProteins = paramsDao.addStarredProteins(starredProteins);

			ScaffoldSettings settings = new ScaffoldSettingsBuilder().setStarredProteins(starredProteins).setSaveOnlyIdentifiedSpectra(false).createScaffoldSettings();
			settings = paramsDao.addScaffoldSettings(settings);

			ScaffoldSettings settings2 = new ScaffoldSettingsBuilder().setStarredProteins(starredProteins).setSaveOnlyIdentifiedSpectra(false).createScaffoldSettings();
			settings2 = paramsDao.addScaffoldSettings(settings2);

			Assert.assertEquals(settings.getId(), settings2.getId(), "Same objects have to get the same id");

			swiftDao.commit();
		} catch (Exception t) {
			swiftDao.rollback();
			throw t;
		}
	}

	@Test
	public void shouldDoubleSaveStarredCaseSensitive() throws Throwable {
		swiftDao.begin();
		try {

			StarredProteins starredProteins = makeRegexStarredProteins("ALBU_HUMAN OVAL_CHICK");
			starredProteins = paramsDao.addStarredProteins(starredProteins);

			StarredProteins starredProteins2 = makeRegexStarredProteins("ALBU_Human Oval_Chick");
			starredProteins2 = paramsDao.addStarredProteins(starredProteins2);

			Assert.assertNotSame(starredProteins, starredProteins2);

		} catch (Exception e) {
			swiftDao.rollback();
			throw e;
		}
	}

	private StarredProteins makeRegexStarredProteins(String proteins) {
		StarredProteins starredProteins = new StarredProteins();
		starredProteins.setDelimiter("\\s+");
		starredProteins.setStarred(proteins);
		starredProteins.setRegularExpression(true);
		return starredProteins;
	}

	@Test
	public void doubleSaveSearchDefinition() throws Throwable {
		swiftDao.begin();
		try {
			SwiftSearchDefinition blank1 = new SwiftSearchDefinition("Blank search", null, null, null, null, null, new ArrayList<FileSearch>(), false, false, false, new HashMap<String, String>(0));
			blank1 = swiftDao.addSwiftSearchDefinition(blank1);
			SwiftSearchDefinition blank2 = new SwiftSearchDefinition("Blank search", null, null, null, null, null, new ArrayList<FileSearch>(), false, false, false, new HashMap<String, String>(0));
			blank2 = swiftDao.addSwiftSearchDefinition(blank2);
			Assert.assertEquals(blank2.getId(), blank1.getId(), "Blank searches has to be stored as one");

			final File f = new File("testFolder");
			final SwiftSearchDefinition folder1 = new SwiftSearchDefinition("Folder search", null, f, null, null, null, new ArrayList<FileSearch>(), false, false, false, new HashMap<String, String>(0));
			blank1 = swiftDao.addSwiftSearchDefinition(blank1);
			final File f2 = new File("testFolder");
			final SwiftSearchDefinition folder2 = new SwiftSearchDefinition("Folder search", null, f2, null, null, null, new ArrayList<FileSearch>(), false, false, false, new HashMap<String, String>(0));
			blank2 = swiftDao.addSwiftSearchDefinition(blank2);
			Assert.assertEquals(folder2.getId(), folder1.getId(), "Folder searches has to be stored as one");

			SearchEngineParameters parameters = getSearchEngineParameters();

			parameters = paramsDao.addSearchEngineParameters(parameters);

			final FileSearch fileSearch1 = new FileSearch(new File("input.RAW"), "bio sample", null, "experiment", parameters);
			final FileSearch fileSearch2 = new FileSearch(new File("input.RAW"), "bio sample", null, "experiment", parameters);

			final LinkedList<FileSearch> fileSearches1 = new LinkedList<FileSearch>();
			fileSearches1.add(fileSearch1);
			final LinkedList<FileSearch> fileSearches2 = new LinkedList<FileSearch>();
			fileSearches2.add(fileSearch2);

			SwiftSearchDefinition file1 = new SwiftSearchDefinition("File search", null, null, null, null, null, fileSearches1, false, false, false, new HashMap<String, String>(0));
			file1 = swiftDao.addSwiftSearchDefinition(file1);
			SwiftSearchDefinition file2 = new SwiftSearchDefinition("File search", null, null, null, null, null, fileSearches2, false, false, false, new HashMap<String, String>(0));
			file2 = swiftDao.addSwiftSearchDefinition(file2);
			Assert.assertEquals(file2.getId(), file1.getId(), "File searches have to be stored as one");

			swiftDao.commit();
		} catch (Exception t) {
			swiftDao.rollback();
			throw t;
		}
	}

	private SearchEngineParameters getSearchEngineParameters() {
		final ModSet fixedModifications = new ModSet();
		final ModSet variableModifications = new ModSet();

		final Instrument orbi = paramsDao.getInstrumentByName("Orbi/FT (ESI-FTICR)");
		final Protease protease = paramsDao.getProteaseByName("Trypsin (allow P)");


		return new SearchEngineParameters(null, protease, 2, 1, fixedModifications, variableModifications,
				new Tolerance(1, MassUnit.Da), new Tolerance(10, MassUnit.Ppm), orbi,
				ExtractMsnSettings.getDefaultExtractMsnSettings(),
				ScaffoldSettings.defaultScaffoldSettings());
	}

	@Test
	public void twoTransactionsPerSession() throws Throwable {
		swiftDao.begin();
		final TaskStateData taskStateData = swiftDao.getTaskState(TaskState.READY);
		final TaskData taskData = new TaskData("Test task", new Date(), new Date(), null, null, taskStateData, "Test task description");
		swiftDao.updateTask(taskData);
		swiftDao.commit();

		swiftDao.begin();
		swiftDao.updateTask(taskData);
		taskData.setDescriptionLong("New description");
		swiftDao.commit();
	}
}
