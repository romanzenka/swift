package edu.mayo.mprc.swift.ui.client.rpc.files;

import java.io.Serializable;
import java.util.Date;

public final class FileInfo implements Serializable {
	private static final long serialVersionUID = 20111119L;
	private String relativePath;
	private long size;
	private Date lastModifiedDate;

	public FileInfo() {

	}

	public FileInfo(final String relativePath, final long size, final Date lastModifiedDate) {
		this.relativePath = relativePath;
		this.size = size;
		this.lastModifiedDate = lastModifiedDate;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public FileInfo setRelativePath(final String relativePath) {
		this.relativePath = relativePath;
		return this;
	}

	public long getSize() {
		return size;
	}

	public FileInfo setSize(final long size) {
		this.size = size;
		return this;
	}

	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}
}
