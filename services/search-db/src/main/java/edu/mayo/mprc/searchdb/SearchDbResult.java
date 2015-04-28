package edu.mayo.mprc.searchdb;

import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.searchdb.dao.SearchDbResultEntry;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.util.List;

/**
 * @author Roman Zenka
 */
public final class SearchDbResult extends FileHolder implements ProgressInfo {
	private static final long serialVersionUID = 20130129L;

	private List<SearchDbResultEntry> loadedSearchResults;
	private Integer analysisId;

	public SearchDbResult(final List<SearchDbResultEntry> loadedSearchResults, final int analysisId) {
		this.loadedSearchResults = loadedSearchResults;
		this.analysisId = analysisId;
	}

	public List<SearchDbResultEntry> getLoadedSearchResults() {
		return loadedSearchResults;
	}

	public Integer getAnalysisId() {
		return analysisId;
	}
}
