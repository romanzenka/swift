package edu.mayo.mprc.filesharing.jms;

import java.io.Serializable;
import java.util.List;

/**
 * Event represent a remote file transfer completion. If file transfer operation
 * is successful, exception is null. Otherwise, exception object should
 * reference error exception.
 */
final class RemoteTransferCompleteEvent implements Serializable {
	private static final long serialVersionUID = 20111119L;
	private long requestId;
	private Exception exception;

	//Files transfered successfully.
	private List<FileInfo> fileInfos;

	public RemoteTransferCompleteEvent(final long requestId, final List<FileInfo> fileInfos) {
		this.requestId = requestId;
		this.fileInfos = fileInfos;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(final Exception exception) {
		this.exception = exception;
	}

	public List<FileInfo> getFileInfos() {
		return fileInfos;
	}

	public void setFileInfos(final List<FileInfo> fileInfos) {
		this.fileInfos = fileInfos;
	}

	public long getRequestId() {
		return requestId;
	}
}
