package edu.mayo.mprc.config;

import java.util.Comparator;

/**
 * Compares two resource configs by their priority.
 *
 * @author Roman Zenka
 */
public final class ResourceConfigComparator implements Comparator<ResourceConfig> {
	@Override
	public int compare(ResourceConfig o1, ResourceConfig o2) {
		final int p1 = o1.getPriority();
		final int p2 = o2.getPriority();
		return p1 < p2 ? 1 : p1 == p2 ? 0 : -1;
	}
}
