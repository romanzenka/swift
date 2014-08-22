package edu.mayo.mprc.searchdb;

import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class SearchDbResult implements ProgressInfo {
	private static final long serialVersionUID = 20130129L;

	private Map<String, Integer> loadedRawFileMetadata;
	private Integer analysisId;

	public SearchDbResult(final Map<String, Integer> loadedRawFileMetadata, final int analysisId) {
		this.loadedRawFileMetadata = loadedRawFileMetadata;
		this.analysisId = analysisId;
	}

	public Map<String, Integer> getLoadedRawFileMetadata() {
		return loadedRawFileMetadata;
	}

	public Integer getAnalysisId() {
		return analysisId;
	}
}
