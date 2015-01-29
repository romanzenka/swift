package edu.mayo.mprc.swift.controller;

import edu.mayo.mprc.utilities.ComparisonChain;

/**
 * @author Roman Zenka
 */
class FilterData implements Comparable<FilterData> {
	final String id;
	final String value;

	public FilterData(final String id, final String value) {
		this.id = id;
		this.value = value;
	}

	public String getId() {
		return id;
	}

	public String getValue() {
		return value;
	}

	@Override
	public int compareTo(final FilterData t) {
		return
				ComparisonChain
						.start()
						.compare(value, t.value)
						.compare(id, t.id)
						.result();
	}
}
