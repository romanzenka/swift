package edu.mayo.mprc.config;

/**
 * A resource marker interface.
 */
public interface ResourceConfig {
	void save(ConfigWriter writer);

	void load(ConfigReader reader);

	/**
	 * @return Int value that indicates the priority which this resource
	 *         must be initialized. Lower values indicate lower priority. If resource A
	 *         has a priority 1 and resource B has a priority 4, resource B must be created
	 *         prior to resource A.
	 */
	int getPriority();
}
