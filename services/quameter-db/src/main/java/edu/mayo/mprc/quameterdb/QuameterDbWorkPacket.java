package edu.mayo.mprc.quameterdb;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class QuameterDbWorkPacket extends WorkPacketBase {
	private static final long serialVersionUID = 20140129L;

	private int analysisId;
	private Integer searchResultId; // Can be null in case the search did not identify any spectra
	private int fileSearchId;
	private File quameterResultFile;
	private File scaffoldSpectraFile;

	public QuameterDbWorkPacket(final boolean fromScratch) {
		super(fromScratch);
	}

	public QuameterDbWorkPacket(final boolean fromScratch,
	                            final int analysisId,
	                            final Integer searchResultId, final int fileSearchId,
	                            final File quameterResultFile,
	                            final File scaffoldSpectraFile) {
		super(fromScratch);
		this.analysisId = analysisId;
		this.searchResultId = searchResultId;
		this.fileSearchId = fileSearchId;
		this.quameterResultFile = quameterResultFile;
		this.scaffoldSpectraFile = scaffoldSpectraFile;
	}

	public int getAnalysisId() {
		return analysisId;
	}

	public Integer getSearchResultId() {
		return searchResultId;
	}

	public int getFileSearchId() {
		return fileSearchId;
	}

	public File getQuameterResultFile() {
		return quameterResultFile;
	}

	public File getScaffoldSpectraFile() {
		return scaffoldSpectraFile;
	}
}
