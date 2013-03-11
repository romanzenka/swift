package edu.mayo.mprc.filesharing;

import java.io.File;

public interface FileTransferHandler extends FileSynchronizer {
	/**
	 * Copies file identify by given file path from remote file source identify by sourceId to localDestinationFile file.
	 * If source file is a directory, method will return successfully and not transfer of data will take place.
	 *
	 * @param sourceId
	 * @param sourcefilePath
	 * @param localDestinationFile File copy of given remote file.
	 * @return
	 */
	FileTransfer getFile(String sourceId, String sourcefilePath, File localDestinationFile);

	/**
	 * Starts file sharing.
	 *
	 * @param processRemoteRequests true if this FileTransferHandler will process remote requests
	 *                              for local files. if it is false, this FileTransferHandler will only process local requests
	 *                              for remote files.
	 */
	void startProcessingRequests(boolean processRemoteRequests);

	/**
	 * Starts file sharing for remote and local file transfer requests.
	 */
	void startProcessingRequests();

	/**
	 * Stops file sharing.
	 */
	void stopProcessingRequest();
}
