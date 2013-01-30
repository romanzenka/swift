package edu.mayo.mprc.daemon.monitor;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ServiceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.*;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.util.HashMap;
import java.util.Map;

/**
 * Responds to ping requests.
 *
 * @author Roman Zenka
 */
public final class PingDaemonWorker implements Worker, NoLoggingWorker {
	public static final String TYPE = "ping";
	public static final String NAME = "Ping";
	public static final String DESC = "A daemon that responds to pings with status information.";

	@Override
	public void processRequest(final WorkPacket workPacket, final ProgressReporter progressReporter) {
		try {
			progressReporter.reportStart();
			process(workPacket, progressReporter);
			workPacket.synchronizeFileTokensOnReceiver();
			progressReporter.reportSuccess();
		} catch (Exception t) {
			progressReporter.reportFailure(t);
		}
	}

	private void process(final WorkPacket workPacket, final ProgressReporter reporter) {
		if (!(workPacket instanceof PingWorkPacket)) {
			throw new DaemonException("Unknown input format: " + workPacket.getClass().getName() + " expected string");
		}
		final PingWorkPacket pingWorkPacket = (PingWorkPacket) workPacket;

		reporter.reportProgress(new PingResponse());
	}

	public static ServiceConfig getPingServiceConfig(DaemonConfig config) {
		if(config.getPingQueueUrl()==null) {
			return null;
		}
		final PingDaemonWorker.Config pingConfig = new PingDaemonWorker.Config();
		final ServiceConfig pingServiceConfig =
				new ServiceConfig(
						config.getName() + "-ping",
						new SimpleRunner.Config(pingConfig),
						config.getPingQueueUrl());
		return pingServiceConfig;

	}

	public String toString() {
		return "ping monitoring support";
	}

	/**
	 * A factory capable of creating the worker
	 */
	public static final class Factory extends WorkerFactoryBase<Config> {

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			return new PingDaemonWorker();
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
