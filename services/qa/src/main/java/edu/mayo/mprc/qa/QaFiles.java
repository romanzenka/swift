package edu.mayo.mprc.qa;

import edu.mayo.mprc.daemon.files.FileHolder;

import java.io.File;
import java.util.HashMap;

/**
 * Links an input file to all the related metadata.
 */
public final class QaFiles extends FileHolder {
	private static final long serialVersionUID = 20101221L;

	// Mgf/mzML file
	private File inputFile;

	// The original .RAW file
	private File rawInputFile;

	// msmsEval information produced from the .mgf file that corresponds to the original .RAW file
	private File msmsEvalOutputFile;

	// Information about the .RAW file as a whole - contains parameters on the file level
	private File rawInfoFile;

	// Per-spectrum information for the .RAW file
	private File rawSpectraFile;

	// Picture of the MS spectra chromatogram
	private File chromatogramFile;

	/**
	 * Map of Engine ID -> engine search result file.
	 */
	private HashMap<String, File> additionalSearchResults = new HashMap<String, File>(1);

	public QaFiles() {
	}

	public File getInputFile() {
		return inputFile;
	}

	public void setInputFile(final File inputFile) {
		this.inputFile = inputFile;
	}

	public File getRawInputFile() {
		return rawInputFile;
	}

	public void setRawInputFile(final File rawInputFile) {
		this.rawInputFile = rawInputFile;
	}

	public File getMsmsEvalOutputFile() {
		return msmsEvalOutputFile;
	}

	public void setMsmsEvalOutputFile(final File msmsEvalOutputFile) {
		this.msmsEvalOutputFile = msmsEvalOutputFile;
	}

	public File getRawInfoFile() {
		return rawInfoFile;
	}

	public void setRawInfoFile(final File rawInfoFile) {
		this.rawInfoFile = rawInfoFile;
	}

	public File getRawSpectraFile() {
		return rawSpectraFile;
	}

	public void setRawSpectraFile(final File rawSpectraFile) {
		this.rawSpectraFile = rawSpectraFile;
	}

	public File getChromatogramFile() {
		return chromatogramFile;
	}

	public void setChromatogramFile(final File chromatogramFile) {
		this.chromatogramFile = chromatogramFile;
	}

	public HashMap<String, File> getAdditionalSearchResults() {
		return additionalSearchResults;
	}

	public void addAdditionalSearchResult(final String searchEngineCode, final File resultingFile) {
		additionalSearchResults.put(searchEngineCode, resultingFile);
	}
}
