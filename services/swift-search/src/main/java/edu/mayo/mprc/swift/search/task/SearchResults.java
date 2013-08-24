package edu.mayo.mprc.swift.search.task;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Search results are submitted as a list of {@link FileSearchResult}. Each list represents
 * all search outputs for one particular input file.
 */
final class SearchResults implements Serializable {
	private static final long serialVersionUID = 20071220L;
	// Full paths to the input files and their search results.
	private List<FileSearchResult> results = new ArrayList<FileSearchResult>();

	public SearchResults() {
	}

	public SearchResults addResult(final FileSearchResult result) {
		results.add(result);
		return this;
	}

	public List<FileSearchResult> getResults() {
		return results;
	}

	/**
	 * For given input file, find all search results that match it and return them.
	 */
	public Map<String/*Search engine code*/, File> getAllResults(final File inputFile) {
		final Map<String/*Search engine code*/, File> result = new HashMap<String, File>();
		for (final FileSearchResult r : results) {
			if (r.getInputFile().equals(inputFile)) {
				result.putAll(r.getResults());
			}
		}
		return result;
	}
}
