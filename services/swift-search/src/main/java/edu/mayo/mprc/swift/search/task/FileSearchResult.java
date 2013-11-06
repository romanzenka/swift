package edu.mayo.mprc.swift.search.task;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains all the search results for given input file.
 * The results are organized in a map.
 * <ul>
 * <li>The key is a search engine .</li>
 * <li>The value is the search result for the particular combination of engine+params.</li>
 * <ul>
 */
final class FileSearchResult implements Serializable {
	private static final long serialVersionUID = 20071220L;

	private File inputFile;
	private Map<String/*Search Engine Code*/, /*search result*/File> results =
			new HashMap<String/*Search Engine Code*/, File>();

	FileSearchResult(final File inputFile) {
		this.inputFile = inputFile;
	}

	public FileSearchResult addResult(final String engineCode, final File file) {
		results.put(engineCode, file);
		return this;
	}

	public File getInputFile() {
		return inputFile;
	}

	public Map<String/*Search Engine Code*/, File> getResults() {
		return results;
	}
}
