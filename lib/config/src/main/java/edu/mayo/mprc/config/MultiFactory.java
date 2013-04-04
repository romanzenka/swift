package edu.mayo.mprc.config;

import java.util.Map;

/**
 * A table of factories. Acts as a super-factory.
 * <p/>
 * Each entry of the table has following fields:
 * <ul>
 * <li>id - used as XStream alias
 * <li>userName - name to show to the user
 * <li>configClass - Class name of the config class
 * <li>factory - A factory capable of taking the config and producing an instance of the item
 * <li>type - additional information about the character of the resource. The resources can be grouped by the type in methods such as {@link #getSupportedConfigClassNames()}
 * </ul>
 */
public interface MultiFactory extends ResourceFactory<ResourceConfig, Object> {

	Map<String/*id*/, Class<? extends ResourceConfig>> getConfigClasses();

	/**
	 * Obtain a factory that can create instances from given config class type.
	 *
	 * @param configClass Config class to get factory for.
	 * @return A factory that can create instances from given config class type.
	 */
	ResourceFactory getFactory(Class<? extends ResourceConfig> configClass);

	String getId(Class<? extends ResourceConfig> configClass);

	String getUserName(String type);

	/**
	 * Return user-friendly name for the object being created by the particular config class.
	 *
	 * @param config Configuration class.
	 * @return User-friendly name of the class.
	 */
	String getUserName(ResourceConfig config);

	String getUserName(Class<? extends ResourceConfig> clazz);

	Object getResourceType(String id);

	Class<? extends ResourceConfig> getConfigClass(String type);
}
