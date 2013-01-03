package edu.mayo.mprc.filesharing.jms;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Response send to requester after receiving a file transfer request. Response contains
 * existing files and not existing files in receiver. The response may contain server socket information
 * if transfer is to be initiated by requester.
 */
final class MultiFileTransferResponse implements Serializable {
	private static final long serialVersionUID = 20111119L;

	private long requestId;

	private List<FileInfo> fileInfos;

	//This are original FileInfo objects that do not exist in the source system.
	private List<FileInfo> notExistingFileInfos;

	private InetSocketAddress inetSocketAddress;

	public MultiFileTransferResponse(final long requestId) {
		this.requestId = requestId;
		fileInfos = new ArrayList<FileInfo>();
		notExistingFileInfos = new ArrayList<FileInfo>();
	}

	public MultiFileTransferResponse(final long requestId, final List<FileInfo> fileInfos) {
		this(requestId);
		this.fileInfos = fileInfos;
	}

	public MultiFileTransferResponse(final long requestId, final List<FileInfo> fileInfos, final InetSocketAddress inetSocketAddress) {
		this(requestId);
		this.fileInfos = fileInfos;
		this.inetSocketAddress = inetSocketAddress;
	}

	public List<FileInfo> getFileInfos() {
		return fileInfos;
	}

	public void setFileInfos(final List<FileInfo> fileInfos) {
		this.fileInfos = fileInfos;
	}

	public List<FileInfo> getNotExistingFileInfos() {
		return notExistingFileInfos;
	}

	public void setNotExistingFileInfos(final List<FileInfo> notExistingFileInfos) {
		this.notExistingFileInfos = notExistingFileInfos;
	}

	public InetSocketAddress getInetSocketAddress() {
		return inetSocketAddress;
	}

	public void setInetSocketAddress(final InetSocketAddress inetSocketAddress) {
		this.inetSocketAddress = inetSocketAddress;
	}

	public long getRequestId() {
		return requestId;
	}
}
