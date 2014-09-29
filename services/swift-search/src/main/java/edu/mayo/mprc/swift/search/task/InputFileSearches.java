package edu.mayo.mprc.swift.search.task;

import java.util.HashSet;
import java.util.Set;

class InputFileSearches {
	private Set<FileProducingTask> searches;

	InputFileSearches() {
		searches = new HashSet<FileProducingTask>();
	}

	public void addSearch(final FileProducingTask search) {
		searches.add(search);
	}

	public Set<FileProducingTask> getSearches() {
		return searches;
	}
}
