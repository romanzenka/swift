package edu.mayo.mprc.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * A daemon is simply a running java VM which exposes one or more services.
 */
@XStreamAlias("daemon")
public final class DaemonConfig implements ResourceConfig {
	public static final String WINE_CMD = "wine";
	public static final String WINECONSOLE_CMD = "wineconsole";
	public static final String XVFB_CMD = "bin/util/unixXvfbWrapper.sh";

	public static final String NAME = "name";
	public static final String HOST_NAME = "hostName";
	public static final String OS_NAME = "osName";
	public static final String OS_ARCH = "osArch";
	public static final String SHARED_FILE_SPACE_PATH = "sharedFileSpacePath";
	public static final String TEMP_FOLDER_PATH = "tempFolderPath";
	private static final String PING_QUEUE_URL = "pingQueueUrl";

	@XStreamAlias(NAME)
	@XStreamAsAttribute
	private String name;

	@XStreamAlias(HOST_NAME)
	private String hostName;

	@XStreamAlias(OS_NAME)
	private String osName;

	@XStreamAlias(OS_ARCH)
	private String osArch;

	@XStreamAlias(SHARED_FILE_SPACE_PATH)
	private String sharedFileSpacePath;

	@XStreamAlias(TEMP_FOLDER_PATH)
	private String tempFolderPath;

	@XStreamAlias(PING_QUEUE_URL)
	private String pingQueueUrl;
	// Services this daemon provides
	@XStreamAlias("services")
	private List<ServiceConfig> services = new ArrayList<ServiceConfig>();

	// Resources this daemon defines locally
	@XStreamAlias("resources")
	private List<ResourceConfig> resources = new ArrayList<ResourceConfig>();

	/**
	 * This is not being serialized - recreated on the fly when {@link ApplicationConfig} is loaded.
	 */
	@XStreamOmitField()
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
		daemon.setSharedFileSpacePath("/");

		if (local) {
			// Host name set by default to this computer
			final InetAddress localHost;
			try {
				localHost = InetAddress.getLocalHost();
				final String hostName = localHost.getHostName();
				daemon.setHostName(hostName);
			} catch (UnknownHostException ignore) {
				daemon.setHostName("localhost");
			}
		}

		return daemon;
	}

	public String getName() {
		return name;
	}

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

	public List<ServiceConfig> getServices() {
		return services;
	}

	public DaemonConfig addService(final ServiceConfig service) {
		services.add(service);
		return this;
	}

	public boolean removeService(final ServiceConfig service) {
		return services.remove(service);
	}

	public List<ResourceConfig> getResources() {
		return resources;
	}

	public DaemonConfig addResource(final ResourceConfig resource) {
		resources.add(resource);
		return this;
	}

	public boolean removeResource(final ResourceConfig resource) {
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

	public Map<String, String> save(final DependencyResolver resolver) {
		final Map<String, String> map = new HashMap<String, String>();
		map.put(NAME, name);
		map.put(HOST_NAME, hostName);
		map.put(OS_NAME, osName);
		map.put(OS_ARCH, osArch);
		map.put(SHARED_FILE_SPACE_PATH, sharedFileSpacePath);
		map.put(TEMP_FOLDER_PATH, tempFolderPath);
		return map;
	}

	public void load(final Map<String, String> values, final DependencyResolver resolver) {
		name = values.get(NAME);
		hostName = values.get(HOST_NAME);
		osName = values.get(OS_NAME);
		osArch = values.get(OS_ARCH);
		sharedFileSpacePath = values.get(SHARED_FILE_SPACE_PATH);
		tempFolderPath = values.get(TEMP_FOLDER_PATH);
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

	public void setPingQueueUrl(String pingQueueUrl) {
		this.pingQueueUrl = pingQueueUrl;
	}

	public String getPingQueueUrl() {
		return pingQueueUrl;
	}
}
