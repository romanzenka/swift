package edu.mayo.mprc.filesharing.jms;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Request of file transfer. List of files represents files in the
 * system receiving the request. List of files is to be transfered to files
 * this receiving system if boolean isSource variable is false. Otherwise, file transfer
 * happens from request receiving system to system requesting transfer.
 */
final class MultiFileTransferRequest implements Serializable {
	private static final long serialVersionUID = 20110212L;

	private long requestId;

	private List<FileInfo> fileInfos;
	private boolean areSource;
	private InetSocketAddress inetSocketAddress;

	public MultiFileTransferRequest(final long requestId, final List<FileInfo> fileInfos) {
		this.requestId = requestId;
		this.fileInfos = fileInfos;
	}

	public MultiFileTransferRequest(final long requestId, final List<FileInfo> fileInfos, final InetSocketAddress inetSocketAddress) {
		this(requestId, fileInfos);
		this.inetSocketAddress = inetSocketAddress;
	}

	public List<FileInfo> getFileInfos() {
		return fileInfos;
	}

	public void setFileInfos(final List<FileInfo> fileInfos) {
		this.fileInfos = fileInfos;
	}

	public InetSocketAddress getInetSocketAddress() {
		return inetSocketAddress;
	}

	public void setInetSocketAddress(final InetSocketAddress inetSocketAddress) {
		this.inetSocketAddress = inetSocketAddress;
	}

	/**
	 * Returns true if the FileInfo objects are a representation of the source files. Default is false.
	 *
	 * @return
	 */
	public boolean areSource() {
		return areSource;
	}

	public void setBeSource(final boolean areSource) {
		this.areSource = areSource;
	}

	public long getRequestId() {
		return requestId;
	}
}
