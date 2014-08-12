package edu.mayo.mprc.config;

/**
 * @author Roman Zenka
 */
public interface PropertyValues<T> {
	/**
	 * Returns a value for given property name.
	 * A property name can be complex, e.g. engine.4.name - name of the 4th engine
	 *
	 * @param propertyName Name of property
	 * @return Value associated with the property
	 */
	T getValue(String propertyName);

	/**
	 * @return List of all property names for which we have values
	 */
	Iterable<String> keySet();
}
