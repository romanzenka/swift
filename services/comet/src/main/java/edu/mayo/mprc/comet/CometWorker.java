package edu.mayo.mprc.comet;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerBase;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.searchengine.EngineFactory;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.sqt.CleanupSqt;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public final class CometWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(CometWorker.class);
	public static final String TYPE = "comet";
	public static final String NAME = "Comet";
	public static final String DESC = "Comet search engine support. <p>Comet is freely available at <a href=\"http://sourceforge.net/projects/comet-ms/\">http://sourceforge.net/projects/comet-ms/</a>.</p>";
	public static final String PEP_XML = ".pep.xml";
	public static final String SQT = ".sqt";
	public static final String MS2 = ".ms2";
	private static final Pattern COMPILE = Pattern.compile("num_threads = \\d+");

	private File cometExecutable;

	public static final String COMET_EXECUTABLE = "cometExecutable";

	public CometWorker(final File cometExecutable) {
		this.cometExecutable = cometExecutable;
	}

	@Override
	public void process(final WorkPacket workPacket, final File tempWorkFolder, final UserProgressReporter progressReporter) {
		if (!(workPacket instanceof CometWorkPacket)) {
			throw new DaemonException("Unexpected packet type " + workPacket.getClass().getName() + ", expected " + CometWorkPacket.class.getName());
		}

		final CometWorkPacket packet = (CometWorkPacket) workPacket;

		final File finalOutputFile = packet.getOutputFile();
		final File outputFile = getTempOutputFile(tempWorkFolder, finalOutputFile);
		final String resultFileName = getResultFileName(outputFile);
		final File parameterFile = makeParameterFile(tempWorkFolder, packet);

		try {
			LOGGER.info("Comet parameters:\n-----\n" + Files.toString(parameterFile, Charsets.UTF_8) + "-----\n");
		} catch (IOException e) {
			throw new MprcException("Could not read contents of Comet parameter file " + parameterFile.getAbsolutePath(), e);
		}

		final List<String> parameters = new LinkedList<String>();
		parameters.add(cometExecutable.getPath());
		parameters.add("-P" + parameterFile.getAbsolutePath()); // Parameter file
		parameters.add("-D" + packet.getDatabaseFile().getAbsolutePath()); // Database file
		parameters.add("-N" + resultFileName); // Result file
		parameters.add(packet.getInputFile().getAbsolutePath()); // Input file

		final ProcessBuilder processBuilder = new ProcessBuilder(parameters);
		processBuilder.directory(tempWorkFolder);

		final ProcessCaller processCaller = new ProcessCaller(processBuilder, progressReporter.getLog());

		processCaller.run();

		if (processCaller.getExitValue() != 0) {
			throw new MprcException("Comet execution failed:\n" + processCaller.getFailedCallDescription());
		}

		if (finalOutputFile.getName().endsWith(SQT)) {
			// We need to clean the sqt file up
			File cleanedSqt = new File(outputFile.getParentFile(), outputFile.getName() + ".cleanup");
			CleanupSqt cleanupSqt = new CleanupSqt(outputFile, cleanedSqt, packet.getDatabaseFile());
			cleanupSqt.run();
			FileUtilities.quietDelete(outputFile);
			publish(cleanedSqt, finalOutputFile);
		} else {
			publish(outputFile, finalOutputFile);
		}

		FileUtilities.quietDelete(parameterFile);

		LOGGER.info("Comet search, " + packet.toString() + ", has been successfully completed.");
	}

	/**
	 * Make parameter file for comet. This involves tweaking the input parameter string based on
	 * requested output file.
	 *
	 * @param tempWorkFolder
	 * @param packet
	 * @return
	 */
	private File makeParameterFile(final File tempWorkFolder, final CometWorkPacket packet) {
		final File parameterFile = new File(tempWorkFolder, "comet.parameters");

		// Replace the database path and write parameters out
		String params = packet.getSearchParams();

		params = COMPILE.matcher(params).replaceAll("num_threads = " + packet.getNumRequiredCores());

		FileUtilities.writeStringToFile(parameterFile, params, true);
		return parameterFile;
	}

	/**
	 * Given an output file name, creates a "name prefix" to be passed to Comet that would fool
	 * Comet into generating the required output file.
	 * <p/>
	 * This can fail if the requested output file suffix does not match .pep.xml or .sqt
	 *
	 * @param outputFile Name of the output file.
	 * @return String to pass to Comet as a {@code -N} parameter.
	 */
	private String getResultFileName(final File outputFile) {
		if (outputFile.getAbsolutePath().endsWith(PEP_XML)) {
			return trimExtension(outputFile, PEP_XML);
		} else if (outputFile.getAbsolutePath().endsWith(SQT)) {
			return trimExtension(outputFile, SQT);
		} else {
			throw new MprcException(String.format("Swift only supports Comet generating a %s output files. Requested file was %s", PEP_XML + ", " + SQT, outputFile.getName()));
		}
	}

	private String trimExtension(final File outputFile, final String extension) {
		return new File(outputFile.getParentFile(),
				outputFile.getName().substring(0, outputFile.getName().length() - extension.length())).getAbsolutePath();
	}

	@Override
	public String check() {
		LOGGER.info("Checking Comet worker");
		if (cometExecutable == null) {
			throw new MprcException("Comet excecutable not set!");
		}
		if (!cometExecutable.isFile()) {
			throw new MprcException(String.format("Comet executable not present [%s]", cometExecutable.getAbsolutePath()));
		}

		if (!cometExecutable.canExecute()) {
			throw new MprcException(String.format("Comet file does not have executable flag set [%s]", cometExecutable.getAbsolutePath()));
		}
		return null;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("cometWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> implements EngineFactory<Config, Worker> {

		private static final EngineMetadata ENGINE_METADATA = new EngineMetadata(
				"COMET", ".sqt", "Comet", true, "comet", new CometMappingFactory(),
				new String[]{TYPE},
				new String[]{CometCache.TYPE},
				new String[]{},
				21, false);


		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			CometWorker worker = null;
			try {
				worker = new CometWorker(FileUtilities.getAbsoluteFileForExecutables(config.getCometExecutable()));
			} catch (final Exception e) {
				throw new MprcException("Comet worker could not be created.", e);
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
	public static final class Config implements ResourceConfig {
		private String cometExecutable;

		public Config() {
		}

		@Override
		public void save(ConfigWriter writer) {
			writer.put(COMET_EXECUTABLE, cometExecutable, "Path to Comet executable");
		}

		@Override
		public void load(ConfigReader reader) {
			cometExecutable = reader.get(COMET_EXECUTABLE);
		}

		@Override
		public int getPriority() {
			return 0;
		}

		public File getCometExecutable() {
			return new File(cometExecutable);
		}

		public void setCometExecutable(String cometExecutable) {
			this.cometExecutable = cometExecutable;
		}
	}

	public static final class Ui implements ServiceUiFactory {

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(COMET_EXECUTABLE, "Executable Path", "Comet executable path. Comet executables can be " +
					"<br/>found at <a href=\"http://sourceforge.net/projects/comet-ms/files/\"/>http://sourceforge.net/projects/comet-ms/files/</a>")
					.required()
					.executable(Arrays.asList(""))
					.defaultValue("comet");
		}
	}
}
