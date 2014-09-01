package edu.mayo.mprc.quameterdb;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class QuameterDbWorkPacket extends WorkPacketBase {
	private static final long serialVersionUID = 20140129L;

	private int analysisId;
	private int searchResultId;
	private int fileSearchId;
	private File quameterResultFile;

	public QuameterDbWorkPacket(final boolean fromScratch) {
		super(fromScratch);
	}

	public QuameterDbWorkPacket(final boolean fromScratch,
	                            final int analysisId,
	                            final int searchResultId, final int fileSearchId, final File quameterResultFile) {
		super(fromScratch);
		this.analysisId = analysisId;
		this.searchResultId = searchResultId;
		this.fileSearchId = fileSearchId;
		this.quameterResultFile = quameterResultFile;
	}

	public int getAnalysisId() {
		return analysisId;
	}

	public int getSearchResultId() {
		return searchResultId;
	}

	public int getFileSearchId() {
		return fileSearchId;
	}

	public File getQuameterResultFile() {
		return quameterResultFile;
	}
}
