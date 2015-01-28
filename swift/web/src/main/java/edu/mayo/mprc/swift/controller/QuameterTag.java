package edu.mayo.mprc.swift.controller;

/**
 * @author Roman Zenka
 */
public class QuameterTag {
	private String directory;
	private String fileName;
	private String instrument;
	private String metric;
	private String tagText;

	public QuameterTag(String directory, String fileName, String instrument, String metric, String tagText) {
		this.directory = directory;
		this.fileName = fileName;
		this.instrument = instrument;
		this.metric = metric;
		this.tagText = tagText;
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

	public String getInstrument() {
		return instrument;
	}

	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}

	public String getMetric() {
		return metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}

	public String getTagText() {
		return tagText;
	}

	public void setTagText(String tagText) {
		this.tagText = tagText;
	}
}
