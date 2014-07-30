package edu.mayo.mprc.scaffold;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

import java.io.File;

/**
 * Asking Scaffold to export spectra from a given Scaffold file.
 *
 * @author Roman Zenka
 */
public final class ScaffoldSpectrumExportWorkPacket extends WorkPacketBase {
	private static final long serialVersionUID = 3551194247963866822L;

	private File scaffoldFile;
	private File spectrumExportFile;

	public ScaffoldSpectrumExportWorkPacket(final boolean fromScratch) {
		super(fromScratch);
	}

	public ScaffoldSpectrumExportWorkPacket(final boolean fromScratch, final File scaffoldFile, final File spectrumExportFile) {
		super(fromScratch);
		this.scaffoldFile = scaffoldFile;
		this.spectrumExportFile = spectrumExportFile;
	}

	public File getScaffoldFile() {
		return scaffoldFile;
	}

	public File getSpectrumExportFile() {
		return spectrumExportFile;
	}
}
