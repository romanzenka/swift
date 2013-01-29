package edu.mayo.mprc.daemon;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.monitor.PingDaemonWorker;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	public static final class Factory {
		/* Factory for the particular daemon components */
		private MultiFactory factory;
		private DependencyResolver dependencies;

		public Daemon createDaemon(final DaemonConfig config) {
			// Create daemon resources
			final List<Object> resources = new ArrayList<Object>(config.getResources().size());
			addResourcesToList(resources, config.getResources(), getDependencies());

			// Create runners
			final List<AbstractRunner> runners = new ArrayList<AbstractRunner>(config.getServices().size());
			addRunnersToList(runners, config.getServices(), getDependencies());

			// Create extra runner for the ping service

			final ServiceConfig pingServiceConfig = PingDaemonWorker.getPingServiceConfig(config);
			if (pingServiceConfig != null) {
				final AbstractRunner pingRunner = createRunner(pingServiceConfig, getDependencies());
				runners.add(pingRunner);
			}

			return new Daemon(runners, resources);
		}

		private void addResourcesToList(final List<Object> resources, final List<ResourceConfig> configs, final DependencyResolver dependencies) {
			Collections.sort(configs, new ResourceComparator());
			for (final ResourceConfig resourceConfig : configs) {
				final ResourceFactory resourceFactory = this.factory.getFactory(resourceConfig.getClass());
				final Object resource = resourceFactory.createSingleton(resourceConfig, dependencies);
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
			final DaemonConnection daemonConnection = (DaemonConnection) factory.createSingleton(serviceConfig, dependencies);
			final RunnerConfig runnerConfig = serviceConfig.getRunner();
			final AbstractRunner runner = (AbstractRunner) factory.createSingleton(runnerConfig, dependencies);
			runner.setDaemonConnection(daemonConnection);
			return runner;
		}

		public MultiFactory getMultiFactory() {
			return factory;
		}

		public void setMultiFactory(final MultiFactory factory) {
			this.factory = factory;
		}

		public DependencyResolver getDependencies() {
			return dependencies;
		}

		public void setDependencies(DependencyResolver dependencies) {
			this.dependencies = dependencies;
		}
	}

	private static final class ResourceComparator implements Comparator<ResourceConfig>, Serializable {
		private static final long serialVersionUID = 20101123L;

		public int compare(final ResourceConfig o1, final ResourceConfig o2) {
			return Integer.valueOf(o2.getPriority()).compareTo(o1.getPriority());
		}
	}
}
