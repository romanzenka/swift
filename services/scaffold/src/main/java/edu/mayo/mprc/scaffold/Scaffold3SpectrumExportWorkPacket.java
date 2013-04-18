package edu.mayo.mprc.scaffold;

import edu.mayo.mprc.daemon.WorkPacketBase;

import java.io.File;

/**
 * Asking Scaffold3 to export spectra from a given Scaffold file.
 *
 * @author Roman Zenka
 */
public final class Scaffold3SpectrumExportWorkPacket extends WorkPacketBase {
	private static final long serialVersionUID = 3551194247963866822L;

	private File scaffoldFile;
	private File spectrumExportFile;

	public Scaffold3SpectrumExportWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	public Scaffold3SpectrumExportWorkPacket(final String taskId, final boolean fromScratch, final File scaffoldFile, final File spectrumExportFile) {
		super(taskId, fromScratch);
		this.scaffoldFile = scaffoldFile;
		this.spectrumExportFile = spectrumExportFile;
	}

	public File getScaffoldFile() {
		return scaffoldFile;
	}

	public File getSpectrumExportFile() {
		return spectrumExportFile;
	}

	@Override
	public void synchronizeFileTokensOnReceiver() {
		uploadAndWait("spectrumExportFile");
	}
}
