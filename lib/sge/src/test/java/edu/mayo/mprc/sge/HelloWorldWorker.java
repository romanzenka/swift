package edu.mayo.mprc.sge;

import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ui.ResourceConfigBase;
import edu.mayo.mprc.daemon.worker.*;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class HelloWorldWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(HelloWorldWorker.class);

	@Override
	protected void process(WorkPacket workPacket, File tempWorkFolder, UserProgressReporter progressReporter) {
		LOGGER.debug("Hello World");
	}

	public static class Config extends ResourceConfigBase {
		public Config() {
		}
	}

	public static final class Factory extends WorkerFactoryBase<Config> implements WorkerFactory<Config, Worker> {

		@Override
		public Worker create(Config config, DependencyResolver dependencies) {
			return new HelloWorldWorker();
		}
	}
}
