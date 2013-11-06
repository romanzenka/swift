package edu.mayo.mprc.swift.search.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Collection of QA input files. Object may contain File objects or FileToken objects.
 */
final class QaTaskInputFiles {
	// The original .RAW file
	private File rawInputFile;

	// Spectrum QA information (msmsEval)
	private SpectrumQaTask spectrumQa;

	// RAW file information
	private RAWDumpTask rawDump;

	// Additional engine search
	private List<EngineSearchTask> additionalSearches = new ArrayList<EngineSearchTask>(1);

	QaTaskInputFiles() {
	}

	public File getRawInputFile() {
		return rawInputFile;
	}

	public void setRawInputFile(final File rawInputFile) {
		this.rawInputFile = rawInputFile;
	}

	public SpectrumQaTask getSpectrumQa() {
		return spectrumQa;
	}

	public void setSpectrumQa(final SpectrumQaTask spectrumQa) {
		this.spectrumQa = spectrumQa;
	}

	public RAWDumpTask getRawDump() {
		return rawDump;
	}

	public void setRawDump(final RAWDumpTask rawDump) {
		this.rawDump = rawDump;
	}

	public void addAdditionalSearch(final EngineSearchTask task) {
		additionalSearches.add(task);
	}

	public List<EngineSearchTask> getAdditionalSearches() {
		return additionalSearches;
	}
}
