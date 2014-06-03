package edu.mayo.mprc.comet;

import com.google.common.base.Charsets;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public final class CometWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(CometWorker.class);
	public static final String TYPE = "comet";
	public static final String NAME = "Comet";
	public static final String DESC = "Comet search engine support. <p>Comet is freely available at <a href=\"http://sourceforge.net/projects/comet-ms/\">http://sourceforge.net/projects/comet-ms/</a>.</p>";

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

		final List<String> parameters = new LinkedList<String>();
		parameters.add(cometExecutable.getPath());
		// parameters.add(paramFile.getAbsolutePath());

		final ProcessBuilder processBuilder = new ProcessBuilder(parameters);
		processBuilder.directory(tempWorkFolder);

		final ProcessCaller processCaller = new ProcessCaller(processBuilder);

		processCaller.run();

		publish(outputFile, finalOutputFile);

		LOGGER.info("Comet search, " + packet.toString() + ", has been successfully completed.");
	}

	@Override
	public String check() {
		LOGGER.info("Checking Comet worker");
		try {
			final List<String> parameters = new LinkedList<String>();
			parameters.add(cometExecutable.getPath());

			final ProcessBuilder processBuilder = new ProcessBuilder(parameters);
			final ProcessCaller processCaller = new ProcessCaller(processBuilder);
			processCaller.setKillTimeout(1000);
			final ByteArrayInputStream stream = new ByteArrayInputStream("\n".getBytes(Charsets.US_ASCII));
			processCaller.setInputStream(stream);
			processCaller.runAndCheck("Comet", 255);
		} catch (Exception e) {
			return e.getMessage();
		}
		return null;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("cometWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> implements EngineFactory<Config, Worker> {

		private static final EngineMetadata ENGINE_METADATA = new EngineMetadata(
				"COMET", ".xml", "Comet", true, "comet", new CometMappingFactory(),
				new String[]{TYPE},
				new String[]{CometCache.TYPE},
				new String[]{},
				21, false);


		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			CometWorker worker = null;
			try {
				worker = new CometWorker(FileUtilities.getAbsoluteFileForExecutables(new File(config.get(COMET_EXECUTABLE))));
			} catch (Exception e) {
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
	public static final class Config extends ResourceConfigBase {
		public Config() {
		}

		public Config(final String cometExecutable) {
			put(COMET_EXECUTABLE, cometExecutable);
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
