package edu.mayo.mprc.msmseval;

import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.daemon.files.ReceiverTokenTranslator;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;

public final class MsmsEvalResult extends FileHolder implements ProgressInfo {
	private static final long serialVersionUID = 20101221L;

	private File outputFile;
	private File emFile;

	private transient ReceiverTokenTranslator translator;

	public MsmsEvalResult(final File outputFile, final File emFile) {
		this.outputFile = outputFile;
		this.emFile = emFile;
	}

	public File getOutputFile() {
		return outputFile;
	}

	public File getEmFile() {
		return emFile;
	}
}
