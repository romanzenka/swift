package edu.mayo.mprc.mgf2mgf;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;

import java.io.File;

public final class MgfTitleCleanupWorkPacket extends WorkPacketBase {
	private static final long serialVersionUID = 20080214L;

	private File mgfToCleanup;
	private File cleanedMgf;

	public MgfTitleCleanupWorkPacket(final File mgfToCleanup, final File cleanedMgf, final boolean fromScratch) {
		super(fromScratch);
		this.mgfToCleanup = mgfToCleanup;
		this.cleanedMgf = cleanedMgf;
		if (mgfToCleanup == cleanedMgf) {
			throw new MprcException("The mgf cleanup process must not take same path for the original and cleaned .mgf");
		}
	}

	public File getMgfToCleanup() {
		return mgfToCleanup;
	}

	public File getCleanedMgf() {
		return cleanedMgf;
	}
}
