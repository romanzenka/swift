package edu.mayo.mprc.daemon.files;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

/**
 * A request for file download from a particular server.
 *
 * @author Roman Zenka
 */
public final class FileTransferWorkPacket extends WorkPacketBase {
	private FileToken fileToken;

	public FileTransferWorkPacket(final boolean fromScratch, final FileToken fileToken) {
		super(fromScratch);
		this.fileToken = fileToken;
	}

	public FileToken getFileToken() {
		return fileToken;
	}
}
