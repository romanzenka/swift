package edu.mayo.mprc.swift.ui.client.rpc;

import edu.mayo.mprc.swift.dbmapping.FileSearch;

import java.io.Serializable;

/**
 * One entry in the table of files. UI equivalent of {@link FileSearch}
 *
 * @author Roman Zenka
 */
public final class ClientFileSearch implements Serializable {
	private static final long serialVersionUID = 20111119L;
	/**
	 * This is a /-separated path relative to the "raw file root" (eg: instruments/foo/bar.RAW)
	 */
	private String path;
	private String biologicalSample;
	private String categoryName;
	private String experiment;
	// Optional - does not have to be filled in. Used to transfer file sizes when search is loaded
	// When negative, it means the file could not be found on the filesystem
	private Long fileSize;

	public ClientFileSearch() {
	}

	/**
	 * Creates new file table entry.
	 *
	 * @param inputFilePath    Path to the input file (relative to browse root}
	 * @param biologicalSample Name of the biological sample.
	 * @param categoryName     Name of the category.
	 * @param experiment       Name of the experiment.
	 */
	public ClientFileSearch(final String inputFilePath, final String biologicalSample, final String categoryName, final String experiment, final Long fileSize) {
		path = inputFilePath;
		this.biologicalSample = biologicalSample;
		this.categoryName = categoryName;
		this.experiment = experiment;
		this.fileSize = fileSize;
	}

	public String getPath() {
		return path;
	}

	public String getBiologicalSample() {
		return biologicalSample;
	}

	public String getExperiment() {
		return experiment;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public Long getFileSize() {
		return fileSize;
	}
}
