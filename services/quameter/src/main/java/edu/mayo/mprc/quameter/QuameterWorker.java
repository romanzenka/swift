package edu.mayo.mprc.quameter;

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
import edu.mayo.mprc.searchengine.EngineFactory;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.utilities.FilePathShortener;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class QuameterWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(QuameterWorker.class);
	public static final String TYPE = "quameter";
	public static final String NAME = "QuaMeter";
	public static final String DESC = "QuaMeter quality metrics support. <p>QuaMeter is freely available at <a href=\"http://fenchurch.mc.vanderbilt.edu/software.php#QuaMeter\">http://fenchurch.mc.vanderbilt.edu/software.php#QuaMeter</a>.</p>";

	private static final String RESULT_EXTENSION = ".qual.txt";
	private static final int MAX_QUAMETER_IDPDB_PATH = 200;

	private File executable;

	public static final String EXECUTABLE = "executable";

	public QuameterWorker(final File executable) {
		this.executable = executable;
	}

	public File getExecutable() {
		return executable;
	}

	public void setExecutable(final File executable) {
		this.executable = executable;
	}

	private static void param(List<String> parameters, String key, String value) {
		parameters.add("-" + key);
		parameters.add(value);
	}

	@Override
	public void process(final WorkPacket workPacket, final File tempWorkFolder, final UserProgressReporter progressReporter) {
		if (!(workPacket instanceof QuameterWorkPacket)) {
			throw new DaemonException("Unexpected packet type " + workPacket.getClass().getName() + ", expected " + QuameterWorkPacket.class.getName());
		}

		final QuameterWorkPacket packet = (QuameterWorkPacket) workPacket;

		checkPacketCorrectness(packet);

		final File finalOutputFile = packet.getOutputFile().getAbsoluteFile();
		final File outputFile = getTempOutputFile(tempWorkFolder, finalOutputFile);

		final File rawFile = packet.getInputFile().getAbsoluteFile();
		final File idpDbFile = packet.getIdpDbFile().getAbsoluteFile();

		if (finalOutputFile.exists() && rawFile.exists() && idpDbFile.exists()
				&& finalOutputFile.lastModified() >= rawFile.lastModified()
				&& finalOutputFile.lastModified() >= idpDbFile.lastModified()) {
			return;
		}

		LOGGER.debug("RAW file " + rawFile.getPath() + " does" + (rawFile.exists() && rawFile.length() > 0 ? " " : " not ") + "exist.");
		LOGGER.debug("idpDB file " + idpDbFile.getPath() + " does" + (idpDbFile.exists() && idpDbFile.length() > 0 ? " " : " not ") + "exist.");
		LOGGER.debug("Final file " + outputFile.getPath() + " does" + (outputFile.exists() && outputFile.length() > 0 ? " " : " not ") + "exist.");

		final String resultName = FileUtilities.getFileNameWithoutExtension(idpDbFile);
		final File createdResultFile = new File(tempWorkFolder, resultName + RESULT_EXTENSION);

		final List<String> parameters = new LinkedList<String>();
		parameters.add(executable.getPath());
		param(parameters, "workdir", tempWorkFolder.getAbsolutePath());
		param(parameters, "OutputFilepath", createdResultFile.getAbsolutePath());
		param(parameters, "Instrument", packet.isMonoisotopic() ? "orbi" : "ltq");
		param(parameters, "RawDataPath", rawFile.getParentFile().getAbsolutePath());
		if (rawFile.getName().endsWith(".mzML")) {
			param(parameters, "RawDataFormat", "mzML");
		} else {
			param(parameters, "RawDataFormat", "raw");
		}
		final NumberFormat format = new DecimalFormat("#0.00");
		param(parameters, "ScoreCutoff", format.format(packet.getFdrScoreCutoff()));
		param(parameters, "SpectrumListFilters", "threshold absolute 0.00000000001 most-intense");
		param(parameters, "StatusUpdateFrequency", "20");
		param(parameters, "UseMultipleProcessors", "true");
		param(parameters, "MetricsType", "nistms");
		param(parameters, "NumChargeStates", "3");
		if (packet.isMonoisotopic()) {
			param(parameters, "ChromatogramMzLowerOffset", "20ppm");
			param(parameters, "ChromatogramMzUpperOffset", "20ppm");
		} else {
			param(parameters, "ChromatogramMzLowerOffset", "0.5mz");
			param(parameters, "ChromatogramMzUpperOffset", "1.0mz");
		}
		param(parameters, "ChromatogramOutput", "false");

		parameters.add("-dump");

		final String idpDbFilePath;
		final FilePathShortener idpDbFileShortened;
		if (executable.getAbsolutePath().contains("wine")) {
			idpDbFileShortened = new FilePathShortener(idpDbFile, MAX_QUAMETER_IDPDB_PATH);
			idpDbFilePath = idpDbFileShortened.getShortenedFile().getAbsolutePath();
		} else {
			idpDbFilePath = idpDbFile.getAbsolutePath();
			idpDbFileShortened = null;
		}
		try {

			parameters.add(idpDbFilePath);

			final ProcessBuilder processBuilder = new ProcessBuilder(parameters);
			processBuilder.directory(tempWorkFolder);
			// Fix the locale to trivial C as QuaMeter does not hardcode it. This means no UTF-8 and similar.
			processBuilder.environment().put("LC_ALL", "C");

			final ProcessCaller processCaller = new ProcessCaller(processBuilder, progressReporter.getLog());

			LOGGER.info("QuaMeter search, " + packet.toString() + ", has been submitted.");
			processCaller.runAndCheck("quameter");
		} finally {
			if (idpDbFileShortened != null) {
				idpDbFileShortened.cleanup();
			}
		}

		if (!createdResultFile.equals(outputFile)) {
			FileUtilities.rename(createdResultFile, outputFile);
		}

		publish(outputFile, finalOutputFile);

		LOGGER.info("QuaMeter search, " + packet.toString() + ", has been successfully completed.");
	}

	private void checkPacketCorrectness(final QuameterWorkPacket packet) {
		if (packet.getInputFile() == null) {
			throw new MprcException("Raw file must not be null");
		}
		if (packet.getIdpDbFile() == null) {
			throw new MprcException("IdpDB file must not be null");
		}
		if (packet.getOutputFile() == null) {
			throw new MprcException("Result file must not be null");
		}
		if (packet.getFdrScoreCutoff() > 1.0) {
			throw new MprcException("FDR score cutoff should be between 0 and 1");
		}
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("quameterWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> implements EngineFactory<Config, Worker> {
		private static final EngineMetadata ENGINE_METADATA = new EngineMetadata(
				"QUAMETER", ".qual.txt", "QuaMeter", false, "quameter", null,
				new String[]{TYPE},
				new String[]{QuameterCache.TYPE},
				new String[]{},
				90, true);

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			QuameterWorker worker = null;
			try {
				worker = new QuameterWorker(FileUtilities.getAbsoluteFileForExecutables(new File(config.get(EXECUTABLE))));
			} catch (final Exception e) {
				throw new MprcException("QuaMeter worker could not be created.", e);
			}
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
					.property(EXECUTABLE, "Executable Path", "<p>QuaMeter executable path. QuaMeter executables can be " +
							"<br/>found at <a href=\"http://fenchurch.mc.vanderbilt.edu/software.php#QuaMeter/\"/>http://fenchurch.mc.vanderbilt.edu/software.php#QuaMeter</a></p>" +
							"<p>Use a wrapper script when running on Linux that takes care of calling Wine.</p>")
					.required()
					.executable(Arrays.asList("-v"))
					.defaultValue("quameter.exe");
		}
	}
}