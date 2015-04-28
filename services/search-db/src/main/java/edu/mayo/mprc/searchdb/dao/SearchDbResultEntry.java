package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.daemon.files.FileHolder;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class SearchDbResultEntry extends FileHolder {
	private static final long serialVersionUID = -9030615025897045642L;

	private File inputFile;
	private Integer searchResultId;

	public SearchDbResultEntry() {
	}

	public SearchDbResultEntry(File inputFile, Integer searchResultId) {
		this.inputFile = inputFile;
		this.searchResultId = searchResultId;
	}

	public File getInputFile() {
		return inputFile;
	}

	public Integer getSearchResultId() {
		return searchResultId;
	}
}
