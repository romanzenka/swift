package edu.mayo.mprc.config;

/**
 * Provides access to the currently running application's configuration.
 *
 * @author Roman Zenka
 */
public interface RunningApplicationContext {
	/**
	 * @return Config for the entire currently running application.
	 */
	public ApplicationConfig getApplicationConfig();

	/**
	 * @return Config for the currently active daemon.
	 */
	public DaemonConfig getDaemonConfig();

	/**
	 * Create a resource from a given config. If the same config is passed in multiple times,
	 * same instance will be returned.
	 *
	 * @param resourceConfig Configuration of the resource to create.
	 * @return Singleton instance of the given resource.
	 */
	Object createResource(ResourceConfig resourceConfig);

	/**
	 * Return the only resource config that matches the given type. If none match, return null.
	 * The resources are searched first in the active
	 * daemon config, if not found there, entire application is considered. If more than one match, exception
	 * is thrown.
	 *
	 * @param clazz Type of the config.
	 * @return The configuration of the given type.
	 */
	ResourceConfig getSingletonConfig(Class clazz);
}
