package edu.mayo.mprc.idpicker;

import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class IdpickerResult extends FileHolder implements ProgressInfo {
	private static final long serialVersionUID = 20101025l;
	private File idpFile;

	public IdpickerResult(final File idpFile) {
		this.idpFile = idpFile;
	}

	public File getIdpFile() {
		return idpFile;
	}
}
