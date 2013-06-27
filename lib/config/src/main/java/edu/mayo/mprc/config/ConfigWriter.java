package edu.mayo.mprc.config;

import java.util.Collection;

/**
 * Provides methods for an instance of {@link ResourceConfig} to serialize itself.
 *
 * @author Roman Zenka
 */
public interface ConfigWriter {
	void put(String key, String value);

	void put(String key, String value, String comment);

	void put(String key, String value, String defaultValue, String comment);

	void put(String key, boolean value);

	void put(String key, boolean value, String comment);

	void put(String key, int value, String comment);

	void put(String key, int value, int defaultValue, String comment);

	void put(String key, ResourceConfig config);

	void put(String key, Collection<? extends ResourceConfig> configs);

	void comment(String comment);

	/**
	 * Save another resource config. If the config was already saved, do nothing and return its previous id.
	 *
	 * @param resourceConfig Config to save.
	 * @return Unique identifier of the config (within this writer).
	 */
	String save(ResourceConfig resourceConfig);

	/**
	 * @param configs A collection of config objects.
	 * @return A comma-separated list of all the referenced objects.
	 */
	String save(Collection<? extends ResourceConfig> configs);
}
