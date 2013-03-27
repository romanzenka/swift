package edu.mayo.mprc.config;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.io.xml.DomDriver;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
@XStreamAlias("application")
public final class ApplicationConfig implements ResourceConfig {
	// List of daemons
	private List<DaemonConfig> daemons = new ArrayList<DaemonConfig>(1);

	public DaemonConfig getDaemonConfig(final String daemonId) {
		for (final DaemonConfig config : daemons) {
			if (daemonId.equals(config.getName())) {
				return config;
			}
		}
		throw new MprcException("No daemon of id " + daemonId + " is defined in " + toString());
	}

	public ApplicationConfig addDaemon(final DaemonConfig daemon) {
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

	private static XStream createXStream(final MultiFactory table) {
		final XStream xStream = new XStream(new DomDriver());
		xStream.setMode(XStream.ID_REFERENCES);

		xStream.processAnnotations(new Class[]{
				ApplicationConfig.class,
		});

		/** Add all the module aliases */
		for (final Map.Entry<String, Class<? extends ResourceConfig>> entry : table.getConfigClasses().entrySet()) {
			xStream.alias(entry.getKey(), entry.getValue());
		}

		return xStream;
	}

	public void save(final File configFile, final MultiFactory table) {
		BufferedWriter bufferedWriter = null;
		try {
			final XStream xStream = createXStream(table);
			bufferedWriter = new BufferedWriter(new FileWriter(configFile.getAbsoluteFile()));
			bufferedWriter.write(xStream.toXML(this));
		} catch (Exception t) {
			throw new MprcException("Could not save swift configuration to " + configFile.getAbsolutePath(), t);
		} finally {
			FileUtilities.closeQuietly(bufferedWriter);
		}
	}

	public static ApplicationConfig load(final File configFile, final MultiFactory table) {
		BufferedReader bufferedReader = null;
		try {
			final XStream xStream = createXStream(table);
			bufferedReader = new BufferedReader(new FileReader(configFile));
			final ApplicationConfig applicationConfig = (ApplicationConfig) xStream.fromXML(bufferedReader);
			// Restore link to the parent from all daemons
			for (final DaemonConfig daemonConfig : applicationConfig.getDaemons()) {
				daemonConfig.setApplicationConfig(applicationConfig);
			}
			return applicationConfig;
		} catch (Exception t) {
			throw new MprcException("Could not load swift configuration from " + configFile.getAbsolutePath(), t);
		} finally {
			FileUtilities.closeQuietly(bufferedReader);
		}
	}

	public List<ResourceConfig> getModulesOfConfigType(final Class<? extends ResourceConfig> type) {
		final List<ResourceConfig> list = new ArrayList<ResourceConfig>();
		for (final DaemonConfig daemonConfig : daemons) {
			for (final ResourceConfig resourceConfig : daemonConfig.getResources()) {
				if (type.equals(resourceConfig.getClass())) {
					list.add(resourceConfig);
				}
			}
			for (final ServiceConfig serviceConfig : daemonConfig.getServices()) {
				if (type.equals(serviceConfig.getRunner().getWorkerConfiguration().getClass())) {
					list.add(serviceConfig.getRunner().getWorkerConfiguration());
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
	public Map<String, String> save(final DependencyResolver resolver) {
		return new HashMap<String, String>(1);
	}

	@Override
	public void load(final Map<String, String> values, final DependencyResolver resolver) {
		// App has no properties
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
					daemonConfig.removeService(service);
					return;
				}
			}
		}
	}
}
