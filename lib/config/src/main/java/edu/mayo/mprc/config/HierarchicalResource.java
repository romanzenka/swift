package edu.mayo.mprc.config;

/**
 * A resource that lives in a hierarchy.
 * <p/>
 * It has a parent that can be set and obtained.
 *
 * @author Roman Zenka
 */
public interface HierarchicalResource {
	ResourceConfig getParentConfig();

	void setParentConfig(ResourceConfig parent);
}
