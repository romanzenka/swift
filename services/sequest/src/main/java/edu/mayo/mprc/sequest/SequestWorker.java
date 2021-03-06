package edu.mayo.mprc.sequest;

import com.google.common.base.Preconditions;
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
import edu.mayo.mprc.sequest.core.Mgf2SequestCaller;
import edu.mayo.mprc.sequest.core.PvmUtilities;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;


public final class SequestWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(SequestWorker.class);
	public static final String TYPE = "sequest";
	public static final String NAME = "Sequest";
	public static final String DESC = "Sequest search engine support. <p>Swift was tested against cluster version of Sequest on Linux, utilizing PVM.</p>";

	private File pvmHosts;
	private String sequestCommand = "sequest";

	private static final String PVM_HOSTS = "pvmHosts";
	private static final String SEQUEST_COMMAND = "sequestCommand";

	@Override
	public void process(final WorkPacket workPacket, final File tempWorkFolder, final UserProgressReporter progressReporter) {
		SequestWorkPacket sequestWorkPacket = null;
		if (workPacket instanceof SequestWorkPacket) {
			sequestWorkPacket = (SequestWorkPacket) workPacket;

			Preconditions.checkNotNull(sequestWorkPacket.getInputFile(), "Sequest search failed: The input file was not specified");
			Preconditions.checkNotNull(sequestWorkPacket.getDatabaseFile(), "Sequest search failed: The .hdr file was not specified");
			Preconditions.checkNotNull(sequestWorkPacket.getOutputFile(), "Sequest search failed: The output folder was not specified");
			Preconditions.checkNotNull(sequestWorkPacket.getSearchParams(), "Sequest search failed: The search parameters were not specified");

		} else {
			throw new DaemonException("Unexpected packet type " + workPacket.getClass().getName() + ", expected " + SequestWorkPacket.class.getName());
		}

		final File finalOutputFile = sequestWorkPacket.getOutputFile();
		final File outputFile = getTempOutputFile(tempWorkFolder, finalOutputFile);

		LOGGER.info("Starting sequest search"
				+ "\n\tinput file: " + sequestWorkPacket.getInputFile()
				+ "\n\thdr file: " + sequestWorkPacket.getDatabaseFile()
				+ "\n\toutput file: " + outputFile);

		final Mgf2SequestCaller m = new Mgf2SequestCaller();

		m.setHostsFile(pvmHosts);
		m.setSequestExe(sequestCommand);

		final File searchParamsFile = new File(tempWorkFolder, "sequest.params");
		FileUtilities.writeStringToFile(searchParamsFile, sequestWorkPacket.getSearchParams(), true);

		m.callSequest(
				outputFile,
				searchParamsFile,
				sequestWorkPacket.getInputFile(),
				120 * 1000/* start timeout */,
				10 * 60 * 1000 /* watchdog timeout */,
				sequestWorkPacket.getDatabaseFile(),
				progressReporter,
				new PvmUtilities()
		);

		publish(outputFile, finalOutputFile);

		LOGGER.info("Sequest search done");
	}

	public File getPvmHosts() {
		return pvmHosts;
	}

	public void setPvmHosts(final File pvmHosts) {
		this.pvmHosts = pvmHosts;
	}

	public String getSequestCommand() {
		return sequestCommand;
	}

	public void setSequestCommand(final String sequestCommand) {
		this.sequestCommand = sequestCommand;
	}

	@Override
	public String toString() {
		return "Sequest worker";
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("sequestWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> implements EngineFactory<Config, Worker> {

		private SequestMappingFactory sequestMappingFactory;

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final SequestWorker worker = new SequestWorker();
			worker.setPvmHosts(new File(config.get(PVM_HOSTS)).getAbsoluteFile());
			worker.setSequestCommand(config.get(SEQUEST_COMMAND));
			return worker;
		}

		@Override
		public EngineMetadata getEngineMetadata() {
			return new EngineMetadata(
					"SEQUEST", ".tar.gz", "Sequest", true, "sequest", sequestMappingFactory,
					new String[]{TYPE},
					new String[]{SequestCache.TYPE},
					new String[]{SequestDeploymentService.TYPE},
					20, false);
		}

		public SequestMappingFactory getSequestMappingFactory() {
			return sequestMappingFactory;
		}

		@Resource(name = "sequestMappingFactory")
		public void setSequestMappingFactory(final SequestMappingFactory sequestMappingFactory) {
			this.sequestMappingFactory = sequestMappingFactory;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config extends ResourceConfigBase {
		public Config() {
		}

		public Config(final String sequestCommand, final String pvmHosts) {
			put(SEQUEST_COMMAND, sequestCommand);
			put(PVM_HOSTS, pvmHosts);
		}
	}

	public static final class Ui implements ServiceUiFactory {
		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(SEQUEST_COMMAND, "Sequest Command", "Sequest command line executable.")
					.required()
					.executable(Arrays.asList("-v"))

					.property(PVM_HOSTS, "PVM Host List File", "Sequest pvm host list file. <a href=\"http://www.netlib.org/pvm3/book/node137.html\">See documentation</a>."
							+ "<p>Swift needs this file to know all the nodes where Sequest runs. "
							+ " Since Sequest has been extremely unstable for us, we are sometimes forced to connect to the nodes and clean up the Sequest daemons."
							+ " Make sure the sequest node can <tt>ssh</tt> to all the child nodes, otherwise the Swift cleanup will not work.</p>"
							+ "<p>This file must be formatted as follows:</p>" +
							"<pre>node1 &lt;options&gt;\n" +
							"node2 &lt;options&gt;\n" +
							"node3 &lt;options&gt;\n" +
							"...</pre>" +
							"<p>The first part of each entry is the name of a node, the options are ignored by Swift.</p>")
					.required()
					.existingFile();
		}
	}
}
