package edu.mayo.mprc.filesharing.jms;

import java.io.Serializable;
import java.util.Date;

final class FileInfo implements Serializable {
	private static final long serialVersionUID = 20111119L;

	private String filePath;
	private long length;
	private long lastModified;

	public FileInfo(final String filePath, final long length, final long lastModified) {
		this.filePath = filePath;
		this.length = length;
		this.lastModified = lastModified;
	}

	public FileInfo(final String filePath) {
		this.filePath = filePath;
	}

	public long getLength() {
		return length;
	}

	public void setLength(final long length) {
		this.length = length;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(final long lastModified) {
		this.lastModified = lastModified;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(final String filePath) {
		this.filePath = filePath;
	}

	public String toString() {
		return "File Path: " + filePath + " | Last Modified: " + (lastModified == 0 ? "0" : new Date(lastModified).toString()) + " | Size in bytes: " + length;
	}
}
