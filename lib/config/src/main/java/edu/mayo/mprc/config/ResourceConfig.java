package edu.mayo.mprc.config;

import java.util.Map;

/**
 * A resource marker interface.
 */
public interface ResourceConfig {
	Map<String, String> save(DependencyResolver resolver);

	void load(Map<String, String> values, DependencyResolver resolver);

	/**
	 * @return Int value that indicates the priority which this resource
	 *         must be initialized. Lower values indicate lower priority. If resource A
	 *         has a priority 1 and resource B has a priority 4, resource B must be created
	 *         prior to resource A.
	 */
	int getPriority();

	/**
	 * @param writer       Writer to write into.
	 *
	 */
	void write(ConfigWriter writer);

}
