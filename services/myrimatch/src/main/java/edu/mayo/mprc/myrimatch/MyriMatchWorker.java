package edu.mayo.mprc.myrimatch;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
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
import edu.mayo.mprc.io.mgf.MgfTitles;
import edu.mayo.mprc.searchengine.EngineFactory;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public final class MyriMatchWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(MyriMatchWorker.class);
	public static final String TYPE = "myrimatch";
	public static final String NAME = "MyriMatch";
	public static final String DESC = "MyriMatch search engine support. <p>MyriMatch is freely available at <a href=\"http://fenchurch.mc.vanderbilt.edu/software.php#MyriMatch\">http://fenchurch.mc.vanderbilt.edu/software.php#MyriMatch</a>.</p>";

	private File executable;

	public static final String EXECUTABLE = "executable";
	public static final String MZ_IDENT_ML = ".mzid";

	public MyriMatchWorker(final File executable) {
		this.executable = executable;
	}

	public File getExecutable() {
		return executable;
	}

	public void setExecutable(final File executable) {
		this.executable = executable;
	}

	@Override
	public void process(final WorkPacket workPacket, final File tempWorkFolder, final UserProgressReporter progressReporter) {
		if (!(workPacket instanceof MyriMatchWorkPacket)) {
			throw new DaemonException("Unexpected packet type " + workPacket.getClass().getName() + ", expected " + MyriMatchWorkPacket.class.getName());
		}

		final MyriMatchWorkPacket packet = (MyriMatchWorkPacket) workPacket;

		checkPacketCorrectness(packet);

		final File fastaFile = packet.getDatabaseFile();

		final File inputFile = packet.getInputFile();
		final String params = packet.getSearchParams();

		final File finalOutputFile = packet.getOutputFile();
		final File outputFile = getTempOutputFile(tempWorkFolder, finalOutputFile);

		final File resultFile = new File(outputFile, outputFile.getName() + ".tmp");
		// The final file has the spectra id replaced with titles of spectra from the .mgf file

		if (finalOutputFile.exists() && inputFile.exists() && finalOutputFile.lastModified() >= inputFile.lastModified()) {
			return;
		}

		LOGGER.debug("Fasta file " + fastaFile.getAbsolutePath() + " does" + (fastaFile.exists() && fastaFile.length() > 0 ? " " : " not ") + "exist.");
		LOGGER.debug("Input file " + inputFile.getAbsolutePath() + " does" + (inputFile.exists() && inputFile.length() > 0 ? " " : " not ") + "exist.");

		final File modifiedParamsFile = new File(tempWorkFolder, "myrimatch.cfg");
		if (!modifiedParamsFile.exists()) {

			try {
				final Iterable<String> lines = Splitter.on(Pattern.compile("\\n|\\r")).omitEmptyStrings().trimResults().split(params);
				int i = 0;
				boolean wasDecoyPrefix = false;
				final List<String> result = new ArrayList<String>(100);
				for (final String line : lines) {
					if (line.startsWith("DecoyPrefix")) {
						wasDecoyPrefix = true;
						result.add("DecoyPrefix = \"" + packet.getDecoySequencePrefix() + "\"");
					} else {
						result.add(line);
					}

					i++;
				}
				if (!wasDecoyPrefix) {
					result.add("");
					result.add("# Decoy sequence prefix is appended to all decoy matches");
					result.add("DecoyPrefix = " + packet.getDecoySequencePrefix());
				}

				final String contents = Joiner.on('\n').join(lines);
				Files.write(contents, modifiedParamsFile, Charsets.US_ASCII);
				LOGGER.info("Myrimatch config to be used:\n" + modifiedParamsFile.getAbsolutePath() + "\n---\n" + contents);

			} catch (final IOException e) {
				throw new MprcException("Could not append information to parameter file " + modifiedParamsFile.getAbsolutePath(), e);
			}
		}

		final List<String> parameters = new LinkedList<String>();
		parameters.add(executable.getPath());
		parameters.add("-ProteinDatabase");
		parameters.add(fastaFile.getAbsolutePath());
		parameters.add("-cfg");
		parameters.add(modifiedParamsFile.getAbsolutePath());
		parameters.add(inputFile.getAbsolutePath());

		final ProcessBuilder processBuilder = new ProcessBuilder(parameters);
		processBuilder.directory(tempWorkFolder);

		final ProcessCaller processCaller = new ProcessCaller(processBuilder, progressReporter.getLog());

		LOGGER.info("MyriMatch search, " + packet.toString() + ", has been submitted.");
		processCaller.setOutputMonitor(new

						MyriMatchLogMonitor(progressReporter)

		);
		processCaller.runAndCheck("myrimatch");

		final File createdResultFile = new File(tempWorkFolder, FileUtilities.stripExtension(packet.getInputFile().getName()) + MZ_IDENT_ML);

		// We need to cleanup after mgf-based run
		if ("mgf".equals(FileUtilities.getExtension(inputFile.getName()))) {
			final List<String> titles = MgfTitles.getTitles(inputFile);
			MzIdentMl.replace(createdResultFile, titles, resultFile);
			if (!resultFile.equals(outputFile)) {
				FileUtilities.rename(resultFile, outputFile);
			}
		} else {
			if (!createdResultFile.equals(outputFile)) {
				FileUtilities.rename(createdResultFile, outputFile);
			}
		}

		if (createdResultFile.exists() && !createdResultFile.equals(outputFile)) {
			FileUtilities.deleteNow(createdResultFile);
		}

		publish(outputFile, finalOutputFile);

		LOGGER.info("MyriMatch search, " + packet.toString() + ", has been successfully completed.");
	}

	private void checkPacketCorrectness(final MyriMatchWorkPacket packet) {
		if (packet.getSearchParams() == null) {
			throw new MprcException("Params must not be null");
		}
		if (packet.getOutputFile() == null) {
			throw new MprcException("Result file must not be null");
		}
		if (packet.getInputFile() == null) {
			throw new MprcException("Input file must not be null");
		}
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("myrimatchWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> implements EngineFactory<Config, Worker> {
		private static final EngineMetadata ENGINE_METADATA = new EngineMetadata(
				"MYRIMATCH", MZ_IDENT_ML, "MyriMatch", false, "myrimatch", new MyriMatchMappingFactory(),
				new String[]{TYPE},
				new String[]{MyriMatchCache.TYPE},
				new String[]{},
				40, false);

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			MyriMatchWorker worker = null;
			try {
				worker = new MyriMatchWorker(FileUtilities.getAbsoluteFileForExecutables(new File(config.get(EXECUTABLE))));
			} catch (final Exception e) {
				throw new MprcException("MyriMatch worker could not be created.", e);
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
			builder.property(EXECUTABLE, "Executable Path", "MyriMatch executable path. MyriMatch executables can be " +
					"<br/>found at <a href=\"http://fenchurch.mc.vanderbilt.edu/software.php#MyriMatch/\"/>http://fenchurch.mc.vanderbilt.edu/software.php#MyriMatch</a>")
					.required()
					.executable(Arrays.asList("-v"))
					.defaultValue("myrimatch");
		}
	}
}
