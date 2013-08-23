package edu.mayo.mprc.myrimatch;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerBase;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.daemon.exception.DaemonException;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
	public void process(WorkPacket workPacket, UserProgressReporter progressReporter) {
		if (!(workPacket instanceof MyriMatchWorkPacket)) {
			throw new DaemonException("Unexpected packet type " + workPacket.getClass().getName() + ", expected " + MyriMatchWorkPacket.class.getName());
		}

		final MyriMatchWorkPacket packet = (MyriMatchWorkPacket) workPacket;

		checkPacketCorrectness(packet);

		FileUtilities.ensureFolderExists(packet.getWorkFolder());

		final File fastaFile = packet.getDatabaseFile();

		final File inputFile = packet.getInputFile();
		final File paramsFile = packet.getSearchParamsFile();
		final File resultFile = new File(packet.getOutputFile().getParentFile(), packet.getOutputFile().getName() + ".tmp");
		// The final file has the spectra id replaced with titles of spectra from the .mgf file
		final File finalFile = new File(packet.getOutputFile().getParentFile(), packet.getOutputFile().getName());

		if (finalFile.exists() && inputFile.exists() && finalFile.lastModified() >= inputFile.lastModified()) {
			return;
		}

		LOGGER.debug("Fasta file " + fastaFile.getAbsolutePath() + " does" + (fastaFile.exists() && fastaFile.length() > 0 ? " " : " not ") + "exist.");
		LOGGER.debug("Input file " + inputFile.getAbsolutePath() + " does" + (inputFile.exists() && inputFile.length() > 0 ? " " : " not ") + "exist.");
		LOGGER.debug("Parameter file " + paramsFile.getAbsolutePath() + " does" + (paramsFile.exists() && paramsFile.length() > 0 ? " " : " not ") + "exist.");

		File modifiedParamsFile = MyriMatchMappingFactory.resultingParamsFile(paramsFile);
		if (!modifiedParamsFile.exists() || modifiedParamsFile.lastModified() < paramsFile.lastModified()) {

			try {
				final List<String> lines = Files.readLines(paramsFile, Charsets.US_ASCII);
				int i = 0;
				boolean wasDecoyPrefix = false;
				for (final String line : lines) {
					if (line.startsWith("DecoyPrefix")) {
						wasDecoyPrefix = true;
						lines.set(i, "DecoyPrefix = \"" + packet.getDecoySequencePrefix() + "\"");
					}

					i++;
				}
				if (!wasDecoyPrefix) {
					lines.add("");
					lines.add("# Decoy sequence prefix is appended to all decoy matches");
					lines.add("DecoyPrefix = " + packet.getDecoySequencePrefix());
				}

				Files.write(Joiner.on('\n').join(lines), modifiedParamsFile, Charsets.US_ASCII);

			} catch (IOException e) {
				throw new MprcException("Could not append information to parameter file " + paramsFile.getAbsolutePath(), e);
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
		processBuilder.directory(packet.getWorkFolder());

		final ProcessCaller processCaller = new ProcessCaller(processBuilder);

		LOGGER.info("MyriMatch search, " + packet.toString() + ", has been submitted.");
		processCaller.setOutputMonitor(new

				MyriMatchLogMonitor(progressReporter)

		);
		processCaller.runAndCheck("myrimatch");

		final File createdResultFile = new File(packet.getWorkFolder(), FileUtilities.stripExtension(packet.getInputFile().getName()) + MZ_IDENT_ML);

		final List<String> titles = MgfTitles.getTitles(inputFile);
		MzIdentMl.replace(createdResultFile, titles, resultFile);

		if (!resultFile.equals(finalFile)) {
			FileUtilities.rename(resultFile, finalFile);
		}

		if (!createdResultFile.equals(finalFile)) {
			FileUtilities.deleteNow(createdResultFile);
		}

		LOGGER.info("MyriMatch search, " + packet.toString() + ", has been successfully completed.");
	}

	private void checkPacketCorrectness(final MyriMatchWorkPacket packet) {
		if (packet.getSearchParamsFile() == null) {
			throw new MprcException("Params file must not be null");
		}
		if (packet.getWorkFolder() == null) {
			throw new MprcException("Work folder must not be null");
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
				worker = new MyriMatchWorker(FileUtilities.getAbsoluteFileForExecutables(new File(config.getExecutable())));
			} catch (Exception e) {
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
	public static final class Config implements ResourceConfig {
		private String executable;

		public Config() {
		}

		public Config(final String executable) {
			this.executable = executable;
		}

		public String getExecutable() {
			return executable;
		}

		public void setExecutable(final String executable) {
			this.executable = executable;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(EXECUTABLE, getExecutable(), "MyriMatch executable");
		}

		@Override
		public void load(final ConfigReader reader) {
			setExecutable(reader.get(EXECUTABLE));
		}

		@Override
		public int getPriority() {
			return 0;
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
