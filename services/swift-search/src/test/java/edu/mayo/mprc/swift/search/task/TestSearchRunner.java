package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.SimpleThreadPoolExecutor;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.CurationDao;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.*;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.params2.mapping.MappingContext;
import edu.mayo.mprc.swift.params2.mapping.MappingFactory;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.swift.params2.mapping.MockParamsInfo;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import edu.mayo.mprc.workspace.User;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.mock;

/**
 * @author Roman Zenka
 */
public class TestSearchRunner {
	private File outputFolder;
	private File raw1;
	private File raw2;

	@BeforeClass
	public void setup() throws IOException {
		outputFolder = FileUtilities.createTempFolder();
		raw1 = new File(outputFolder, "file1.RAW");
		raw1.createNewFile();
		raw2 = new File(outputFolder, "file2.RAW");
		raw2.createNewFile();
	}

	@AfterClass
	public void teardown() {
		FileUtilities.cleanupTempFile(outputFolder);
	}

	@Test
	public void singleExperimentRunner() throws IOException {
		final Collection<SearchEngine> searchEngines = searchEngines();

		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment", searchEngineParameters1()),
				new FileSearch(raw2, "biosample2", "category", "experiment", searchEngineParameters1())
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);

		final SearchRunner runner = getSearchRunner(searchEngines, definition);

		final int numEngines = enabledEngines().size();
		final int tasksPerFile = (numEngines - 1) /* 1 for each engine except Scaffold */
				+ 1 /* Raw->mgf */
				+ 1 /* Raw->mzML for comet */
				+ 1 /* Raw->ms2 for comet */
				+ 1 /* sqt+ms2 combiner for comet */
				+ 1 /* RawDump */
				+ 1 /* msmsEval */;

		final int tasksPerSearch = 0
				+ 1 /* Fasta DB load */
				+ 1 /* Search DB load */
				+ 1 /* QA Task */
				+ 1 /* Scaffold report */

				+ numEngines /* DB deploys */ - getEnabledNoDeploy()
				+ 1 /* Scaffold */;

		final int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		// 23 + 2 * 5 + 1
		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}

	@Test
	public void singleExperimentNoQuameterRunner() throws IOException {
		final Collection<SearchEngine> searchEngines = searchEnginesNoQuameter();

		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment", searchEngineParameters1()),
				new FileSearch(raw2, "biosample2", "category", "experiment", searchEngineParameters1())
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);

		final SearchRunner runner = getSearchRunner(searchEngines, definition);

		final int numEngines = enabledEngines().size();
		final int tasksPerFile = (numEngines - 1) /* 1 for each engine except Scaffold */
				+ 1 /* Raw->mgf */
				+ 1 /* Raw->mzML for comet */
				+ 1 /* Raw->ms2 for comet */
				+ 1 /* sqt+ms2 combiner for comet */
				+ 1 /* RawDump */
				+ 1 /* msmsEval */;

		final int tasksPerSearch = 0
				+ 1 /* Fasta DB load */
				+ 1 /* Search DB load */
				+ 1 /* QA Task */
				+ 1 /* Scaffold report */

				+ numEngines /* DB deploys */ - getEnabledNoDeploy()
				+ 1 /* Scaffold */;

		final int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		// 23 + 2 * 5 + 1
		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}

	@Test
	public void onlyProvideMgfAndMzxmlRunner() throws IOException {
		final Collection<SearchEngine> searchEngines = searchEngines();

		final SearchEngineParameters searchParameters = searchEngineParameters1(new EnabledEngines());
		searchParameters.setExtractMsnSettings(new ExtractMsnSettings("", ExtractMsnSettings.MSCONVERT));
		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment", searchParameters),
				new FileSearch(raw2, "biosample2", "category", "experiment", searchParameters)
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);
		definition.setSearchParameters(searchParameters);
		definition.setPublicMgfFiles(true);
		definition.setPublicMzxmlFiles(true);

		final SearchRunner runner = getSearchRunner(searchEngines, definition);

		final int tasksPerFile = 0
				+ 1 /* Raw->mgf */
				+ 1 /* Raw->mzxml */
				+ 1 /* msmsEval */;

		final int tasksPerSearch = 0;

		final int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}

	@Test
	public void singleExperimentTwoDatabasesRunner() throws IOException {
		final Collection<SearchEngine> searchEngines = searchEngines();

		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment", searchEngineParameters1()),
				new FileSearch(raw2, "biosample2", "category", "experiment", searchEngineParameters2())
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);

		final SearchRunner runner = getSearchRunner(searchEngines, definition);

		final int numEngines = enabledEngines().size();
		final int tasksPerFile = (numEngines - 1) /* 1 for each engine except Scaffold */
				+ 1 /* Raw->mgf */
				+ 1 /* Raw->mzML for comet */
				+ 1 /* Raw->ms2 for comet */
				+ 1 /* sqt+ms2 combiner for comet */
				+ 1 /* RawDump */
				+ 1 /* msmsEval */
				+ 1 /* Fasta DB load (two different DBs) */
				+ numEngines /* DB deploys */ - getEnabledNoDeploy();

		final int tasksPerSearch = 0
				+ 1 /* Search DB load */
				+ 1 /* QA Task */
				+ 1 /* Scaffold report */

				+ 1 /* Scaffold */;

		final int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}

	@Test
	public void singleExperimentTwoProteasesRunner() throws IOException {
		final Collection<SearchEngine> searchEngines = searchEngines();

		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment", searchEngineParameters1()),
				new FileSearch(raw2, "biosample2", "category", "experiment", searchEngineParameters3())
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);

		final SearchRunner runner = getSearchRunner(searchEngines, definition);

		final int numEngines = enabledEngines().size();
		final int tasksPerFile = (numEngines - 1) /* 1 for each engine except Scaffold */
				+ 1 /* Raw->mgf */
				+ 1 /* Raw->mzML for comet */
				+ 1 /* Raw->ms2 for comet */
				+ 1 /* sqt+ms2 combiner for comet */
				+ 1 /* RawDump */
				+ 1 /* msmsEval */;

		final int tasksPerSearch = 0
				+ numEngines /* DB deploys */ - getEnabledNoDeploy()
				+ 1 /* Search DB load */
				+ 1 /* QA Task */
				+ 1 /* Scaffold report */
				+ 1 /* Fasta DB load */

				+ 1 /* Scaffold */;

		final int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}


	@Test
	public void multipleExperimentRunner() throws IOException {
		final Collection<SearchEngine> searchEngines = searchEngines();

		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment1", searchEngineParameters1()),
				new FileSearch(raw2, "biosample2", "category", "experiment2", searchEngineParameters1())
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);

		final SearchRunner runner = getSearchRunner(searchEngines, definition);

		final int numEngines = enabledEngines().size();
		final int tasksPerFile = numEngines /* 1 for each engine */
				+ 1 /* Raw->mgf */
				+ 1 /* Raw->mzML for comet */
				+ 1 /* Raw->ms2 for comet */
				+ 1 /* sqt+ms2 combiner for comet */
				+ 1 /* RawDump */
				+ 1 /* msmsEval */;

		final int tasksPerSearch = 0
				+ 1 /* Search DB load */
				+ 1 /* Fasta DB load */
				+ 1 /* QA Task */
				+ 1 /* Scaffold report */

				+ numEngines /* DB Deploys */ - getEnabledNoDeploy()
				+ 1 /* One extra DB deploy for Sequest */;

		final int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}

	@Test
	public void mzMlRunner() throws IOException {
		// msconvert with mzml
		final Collection<SearchEngine> searchEngines = searchEngines();

		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment", searchEngineParametersMzml()),
				new FileSearch(raw2, "biosample2", "category", "experiment", searchEngineParametersMzml())
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);

		final SearchRunner runner = getSearchRunner(searchEngines, definition);

		final int numEngines = enabledEngines().size();
		final int tasksPerFile = (numEngines - 1) /* 1 for each engine except Scaffold */
				+ 1 /* Raw->mzML can be used by Comet directly */
				+ 1 /* Raw->ms2 for comet */
				+ 1 /* sqt+ms2 combiner for comet */
				+ 1 /* RawDump */
				+ 1 /* no msmsEval */;

		final int tasksPerSearch = 0
				+ 1 /* Fasta DB load */
				+ 1 /* Search DB load */
				+ 1 /* QA Task */
				+ 1 /* Scaffold report */

				+ numEngines /* DB deploys */ - getEnabledNoDeploy()
				+ 1 /* Scaffold */;

		final int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		// 23 + 2 * 5 + 1
		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}

	/**
	 * We switch QC on and expect extra tasks for mzML conversion, myrimatch, idpqonvert and quameter to pop up
	 */
	@Test
	public void qualityControlRunner() throws IOException {
		final Collection<SearchEngine> searchEngines = searchEngines();

		final EnabledEngines engines = enabledEnginesWithQuameter();

		final SearchEngineParameters searchParameters = searchEngineParameters1(engines);
		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment", searchParameters),
				new FileSearch(raw2, "biosample2", "category", "experiment", searchParameters)
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);
		definition.setSearchParameters(searchParameters);

		final SearchRunner runner = getSearchRunner(searchEngines, definition);

		final int numEngines = enabledEngines().size();
		final int tasksPerFile = (numEngines - 1) /* 1 for each engine except Scaffold */
				+ 1 /* Raw->mgf */
				+ 1 /* Raw->mzML for comet */
				+ 1 /* Raw->ms2 for comet */
				+ 1 /* sqt+ms2 combiner for comet */
				+ 1 /* RawDump */
				+ 1 /* msmsEval */

				+ 1 /* Comet mzML */
				+ 1 /* IdpQonvert mzML */
				+ 1 /* QuaMeter */
				+ 1 /* QuaMeter DB load */;

		final int tasksPerSearch = 0
				+ 1 /* Fasta DB load */
				+ 1 /* Search DB load */
				+ 1 /* QA Task */
				+ 1 /* Scaffold report */

				+ numEngines /* DB deploys */ - getEnabledNoDeploy()
				+ 1 /* Scaffold */;

		final int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}

	@DataProvider(name = "twoBools")
	public Object[][] createData(final Method m) {
		return new Object[][]{new Object[]{Boolean.FALSE}, new Object[]{Boolean.TRUE}};
	}

	/**
	 * We switch QC on for a mzML-based search. While we did mzML conversion before, we need
	 * to do an extra conversion to obtain mzML containing MS1 spectra. This conversion is a superset
	 * of the common conversion, which allows us to do less work.
	 * <p/>
	 * msconvert, Comet, Idpicker and quameter should be added, because comet will operate with different settings
	 * (semi-tryptic).
	 */
	@Test(dataProvider = "twoBools")
	public void qualityControlRunnerWithMzML(final Boolean doSemiTryptic) throws IOException {
		final Collection<SearchEngine> searchEngines = searchEngines();

		final EnabledEngines engines = enabledEnginesWithQuameter();

		final SearchEngineParameters parameters = searchEngineParametersMzml();
		parameters.setEnabledEngines(engines);
		if (doSemiTryptic) {
			parameters.setMinTerminiCleavages(1);
		}

		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment", parameters),
				new FileSearch(raw2, "biosample2", "category", "experiment", parameters)
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);
		definition.setSearchParameters(parameters);

		final SearchRunner runner = getSearchRunner(searchEngines, definition);

		final int numEngines = enabledEngines().size();
		final int tasksPerFile = (numEngines - 1) /* 1 for each engine except Scaffold */
				+ 1 /* Raw->mzML for comet */
				+ 1 /* Raw->ms2 for comet */
				+ 1 /* sqt+ms2 combiner for comet */
				+ 1 /* RawDump */
				+ 1 /* msmsEval */

				+ (doSemiTryptic ?
					2 : /* We used to be able to reuse Comet result, but requiring sqt for Scaffold prevents us from doing so */
					2 /* tryptic means we need extra semitryptic comet and idpicker */)
				+ 1 /* QuaMeter */
				+ 1 /* QuaMeter db load */;

		final int tasksPerSearch = 0
				+ 1 /* Fasta DB load */
				+ 1 /* Search DB load */
				+ 1 /* QA Task */
				+ 1 /* Scaffold report */

				+ numEngines /* DB deploys */ - getEnabledNoDeploy()
				+ 1 /* Scaffold */;

		final int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}

	private SearchRunner getSearchRunner(final Collection<SearchEngine> searchEngines, final SwiftSearchDefinition definition) {
		final ProgressReporter reporter = mock(ProgressReporter.class);
		final ExecutorService service = new SimpleThreadPoolExecutor(1, "testSwiftSearcher", true);

		final SearchRun searchRun = new SearchRun("Test search", null, definition, new Date(), null, 0, null, 0, 0, 0, 0, false);


		final SearchRunner runner = makeSearchRunner("task1", false, searchEngines, definition, reporter, service, searchRun);

		runner.initialize();
		return runner;
	}


	private SearchRunner makeSearchRunner(final String taskId, final boolean fromScratch, final Collection<SearchEngine> searchEngines, final SwiftSearchDefinition definition, final ProgressReporter reporter, final ExecutorService service, final SearchRun searchRun) {
		return new SearchRunner(
				definition,
				mock(DaemonConnection.class),
				mock(DaemonConnection.class),
				mock(DaemonConnection.class),
				mock(DaemonConnection.class),
				mock(DaemonConnection.class),
				mock(DaemonConnection.class),
				mock(DaemonConnection.class),
				mock(DaemonConnection.class),
				mock(DaemonConnection.class),
				mock(DaemonConnection.class),
				searchEngines,
				reporter,
				service,
				mock(CurationDao.class),
				mock(SwiftDao.class),
				dummyFileTokenFactory(),
				searchRun,
				false, /* reportDecoys */
				true, /* semiQuameter */
				0,
				new MockParamsInfo(),
				taskId,
				fromScratch);
	}

	private SwiftSearchDefinition defaultSearchDefinition(final List<FileSearch> inputFiles) {
		final SwiftSearchDefinition swiftSearchDefinition = new SwiftSearchDefinition(
				"Test search",
				new User("Tester", "Testov", "test", "pwd"),
				outputFolder,
				new SpectrumQa("orbitrap", "msmsEval"),
				new PeptideReport(),
				searchEngineParameters1(),
				inputFiles,
				false,
				false,
				false,
				new HashMap<String, String>(0)
		);
		// Pretend we are in the database already
		swiftSearchDefinition.setId(1);
		return swiftSearchDefinition;
	}

	private SearchEngineParameters searchEngineParameters1() {
		return searchEngineParameters1(enabledEngines());
	}

	private SearchEngineParameters searchEngineParameters1(final EnabledEngines enabledEngines) {
		final ScaffoldSettings scaffoldSettings = new ScaffoldSettingsBuilder()
				.setMinimumNonTrypticTerminii(0)
				.setStarredProteins(new StarredProteins("ALBU_HUMAN", ",", false))
				.setSaveOnlyIdentifiedSpectra(false)
				.setConnectToNCBI(true)
				.setAnnotateWithGOA(true)
				.createScaffoldSettings();
		return new SearchEngineParameters(curation1(), Protease.getInitial().get(0),
				2, 1, new ModSet(), new ModSet(), new Tolerance(10, MassUnit.Ppm),
				new Tolerance(1, MassUnit.Da), Instrument.ORBITRAP,
				new ExtractMsnSettings("-M100", ExtractMsnSettings.EXTRACT_MSN),
				scaffoldSettings,
				enabledEngines,
				""
		);
	}

	private SearchEngineParameters searchEngineParameters2() {
		final SearchEngineParameters parameters = searchEngineParameters1();
		parameters.setDatabase(curation2());
		return parameters;
	}

	private SearchEngineParameters searchEngineParameters3() {
		final SearchEngineParameters parameters = searchEngineParameters1();
		parameters.setProtease(Protease.getInitial().get(10));
		return parameters;
	}

	private SearchEngineParameters searchEngineParametersMzml() {
		final SearchEngineParameters parameters = searchEngineParameters1();
		parameters.setExtractMsnSettings(new ExtractMsnSettings(ExtractMsnSettings.MZML_MODE, ExtractMsnSettings.MSCONVERT));
		return parameters;
	}

	private Collection<SearchEngine> searchEngines() {
		final Collection<SearchEngine> searchEngines = new ArrayList<SearchEngine>();
		searchEngines.add(searchEngine("MASCOT"));
		searchEngines.add(searchEngine("COMET"));
		searchEngines.add(searchEngine("TANDEM"));
		searchEngines.add(searchEngine("MYRIMATCH"));
		searchEngines.add(searchEngine("SCAFFOLD"));
		searchEngines.add(searchEngine("IDPQONVERT"));
		searchEngines.add(searchEngine("QUAMETER"));
		return searchEngines;
	}

	private Collection<SearchEngine> searchEnginesNoQuameter() {
		final Collection<SearchEngine> searchEngines = new ArrayList<SearchEngine>();
		searchEngines.add(searchEngine("MASCOT"));
		searchEngines.add(searchEngine("COMET"));
		searchEngines.add(searchEngine("TANDEM"));
		searchEngines.add(searchEngine("MYRIMATCH"));
		searchEngines.add(searchEngine("SCAFFOLD"));
		searchEngines.add(searchEngine("IDPQONVERT"));
		return searchEngines;
	}

	private EnabledEngines enabledEngines() {
		final EnabledEngines engines = new EnabledEngines();
		engines.add(createSearchEngineConfig("MASCOT"));
		engines.add(createSearchEngineConfig("COMET"));
		engines.add(createSearchEngineConfig("TANDEM")); // No db deploy
		engines.add(createSearchEngineConfig("MYRIMATCH")); // No db deploy
		engines.add(createSearchEngineConfig("SCAFFOLD"));
		engines.add(createSearchEngineConfig("IDPQONVERT"));  // No db deploy
		return engines;
	}

	private EnabledEngines enabledEnginesWithQuameter() {
		final EnabledEngines enabledEngines = enabledEngines();
		enabledEngines.add(createSearchEngineConfig("QUAMETER"));
		return enabledEngines;
	}

	private DatabaseFileTokenFactory dummyFileTokenFactory() {
		final DatabaseFileTokenFactory fileTokenFactory = new DatabaseFileTokenFactory();
		final DaemonConfigInfo mainDaemon = new DaemonConfigInfo("daemon1", "/");
		fileTokenFactory.setDaemonConfigInfo(mainDaemon);
		fileTokenFactory.setDatabaseDaemonConfigInfo(mainDaemon);
		fileTokenFactory.start();
		return fileTokenFactory;
	}

	private Curation curation1() {
		final Curation curation = new Curation();
		curation.setId(1);
		curation.setShortName("1");
		curation.setCurationFile(new File("1.fasta"));
		return curation;
	}

	private Curation curation2() {
		final Curation curation = new Curation();
		curation.setId(2);
		curation.setShortName("2");
		curation.setCurationFile(new File("2.fasta"));
		return curation;
	}

	private SearchEngine searchEngine(final String code) {
		final SearchEngine engine = new SearchEngine();
		final EngineMetadata metadata = new EngineMetadata(
				code, "." + code, code, false, code + "_output_dir", mappingFactory(code),
				new String[]{}, new String[]{}, new String[]{}, 0, isAggregator(code));
		engine.setEngineMetadata(metadata);
		engine.setSearchDaemon(mock(DaemonConnection.class));
		engine.setDbDeployDaemon(dbDeployer(code) ? mock(DaemonConnection.class) : null);
		engine.setConfig(new SearchEngine.Config(code, "1.0", null, null));
		return engine;
	}

	private boolean isAggregator(final String code) {
		return code.equals("SCAFFOLD") || code.equals("IDPQONVERT") || code.equals("QUAMETER");
	}

	private boolean dbDeployer(final String engineCode) {
		return !("TANDEM".equals(engineCode) || "MYRIMATCH".equals(engineCode)
				|| "IDPQONVERT".equals(engineCode) || "COMET".equals(engineCode));
	}

	/**
	 * Number of enabled engines wiht no deployment
	 * See {@link #dbDeployer(String)}.
	 */
	private int getEnabledNoDeploy() {
		return 4; // TANDEM, MYRIMATCH, IDPQONVERT, COMET
	}

	private MappingFactory mappingFactory(final String code) {
		return new MyMappingFactory(code);
	}

	private SearchEngineConfig createSearchEngineConfig(final String code) {
		final SearchEngineConfig config = new SearchEngineConfig(code, "1.0");
		return config;
	}

	private static class MyMappingFactory implements MappingFactory {
		private final String code;

		MyMappingFactory(final String code) {
			this.code = code;
		}

		@Override
		public String getSearchEngineCode() {
			return code;
		}

		@Override
		public String getCanonicalParamFileName(final String distinguishingString) {
			return code + distinguishingString + ".cfg";
		}

		@Override
		public Mappings createMapping() {
			return new Mappings() {
				private String str;

				@Override
				public Reader baseSettings() {
					return new StringReader("");
				}

				@Override
				public void read(final Reader isr) {
					FileUtilities.closeQuietly(isr);
				}

				@Override
				public void write(final Reader oldParams, final Writer out) {
					try {
						out.write(str);
					} catch (IOException e) {
						throw new MprcException("Could not write params out");
					}
					FileUtilities.closeQuietly(oldParams);
					FileUtilities.closeQuietly(out);
				}

				@Override
				public void setPeptideTolerance(final MappingContext context, final Tolerance peptideTolerance) {
					str += peptideTolerance.toString();
				}

				@Override
				public void setFragmentTolerance(final MappingContext context, final Tolerance fragmentTolerance) {
					str += fragmentTolerance.toString();
				}

				@Override
				public void setVariableMods(final MappingContext context, final ModSet variableMods) {
					str += variableMods.toString();
				}

				@Override
				public void setFixedMods(final MappingContext context, final ModSet fixedMods) {
					str += fixedMods.toString();
				}

				@Override
				public void setSequenceDatabase(final MappingContext context, final String shortDatabaseName) {
					str += shortDatabaseName;
				}

				@Override
				public void setProtease(final MappingContext context, final Protease protease) {
					str += protease;
				}

				@Override
				public void setMinTerminiCleavages(final MappingContext context, final Integer minTerminiCleavages) {
					str += minTerminiCleavages;
				}

				@Override
				public void setMissedCleavages(final MappingContext context, final Integer missedCleavages) {
					str += missedCleavages;
				}

				@Override
				public void setInstrument(final MappingContext context, final Instrument instrument) {
					str += instrument;
				}

				@Override
				public void checkValidity(final MappingContext context) {
				}

				@Override
				public String getNativeParam(final String name) {
					return "";
				}
			};
		}
	}
}
