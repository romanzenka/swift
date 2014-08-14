package edu.mayo.mprc.msconvert;

import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class MsconvertResult extends FileHolder implements ProgressInfo {
	private static final long serialVersionUID = 20101025l;
	private File outputFile;

	public MsconvertResult(final File outputFile) {
		this.outputFile = outputFile;
	}

	public File getOutputFile() {
		return outputFile;
	}
}
