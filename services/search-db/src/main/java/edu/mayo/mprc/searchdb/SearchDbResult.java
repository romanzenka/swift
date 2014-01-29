package edu.mayo.mprc.searchdb;

import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class SearchDbResult implements ProgressInfo {
	private static final long serialVersionUID = 20130129L;

	private Map<String, Integer> loadedRawFileMetadata;

	public SearchDbResult(Map<String, Integer> loadedRawFileMetadata) {
		this.loadedRawFileMetadata = loadedRawFileMetadata;
	}

	public Map<String, Integer> getLoadedRawFileMetadata() {
		return loadedRawFileMetadata;
	}

	public void setLoadedRawFileMetadata(Map<String, Integer> loadedRawFileMetadata) {
		this.loadedRawFileMetadata = loadedRawFileMetadata;
	}
}
