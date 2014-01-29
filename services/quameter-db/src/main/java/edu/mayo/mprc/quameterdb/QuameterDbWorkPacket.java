package edu.mayo.mprc.quameterdb;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class QuameterDbWorkPacket extends WorkPacketBase {
	private static final long serialVersionUID = 20140129L;

	private int tandemMassSpectrometrySampleId;
	private int fileSearchId;
	private File quameterResultFile;

	public QuameterDbWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	public QuameterDbWorkPacket(final String taskId, final boolean fromScratch,
	                            final int tandemMassSpectrometrySampleId, final int fileSearchId, final File quameterResultFile) {
		super(taskId, fromScratch);
		this.tandemMassSpectrometrySampleId = tandemMassSpectrometrySampleId;
		this.fileSearchId = fileSearchId;
		this.quameterResultFile = quameterResultFile;
	}

	public int getTandemMassSpectrometrySampleId() {
		return tandemMassSpectrometrySampleId;
	}

	public int getFileSearchId() {
		return fileSearchId;
	}

	public File getQuameterResultFile() {
		return quameterResultFile;
	}
}
