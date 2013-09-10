package edu.mayo.mprc.daemon.monitor;

import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.*;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.springframework.stereotype.Component;

/**
 * Responds to ping requests.
 *
 * @author Roman Zenka
 */
public final class PingDaemonWorker extends WorkerBase implements NoLoggingWorker {
	public static final String TYPE = "ping";
	public static final String NAME = "Ping Responder";
	public static final String DESC = "Responds to pings with status information. Automatically set for each daemon.";

	@Override
	public void process(final WorkPacket workPacket, final UserProgressReporter reporter) {
		if (!(workPacket instanceof PingWorkPacket)) {
			throw new DaemonException("Unknown input format: " + workPacket.getClass().getName() + " expected string");
		}
		reporter.reportProgress(new PingResponse());
	}

	public static ServiceConfig getPingServiceConfig(final DaemonConfig config) {
		final PingDaemonWorker.Config pingConfig = new PingDaemonWorker.Config();
		final ServiceConfig pingServiceConfig =
				new ServiceConfig(
						config.getName() + "-ping",
						new SimpleRunner.Config(pingConfig));
		return pingServiceConfig;

	}

	public String toString() {
		return "ping monitoring support";
	}

	@Override
	public void check() {
		// No check needed
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("pingDaemonWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			return new PingDaemonWorker();
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
