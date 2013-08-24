package edu.mayo.mprc.swift.search.task;

import java.util.HashSet;
import java.util.Set;

class InputFileSearches {
	private Set<EngineSearchTask> searches;

	InputFileSearches() {
		searches = new HashSet<EngineSearchTask>();
	}

	public void addSearch(final EngineSearchTask search) {
		searches.add(search);
	}

	public Set<EngineSearchTask> getSearches() {
		return searches;
	}
}
