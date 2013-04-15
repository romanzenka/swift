package edu.mayo.mprc.config;

/**
 * A resource that is aware of its own type name.
 *
 * @author Roman Zenka
 */
public interface TypedResource {
	String getType();

	void setType(String type);
}
