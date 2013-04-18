package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.scaffold.ScaffoldSpectrumExportWorkPacket;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldSpectraReader;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;

/**
 * Exports spectra from a Scaffold file
 *
 * @author Roman Zenka
 */
public final class ScaffoldSpectraExportTask extends AsyncTaskBase {
	private File scaffoldFile;

	private File spectrumExportFile;

	public ScaffoldSpectraExportTask(DaemonConnection daemon, FileTokenFactory fileTokenFactory, boolean fromScratch, File scaffoldFile) {
		super(daemon, fileTokenFactory, fromScratch);
		this.scaffoldFile = scaffoldFile;
		spectrumExportFile = getDefaultSpectrumExportFile(scaffoldFile);
		setName("Scaffold spectra export");
		setDescription("Exporting " + fileTokenFactory.fileToTaggedDatabaseToken(scaffoldFile) + " to " + fileTokenFactory.fileToTaggedDatabaseToken(spectrumExportFile));
	}

	public static File getDefaultSpectrumExportFile(File scaffoldFile) {
		final File scaffoldSpectrumExport = new File(
				scaffoldFile.getParentFile(),
				FileUtilities.getFileNameWithoutExtension(scaffoldFile) + ScaffoldSpectraReader.EXTENSION);
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
