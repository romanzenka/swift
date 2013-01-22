package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.utilities.ComparisonChain;

import java.io.File;
import java.util.LinkedHashMap;

class QaTaskExperiment implements Comparable<QaTaskExperiment> {
	private final String name;
	// Scaffold spectrum report in .tsv format
	private final File spectraFile;
	// Map from each .mgf file to a set of data files with more detailed information about that .mgf
	private final LinkedHashMap<FileProducingTask, QaTaskInputFiles> mgfToQaMap = new LinkedHashMap<FileProducingTask, QaTaskInputFiles>();
	// Scaffold version
	private final String scaffoldVersion;

	QaTaskExperiment(final String name, final File spectraFile, final String scaffoldVersion) {
		this.name = name;
		this.spectraFile = spectraFile;
		this.scaffoldVersion = scaffoldVersion;
	}

	public File getSpectraFile() {
		return spectraFile;
	}

	public LinkedHashMap<FileProducingTask, QaTaskInputFiles> getMgfToQaMap() {
		return mgfToQaMap;
	}

	public void addMgfToRawEntry(final FileProducingTask mgfFile, final File rawFile, final RAWDumpTask rawDumpTask) {
		synchronized (mgfToQaMap) {

			QaTaskInputFiles qaInputFiles = null;

			if ((qaInputFiles = mgfToQaMap.get(mgfFile)) == null) {
				qaInputFiles = new QaTaskInputFiles();
				mgfToQaMap.put(mgfFile, qaInputFiles);
			}

			qaInputFiles.setRawInputFile(rawFile);
			qaInputFiles.setRawDump(rawDumpTask);
		}
	}

	public void addMgfToMsmsEvalEntry(final FileProducingTask mgfFile, final SpectrumQaTask spectrumQaTask) {
		synchronized (mgfToQaMap) {

			QaTaskInputFiles qaInputFiles = null;

			if ((qaInputFiles = mgfToQaMap.get(mgfFile)) == null) {
				qaInputFiles = new QaTaskInputFiles();
				mgfToQaMap.put(mgfFile, qaInputFiles);
			}

			qaInputFiles.setSpectrumQa(spectrumQaTask);
		}
	}

	public void addAdditionalSearchEntry(final FileProducingTask mgfFile, final EngineSearchTask engineSearchTask) {
		synchronized (mgfToQaMap) {

			QaTaskInputFiles qaInputFiles = null;

			if ((qaInputFiles = mgfToQaMap.get(mgfFile)) == null) {
				qaInputFiles = new QaTaskInputFiles();
				mgfToQaMap.put(mgfFile, qaInputFiles);
			}

			qaInputFiles.addAdditionalSearch(engineSearchTask);
		}
	}

	public String getName() {
		return name;
	}

	public String getScaffoldVersion() {
		return scaffoldVersion;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final QaTaskExperiment that = (QaTaskExperiment) o;

		if (name != null ? !name.equals(that.name) : that.name != null) {
			return false;
		}
		if (scaffoldVersion != null ? !scaffoldVersion.equals(that.scaffoldVersion) : that.scaffoldVersion != null) {
			return false;
		}
		if (spectraFile != null ? !spectraFile.equals(that.spectraFile) : that.spectraFile != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (spectraFile != null ? spectraFile.hashCode() : 0);
		result = 31 * result + (scaffoldVersion != null ? scaffoldVersion.hashCode() : 0);
		return result;
	}

	@Override
	public int compareTo(final QaTaskExperiment o) {
		return ComparisonChain.start()
				.compare(this.name, o.name)
				.compare(this.spectraFile, o.spectraFile)
				.compare(this.scaffoldVersion, o.scaffoldVersion)
				.result();
	}
}
