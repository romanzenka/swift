package edu.mayo.mprc.config;

import java.util.List;

/**
 * Provides methods for deserializing a {@link ResourceConfig} instance.
 *
 * @author Roman Zenka
 */
public interface ConfigReader {
	String get(String key);

	String get(String key, String defaultValue);

	boolean getBoolean(String key);

	int getInteger(String key);

	int getInteger(String key, int defaultValue);

	ResourceConfig getObject(String key);

	ResourceConfig getObjectFromId(String id);

	/**
	 * Returns a list of the referenced configs given a particular key.
	 */
	List<? extends ResourceConfig> getResourceList(String key);

	/**
	 * @return List of all keys associated with the current item.
	 */
	Iterable<String> getKeys();
}
