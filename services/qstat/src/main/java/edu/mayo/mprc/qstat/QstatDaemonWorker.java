package edu.mayo.mprc.qstat;

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
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;


/**
 * Simple worker that accepts a string with grid engine id and sends back a String with results of the qstat call.
 */
public final class QstatDaemonWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(QstatDaemonWorker.class);

	public static final String TYPE = "qstat";
	public static final String NAME = "SGE Qstat";
	public static final String DESC = "A trivial daemon running <tt>qstat</tt> command to retrieve status of a job running in Sun Grid Engine. This is used only in the web interface and is provided for convenience only. The module has to be enabled on a computer that is Sun Grid Engine submit host.";

	@Override
	public void process(final WorkPacket workPacket, final File tempWorkFolder, final UserProgressReporter progressReporter) {
		if (!(workPacket instanceof QstatWorkPacket)) {
			throw new DaemonException("Unknown input format: " + workPacket.getClass().getName() + " expected string");
		}
		final QstatWorkPacket qstatWorkPacket = (QstatWorkPacket) workPacket;

		final int jobId = qstatWorkPacket.getJobId();
		final ProcessBuilder builder = new ProcessBuilder("qstat", "-j", String.valueOf(jobId));
		final ProcessCaller caller = new ProcessCaller(builder);
		try {
			caller.runAndCheck("qstat");
		} catch (Exception t) {
			throw new DaemonException(t);
		}

		progressReporter.reportProgress(new QstatOutput(caller.getOutputLog()));
	}

	public String toString() {
		return "qstat querying support";
	}

	@Override
	public String check() {
		LOGGER.info("Checking qstat");
		// Just run qstat to see it works
		try {
			final ProcessBuilder builder = new ProcessBuilder("qstat");
			final ProcessCaller caller = new ProcessCaller(builder);
			caller.runAndCheck("qstat");
		} catch (Exception e) {
			// SWALLOWED: We return a string
			return e.getMessage();
		}
		return null;
	}

	@Override
	public File createTempWorkFolder() {
		// No temp work folder needed
		return null;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("qstatDaemonWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			return new QstatDaemonWorker();
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
			// No UI needed
		}
	}
}
