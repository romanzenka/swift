package edu.mayo.mprc.raw2mgf;

import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;

public final class RawToMgfResult extends FileHolder implements ProgressInfo {
	private static final long serialVersionUID = 20101025l;
	private File mgf;

	public RawToMgfResult(final File mgfFile) {
		mgf = mgfFile;
	}

	public File getMgf() {
		return mgf;
	}
}
