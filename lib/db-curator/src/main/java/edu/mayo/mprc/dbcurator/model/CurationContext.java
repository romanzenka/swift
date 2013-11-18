package edu.mayo.mprc.dbcurator.model;

import java.io.File;

/**
 * Configuration for the db-curator.
 */
public final class CurationContext {
	private File fastaFolder;
	private File fastaUploadFolder;
	private File fastaArchiveFolder;
	private File localTempFolder;

	public CurationContext() {
	}

	public void initialize(final File fastaFolder, final File fastaUploadFolder, final File fastaArchiveFolder, final File localTempFolder) {
		this.fastaFolder = fastaFolder;
		this.fastaUploadFolder = fastaUploadFolder;
		this.fastaArchiveFolder = fastaArchiveFolder;
		this.localTempFolder = localTempFolder;
	}

	public File getFastaFolder() {
		return fastaFolder;
	}

	public File getFastaUploadFolder() {
		return fastaUploadFolder;
	}

	public File getFastaArchiveFolder() {
		return fastaArchiveFolder;
	}

	public File getLocalTempFolder() {
		return localTempFolder;
	}
}
