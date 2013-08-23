package edu.mayo.mprc.swift.webservice.diff;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Roman Zenka
 */
@XStreamAlias("idCounts")
public final class IdCounts {
	private final boolean present;
	private final int proteinGroups;

	public IdCounts(boolean present, int proteinGroups) {
		this.present = present;
		this.proteinGroups = proteinGroups;
	}

	public boolean isPresent() {
		return present;
	}

	public int getProteinGroups() {
		return proteinGroups;
	}
}
