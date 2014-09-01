package edu.mayo.mprc.quameterdb.dao;

import java.util.List;

/**
 * Information about updated protein groups.
 *
 * @author Roman Zenka
 */
public final class ProteinGroupUpdate {
	private final List<QuameterProteinGroup> allGroups;
	private final List<QuameterProteinGroup> addedGroups;
	private final List<QuameterProteinGroup> removedGroups;
	private final List<QuameterProteinGroup> modifiedGroups;

	public ProteinGroupUpdate(final List<QuameterProteinGroup> allGroups, final List<QuameterProteinGroup> addedGroups, final List<QuameterProteinGroup> removedGroups, final List<QuameterProteinGroup> modifiedGroups) {
		this.allGroups = allGroups;
		this.addedGroups = addedGroups;
		this.removedGroups = removedGroups;
		this.modifiedGroups = modifiedGroups;
	}

	public List<QuameterProteinGroup> getAllGroups() {
		return allGroups;
	}

	public List<QuameterProteinGroup> getAddedGroups() {
		return addedGroups;
	}

	public List<QuameterProteinGroup> getRemovedGroups() {
		return removedGroups;
	}

	public List<QuameterProteinGroup> getModifiedGroups() {
		return modifiedGroups;
	}
}
