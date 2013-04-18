package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.searchdb.RawFileMetaData;
import edu.mayo.mprc.searchdb.SearchDbWorkPacket;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Take Scaffold's spectrum report and loads it into a relational database.
 *
 * @author Roman Zenka
 */
public final class SearchDbTask extends AsyncTaskBase {

	private ScaffoldTask scaffoldTask;
	private Long reportId;
	private File scaffoldSpectraFile;
	private Map<String, RAWDumpTask> rawDumpTaskMap = new HashMap<String, RAWDumpTask>(5);

	/**
	 * Create the task independently on Scaffold invocation.
	 */
	public SearchDbTask(final DaemonConnection daemon, final FileTokenFactory fileTokenFactory, final boolean fromScratch, final Long reportId, final File scaffoldSpectraFile) {
		super(daemon, fileTokenFactory, fromScratch);
		setReportId(reportId);
		setScaffoldSpectraFile(scaffoldSpectraFile);
		setName("SearchDb");
		setDescription("Load " + fileTokenFactory.fileToTaggedDatabaseToken(getScaffoldSpectraFile()) + " into database");
	}

	/**
	 * Create the task that depends on Scaffold invocation.
	 */
	public SearchDbTask(final DaemonConnection daemon, final FileTokenFactory fileTokenFactory, final boolean fromScratch, final ScaffoldTask scaffoldTask) {
		super(daemon, fileTokenFactory, fromScratch);
		this.scaffoldTask = scaffoldTask;
		setName("SearchDb");
		setDescription("Load " + fileTokenFactory.fileToTaggedDatabaseToken(getScaffoldSpectraFile()) + " into database");
	}

	/**
	 * @param task Raw dump task to add to the map. The results are mapped based on file name.
	 */
	public void addRawDumpTask(final RAWDumpTask task) {
		final String fileName = FileUtilities.stripExtension(task.getRawFile().getName());
		if (rawDumpTaskMap.containsKey(fileName)) {
			throw new MprcException("Two files of identical name: " + task.getRawFile().getName() + " cannot be distinguished in resulting analysis.");
		}
		rawDumpTaskMap.put(fileName, task);
	}

	private File getScaffoldSpectraFile() {
		return scaffoldSpectraFile == null ? scaffoldTask.getScaffoldSpectraFile() : scaffoldSpectraFile;
	}

	/**
	 * Override the spectra file if the scaffold3 task is not available.
	 *
	 * @param scaffoldSpectraFile Scaffold spectra file to load.
	 */
	public void setScaffoldSpectraFile(File scaffoldSpectraFile) {
		this.scaffoldSpectraFile = scaffoldSpectraFile;
	}

	private Long getReportId() {
		return reportId == null ? scaffoldTask.getReportData().getId() : reportId;
	}

	/**
	 * Override the report id if scaffold task is not available.
	 *
	 * @param reportId Report ID to link to
	 */
	public void setReportId(Long reportId) {
		this.reportId = reportId;
	}

	@Override
	public WorkPacket createWorkPacket() {
		final HashMap<String, RawFileMetaData> metaDataMap = new HashMap<String, RawFileMetaData>(rawDumpTaskMap.size());
		for (final Map.Entry<String, RAWDumpTask> entry : rawDumpTaskMap.entrySet()) {
			final RAWDumpTask task = entry.getValue();
			metaDataMap.put(entry.getKey(), task.getRawFileMetadata());
		}

		return new SearchDbWorkPacket(getFullId(), isFromScratch(), getReportId(), getScaffoldSpectraFile(), metaDataMap);
	}

	@Override
	public void onSuccess() {
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
	}
}
