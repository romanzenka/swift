package edu.mayo.mprc.config;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Application configuration file contains information about every single daemon in the entire distributed application.
 * The structure is as follows:
 * <pre>
 *     {@link ApplicationConfig} - entire application, running on multiple computers
 *     * {@link DaemonConfig} - instance running on a single computer. Knows OS, basic machine setup
 *       * {@link ServiceConfig} - you can send work packets to services. Service defines the end-point for receiving the work.
 *       |  * {@link RunnerConfig} - defines how to run the actual algorithm (multiple threads? grid?)
 *       |    * {@link ResourceConfig} - used to create an instance of a daemon Worker.
 *       * {@link ResourceConfig} - just a dumb resource, being available, not doing actual work
 * </pre>
 */
public final class ApplicationConfig implements ResourceConfig {
	public static final String DAEMONS = "daemons";
	// List of daemons
	private List<DaemonConfig> daemons = new ArrayList<DaemonConfig>(1);
	private DependencyResolver dependencyResolver;

	public ApplicationConfig() {
	}

	public ApplicationConfig(DependencyResolver dependencyResolver) {
		this.dependencyResolver = dependencyResolver;
	}

	public DaemonConfig getDaemonConfig(final String daemonId) {
		for (final DaemonConfig config : daemons) {
			if (daemonId.equals(config.getName())) {
				return config;
			}
		}
		throw new MprcException("No daemon of id " + daemonId + " is defined in " + toString());
	}

	public ApplicationConfig addDaemon(final DaemonConfig daemon) {
		for (DaemonConfig config : daemons) {
			if (config.equals(daemon)) {
				throw new MprcException("You cannot add the same daemon configuration twice to one application");
			}
		}
		daemons.add(daemon);
		daemon.setApplicationConfig(this);
		return this;
	}

	public void removeDaemon(final DaemonConfig daemon) {
		daemons.remove(daemon);
		daemon.setApplicationConfig(null);
	}

	public List<DaemonConfig> getDaemons() {
		return daemons;
	}

	/**
	 * Save the whole application config into a text file that is easy to edit by hand.
	 *
	 * @param configFile File to save to
	 * @param table      Table of all the recognized elements
	 */
	public void save(final File configFile, final MultiFactory table) {
		AppConfigWriter writer = null;
		try {
			writer = new AppConfigWriter(configFile, table);
			writer.save(this);
		} catch (Exception e) {
			throw new MprcException("Cannot write config file into " + configFile.getAbsolutePath(), e);
		} finally {
			FileUtilities.closeQuietly(writer);
		}
	}

	public static void load(final ApplicationConfig config, final File configFile, final ReaderFactory readerFactory) {
		AppConfigReader reader = null;
		try {
			reader = new AppConfigReader(configFile, readerFactory);

			reader.load(config);
		} catch (Exception e) {
			throw new MprcException("Cannot read config file from " + configFile.getAbsolutePath(), e);
		} finally {
			FileUtilities.closeQuietly(reader);
		}
	}

	public <T extends ResourceConfig> List<T> getModulesOfConfigType(final Class<T> type) {
		final List<T> list = new ArrayList<T>();
		for (final DaemonConfig daemonConfig : daemons) {
			for (final ResourceConfig resourceConfig : daemonConfig.getResources()) {
				if (type.equals(resourceConfig.getClass())) {
					list.add((T) resourceConfig);
				}
			}
			for (final ServiceConfig serviceConfig : daemonConfig.getServices()) {
				if (type.equals(serviceConfig.getRunner().getWorkerConfiguration().getClass())) {
					list.add((T) serviceConfig.getRunner().getWorkerConfiguration());
				}
			}
		}
		return list;
	}

	public DaemonConfig getDaemonForResource(final ResourceConfig resource) {
		for (final DaemonConfig daemonConfig : daemons) {
			for (final ResourceConfig resourceConfig : daemonConfig.getResources()) {
				if (resource.equals(resourceConfig)) {
					return daemonConfig;
				}
			}
			for (final ServiceConfig serviceConfig : daemonConfig.getServices()) {
				if (resource.equals(serviceConfig.getRunner().getWorkerConfiguration())) {
					return daemonConfig;
				}
			}
		}
		return null;
	}

	@Override
	public void save(final ConfigWriter writer) {
		writer.put(DAEMONS, getDaemons());
	}

	@Override
	public void load(final ConfigReader reader) {
		daemons = (List<DaemonConfig>) reader.getResourceList("daemons");
	}

	@Override
	public int getPriority() {
		return 0;
	}

	/**
	 * Remove a resource no matter where or what it is.
	 *
	 * @param resourceConfig Resource or Daemon to be removed
	 */
	public void remove(final ResourceConfig resourceConfig) {
		if (resourceConfig instanceof DaemonConfig) {
			removeDaemon((DaemonConfig) resourceConfig);
			return;
		}

		for (final DaemonConfig daemonConfig : getDaemons()) {
			for (final ResourceConfig resource : daemonConfig.getResources()) {
				if (resource.equals(resourceConfig)) {
					daemonConfig.removeResource(resource);
					return;
				}
			}

			for (final ServiceConfig service : daemonConfig.getServices()) {
				if (service.getRunner().getWorkerConfiguration().equals(resourceConfig)) {
					daemonConfig.removeResource(service);
					return;
				}
			}
		}
	}

	public void setDependencyResolver(DependencyResolver dependencyResolver) {
		this.dependencyResolver = dependencyResolver;
	}

	public DependencyResolver getDependencyResolver() {
		return dependencyResolver;
	}

	public void clear() {
		daemons.clear();
	}
}
