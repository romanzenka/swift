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
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class QuaMeterWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(QuaMeterWorker.class);
	public static final String TYPE = "quameter";
	public static final String NAME = "QuaMeter";
	public static final String DESC = "QuaMeter quality metrics support. <p>QuaMeter is freely available at <a href=\"http://fenchurch.mc.vanderbilt.edu/software.php#QuaMeter\">http://fenchurch.mc.vanderbilt.edu/software.php#QuaMeter</a>.</p>";

	private static final String RESULT_EXTENSION = ".qual.txt";

	private File executable;

	public static final String EXECUTABLE = "executable";

	public QuaMeterWorker(final File executable) {
		this.executable = executable;
	}

	public File getExecutable() {
		return executable;
	}

	public void setExecutable(final File executable) {
		this.executable = executable;
	}

	@Override
	public void process(WorkPacket workPacket, UserProgressReporter progressReporter) {
		if (!(workPacket instanceof QuaMeterWorkPacket)) {
			throw new DaemonException("Unexpected packet type " + workPacket.getClass().getName() + ", expected " + QuaMeterWorkPacket.class.getName());
		}

		final QuaMeterWorkPacket packet = (QuaMeterWorkPacket) workPacket;

		checkPacketCorrectness(packet);

		final File workFolder = packet.getOutputFile().getParentFile().getAbsoluteFile();
		FileUtilities.ensureFolderExists(workFolder);

		final File rawFile = packet.getInputFile().getAbsoluteFile();
		final File idpDbFile = packet.getIdpDbFile().getAbsoluteFile();
		final File finalFile = packet.getOutputFile().getAbsoluteFile();

		if (finalFile.exists() && rawFile.exists() && idpDbFile.exists()
				&& finalFile.lastModified() >= rawFile.lastModified()
				&& finalFile.lastModified() >= idpDbFile.lastModified()) {
			return;
		}

		LOGGER.debug("RAW file " + rawFile.getPath() + " does" + (rawFile.exists() && rawFile.length() > 0 ? " " : " not ") + "exist.");
		LOGGER.debug("idpDB file " + idpDbFile.getPath() + " does" + (idpDbFile.exists() && idpDbFile.length() > 0 ? " " : " not ") + "exist.");
		LOGGER.debug("Final file " + finalFile.getPath() + " does" + (finalFile.exists() && finalFile.length() > 0 ? " " : " not ") + "exist.");

		String resultName = FileUtilities.getFileNameWithoutExtension(idpDbFile);
		File createdResultFile = new File(workFolder, resultName + RESULT_EXTENSION);

		final List<String> parameters = new LinkedList<String>();
		parameters.add(executable.getPath());
		parameters.add("-workdir");
		parameters.add(workFolder.getAbsolutePath());
		parameters.add("-OutputFilepath");
		parameters.add(createdResultFile.getAbsolutePath());
		parameters.add("-StatusUpdateFrequency");
		parameters.add("20");
		parameters.add("-Instrument");
		parameters.add(packet.isMonoisotopic() ? "orbi" : "ltq");
		parameters.add("-RawDataPath");
		parameters.add(rawFile.getParentFile().getAbsolutePath());
		parameters.add("-RawDataFormat");
		parameters.add("raw");
		parameters.add("-ScoreCutoff");
		parameters.add(String.valueOf(packet.getFdrScoreCutoff()));
		parameters.add("-ChromatogramOutput");
		parameters.add("false");
		parameters.add("-dump");

		parameters.add(idpDbFile.getAbsolutePath());

		final ProcessBuilder processBuilder = new ProcessBuilder(parameters);
		processBuilder.directory(workFolder);

		final ProcessCaller processCaller = new ProcessCaller(processBuilder);

		LOGGER.info("QuaMeter search, " + packet.toString() + ", has been submitted.");
		processCaller.runAndCheck("quameter");

		if (!createdResultFile.equals(finalFile)) {
			FileUtilities.rename(createdResultFile, finalFile);
		}

		LOGGER.info("QuaMeter search, " + packet.toString() + ", has been successfully completed.");
	}

	private void checkPacketCorrectness(final QuaMeterWorkPacket packet) {
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
				new String[]{QuaMeterCache.TYPE},
				new String[]{},
				90, true);

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			QuaMeterWorker worker = null;
			try {
				worker = new QuaMeterWorker(FileUtilities.getAbsoluteFileForExecutables(new File(config.get(EXECUTABLE))));
			} catch (Exception e) {
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
			builder.property(EXECUTABLE, "Executable Path", "<p>QuaMeter executable path. QuaMeter executables can be " +
					"<br/>found at <a href=\"http://fenchurch.mc.vanderbilt.edu/software.php#QuaMeter/\"/>http://fenchurch.mc.vanderbilt.edu/software.php#QuaMeter</a></p>" +
					"<p>Use a wrapper script when running on Linux that takes care of calling Wine.</p>")
					.required()
					.executable(Arrays.asList("-v"))
					.defaultValue("quameter.exe");
		}
	}
}