package edu.mayo.mprc.qa;

import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;

/**
 * Result of the RAW dump operation - a slew of files containing metadata about the .RAW file.
 */
public final class RAWDumpResult extends FileHolder implements ProgressInfo {
	private static final long serialVersionUID = 20101221L;
	private File rawInfoFile;
	private File rawSpectraFile;
	private File chromatogramFile;
	private File tuneMethodFile;
	private File instrumentMethodFile;
	private File sampleInformationFile;
	private File errorLogFile;
	private File uvDataFile;

	public RAWDumpResult(final File rawInfoFile, final File rawSpectraFile,
	                     final File chromatogramFile, final File tuneMethodFile, final File instrumentMethodFile,
	                     final File sampleInformationFile, final File errorLogFile,
	                     final File uvDataFile) {
		this.rawInfoFile = rawInfoFile;
		this.rawSpectraFile = rawSpectraFile;
		this.chromatogramFile = chromatogramFile;
		this.tuneMethodFile = tuneMethodFile;
		this.instrumentMethodFile = instrumentMethodFile;
		this.sampleInformationFile = sampleInformationFile;
		this.errorLogFile = errorLogFile;
		this.uvDataFile = uvDataFile;
	}

	public File getRawInfoFile() {
		return rawInfoFile;
	}

	public File getRawSpectraFile() {
		return rawSpectraFile;
	}

	public File getChromatogramFile() {
		return chromatogramFile;
	}

	public File getTuneMethodFile() {
		return tuneMethodFile;
	}

	public File getInstrumentMethodFile() {
		return instrumentMethodFile;
	}

	public File getSampleInformationFile() {
		return sampleInformationFile;
	}

	public File getErrorLogFile() {
		return errorLogFile;
	}

	public File getUvDataFile() {
		return uvDataFile;
	}
}
