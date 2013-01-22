package edu.mayo.mprc.myrimatch;

import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.daemon.DaemonWorkerTester;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.enginedeployment.DeploymentResult;
import edu.mayo.mprc.fasta.DatabaseAnnotation;
import edu.mayo.mprc.fasta.FastaFile;
import edu.mayo.mprc.integration.Installer;
import edu.mayo.mprc.swift.params2.Instrument;
import edu.mayo.mprc.swift.params2.ParamName;
import edu.mayo.mprc.swift.params2.Protease;
import edu.mayo.mprc.swift.params2.Tolerance;
import edu.mayo.mprc.swift.params2.mapping.MappingContext;
import edu.mayo.mprc.swift.params2.mapping.MockParamsInfo;
import edu.mayo.mprc.swift.params2.mapping.ParamsInfo;
import edu.mayo.mprc.unimod.*;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MyrimatchTest {
	private static final Logger LOGGER = Logger.getLogger(MyrimatchTest.class);
	private static final MockUnimodDao UNIMOD_DAO = new MockUnimodDao();
	public static final Charset CHARSET = Charset.forName("ISO-8859-1");

	@Test
	public final void shouldCreate() {
		final MyrimatchWorker worker = createWorker("myrimatch.exe");
		Assert.assertNotNull(worker);
		Assert.assertEquals(worker.getExecutable(), new File("myrimatch.exe"));
	}

	@Test
	public final void shouldStripComments() {
		Assert.assertEquals(MyrimatchMappings.stripComment("hello world"), "hello world");
		Assert.assertEquals(MyrimatchMappings.stripComment("hello # world"), "hello ");
		Assert.assertEquals(MyrimatchMappings.stripComment("hello # world # test"), "hello ");
		Assert.assertEquals(MyrimatchMappings.stripComment("# hello # world # test"), "");
	}

	@Test
	public final void shouldMapFixedMods() {
		final MyrimatchMappings mappings = createMappings();
		final MappingContext mappingContext = createMappingContext();
		final ModSet mods = new ModSet();
		final Unimod unimod = UNIMOD_DAO.load();

		mappings.setFixedMods(mappingContext, mods);
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.STATIC_MODS), "", "Should report no mods");

		final ModSpecificity oxidationMethionine = unimod.findSingleMatchingModificationSet(15.995, 0.005, 'M', null, null, null);
		// final ModSpecificity oxidationMethionine = unimod.getSpecificitiesByMascotName("Oxidation (M)").get(0);
		Assert.assertNotNull(oxidationMethionine, "Not found Oxidation(M)");
		mods.add(oxidationMethionine);
		mappings.setFixedMods(mappingContext, mods);
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.STATIC_MODS), "M 15.994915", "Should report Methionine modification");

		final ModSpecificity carbamidomethyl = unimod.findSingleMatchingModificationSet(57.025, 0.025, 'C', null, null, null);
		Assert.assertNotNull(carbamidomethyl, "Not found Carbamidomethyl(C)");
		mods.add(carbamidomethyl);
		mappings.setFixedMods(mappingContext, mods);
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.STATIC_MODS), "C 57.021464 M 15.994915", "Should report two mods");
	}

	@Test
	public final void shouldMapVariableMods() {
		final MyrimatchMappings mappings = createMappings();
		final MappingContext mappingContext = createMappingContext();
		final ModSet mods = new ModSet();
		final Unimod unimod = UNIMOD_DAO.load();

		mappings.setVariableMods(mappingContext, mods);
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.DYNAMIC_MODS), "", "Should report no mods");

		final ModSpecificity oxidationMethionine = unimod.findSingleMatchingModificationSet(15.995, 0.005, 'M', null, null, null);
		// final ModSpecificity oxidationMethionine = unimod.getSpecificitiesByMascotName("Oxidation (M)").get(0);
		Assert.assertNotNull(oxidationMethionine, "Not found Oxidation(M)");
		mods.add(oxidationMethionine);
		mappings.setVariableMods(mappingContext, mods);
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.DYNAMIC_MODS), "M * 15.994915", "Should report Methionine modification");

		final ModSpecificity carbamidomethyl = unimod.findSingleMatchingModificationSet(57.025, 0.025, 'C', null, null, null);
		Assert.assertNotNull(carbamidomethyl, "Not found Carbamidomethyl(C)");
		mods.add(carbamidomethyl);
		mappings.setVariableMods(mappingContext, mods);
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.DYNAMIC_MODS), "C * 57.021464 M ^ 15.994915", "Should report two mods");

		final ModSpecificity dimethyl = unimod.findSingleMatchingModificationSet(28.0315, 0.0005, 'P', null, null, null);
		Assert.assertNotNull(dimethyl, "Not found Dimethyl(Protein N-term P)");
		mods.add(dimethyl);
		mappings.setVariableMods(mappingContext, mods);
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.DYNAMIC_MODS), "C * 57.021464 (P ^ 28.0313 M @ 15.994915", "Should report three mods");

		final ModSpecificity homoserine = unimod.findSingleMatchingModificationSet(-29.99285, 0.00005, 'M', Terminus.Cterm, false, null);
		Assert.assertNotNull(homoserine, "Not found Homoserine(C-term M)");
		mods.add(homoserine);
		mappings.setVariableMods(mappingContext, mods);
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.DYNAMIC_MODS), "C * 57.021464 (P ^ 28.0313 M) @ -29.992806 M % 15.994915", "Should report four mods");
	}

	@Test
	public final void shouldMapEnzymes() {
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("Arg-C", "R", "!P")), "(?<=R)(?!P)");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("Asp-N", "", "BD")), "(?=[BD])");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("Asp-N_ambic", "", "DE")), "(?=[DE])");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("Chymotrypsin", "FYWL", "!P")), "(?<=[FYWL])(?!P)");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("CNBr", "M", "")), "(?<=M)");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("Formic_acid", "D", "")), "(?<=D)"); // Problem. Formic_acid cleaves on both sides. We support just one
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("Lys-C (restrict P)", "K", "!P")), "(?<=K)(?!P)");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("Lys-C (allow P)", "K", "")), "(?<=K)");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("PepsinA", "FL", "")), "(?<=[FL])");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("Tryp-CNBr", "KRM", "!P")), "(?<=[KRM])(?!P)");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("TrypChymo", "FYWLKR", "!P")), "(?<=[FYWLKR])(?!P)");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("TrypChymoKRWFYnoP", "KRWFY", "")), "(?<=[KRWFY])");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("Trypsin (allow P)", "KR", "")), "(?<=[KR])");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("Trypsin (restrict P)", "KR", "!P")), "(?<=[KR])(?!P)");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("V8-DE", "BDEZ", "!P")), "(?<=[BDEZ])(?!P)");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("V8-E", "EZ", "!P")), "(?<=[EZ])(?!P)");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("ChymoAndGluC", "FYWLE", "")), "(?<=[FYWLE])");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("Non-Specific", "", "")), "NoEnzyme");
		Assert.assertEquals(MyrimatchMappings.enzymeToString(new Protease("DoubleNeg", "!A", "!EF")), "(?<!A)(?![EF])");
	}

	@Test
	public final void shouldMapPeptideTolerance() {
		final MyrimatchMappings mappings = createMappings();
		final MappingContext mappingContext = createMappingContext();

		mappings.setPeptideTolerance(mappingContext, new Tolerance("2.3 Da"));
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.PRECURSOR_MZ_TOLERANCE), "2.3");
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.PRECURSOR_MZ_TOLERANCE_UNITS), "daltons");

		mappings.setPeptideTolerance(mappingContext, new Tolerance("10 ppm"));
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.PRECURSOR_MZ_TOLERANCE), "10.0");
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.PRECURSOR_MZ_TOLERANCE_UNITS), "ppm");
	}

	@Test
	public final void shouldMapFragmentTolerance() {
		final MyrimatchMappings mappings = createMappings();
		final MappingContext mappingContext = createMappingContext();

		mappings.setFragmentTolerance(mappingContext, new Tolerance("10.37 Da"));
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.FRAGMENT_MZ_TOLERANCE), "10.37 daltons");
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.FRAGMENT_MZ_TOLERANCE_UNITS), "daltons");

		mappings.setFragmentTolerance(mappingContext, new Tolerance("0.12 ppm"));
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.FRAGMENT_MZ_TOLERANCE), "0.12 ppm");
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.FRAGMENT_MZ_TOLERANCE_UNITS), "ppm");
	}

	@Test
	public final void shouldMapInstrument() {
		final MyrimatchMappings mappings = createMappings();
		final MappingContext mappingContext = createMappingContext();

		mappings.setInstrument(mappingContext, Instrument.ORBITRAP);
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.USE_AVG_MASS_OF_SEQUENCES), "false", "Orbitrap uses monoisotopic mass");
	}

	@Test
	public final void shouldMapMissedCleavages() {
		final MyrimatchMappings mappings = createMappings();
		final MappingContext mappingContext = createMappingContext();
		mappings.setMissedCleavages(mappingContext, 3);
		Assert.assertEquals(mappings.getNativeParam(MyrimatchMappings.NUM_MAX_MISSED_CLEAVAGES), "3", "Missed cleavages do not match");
	}

	@Test
	public final void shouldWriteChanges() throws IOException {
		final MyrimatchMappings mappings = createMappings();
		compareMappingsToBase(mappings, null, null);

		final MappingContext mappingContext = createMappingContext();
		mappings.setProtease(mappingContext, new Protease("Lys-C (restrict P)", "K", "!P"));
		compareMappingsToBase(mappings, "CleavageRules = Trypsin/P", "CleavageRules = (?<=K)(?!P)");
	}

	@Test
	public final void shouldRunSearch() throws IOException, InterruptedException {
		final File myrimatchExecutable = getMyrimatchExecutable();
		try {
			final File tempFolder = FileUtilities.createTempFolder();

			final MyrimatchMappings mappings = createMappings();
			final MappingContext mappingContext = createMappingContext();
			mappings.setProtease(mappingContext, new Protease("Trypsin (allow P)", "KR", ""));
			mappings.setMissedCleavages(mappingContext, 2);

			final File configFile = new File(tempFolder, "myrimatch.cfg");
			final FileWriter writer = new FileWriter(configFile);
			try {
				mappings.write(mappings.baseSettings(), writer);
			} finally {
				FileUtilities.closeQuietly(writer);
			}

			final File fastaFile =
					TestingUtilities.getTempFileFromResource(MyrimatchTest.class, "/edu/mayo/mprc/myrimatch/database.fasta", false, tempFolder, ".fasta");
			final File mgfFile =
					TestingUtilities.getTempFileFromResource(MyrimatchTest.class, "/edu/mayo/mprc/myrimatch/test.mgf", false, tempFolder, ".mgf");

			final File resultFile = new File(tempFolder, "result.pepXML");

			final MyrimatchWorkPacket work = new MyrimatchWorkPacket(resultFile, configFile, mgfFile, tempFolder, fastaFile, 2, "Rev_", false, "Test Myrimatch run", false);

			final MyrimatchWorker worker = new MyrimatchWorker(myrimatchExecutable);

			final DaemonWorkerTester tester = new DaemonWorkerTester(worker);

			final Object workToken = tester.sendWork(work, new ProgressListener() {
				@Override
				public void requestEnqueued(final String s) {
					LOGGER.debug("Enqueued MyriMatch request: " + s);
				}

				@Override
				public void requestProcessingStarted() {
					LOGGER.debug("Starting to process MyriMatch request");
				}

				@Override
				public void requestProcessingFinished() {
					LOGGER.debug("MyriMatch request processing finished");
				}

				@Override
				public void requestTerminated(final Exception e) {
					LOGGER.error("MyriMatch request terminated", e);
					Assert.fail("Myrimatch failed", e);
				}

				@Override
				public void userProgressInformation(final ProgressInfo progressInfo) {
					LOGGER.debug("MyriMatch progress: " + progressInfo.toString());
				}
			});

			while (true) {
				synchronized (workToken) {
					if (tester.isDone(workToken)) {
						break;
					}
					workToken.wait(100);
				}
			}

			Assert.assertTrue(resultFile.exists() && resultFile.isFile() && resultFile.length() > 0, "Myrimatch did not produce valid result file");
			String resultString = Files.toString(resultFile, CHARSET);
			resultString = replace(resultString, fastaFile.getAbsolutePath(), "$$DB$$");
			resultString = replace(resultString, work.getWorkFolder().getAbsolutePath(), "$$WORK_DIR$$");
			resultString = replace(resultString, FileUtilities.stripExtension(mgfFile.getName()), "$$BASE$$");
			resultString = replace(resultString, "9.9999999999999995e-008", "9.9999999999999995e-08");
			resultString = replaceTime(resultString, "date=");
			resultString = replaceTime(resultString, "time=");
			resultString = replaceTime(resultString, "SearchTime: Duration\" value=");
			resultString = replaceTime(resultString, "SearchTime: Started\" value=");
			resultString = replaceTime(resultString, "SearchTime: Stopped\" value=");
			resultString = replaceLongFloats(resultString);


			final URL resource = Resources.getResource(MyrimatchTest.class, "result.pepXML");
			String expectedString = Resources.toString(resource, CHARSET);
			expectedString = expectedString.replaceAll("\r\n", "\n");

			FileUtilities.cleanupTempFile(configFile);
			FileUtilities.cleanupTempFile(fastaFile);
			FileUtilities.cleanupTempFile(resultFile);
			FileUtilities.cleanupTempFile(new File(tempFolder, fastaFile.getName() + ".index"));
			FileUtilities.cleanupTempFile(mgfFile);
			FileUtilities.cleanupTempFile(tempFolder);

			Assert.assertEquals(resultString, expectedString, "The Myrimatch results do not match expected ones");
		} finally {
			Installer.myrimatch(myrimatchExecutable, Installer.Action.UNINSTALL);
		}
	}

	@Test
	public final void shouldDeploy() throws IOException {
		final File tempFolder = FileUtilities.createTempFolder();

		final File databaseFile = TestingUtilities.getTempFileFromResource(MyrimatchTest.class, "/edu/mayo/mprc/myrimatch/database.fasta", false, tempFolder, ".fasta");

		final MyrimatchDeploymentService.Config config = new MyrimatchDeploymentService.Config();
		config.setDeployableDbFolder(tempFolder.getAbsolutePath());
		final MyrimatchDeploymentService.Factory factory = new MyrimatchDeploymentService.Factory();
		final MyrimatchDeploymentService deployer = (MyrimatchDeploymentService) factory.create(config, new DependencyResolver(null));
		final DatabaseAnnotation annotation = new DatabaseAnnotation("Rev_");
		final DeploymentRequest request = new DeploymentRequest("test deployment",
				new FastaFile("database", "test database for Myrimatch", databaseFile, annotation));
		deployCheckResult(deployer, request);
		deployCheckResult(deployer, request);

		request.setUndeployment(true);
		deployer.performUndeployment(request);

		FileUtilities.cleanupTempFile(databaseFile);
		FileUtilities.cleanupTempFile(tempFolder);
	}

	private void deployCheckResult(final MyrimatchDeploymentService deployer, final DeploymentRequest request) {
		final DeploymentResult result = deployer.performDeployment(request);
		final MyrimatchDeploymentResult myrimatchResult = (MyrimatchDeploymentResult) result;
		Assert.assertEquals(myrimatchResult.getDecoySequencePrefix(), request.getAnnotation().getDecoyRegex(), "Determined prefix does not match");
		Assert.assertEquals(myrimatchResult.getNumForwardEntries(), 2, "Different amount of forward entries");
	}

	private static String replaceLongFloats(final String inputString) {
		return inputString.replaceAll("(?<=\\d+\\.\\d{5})\\d+", Matcher.quoteReplacement("~"));
	}

	@Test
	public final void shouldReplaceLongFloats() {
		Assert.assertEquals(replaceLongFloats("hello 1.123456789"), "hello 1.12345~");
		Assert.assertEquals(replaceLongFloats("hello -1.1234"), "hello -1.1234");
		Assert.assertEquals(replaceLongFloats("hello -1.12345"), "hello -1.12345");
		Assert.assertEquals(replaceLongFloats("hello -1.123456 test"), "hello -1.12345~ test");
	}

	/**
	 * Find prefix, then replace everything in double quotes after the prefix with "$$TIME$$"
	 *
	 * @param input  String to replace in.
	 * @param prefix Prefix (e.g. date= for replacing date="whatever" with date="$$TIME$$")
	 * @return String with replacements
	 */
	private static String replaceTime(final String input, final String prefix) {
		return input.replaceAll("(?<=" + Pattern.quote(prefix) + ")\"([^\"]*)\"", Matcher.quoteReplacement("\"$$TIME$$\""));
	}

	@Test
	public static void shouldReplaceTime() {
		Assert.assertEquals(replaceTime("hello this is time=\"11-12-2011 10:20;30\" test", "time="), "hello this is time=\"$$TIME$$\" test");
	}

	private static String replace(String resultString, final String toReplace, final String replaceWith) {
		resultString = resultString.replaceAll(Pattern.quote(toReplace), Matcher.quoteReplacement(replaceWith));
		return resultString;
	}

	private File getMyrimatchExecutable() {
		final File myrimatchExecutable = Installer.myrimatch(null, Installer.Action.INSTALL);
		Assert.assertTrue(myrimatchExecutable.exists(), "Myrimatch executable must exist");
		Assert.assertTrue(myrimatchExecutable.isFile(), "Myrimatch executable must be a file");
		Assert.assertTrue(myrimatchExecutable.canExecute(), "Myrimatch executable must be actually executable");
		return myrimatchExecutable;
	}

	private void compareMappingsToBase(final MyrimatchMappings mappings, final String toReplaceInBase, final String replaceWith) throws IOException {
		final StringWriter writer = new StringWriter(10000);
		try {
			mappings.write(mappings.baseSettings(), writer);
		} finally {
			FileUtilities.closeQuietly(writer);
		}
		final String newString = writer.getBuffer().toString();
		String oldString = CharStreams.toString(mappings.baseSettings());
		if (toReplaceInBase != null) {
			oldString = oldString.replaceAll(toReplaceInBase, replaceWith);
		}

		Assert.assertEquals(newString.replaceAll("\\s+", " "), oldString.replaceAll("\\s+", " "), "No change must be presented");
	}

	private MappingContext createMappingContext() {
		final ParamsInfo paramsInfo = new MockParamsInfo();

		return new MappingContext() {
			private boolean noErrors = true;

			@Override
			public ParamsInfo getAbstractParamsInfo() {
				return paramsInfo;
			}

			@Override
			public void startMapping(final ParamName paramName) {
				LOGGER.debug("Started mapping " + paramName);
			}

			@Override
			public void reportError(final String s, final Throwable throwable) {
				LOGGER.error("Mapping error: " + s, throwable);
				noErrors = false;
			}

			@Override
			public void reportWarning(final String s) {
				LOGGER.debug("Mapping warning: " + s);
			}

			@Override
			public void reportInfo(final String s) {
				LOGGER.debug("Mapping info: " + s);
			}

			@Override
			public boolean noErrors() {
				return noErrors;
			}

			@Override
			public Curation addLegacyCuration(final String s) {
				Assert.fail("Should not be invoked");
				return null;
			}
		};
	}

	private MyrimatchMappings createMappings() {
		final MyrimatchMappingFactory factory = new MyrimatchMappingFactory();
		Assert.assertEquals(factory.getCanonicalParamFileName(), "myrimatch.template.cfg");
		final MyrimatchMappings mapping = (MyrimatchMappings) factory.createMapping();
		return mapping;
	}

	private MyrimatchWorker createWorker(final String executable) {
		final MyrimatchWorker.Factory factory = new MyrimatchWorker.Factory();
		factory.setDescription("Myrimatch");

		final MyrimatchWorker.Config config = new MyrimatchWorker.Config(executable);
		final DependencyResolver resolver = new DependencyResolver(null);

		return (MyrimatchWorker) factory.create(config, resolver);
	}

}
