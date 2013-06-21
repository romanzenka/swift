package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.SimpleThreadPoolExecutor;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.*;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.params2.mapping.MappingContext;
import edu.mayo.mprc.swift.params2.mapping.MappingFactory;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.swift.params2.mapping.MockParamsInfo;
import edu.mayo.mprc.swift.search.SwiftSearchWorkPacket;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import edu.mayo.mprc.workspace.User;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.mock;

/**
 * @author Roman Zenka
 */
public class TestSearchRunner {
	private File outputFolder;
	private File raw1;
	private File raw2;

	@BeforeTest
	public void setup() throws IOException {
		outputFolder = FileUtilities.createTempFolder();
		raw1 = new File(outputFolder, "file1.RAW");
		raw1.createNewFile();
		raw2 = new File(outputFolder, "file2.RAW");
		raw2.createNewFile();
	}

	@AfterTest
	public void teardown() {
		FileUtilities.cleanupTempFile(outputFolder);
	}

	@Test
	public void singleExperimentRunner() throws IOException {
		final SwiftSearchWorkPacket packet = new SwiftSearchWorkPacket(1, "task1", false, 0);

		final Collection<SearchEngine> searchEngines = searchEngines();

		final EnabledEngines engines = enabledEngines();

		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment", engines, searchEngineParameters1()),
				new FileSearch(raw2, "biosample2", "category", "experiment", engines, searchEngineParameters1())
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);

		final ProgressReporter reporter = mock(ProgressReporter.class);
		final ExecutorService service = new SimpleThreadPoolExecutor(1, "testSwiftSearcher", true);

		final SearchRun searchRun = null;

		final SearchRunner runner = makeSearchRunner(packet, searchEngines, definition, reporter, service, searchRun);

		runner.initialize();

		final int numEngines = enabledEngines().size();
		final int tasksPerFile = (numEngines - 1) /* 1 for each engine except Scaffold */
				+ 1 /* Raw->mgf */
				+ 1 /* RawDump */
				+ 1 /* msmsEval */;

		final int tasksPerSearch = 0
				+ 1 /* Fasta DB load */
				+ 1 /* Search DB load */
				+ 1 /* QA Task */
				+ 1 /* Scaffold report */

				+ numEngines /* DB deploys */
				+ 1 /* Scaffold */;

		int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		// 23 + 2 * 5 + 1
		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}

	@Test
	public void singleExperimentTwoDatabasesRunner() throws IOException {
		final SwiftSearchWorkPacket packet = new SwiftSearchWorkPacket(1, "task1", false, 0);

		final Collection<SearchEngine> searchEngines = searchEngines();

		final EnabledEngines engines = enabledEngines();

		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment", engines, searchEngineParameters1()),
				new FileSearch(raw2, "biosample2", "category", "experiment", engines, searchEngineParameters2())
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);

		final ProgressReporter reporter = mock(ProgressReporter.class);
		final ExecutorService service = new SimpleThreadPoolExecutor(1, "testSwiftSearcher", true);

		final SearchRun searchRun = null;

		final SearchRunner runner = makeSearchRunner(packet, searchEngines, definition, reporter, service, searchRun);

		runner.initialize();

		final int numEngines = enabledEngines().size();
		final int tasksPerFile = (numEngines - 1) /* 1 for each engine except Scaffold */
				+ 1 /* Raw->mgf */
				+ 1 /* RawDump */
				+ 1 /* msmsEval */
				+ 1 /* Fasta DB load (two different DBs) */
				+ numEngines /* DB deploys */;

		final int tasksPerSearch = 0
				+ 1 /* Search DB load */
				+ 1 /* QA Task */
				+ 1 /* Scaffold report */

				+ 1 /* Scaffold */;

		int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}

	@Test
	public void singleExperimentTwoProteasesRunner() throws IOException {
		final SwiftSearchWorkPacket packet = new SwiftSearchWorkPacket(1, "task1", false, 0);

		final Collection<SearchEngine> searchEngines = searchEngines();

		final EnabledEngines engines = enabledEngines();

		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment", engines, searchEngineParameters1()),
				new FileSearch(raw2, "biosample2", "category", "experiment", engines, searchEngineParameters3())
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);

		final ProgressReporter reporter = mock(ProgressReporter.class);
		final ExecutorService service = new SimpleThreadPoolExecutor(1, "testSwiftSearcher", true);

		final SearchRun searchRun = null;

		final SearchRunner runner = makeSearchRunner(packet, searchEngines, definition, reporter, service, searchRun);

		runner.initialize();

		final int numEngines = enabledEngines().size();
		final int tasksPerFile = (numEngines - 1) /* 1 for each engine except Scaffold */
				+ 1 /* Raw->mgf */
				+ 1 /* RawDump */
				+ 1 /* msmsEval */;

		final int tasksPerSearch = 0
				+ numEngines /* DB deploys */
				+ 1 /* One extra Sequest db deployment due to different protease */
				+ 1 /* Search DB load */
				+ 1 /* QA Task */
				+ 1 /* Scaffold report */
				+ 1 /* Fasta DB load */

				+ 1 /* Scaffold */;

		int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}


	@Test
	public void multipleExperimentRunner() throws IOException {
		final SwiftSearchWorkPacket packet = new SwiftSearchWorkPacket(1, "task1", false, 0);

		final Collection<SearchEngine> searchEngines = searchEngines();

		final EnabledEngines engines = enabledEngines();

		final List<FileSearch> inputFiles = Arrays.asList(
				new FileSearch(raw1, "biosample", "category", "experiment1", engines, searchEngineParameters1()),
				new FileSearch(raw2, "biosample2", "category", "experiment2", engines, searchEngineParameters1())
		);

		final SwiftSearchDefinition definition = defaultSearchDefinition(inputFiles);

		final ProgressReporter reporter = mock(ProgressReporter.class);
		final ExecutorService service = new SimpleThreadPoolExecutor(1, "testSwiftSearcher", true);

		final SearchRun searchRun = null;

		final SearchRunner runner = makeSearchRunner(packet, searchEngines, definition, reporter, service, searchRun);

		runner.initialize();

		final int numEngines = enabledEngines().size();
		final int tasksPerFile = numEngines /* 1 for each engine */
				+ 1 /* Raw->mgf */
				+ 1 /* RawDump */
				+ 1 /* msmsEval */;

		final int tasksPerSearch = 0
				+ 1 /* Search DB load */
				+ 1 /* Fasta DB load */
				+ 1 /* QA Task */
				+ 1 /* Scaffold report */

				+ numEngines /* DB Deploys */
				+ 1 /* One extra DB deploy for Sequest */;

		int expectedNumTasks = inputFiles.size() * tasksPerFile + tasksPerSearch;

		Assert.assertEquals(runner.getWorkflowEngine().getNumTasks(), expectedNumTasks);
	}

	private SearchRunner makeSearchRunner(SwiftSearchWorkPacket packet, Collection<SearchEngine> searchEngines, SwiftSearchDefinition definition, ProgressReporter reporter, ExecutorService service, SearchRun searchRun) {
		return new SearchRunner(packet,
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
				searchEngines,
				reporter,
				service,
				mock(CurationDao.class),
				mock(SwiftDao.class),
				dummyFileTokenFactory(),
				searchRun,
				false,
				0,
				new MockParamsInfo());
	}

	private SwiftSearchDefinition defaultSearchDefinition(final List<FileSearch> inputFiles) {
		return new SwiftSearchDefinition(
				"Test search",
				new User("Tester", "Testov", "test", "pwd"),
				outputFolder,
				new SpectrumQa("orbitrap", "msmsEval"),
				new PeptideReport(),
				searchEngineParameters1(),
				inputFiles,
				false,
				false
		);
	}

	private SearchEngineParameters searchEngineParameters1() {
		return new SearchEngineParameters(curation1(), Protease.getInitial().get(0),
				1, new ModSet(), new ModSet(), new Tolerance(10, MassUnit.Ppm),
				new Tolerance(1, MassUnit.Da), Instrument.ORBITRAP,
				new ExtractMsnSettings("-M100", ExtractMsnSettings.EXTRACT_MSN),
				new ScaffoldSettings(0.95, 0.95, 2, 0, new StarredProteins("ALBU_HUMAN", ",", false), false, false, true, true)
		);
	}

	private SearchEngineParameters searchEngineParameters2() {
		return new SearchEngineParameters(curation2(), Protease.getInitial().get(0),
				1, new ModSet(), new ModSet(), new Tolerance(10, MassUnit.Ppm),
				new Tolerance(1, MassUnit.Da), Instrument.ORBITRAP,
				new ExtractMsnSettings("-M100", ExtractMsnSettings.EXTRACT_MSN),
				new ScaffoldSettings(0.95, 0.95, 2, 0, new StarredProteins("ALBU_HUMAN", ",", false), false, false, true, true)
		);
	}

	private SearchEngineParameters searchEngineParameters3() {
		return new SearchEngineParameters(curation1(), Protease.getInitial().get(10),
				1, new ModSet(), new ModSet(), new Tolerance(10, MassUnit.Ppm),
				new Tolerance(1, MassUnit.Da), Instrument.ORBITRAP,
				new ExtractMsnSettings("-M100", ExtractMsnSettings.EXTRACT_MSN),
				new ScaffoldSettings(0.95, 0.95, 2, 0, new StarredProteins("ALBU_HUMAN", ",", false), false, false, true, true)
		);
	}

	private Collection<SearchEngine> searchEngines() {
		final Collection<SearchEngine> searchEngines = new ArrayList<SearchEngine>();
		searchEngines.add(searchEngine("MASCOT"));
		searchEngines.add(searchEngine("SEQUEST"));
		searchEngines.add(searchEngine("TANDEM"));
		searchEngines.add(searchEngine("MYRIMATCH"));
		searchEngines.add(searchEngine("SCAFFOLD"));
		searchEngines.add(searchEngine("IDPICKER"));
		return searchEngines;
	}

	private EnabledEngines enabledEngines() {
		final EnabledEngines engines = new EnabledEngines();
		engines.add(createSearchEngineConfig("MASCOT"));
		engines.add(createSearchEngineConfig("SEQUEST"));
		engines.add(createSearchEngineConfig("TANDEM"));
		engines.add(createSearchEngineConfig("MYRIMATCH"));
		engines.add(createSearchEngineConfig("SCAFFOLD"));
		engines.add(createSearchEngineConfig("IDPICKER"));
		return engines;
	}

	private FileTokenFactory dummyFileTokenFactory() {
		final FileTokenFactory fileTokenFactory = new FileTokenFactory();
		final DaemonConfigInfo mainDaemon = new DaemonConfigInfo("daemon1", "/");
		fileTokenFactory.setDaemonConfigInfo(mainDaemon);
		fileTokenFactory.setDatabaseDaemonConfigInfo(mainDaemon);
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
				code, null, code, false, code + "_output_dir", mappingFactory(code),
				new String[]{}, new String[]{}, new String[]{}, 0, code.equals("SCAFFOLD") || code.equals("IDPICKER"));
		engine.setEngineMetadata(metadata);
		engine.setSearchDaemon(mock(DaemonConnection.class));
		engine.setDbDeployDaemon(mock(DaemonConnection.class));
		engine.setConfig(new SearchEngine.Config(code, "1.0", null, null));
		return engine;
	}

	private MappingFactory mappingFactory(final String code) {
		return new MyMappingFactory(code);
	}

	private SearchEngineConfig createSearchEngineConfig(final String code) {
		final SearchEngineConfig config = new SearchEngineConfig(code, "1.0");
		return config;
	}

	private static class MyMappingFactory implements MappingFactory {
		private static final long serialVersionUID = -6099034607375054441L;
		private final String code;

		public MyMappingFactory(String code) {
			this.code = code;
		}

		@Override
		public String getSearchEngineCode() {
			return code;
		}

		@Override
		public String getCanonicalParamFileName(String distinguishingString) {
			return code + distinguishingString + ".cfg";
		}

		@Override
		public Mappings createMapping() {
			return new Mappings() {
				@Override
				public Reader baseSettings() {
					return new StringReader("");
				}

				@Override
				public void read(Reader isr) {
					FileUtilities.closeQuietly(isr);
				}

				@Override
				public void write(Reader oldParams, Writer out) {
					FileUtilities.closeQuietly(oldParams);
					FileUtilities.closeQuietly(out);
				}

				@Override
				public void setPeptideTolerance(MappingContext context, Tolerance peptideTolerance) {
				}

				@Override
				public void setFragmentTolerance(MappingContext context, Tolerance fragmentTolerance) {
				}

				@Override
				public void setVariableMods(MappingContext context, ModSet variableMods) {
				}

				@Override
				public void setFixedMods(MappingContext context, ModSet fixedMods) {
				}

				@Override
				public void setSequenceDatabase(MappingContext context, String shortDatabaseName) {
				}

				@Override
				public void setProtease(MappingContext context, Protease protease) {
				}

				@Override
				public void setMissedCleavages(MappingContext context, Integer missedCleavages) {
				}

				@Override
				public void setInstrument(MappingContext context, Instrument instrument) {
				}

				@Override
				public String getNativeParam(String name) {
					return "";
				}

				@Override
				public void setNativeParam(String name, String value) {
				}
			};
		}
	}
}
