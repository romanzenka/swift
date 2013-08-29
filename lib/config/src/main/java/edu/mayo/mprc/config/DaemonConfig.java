package edu.mayo.mprc.config;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;

import java.util.*;

/**
 * A daemon is simply a running java VM which exposes one or more services.
 */
public final class DaemonConfig implements ResourceConfig, NamedResource {
	public static final String WINE_CMD = "wine";
	public static final String WINECONSOLE_CMD = "wineconsole";
	public static final String XVFB_CMD = "bin/util/unixXvfbWrapper.sh";

	public static final String NAME = "name";
	public static final String HOST_NAME = "hostName";
	public static final String OS_NAME = "osName";
	public static final String OS_ARCH = "osArch";
	public static final String SHARED_FILE_SPACE_PATH = "sharedFileSpacePath";
	public static final String TEMP_FOLDER_PATH = "tempFolderPath";
	public static final String DUMP_ERRORS = "dumpErrors";
	public static final String DUMP_FOLDER_PATH = "dumpFolderPath";
	public static final String SERVICES = "services";
	public static final String RESOURCES = "resources";
	public static final String TYPE = "daemon";
	public static final String DEFAULT_LOG_FOLDER = "var/log";

	private String name;

	private String hostName;

	private String osName;

	private String osArch;

	private String sharedFileSpacePath;

	private String tempFolderPath;

	/**
	 * When enabled, the daemon would dump a file on every error. This dump contains
	 * the work packet + information about the environment and machine where the error occurred +
	 */
	private boolean dumpErrors;

	/**
	 * Where should the daemon dump files when an error occurs. If not set, the tempFolderPath is used.
	 */
	private String dumpFolderPath;

	/**
	 * Default folder where to put the logs.
	 */
	private String logOutputFolder = DEFAULT_LOG_FOLDER;

	// Services this daemon provides
	private List<ServiceConfig> services = new ArrayList<ServiceConfig>();

	// Resources this daemon defines locally
	private List<ResourceConfig> resources = new ArrayList<ResourceConfig>();

	/**
	 * This is not being serialized - recreated on the fly when {@link ApplicationConfig} is loaded.
	 */
	private ApplicationConfig applicationConfig;

	public DaemonConfig() {
	}

	/**
	 * Create daemon config with given index.
	 *
	 * @param name  Name of the daemon.
	 * @param local If true, the daemon is expected to run on the local computer.
	 * @return Default daemon setup.
	 */
	public static DaemonConfig getDefaultDaemonConfig(final String name, final boolean local) {
		final DaemonConfig daemon = new DaemonConfig();
		daemon.setName(name);
		daemon.setOsName(System.getProperty("os.name"));
		daemon.setOsArch(System.getProperty("os.arch"));
		daemon.setTempFolderPath("var/tmp");
		daemon.setDumpErrors(false);
		daemon.setDumpFolderPath("var/tmp/dump");
		daemon.setSharedFileSpacePath("/");

		if (local) {
			// Host name set by default to this computer
			String hostName = FileUtilities.getHostname();
			daemon.setHostName(hostName);
		}

		return daemon;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(final String name) {
		this.name = name;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(final String hostName) {
		this.hostName = hostName;
	}

	public String getOsName() {
		return osName;
	}

	public void setOsName(final String osName) {
		this.osName = osName;
	}

	public String getOsArch() {
		return osArch;
	}

	public void setOsArch(final String osArch) {
		this.osArch = osArch;
	}

	public String getSharedFileSpacePath() {
		return sharedFileSpacePath;
	}

	public void setSharedFileSpacePath(final String sharedFileSpacePath) {
		this.sharedFileSpacePath = sharedFileSpacePath;
	}

	public String getTempFolderPath() {
		return tempFolderPath;
	}

	public void setTempFolderPath(final String tempFolderPath) {
		this.tempFolderPath = tempFolderPath;
	}

	public boolean isDumpErrors() {
		return dumpErrors;
	}

	public void setDumpErrors(final boolean dumpErrors) {
		this.dumpErrors = dumpErrors;
	}

	public String getDumpFolderPath() {
		return dumpFolderPath;
	}

	public void setDumpFolderPath(final String dumpFolderPath) {
		this.dumpFolderPath = dumpFolderPath;
	}

	public String getLogOutputFolder() {
		return logOutputFolder;
	}

	public void setLogOutputFolder(String logOutputFolder) {
		this.logOutputFolder = logOutputFolder;
	}

	public List<ServiceConfig> getServices() {
		return services;
	}

	public List<ResourceConfig> getResources() {
		return resources;
	}

	public DaemonConfig addResource(final ResourceConfig resource) {
		if (resource instanceof ServiceConfig) {
			if (services.contains(resource)) {
				throw new MprcException("Daemon " + getName() + " already contains service " + ((ServiceConfig) resource).getName());
			}
			services.add((ServiceConfig) resource);
		} else {
			if (resources.contains(resource)) {
				throw new MprcException("Daemon " + getName() + " already contains resource " + resource.toString());
			}
			resources.add(resource);
		}
		return this;
	}

	public boolean removeResource(final ResourceConfig resource) {
		if (resource instanceof ServiceConfig) {
			return services.remove(resource);
		}
		return resources.remove(resource);
	}

	public ApplicationConfig getApplicationConfig() {
		return applicationConfig;
	}

	public void setApplicationConfig(final ApplicationConfig applicationConfig) {
		this.applicationConfig = applicationConfig;
	}

	public boolean isWindows() {
		return isOs("windows");
	}

	public boolean isLinux() {
		return isOs("linux");
	}

	public boolean isMac() {
		return isOs("mac");
	}

	/**
	 * @return A wrapper script that executes a windows command on linux. On windows it is not necessary - return empty string.
	 */
	public String getWrapperScript() {
		if (isWindows()) {
			return "";
		} else {
			return WINECONSOLE_CMD;
		}
	}

	/**
	 * @return A wrapper script that executes a windows command that needs a graphical console on linux (using Xvfb) -
	 *         virtual frame buffer. On windows not necessary - return empty string.
	 */
	public String getXvfbWrapperScript() {
		if (isWindows()) {
			return "";
		} else {
			return XVFB_CMD;
		}
	}

	private boolean isOs(final String osString) {
		final String osName = getOsName() == null ? "" : getOsName().toLowerCase(Locale.ENGLISH);
		return osName.contains(osString);
	}

	@Override
	public void save(final ConfigWriter writer) {
		writer.put(NAME, getName(), "User-friendly name of this daemon");
		writer.put(HOST_NAME, getHostName(), "Host the daemon runs on");
		writer.put(OS_NAME, getOsName(), "Host system operating system name: e.g. Windows or Linux.");
		writer.put(OS_ARCH, getOsArch(), "Host system architecture: x86, x86_64");
		writer.put(SHARED_FILE_SPACE_PATH, getSharedFileSpacePath(), "Directory on a shared file system can be accessed from all the daemons");
		writer.put(TEMP_FOLDER_PATH, getTempFolderPath(), "Temporary folder that can be used for caching. Transferred files from other daemons with no shared file system with this daemon are cached to this folder.");
		writer.put(DUMP_ERRORS, isDumpErrors(), "Not implemented yet");
		writer.put(DUMP_FOLDER_PATH, getDumpFolderPath(), "Not implemented yet");
		writer.put(RunnerConfig.LOG_OUTPUT_FOLDER, getLogOutputFolder(), "Shared log folder to be used as a default for all services");

		// It is important to save the resources before the services.
		// There are unstated dependencies between the resources and the services
		// .. e.g. a message broker has to exist before a service can be defined
		writer.put(RESOURCES, getResourceList(writer, getResources()), "Comma separated list of provided resources");
		writer.put(SERVICES, getResourceList(writer, getServices()), "Comma separated list of provided services");
	}

	private String getResourceList(final ConfigWriter writer, final Collection<? extends ResourceConfig> resources) {
		final StringBuilder result = new StringBuilder(resources.size() * 10);
		ArrayList<ResourceConfig> sorted = new ArrayList<ResourceConfig>(resources.size());
		for (final ResourceConfig config : resources) {
			sorted.add(config);
		}
		Collections.sort(sorted, new ResourceConfigComparator());

		for (final ResourceConfig config : sorted) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append(writer.save(config));
		}
		return result.toString();
	}

	@Override
	public void load(final ConfigReader reader) {
		name = reader.get(NAME);
		hostName = reader.get(HOST_NAME);
		osName = reader.get(OS_NAME);
		osArch = reader.get(OS_ARCH);
		sharedFileSpacePath = reader.get(SHARED_FILE_SPACE_PATH);
		tempFolderPath = reader.get(TEMP_FOLDER_PATH);
		dumpErrors = reader.getBoolean(DUMP_ERRORS);
		dumpFolderPath = reader.get(DUMP_FOLDER_PATH);
		logOutputFolder = reader.get(RunnerConfig.LOG_OUTPUT_FOLDER);

		{
			final List<? extends ResourceConfig> resourcesList = reader.getResourceList(RESOURCES);
			resources.clear();
			for (final ResourceConfig resource : resourcesList) {
				resources.add(resource);
			}
		}

		{
			final List<? extends ResourceConfig> servicesList = reader.getResourceList(SERVICES);
			services.clear();
			for (final ResourceConfig config : servicesList) {
				final ServiceConfig service = (ServiceConfig) config;
				services.add(service);
			}
		}
	}

	public DaemonConfigInfo createDaemonConfigInfo() {
		return new DaemonConfigInfo(name, sharedFileSpacePath);
	}

	@Override
	public int getPriority() {
		return 0;
	}

	public ResourceConfig firstResourceOfType(final Class<?> clazz) {
		for (final ResourceConfig resourceConfig : resources) {
			if (clazz.isAssignableFrom(resourceConfig.getClass())) {
				return resourceConfig;
			}
		}
		return null;
	}

	public ResourceConfig firstServiceOfType(final Class<?> clazz) {
		for (final ServiceConfig serviceConfig : services) {
			if (clazz.isAssignableFrom(serviceConfig.getRunner().getWorkerConfiguration().getClass())) {
				return serviceConfig;
			}
		}
		return null;
	}

}
