package edu.mayo.mprc.swift.webservice.diff;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.List;
import java.util.Map;

/**
 * @author Roman Zenka
 */
@XStreamAlias("searchDiff")
public final class SearchDiff {
	private final List<InputFile> inputFiles;
	private final List<SwiftSearch> searches;
	private final Map<Integer/*Input file id*/, Map<Integer/* Swift search id*/, IdCounts>> results;

	public SearchDiff(Map<Integer/*Input file id*/, Map<Integer/* Swift search id*/, IdCounts>> results,
	                  List<SwiftSearch> searches,
	                  List<InputFile> inputFiles) {
		this.results = results;
		this.searches = searches;
		this.inputFiles = inputFiles;
	}

	public List<InputFile> getInputFiles() {
		return inputFiles;
	}

	public List<SwiftSearch> getSearches() {
		return searches;
	}

	public Map<Integer, Map<Integer, IdCounts>> getResults() {
		return results;
	}
}
