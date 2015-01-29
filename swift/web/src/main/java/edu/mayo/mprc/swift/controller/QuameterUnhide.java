package edu.mayo.mprc.swift.controller;

/**
 * @author Roman Zenka
 */
public final class QuameterUnhide {
	private int id;
	private String directory;
	private String fileName;
	private String reason;

	public QuameterUnhide(String directory, String fileName, int id, String reason) {
		this.directory = directory;
		this.fileName = fileName;
		this.id = id;
		this.reason = reason;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
