package edu.mayo.mprc.qstat;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerBase;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;

import java.util.HashMap;
import java.util.Map;


/**
 * Simple worker that accepts a string with grid engine id and sends back a String with results of the qstat call.
 */
public final class QstatDaemonWorker extends WorkerBase {
	public static final String TYPE = "qstat";
	public static final String NAME = "Qstat";
	public static final String DESC = "A trivial daemon running <tt>qstat</tt> command to retrieve status of a job running in Sun Grid Engine. This is used only in the web interface and is provided for convenience only. The module has to be enabled on a computer that is Sun Grid Engine submit host.";

	@Override
	public void process(final WorkPacket workPacket, final UserProgressReporter reporter) {
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

		reporter.reportProgress(new QstatOutput(caller.getOutputLog()));
	}

	public String toString() {
		return "qstat querying support";
	}

	/**
	 * A factory capable of creating the worker
	 */
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			return new QstatDaemonWorker();
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		public Config() {
		}

		@Override
		public Map<String, String> save(final DependencyResolver resolver) {
			return new HashMap<String, String>(1);
		}

		@Override
		public void load(final Map<String, String> values, final DependencyResolver resolver) {
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			// No UI needed
		}
	}
}
