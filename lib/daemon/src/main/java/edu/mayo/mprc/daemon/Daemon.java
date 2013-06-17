package edu.mayo.mprc.daemon;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.monitor.PingDaemonWorker;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.exceptions.CompositeException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A daemon - collection of multiple runners that provide services.
 */
public final class Daemon {
	private static final Logger LOGGER = Logger.getLogger(Daemon.class);

	private List<AbstractRunner> runners;
	private List<Object> resources;

	public Daemon(final List<AbstractRunner> runners, final List<Object> resources) {
		this.runners = runners;
		this.resources = resources;
	}

	/**
	 * Runs all the defined daemons runners.
	 */
	public void start() {
		for (final AbstractRunner runner : runners) {
			startRunner(runner);
		}
	}

	private static void startRunner(final AbstractRunner runner) {
		if (runner != null) {
			try {
				runner.start();
			} catch (Exception t) {
				throw new MprcException("The runner " + runner.toString() + " failed to start.", t);
			}
		}
	}

	/**
	 * Stops the daemon runners. Does not block until the runners terminate.
	 */
	public void stop() {
		for (final AbstractRunner runner : runners) {
			runner.stop();
		}
		for (Object resource : resources) {
			if (resource instanceof Closeable) {
				FileUtilities.closeQuietly((Closeable) resource);
			}
		}
	}

	/**
	 * Check all workers defined in this daemon
	 */
	public void check() {
		final CompositeException exception = new CompositeException();
		for (final AbstractRunner runner : runners) {
			try {
				runner.check();
			} catch (Exception e) {
				exception.addCause(e);
			}
		}
		if (!exception.getCauses().isEmpty()) {
			throw exception;
		}
	}

	/**
	 * Wait until the runners all terminate.
	 */
	public void awaitTermination() {
		for (final AbstractRunner runner : runners) {
			runner.awaitTermination();
		}
	}

	public int getNumRunners() {
		return runners.size();
	}

	public List<Object> getResources() {
		return resources;
	}

	@Override
	public String toString() {
		return "Daemon running following services: " +
				Joiner.on(",\n").join(runners);
	}

	/**
	 * Runs a daemon from its config.
	 */
	@Component("daemonFactory")
	public static final class Factory extends FactoryBase<DaemonConfig, Daemon> implements FactoryDescriptor {
		private void addResourcesToList(final List<Object> resources, final List<ResourceConfig> configs, final DependencyResolver dependencies) {
			Collections.sort(configs, new ResourceConfigComparator());
			for (final ResourceConfig resourceConfig : configs) {
				final Object resource = dependencies.createSingleton(resourceConfig);
				resources.add(resource);
			}
		}

		private void addRunnersToList(final List<AbstractRunner> runners, final List<ServiceConfig> services, final DependencyResolver dependencies) {
			for (final ServiceConfig serviceConfig : services) {
				if (serviceConfig == null) {
					LOGGER.error("Programmer error: service configuration was null - listing the configurations: ");
					for (final ServiceConfig config : services) {
						LOGGER.error(config);
					}
					throw new MprcException("Programmer error: service configuration was null.");
				}
				final AbstractRunner runner = createRunner(serviceConfig, dependencies);
				runners.add(runner);
			}
		}

		private AbstractRunner createRunner(final ServiceConfig serviceConfig, final DependencyResolver dependencies) {
			final DaemonConnection daemonConnection = (DaemonConnection) dependencies.createSingleton(serviceConfig);
			final RunnerConfig runnerConfig = serviceConfig.getRunner();
			final AbstractRunner runner = (AbstractRunner) dependencies.createSingleton(runnerConfig);
			runner.setDaemonConnection(daemonConnection);
			return runner;
		}

		@Override
		public String getType() {
			return "daemon";
		}

		@Override
		public String getUserName() {
			return "Daemon";
		}

		@Override
		public String getDescription() {
			return "A daemon is a collection of services and resources that runs on a particular machine";
		}

		@Override
		public Class<? extends ResourceConfig> getConfigClass() {
			return DaemonConfig.class;
		}

		@Override
		public ServiceUiFactory getServiceUiFactory() {
			return null;
		}

		@Override
		public Daemon create(DaemonConfig config, DependencyResolver dependencies) {
			// Create daemon resources
			final List<Object> resources = new ArrayList<Object>(config.getResources().size());
			addResourcesToList(resources, config.getResources(), dependencies);

			// Create runners
			final List<AbstractRunner> runners = new ArrayList<AbstractRunner>(config.getServices().size());
			addRunnersToList(runners, config.getServices(), dependencies);

			// Create extra runner for the ping service

			final ServiceConfig pingServiceConfig = PingDaemonWorker.getPingServiceConfig(config);
			if (pingServiceConfig != null) {
				final AbstractRunner pingRunner = createRunner(pingServiceConfig, dependencies);
				runners.add(pingRunner);
			}

			return new Daemon(runners, resources);
		}
	}
}
