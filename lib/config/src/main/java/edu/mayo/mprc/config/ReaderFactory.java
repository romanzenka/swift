package edu.mayo.mprc.config;

/**
 * The absolute minimal factory that is needed to successfully read
 * a Swift config file. No UI, no writing is supported here.
 *
 * @author Roman Zenka
 */
public interface ReaderFactory extends ResourceFactory<ResourceConfig, Object> {
	/**
	 * @param type Name of the type of resource.
	 * @return Configuration class for the type of resource.
	 */
	Class<? extends ResourceConfig> getConfigClass(String type);
}
