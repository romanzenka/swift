package edu.mayo.mprc.searchdb;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.searchdb.builder.RawFileMetaData;
import edu.mayo.mprc.searchdb.dao.BiologicalSampleId;
import edu.mayo.mprc.swift.dbmapping.ReportData;

import java.io.File;
import java.util.Map;

/**
 * Load search results for Swift search report (read - Scaffold file) of given ID.
 * <p/>
 * The packet sends just an ID because it makes no sense to send Hibernate object - it will be disconnected from the session
 * anyway.
 */
public final class SearchDbWorkPacket extends WorkPacketBase {
	private static final long serialVersionUID = 100830535859947146L;
	/**
	 * ID of the particular Swift report that ties to the Scaffold file.
	 * Links to {@link ReportData} object.
	 * This way
	 */
	private long reportDataId;

	/**
	 * Scaffold-produced spectrum report file.
	 */
	private File scaffoldSpectrumReport;

	private File scaffoldUnimod;

	/**
	 * Map from raw file name -> metadata about the raw file
	 */
	private Map<String, RawFileMetaData> fileMetaDataMap;

	/**
	 * Map from msms sample name (name of the file sans the extension) -> biological sample it is a part of
	 */
	private Map<String, BiologicalSampleId> fileToBiologicalSampleMap;

	public SearchDbWorkPacket(final boolean fromScratch, final long reportDataId,
	                          final File scaffoldSpectrumReport,
	                          final File scaffoldUnimod,
	                          final Map<String, RawFileMetaData> fileMetaDataMap,
	                          final Map<String, BiologicalSampleId> fileToBiologicalSampleMap) {
		super(fromScratch);
		this.reportDataId = reportDataId;
		this.scaffoldSpectrumReport = scaffoldSpectrumReport;
		this.scaffoldUnimod = scaffoldUnimod;
		this.fileMetaDataMap = fileMetaDataMap;
		this.fileToBiologicalSampleMap = fileToBiologicalSampleMap;
	}

	public long getReportDataId() {
		return reportDataId;
	}

	public File getScaffoldSpectrumReport() {
		return scaffoldSpectrumReport;
	}

	public File getScaffoldUnimod() {
		return scaffoldUnimod;
	}

	public Map<String, RawFileMetaData> getFileMetaDataMap() {
		return fileMetaDataMap;
	}

	public Map<String, BiologicalSampleId> getFileToBiologicalSampleMap() {
		return fileToBiologicalSampleMap;
	}
}
