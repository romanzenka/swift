package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.scaffold.ScaffoldSpectrumExportWorkPacket;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;

/**
 * Exports spectra from a Scaffold file
 *
 * @author Roman Zenka
 */
public final class ScaffoldSpectraExportTask extends AsyncTaskBase implements ScaffoldSpectrumExportProducer {
	private final File scaffoldFile;
	private final File spectrumExportFile;
	private ReportData reportData;

	public ScaffoldSpectraExportTask(final WorkflowEngine engine, DaemonConnection daemon, DatabaseFileTokenFactory fileTokenFactory, boolean fromScratch, File scaffoldFile) {
		super(engine, daemon, fileTokenFactory, fromScratch);
		this.scaffoldFile = scaffoldFile;
		spectrumExportFile = getDefaultSpectrumExportFile(scaffoldFile);
		setName("Scaffold spectra export");
		setDescription("Exporting " + fileTokenFactory.fileToTaggedDatabaseToken(scaffoldFile) + " to " + fileTokenFactory.fileToTaggedDatabaseToken(spectrumExportFile));
	}

	public static File getDefaultSpectrumExportFile(File scaffoldFile) {
		final File scaffoldSpectrumExport = new File(
				scaffoldFile.getParentFile(),
				FileUtilities.getFileNameWithoutExtension(scaffoldFile) + ScaffoldReportReader.SPECTRA_EXTENSION);
		return scaffoldSpectrumExport;
	}

	@Override
	public WorkPacket createWorkPacket() {
		return new ScaffoldSpectrumExportWorkPacket(getFullId(), isFromScratch(), scaffoldFile, spectrumExportFile);
	}

	@Override
	public void onSuccess() {

	}

	@Override
	public void onProgress(ProgressInfo progressInfo) {

	}

	@Override
	public int hashCode() {
		return Objects.hashCode(scaffoldFile, spectrumExportFile);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ScaffoldSpectraExportTask other = (ScaffoldSpectraExportTask) obj;
		return Objects.equal(scaffoldFile, other.scaffoldFile) && Objects.equal(spectrumExportFile, other.spectrumExportFile);
	}

	@Override
	public File getScaffoldSpectraFile() {
		return spectrumExportFile;
	}

	public ReportData getReportData() {
		return reportData;
	}

	public void setReportData(ReportData reportData) {
		this.reportData = reportData;
	}

	@Override
	public File getUnimod() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
