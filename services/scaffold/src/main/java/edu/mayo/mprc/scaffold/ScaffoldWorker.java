package edu.mayo.mprc.scaffold;

import com.google.common.collect.Lists;
import com.jamesmurty.utils.XMLBuilder;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ResourceConfigBase;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerBase;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldSpectraVersion;
import edu.mayo.mprc.searchengine.EngineFactory;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.xml.transform.OutputKeys;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public final class ScaffoldWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(ScaffoldWorker.class);
	private static final String SCAFFOLD_BATCH_SCRIPT = "scaffoldBatchScript";
	public static final String SCAFFOLD_UNIMOD = "scaffoldUnimod";
	public static final String TYPE = "scaffold";
	public static final String NAME = "Scaffold";
	public static final String DESC = "Scaffold integrates results from multiple search engines into a single file. You need Scaffold Batch license from <a href=\"http://www.proteomesoftware.com/\">http://www.proteomesoftware.com/</a>";

	private File scaffoldBatchScript;
	private File scaffoldUnimod;

	public ScaffoldWorker() {
	}

	@Override
	public String check() {
		LOGGER.info("Checking scaffold batch");
		if (!getScaffoldBatchScript().canExecute()) {
			return "Scaffold batch script not executable: " + scaffoldBatchScript.getAbsolutePath();
		}
		if (getScaffoldUnimod().isFile()) {
			return "Scaffold's unimod file not present: " + getScaffoldUnimod().getAbsolutePath();
		}
		return null;
	}

	/**
	 * TODO: We need to modify Scaffold worker to explicitly publish the files it generates
	 * <p/>
	 * Scaffold currently takes a work packet with the scafml document that fully drives the entire process.
	 * The scafml document lists output folder and all the reports to be generated.
	 * This needs to be changed to go into a temp work folder, and only when Scaffold finishes, all the resulting
	 * files get moved to their target location.
	 */
	@Override
	public void process(final WorkPacket workPacket, final File tempWorkFolder, final UserProgressReporter progressReporter) {
		if (workPacket instanceof ScaffoldWorkPacket) {
			processSearch((ScaffoldWorkPacket) workPacket, progressReporter);
		} else if (workPacket instanceof ScaffoldSpectrumExportWorkPacket) {
			processSpectrumExport((ScaffoldSpectrumExportWorkPacket) workPacket, progressReporter);
		} else {
			throw new DaemonException("Unexpected packet type " + workPacket.getClass().getName() + ", expected " + ScaffoldWorkPacket.class.getName() + " or " + ScaffoldSpectrumExportWorkPacket.class.getName());
		}
	}

	/**
	 * Run Scaffold search.
	 *
	 * @param scaffoldWorkPacket Work to do.
	 * @param progressReporter   Where to report progress.
	 */
	private void processSearch(final ScaffoldWorkPacket scaffoldWorkPacket, final UserProgressReporter progressReporter) {
		LOGGER.debug("Scaffold search processing request");

		final File outputFolder = scaffoldWorkPacket.getOutputFolder();
		// Make sure the output folder is there
		FileUtilities.ensureFolderExists(outputFolder);

		final File scaffoldWorkFolder = getScaffoldBatchScript().getParentFile();
		final File scafmlFile = createScafmlFile(scaffoldWorkPacket);

		final List<File> expectedFiles = Lists.newArrayListWithExpectedSize(scaffoldWorkPacket.getOutputFiles().size());
		for (final String expectedFileName : scaffoldWorkPacket.getOutputFiles()) {
			expectedFiles.add(new File(outputFolder, expectedFileName));
		}

		runScaffold(progressReporter, scafmlFile, scaffoldWorkFolder, outputFolder, expectedFiles);
	}

	/**
	 * Run Scaffold spectrum expprt.
	 *
	 * @param scaffoldWorkPacket Work to do.
	 * @param progressReporter   Where to report progress.
	 */
	private void processSpectrumExport(final ScaffoldSpectrumExportWorkPacket scaffoldWorkPacket, final UserProgressReporter progressReporter) {
		LOGGER.debug("Scaffold spectrum export request");

		final File result = scaffoldWorkPacket.getSpectrumExportFile();
		if (result.exists() && result.isFile() && result.canRead()) {
			// The file is there. Is it the correct version?
			if (isScaffoldSpectrumExport(result)) {
				LOGGER.info("The spectrum export has already been performed: [" + result.getAbsolutePath() + "]");
				return;
			} else {
				if (!result.delete()) {
					throw new MprcException("Could not delete old version of Scaffold spectrum report: [" + result.getAbsolutePath() + "]");
				}
			}
		}
		final File outputFolder = result.getParentFile();
		// Make sure the parent folder is there
		FileUtilities.ensureFolderExists(outputFolder);

		final File scaffoldWorkFolder = getScaffoldBatchScript().getParentFile();
		final File scafmlFile = createSpectrumExportScafmlFile(scaffoldWorkPacket, outputFolder);

		runScaffold(progressReporter, scafmlFile, scaffoldWorkFolder, outputFolder, Arrays.asList(result));

		if (!isScaffoldSpectrumExport(result)) {
			throw new MprcException("Even after rerunning Scaffold, the spectrum report is still the old version");
		}
	}

	/**
	 * @param export The Scaffold spectrum export to check.
	 * @return True if this is a proper Scaffold spectrum export (not an older version).
	 */
	private boolean isScaffoldSpectrumExport(final File export) {
		final ScaffoldSpectraVersion version = new ScaffoldSpectraVersion();
		version.load(export, null/*Not sure which version*/, null/* No progress reporting */);
		// Currently if the version starts with 3, it is deemed ok
		return version.getScaffoldVersion().startsWith("Scaffold_3");
	}

	/**
	 * Execute Scaffold.
	 *
	 * @param progressReporter   Where to report progress.
	 * @param scafmlFile         Scafml file driving Scaffold
	 * @param scaffoldWorkFolder Where should Scaffold run (usually the Scaffold install folder)
	 * @param outputFolder       Where do the Scaffold outputs go.
	 * @param expectedResultFiles What files are we expected to obtain from Scaffold.
	 */
	private void runScaffold(final UserProgressReporter progressReporter, final File scafmlFile, final File scaffoldWorkFolder, final File outputFolder,
	                         final List<File> expectedResultFiles) {
		final ProcessBuilder processBuilder = new ProcessBuilder(getScaffoldBatchScript().getAbsolutePath(), scafmlFile.getAbsolutePath())
				.directory(scaffoldWorkFolder);

		final ProcessCaller caller = new ProcessCaller(processBuilder, progressReporter.getLog());
		final ScaffoldLogMonitor outputMonitor = new ScaffoldLogMonitor(progressReporter);
		caller.setOutputMonitor(outputMonitor);

		try {
			caller.runAndCheck("Scaffold3");
		} catch (final Exception e) {
			throw new MprcException(e);
		}

		FileUtilities.restoreUmaskRights(outputFolder, true);

		if (outputMonitor.isStaleFile()) {
			LOGGER.warn("Scaffold reported stale file handles, deleting its output");
			// Scaffold output is not trustworthy
			for (final File result : expectedResultFiles) {
				LOGGER.warn("Deleting stale Scaffold file: " + result.getAbsolutePath());
				FileUtilities.quietDelete(result);
			}
			throw new MprcException("Scaffold failed (reported stale file handle). This signalizes a filesystem glitch");
		}
	}

	/**
	 * Make a scafml file for running Scaffold search.
	 *
	 * @param workPacket Description of work to do.
	 * @return Created scafml file.
	 */
	private File createScafmlFile(final ScaffoldWorkPacket workPacket) {
		// Create the .scafml file
		final String scafmlDocument = workPacket.getScafmlFile().getDocument();
		final File scafmlFile = workPacket.getScafmlFileLocation();
		FileUtilities.writeStringToFile(scafmlFile, scafmlDocument, true);
		return scafmlFile;
	}

	/**
	 * Create a .scafml file instructing Scaffold to export spectra.
	 *
	 * @param work         Work to do.
	 * @param outputFolder Where to put the {@code .scafml} file.
	 * @return The created .scafml file
	 */
	private File createSpectrumExportScafmlFile(final ScaffoldSpectrumExportWorkPacket work, final File outputFolder) {
		final String experimentName = FileUtilities.stripGzippedExtension(work.getScaffoldFile().getName());
		final File scafmlFile = new File(outputFolder,
				experimentName + "_spectrum_export.scafml");

		final String contents;
		try {
			contents = getScafmlSpectrumExport(work);
		} catch (final Exception e) {
			throw new MprcException("Could not export " + scafmlFile.getAbsolutePath(), e);
		}

		FileUtilities.writeStringToFile(scafmlFile, contents, true);
		return scafmlFile;
	}

	/**
	 * @param work Spectrum export to do.
	 * @return String to put into .scafml file that will produce the export.
	 */
	static String getScafmlSpectrumExport(final ScaffoldSpectrumExportWorkPacket work) {
		try {
			final String experimentName = FileUtilities.stripGzippedExtension(work.getScaffoldFile().getName());
			final XMLBuilder builder = XMLBuilder.create("Scaffold");
			builder.getDocument().setXmlStandalone(true);

			builder.a("version", "1.5")
					.e("Experiment")
					.a("name", experimentName)
					.a("load", work.getScaffoldFile().getAbsolutePath())

					.e("DisplayThresholds")
					.a("name", "Some Thresholds")
					.a("id", "thresh")
					.a("proteinProbability", "0.8")
					.a("minimumPeptideCount", "1")
					.a("peptideProbability", "0.8")
					.a("minimumNTT", "1")
					.a("useCharge", "true,true,true")
					.a("useMergedPeptideProbability", "true")
					.t("")
					.up()

					.e("Export")
					.a("type", "spectrum")
					.a("thresholds", "thresh")
					.a("path", work.getSpectrumExportFile().getAbsolutePath())
					.up();

			final Properties outputProperties = new Properties();
			// Explicitly identify the output as an XML document
			outputProperties.setProperty(OutputKeys.METHOD, "xml");
			// Pretty-print the XML output (doesn't work in all cases)
			outputProperties.setProperty(OutputKeys.INDENT, "yes");
			outputProperties.setProperty("{http://xml.apache.org/xalan}indent-amount", "2");
			outputProperties.setProperty(OutputKeys.STANDALONE, "yes");

			return builder.asString(outputProperties);
		} catch (final Exception e) {
			throw new MprcException("Could not create .scafml for spectrum export", e);
		}
	}


	private File getScaffoldBatchScript() {
		return scaffoldBatchScript;
	}

	private void setScaffoldBatchScript(final File scaffoldBatchScript) {
		this.scaffoldBatchScript = scaffoldBatchScript;
	}

	public void setScaffoldUnimod(final File scaffoldUnimod) {
		this.scaffoldUnimod = scaffoldUnimod;
	}

	public File getScaffoldUnimod() {
		return scaffoldUnimod;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("scaffoldWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> implements EngineFactory<Config, Worker> {

		private static final EngineMetadata ENGINE_METADATA = new EngineMetadata(
				"SCAFFOLD", ".sf3", "Scaffold", true, "scaffold", null,
				new String[]{TYPE},
				new String[]{},
				new String[]{ScaffoldDeploymentService.TYPE},
				70, true);

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final ScaffoldWorker worker = new ScaffoldWorker();
			worker.setScaffoldBatchScript(getFile(config, SCAFFOLD_BATCH_SCRIPT));
			worker.setScaffoldUnimod(getFile(config, SCAFFOLD_UNIMOD));

			return worker;
		}

		@Override
		public EngineMetadata getEngineMetadata() {
			return ENGINE_METADATA;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config extends ResourceConfigBase {
		public Config() {
		}
	}

	public static final class Ui implements ServiceUiFactory {
		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(SCAFFOLD_BATCH_SCRIPT, "ScaffoldBatch path", "Path to the ScaffoldBatch script<p>Default for Linux: <code>/opt/Scaffold3/ScaffoldBatch3</code></p>")
					.defaultValue("/opt/Scaffold3/ScaffoldBatch3")
					.required()
					.executable(Arrays.asList("-v"))

					.property(SCAFFOLD_UNIMOD, "Scaffold unimod.xml", "Path to the Scaffold's unimod.xml config file. Must be accessible from the daemon where Search Result Loader runs.<p>Default for Linux: <code>/opt/Scaffold?/parameters/unimod.xml</code></p>")
					.defaultValue("/opt/Scaffold3/parameters/unimod.xml")
					.required()
					.existingFile();
		}
	}

}
