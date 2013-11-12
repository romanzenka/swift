package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.scaffold.ScaffoldSpectrumExportWorkPacket;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;

/**
 * Exports spectra from a Scaffold file
 *
 * @author Roman Zenka
 */
public final class ScaffoldSpectraExportTask extends AsyncTaskBase {
	private File scaffoldFile;

	private File spectrumExportFile;

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

	public File getSpectrumExportFile() {
		return spectrumExportFile;
	}

	public void setSpectrumExportFile(File spectrumExportFile) {
		this.spectrumExportFile = spectrumExportFile;
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
}
