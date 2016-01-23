package edu.mayo.mprc.daemon.files;

import edu.mayo.mprc.utilities.progress.ProgressInfo;

/**
 * @author Roman Zenka
 */
public final class FileTransferChunk implements ProgressInfo {
	private byte[] chunk;
	private long offset;

	public FileTransferChunk(byte[] chunk, long offset) {
		this.chunk = chunk;
		this.offset = offset;
	}

	public byte[] getChunk() {
		return chunk;
	}

	public long getOffset() {
		return offset;
	}
}
