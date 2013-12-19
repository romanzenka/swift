package edu.mayo.mprc.daemon;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.FactoryDescriptor;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.monitor.PingDaemonWorker;
import edu.mayo.mprc.messaging.ResponseDispatcher;
import edu.mayo.mprc.messaging.ServiceFactory;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.exceptions.CompositeException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A daemon - collection of multiple runners that provide services.
 */
public final class Daemon implements Checkable, Installable {
	private static final Logger LOGGER = Logger.getLogger(Daemon.class);

	private String name;
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

	/**
	 * Holder of a connection that allows us to set the response dispatcher.
	 */
	private ServiceFactory serviceFactory;

	/**
	 * Daemon owns the central response dispatcher for messaging.
	 */
	private ResponseDispatcher responseDispatcher;

	public Daemon() {
	}

	public void install(final Map<String, String> params) {
		LOGGER.info("Installing daemon");
		for (final Object resource : getResources()) {
			if (resource instanceof Installable) {
				((Installable) resource).install(params);
			}
		}
		for (final AbstractRunner runner : getRunners()) {
			runner.install(params);
		}
	}

	/**
	 * Runs all the defined daemons runners.
	 */
	public void start() {
		for (final Object resource : resources) {
			if (resource instanceof Lifecycle) {
				((Lifecycle) resource).start();
			}
		}
		for (final AbstractRunner runner : getRunners()) {
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
		for (final Object resource : resources) {
			if (resource instanceof Lifecycle) {
				((Lifecycle) resource).stop();
			}
		}
		if (getResponseDispatcher() != null) {
			getResponseDispatcher().stop();
		}
	}

	/**
	 * Check all workers defined in this daemon
	 */
	@Override
	public String check() {
		LOGGER.info("Checking daemon");
		final CompositeException exception = new CompositeException();
		for (final Object resource : resources) {
			if (resource instanceof Checkable) {
				final String check = ((Checkable) resource).check();
				if (check != null) {
					exception.addCause(new Exception(check));
				}
			}
		}
		for (final AbstractRunner runner : runners) {
			final String check = runner.check();
			if (check != null) {
				exception.addCause(new Exception(check));
			}
		}
		if (!exception.getCauses().isEmpty()) {
			return exception.getMessage();
		}
		return null;
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

	public static ServiceConfig getPingServiceConfig(final DaemonConfig config) {
		final PingDaemonWorker.Config pingConfig = new PingDaemonWorker.Config();
		final ServiceConfig pingServiceConfig =
				new ServiceConfig(
						config.getName() + "-ping",
						new SimpleRunner.Config(pingConfig));
		return pingServiceConfig;

	}

	@Override
	public String toString() {
		return "Daemon [" + getName() + "] running following services: " +
				Joiner.on(",\n").join(runners);
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public List<AbstractRunner> getRunners() {
		return runners;
	}

	public void setRunners(final List<AbstractRunner> runners) {
		this.runners = runners;
	}

	public void setResources(final List<Object> resources) {
		this.resources = resources;
	}

	public File getSharedFileSpace() {
		return sharedFileSpace;
	}

	public void setSharedFileSpace(final File sharedFileSpace) {
		this.sharedFileSpace = sharedFileSpace;
	}

	public File getTempFolder() {
		return tempFolder;
	}

	public void setTempFolder(final File tempFolder) {
		this.tempFolder = tempFolder;
	}

	public boolean isDumpErrors() {
		return dumpErrors;
	}

	public void setDumpErrors(final boolean dumpErrors) {
		this.dumpErrors = dumpErrors;
	}

	public File getDumpFolder() {
		return dumpFolder;
	}

	public void setDumpFolder(final File dumpFolder) {
		this.dumpFolder = dumpFolder;
	}

	public File getLogOutputFolder() {
		return logOutputFolder;
	}

	public void setLogOutputFolder(final File logOutputFolder) {
		this.logOutputFolder = logOutputFolder;
	}

	public ServiceFactory getServiceFactory() {
		return serviceFactory;
	}

	public void setServiceFactory(final ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public void setResponseDispatcher(ResponseDispatcher responseDispatcher) {
		this.responseDispatcher = responseDispatcher;
	}

	public ResponseDispatcher getResponseDispatcher() {
		return responseDispatcher;
	}

	/**
	 * Runs a daemon from its config.
	 */
	@Component("daemonFactory")
	public static final class Factory extends FactoryBase<DaemonConfig, Daemon> implements FactoryDescriptor {
		/**
		 * We need a link to this factory because it needs to be initialized before we run.
		 */
		private DaemonConnectionFactory daemonConnectionFactory;

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
		public Daemon create(final DaemonConfig config, final DependencyResolver dependencies) {
			final Daemon daemon = new Daemon();
			daemon.setName(config.getName());

			ServiceFactory serviceFactory = getDaemonConnectionFactory().getServiceFactory();
			daemon.setServiceFactory(serviceFactory);
			daemon.setResponseDispatcher(new ResponseDispatcher(serviceFactory, daemon.getName()));

			// We need to set this up before we start to deserialize the daemon configuration
			getDaemonConnectionFactory().setResponseDispatcher(daemon.getResponseDispatcher());

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

			final ServiceConfig pingServiceConfig = getPingServiceConfig(config);
			if (pingServiceConfig != null) {
				final AbstractRunner pingRunner = createRunner(daemon, pingServiceConfig, dependencies);
				runners.add(pingRunner);
			}
			daemon.setRunners(runners);
			return daemon;
		}

		public DaemonConnectionFactory getDaemonConnectionFactory() {
			return daemonConnectionFactory;
		}

		@Resource(name = "daemonConnectionFactory")
		public void setDaemonConnectionFactory(final DaemonConnectionFactory daemonConnectionFactory) {
			this.daemonConnectionFactory = daemonConnectionFactory;
		}
	}
}
