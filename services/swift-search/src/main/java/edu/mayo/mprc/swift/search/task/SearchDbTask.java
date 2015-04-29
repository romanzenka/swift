package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.searchdb.SearchDbResult;
import edu.mayo.mprc.searchdb.SearchDbWorkPacket;
import edu.mayo.mprc.searchdb.builder.RawFileMetaData;
import edu.mayo.mprc.searchdb.dao.BiologicalSampleId;
import edu.mayo.mprc.searchdb.dao.SearchDbResultEntry;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Take Scaffold's spectrum report and loads it into a relational database.
 *
 * @author Roman Zenka
 */
public final class SearchDbTask extends AsyncTaskBase {

	private final ScaffoldSpectrumExportProducer scaffoldTask;

	private final Object progressLock = new Object();

	/**
	 * Map from input .RAW file name (name only, sans extension) to a task that dumps the data
	 */
	private final Map<String, RAWDumpTask> rawDumpTaskMap = new HashMap<String, RAWDumpTask>(5);
	/**
	 * A map from full input .RAW file path to an id of {@link edu.mayo.mprc.searchdb.dao.SearchResult} object.
	 */
	private Map<String, Integer> loadedSearchResults;
	/**
	 * The scaffold report gets loaded into an analysis object. This is the analysis ID.
	 */
	private Integer analysisId;

	/**
	 * Map from msms sample name (file name sans extension) to the biological sample the file is a part of.
	 * This helps with adding files that had no spectra identified by Scaffold into the data structures.
	 */
	private Map<String, BiologicalSampleId> fileToBiologicalSampleMap;

	/**
	 * Create the task that depends on Scaffold invocation.
	 */
	public SearchDbTask(final WorkflowEngine engine, final DaemonConnection daemon, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch, final ScaffoldSpectrumExportProducer scaffoldTask) {
		super(engine, daemon, fileTokenFactory, fromScratch);
		fileToBiologicalSampleMap = Maps.newHashMap();
		this.scaffoldTask = scaffoldTask;
		setName("SearchDb");
		setDescription("Load " + fileTokenFactory.fileToTaggedDatabaseToken(getScaffoldSpectraFile()) + " into database");
	}

	/**
	 * @param task Raw dump task to add to the map. The results are mapped based on file name.
	 */
	public void addRawDumpTask(final RAWDumpTask task, final BiologicalSampleId biologicalSampleId) {
		final String fileName = FileUtilities.stripExtension(task.getRawFile().getName());
		if (rawDumpTaskMap.containsKey(fileName)) {
			throw new MprcException("Two files of identical name: " + task.getRawFile().getName() + " cannot be distinguished in resulting analysis.");
		}
		rawDumpTaskMap.put(fileName, task);
		fileToBiologicalSampleMap.put(FileUtilities.stripGzippedExtension(task.getRawFileMetadata().getRawFile().getName()), biologicalSampleId);
	}

	private File getScaffoldSpectraFile() {
		return scaffoldTask.getScaffoldSpectraFile();
	}

	private File getScaffoldUnimodFile() {
		return scaffoldTask.getUnimod();
	}


	private Long getReportId() {
		return scaffoldTask.getReportData().getId();
	}

	public Map<String, Integer> getLoadedSearchResults() {
		synchronized (progressLock) {
			return loadedSearchResults;
		}
	}

	public Integer getAnalysisId() {
		synchronized (progressLock) {
			return analysisId;
		}
	}

	@Override
	public WorkPacket createWorkPacket() {
		final HashMap<String, RawFileMetaData> metaDataMap = new HashMap<String, RawFileMetaData>(rawDumpTaskMap.size());
		for (final Map.Entry<String, RAWDumpTask> entry : rawDumpTaskMap.entrySet()) {
			final RAWDumpTask task = entry.getValue();
			metaDataMap.put(entry.getKey(), task.getRawFileMetadata());
		}

		return new SearchDbWorkPacket(isFromScratch(), getReportId(), getScaffoldSpectraFile(), getScaffoldUnimodFile(), metaDataMap, fileToBiologicalSampleMap);
	}

	@Override
	public void onSuccess() {
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
		if (progressInfo instanceof SearchDbResult) {
			synchronized (progressLock) {
				final SearchDbResult result = (SearchDbResult) progressInfo;
				analysisId = result.getAnalysisId();
				loadedSearchResults = Maps.newTreeMap();
				for (SearchDbResultEntry entry : result.getLoadedSearchResults()) {
					loadedSearchResults.put(entry.getInputFile().getAbsolutePath(), entry.getSearchResultId());
				}
			}
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(scaffoldTask);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final SearchDbTask other = (SearchDbTask) obj;
		return Objects.equal(scaffoldTask, other.scaffoldTask);
	}
}
