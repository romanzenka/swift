package edu.mayo.mprc.scaffold;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerBase;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.searchengine.EngineFactory;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs scaffold search directly (without grid engine).
 */
public final class ScaffoldWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(ScaffoldWorker.class);
	private static final String SCAFFOLD_DIR = "scaffoldDir";
	private static final String SCAFFOLD_JAVA_VM_PATH = "scaffoldJavaVmPath";
	private static final String MEMORY_LIMIT = "memoryLimit";
	public static final String TYPE = "scaffold";
	public static final String NAME = "Scaffold";
	public static final String DESC = "Scaffold 2 integrates results from multiple search engines into a single file. You need Scaffold 2 Batch license from <a href=\"http://www.proteomesoftware.com/\">http://www.proteomesoftware.com/</a>";

	private List<String> args = null;
	private File scaffoldDir;
	private String scaffoldJavaVmPath = "java";
	private String memoryLimit = "256M";

	public String toString() {
		final StringBuilder arguments = new StringBuilder();
		if (args != null) {
			for (final String arg : args) {
				arguments.append(arg);
				arguments.append(" ");
			}
		}
		return "Scaffold worker that executes Scaffold directly\n\tArgs: " + arguments;

	}

	public File getScaffoldDir() {
		return scaffoldDir;
	}

	public void setScaffoldDir(final File scaffoldDir) {
		this.scaffoldDir = scaffoldDir;
	}

	public String getScaffoldJavaVmPath() {
		return scaffoldJavaVmPath;
	}

	public void setScaffoldJavaVmPath(final String scaffoldJavaVmPath) {
		this.scaffoldJavaVmPath = scaffoldJavaVmPath;
	}

	public String getMemoryLimit() {
		return memoryLimit;
	}

	public void setMemoryLimit(final String memoryLimit) {
		this.memoryLimit = memoryLimit;
	}

	@Override
	public void process(final WorkPacket workPacket, final UserProgressReporter progressReporter) {
		if (args == null) {
			initialize();
		}
		if (!(workPacket instanceof ScaffoldWorkPacket)) {
			throw new DaemonException("Unexpected packet type " + workPacket.getClass().getName() + ", expected " + ScaffoldWorkPacket.class.getName());
		}

		final ScaffoldWorkPacket scaffoldWorkPacket = (ScaffoldWorkPacket) workPacket;
		LOGGER.debug("Scaffold search processing request");

		final File outputFolder = scaffoldWorkPacket.getOutputFolder();
		// Make sure the output folder is there
		FileUtilities.ensureFolderExists(outputFolder);

		final ScaffoldArgsBuilder scaffoldArgsBuilder = new ScaffoldArgsBuilder(scaffoldDir);
		// Returns work folder for scaffold. Depending on the version, it is either the folder where the output is produced,
		// or the Scaffold install folder itself.
		final File scaffoldWorkFolder = scaffoldArgsBuilder.getWorkFolder(outputFolder);
		// Make sure the work folder is there.
		FileUtilities.ensureFolderExists(scaffoldWorkFolder);
		final File scafmlFile = createScafmlFile(scaffoldWorkPacket, outputFolder);

		final List<String> thisargs = new ArrayList<String>(args.size() + 2);
		thisargs.add(scaffoldJavaVmPath);
		for (final String arg : args) {
			thisargs.add(arg);
		}
		thisargs.add(scafmlFile.getAbsolutePath());

		final ProcessBuilder processBuilder = new ProcessBuilder(thisargs)
				.directory(scaffoldWorkFolder);

		final ProcessCaller caller = new ProcessCaller(processBuilder);
		caller.setOutputMonitor(new ScaffoldLogMonitor(progressReporter));

		try {
			caller.runAndCheck("Scaffold");
		} catch (Exception e) {
			throw new MprcException(e);
		}
		FileUtilities.restoreUmaskRights(outputFolder, true);
	}

	public static File createScafmlFile(final ScaffoldWorkPacket workPacket, final File outputFolder) {
		// Create the .scafml file
		final String scafmlDocument = workPacket.getScafmlFile().getDocument();
		final File scafmlFile = new File(outputFolder, workPacket.getExperimentName() + ".scafml");
		FileUtilities.writeStringToFile(scafmlFile, scafmlDocument, true);
		return scafmlFile;
	}

	public void initialize() {
		final ScaffoldArgsBuilder execution = new ScaffoldArgsBuilder(scaffoldDir);
		args = execution.buildScaffoldArgs(memoryLimit, execution.getScaffoldBatchClassName());
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("scaffoldWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> implements EngineFactory {

		private static final EngineMetadata ENGINE_METADATA = new EngineMetadata(
				"SCAFFOLD", ".sfd", "Scaffold", true, "scaffold", null,
				new String[]{TYPE},
				new String[]{},
				new String[]{ScaffoldDeploymentService.TYPE},
				60, true);

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final ScaffoldWorker worker = new ScaffoldWorker();
			worker.setScaffoldDir(new File(config.getScaffoldDir()).getAbsoluteFile());
			worker.setScaffoldJavaVmPath(config.getScaffoldJavaVmPath());
			worker.setMemoryLimit(config.getMemoryLimit());

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

		private String scaffoldDir;
		private String scaffoldJavaVmPath;
		private String memoryLimit;

		public Config() {
		}

		public Config(final String scaffoldDir, final String scaffoldJavaVmPath, final String memoryLimit) {
			this.scaffoldDir = scaffoldDir;
			this.scaffoldJavaVmPath = scaffoldJavaVmPath;
			this.memoryLimit = memoryLimit;
		}

		public String getMemoryLimit() {
			return memoryLimit;
		}

		public void setMemoryLimit(final String memoryLimit) {
			this.memoryLimit = memoryLimit;
		}

		public String getScaffoldDir() {
			return scaffoldDir;
		}

		public void setScaffoldDir(final String scaffoldDir) {
			this.scaffoldDir = scaffoldDir;
		}

		public String getScaffoldJavaVmPath() {
			return scaffoldJavaVmPath;
		}

		public void setScaffoldJavaVmPath(final String scaffoldJavaVmPath) {
			this.scaffoldJavaVmPath = scaffoldJavaVmPath;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(SCAFFOLD_DIR, getScaffoldDir());
			writer.put(SCAFFOLD_JAVA_VM_PATH, getScaffoldJavaVmPath());
			writer.put(MEMORY_LIMIT, getMemoryLimit());
		}

		@Override
		public void load(final ConfigReader reader) {
			setScaffoldDir(reader.get(SCAFFOLD_DIR));
			setScaffoldJavaVmPath(reader.get(SCAFFOLD_JAVA_VM_PATH));
			setMemoryLimit(reader.get(MEMORY_LIMIT));
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(SCAFFOLD_DIR, "Installation Folder", "Scaffold installation folder")
					.required()
					.existingDirectory()

					.property(SCAFFOLD_JAVA_VM_PATH, "Java VM Path", "<tt>java</tt> executable to run Scaffold with")
					.required()
					.executable(Arrays.asList("-version"))
					.defaultValue("java")

					.property(MEMORY_LIMIT, "Memory", "Memory requirement to execute Scaffold. Example, 256m is 256 megabytes")
					.required()
					.defaultValue("256m");
		}
	}

}
