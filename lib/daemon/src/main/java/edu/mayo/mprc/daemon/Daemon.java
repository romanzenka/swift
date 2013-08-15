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
import java.io.File;
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
	private File sharedFileSpace;
	private File tempFolder;

	/**
	 * When enabled, the daemon would dump a file on every error. This dump contains
	 * the work packet + information about the environment and machine where the error occurred +
	 */
	private boolean dumpErrors;

	/**
	 * Where should the daemon dump files when an error occurs. If not set, the tempFolderPath is used.
	 */
	private File dumpFolder;

	/**
	 * Default folder where to put the logs.
	 */
	private File logOutputFolder;

	public Daemon() {
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

	public List<AbstractRunner> getRunners() {
		return runners;
	}

	public void setRunners(List<AbstractRunner> runners) {
		this.runners = runners;
	}

	public void setResources(List<Object> resources) {
		this.resources = resources;
	}

	public File getSharedFileSpace() {
		return sharedFileSpace;
	}

	public void setSharedFileSpace(File sharedFileSpace) {
		this.sharedFileSpace = sharedFileSpace;
	}

	public File getTempFolder() {
		return tempFolder;
	}

	public void setTempFolder(File tempFolder) {
		this.tempFolder = tempFolder;
	}

	public boolean isDumpErrors() {
		return dumpErrors;
	}

	public void setDumpErrors(boolean dumpErrors) {
		this.dumpErrors = dumpErrors;
	}

	public File getDumpFolder() {
		return dumpFolder;
	}

	public void setDumpFolder(File dumpFolder) {
		this.dumpFolder = dumpFolder;
	}

	public File getLogOutputFolder() {
		return logOutputFolder;
	}

	public void setLogOutputFolder(File logOutputFolder) {
		this.logOutputFolder = logOutputFolder;
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

		private void addRunnersToList(final Daemon daemon, final List<AbstractRunner> runners, final List<ServiceConfig> services, final DependencyResolver dependencies) {
			for (final ServiceConfig serviceConfig : services) {
				if (serviceConfig == null) {
					LOGGER.error("Programmer error: service configuration was null - listing the configurations: ");
					for (final ServiceConfig config : services) {
						LOGGER.error(config);
					}
					throw new MprcException("Programmer error: service configuration was null.");
				}
				final AbstractRunner runner = createRunner(daemon, serviceConfig, dependencies);
				runners.add(runner);
			}
		}

		private AbstractRunner createRunner(final Daemon daemon, final ServiceConfig serviceConfig, final DependencyResolver dependencies) {
			final DaemonConnection daemonConnection = (DaemonConnection) dependencies.createSingleton(serviceConfig);
			final RunnerConfig runnerConfig = serviceConfig.getRunner();
			final AbstractRunner runner = (AbstractRunner) dependencies.createSingleton(runnerConfig);
			runner.setDaemonConnection(daemonConnection);
			runner.setDaemon(daemon);
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
			Daemon daemon = new Daemon();
			daemon.setDumpErrors(config.isDumpErrors());
			daemon.setDumpFolder(config.getDumpFolderPath() == null ? null : new File(config.getDumpFolderPath()));
			if (config.getLogOutputFolder() == null) {
				daemon.setLogOutputFolder(new File(DaemonConfig.DEFAULT_LOG_FOLDER));
			} else {
				daemon.setLogOutputFolder(new File(config.getLogOutputFolder()));
			}
			daemon.setSharedFileSpace(config.getSharedFileSpacePath() == null ? null : new File(config.getSharedFileSpacePath()));
			daemon.setTempFolder(config.getTempFolderPath() == null ? FileUtilities.getDefaultTempDirectory() : new File(config.getTempFolderPath()));
			// Create daemon resources
			final List<Object> resources = new ArrayList<Object>(config.getResources().size());
			addResourcesToList(resources, config.getResources(), dependencies);
			daemon.setResources(resources);

			// Create runners
			final List<AbstractRunner> runners = new ArrayList<AbstractRunner>(config.getServices().size());
			addRunnersToList(daemon, runners, config.getServices(), dependencies);

			// Create extra runner for the ping service

			final ServiceConfig pingServiceConfig = PingDaemonWorker.getPingServiceConfig(config);
			if (pingServiceConfig != null) {
				final AbstractRunner pingRunner = createRunner(daemon, pingServiceConfig, dependencies);
				runners.add(pingRunner);
			}
			daemon.setRunners(runners);
			return daemon;
		}
	}
}
