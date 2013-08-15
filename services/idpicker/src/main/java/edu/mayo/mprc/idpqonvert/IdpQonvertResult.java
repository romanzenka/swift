package edu.mayo.mprc.idpqonvert;

import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class IdpQonvertResult extends FileHolder implements ProgressInfo {
	private static final long serialVersionUID = 20101025l;
	private File idpFile;

	public IdpQonvertResult(final File idpFile) {
		this.idpFile = idpFile;
	}

	public File getIdpFile() {
		return idpFile;
	}
}
