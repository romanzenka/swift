package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.fastadb.ProteinSequence;
import edu.mayo.mprc.fastadb.ProteinSequenceTranslator;
import edu.mayo.mprc.searchdb.dao.*;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Roman Zenka
 */
public class AnalysisBuilder implements Builder<Analysis> {
	/**
	 * To translate protein accession numbers into protein sequences.
	 */
	private ProteinSequenceTranslator translator;

	/**
	 * Give a name of a .RAW file, extracts various metadata.
	 */
	private MassSpecDataExtractor massSpecDataExtractor;

	/**
	 * Which search report (read = Scaffold file) does this Analysis link to?
	 */
	private ReportData reportData;

	/**
	 * Scaffold version as a string. Can be null if the version could not be determined.
	 */
	private String scaffoldVersion;

	/**
	 * Date and time of when the analysis was run, as recorded in the report. This information
	 * is somewhat duplicated (we know when the user submitted the search).
	 * It can be null if the date could not be determined.
	 */
	private DateTime analysisDate;

	/**
	 * A list of all biological samples defined within the Scaffold analysis report.
	 */
	private BiologicalSampleListBuilder biologicalSamples = new BiologicalSampleListBuilder(this);

	/**
	 * Cache of all protein sequences.
	 */
	private Map<String/*accnum*/, ProteinSequence> proteinSequencesByAccnum = new HashMap<String, ProteinSequence>();

	/**
	 * Cache of all protein sequences by sequence.
	 */
	private Map<String/*sequence*/, ProteinSequence> proteinSequencesBySequence = new HashMap<String, ProteinSequence>();

	private Analysis analysis;

	public AnalysisBuilder(final ProteinSequenceTranslator translator, final MassSpecDataExtractor massSpecDataExtractor) {
		this.translator = translator;
		this.massSpecDataExtractor = massSpecDataExtractor;
	}

	public Analysis getAnalysis() {
		return analysis;
	}

	@Override
	public Analysis build() {
		if (analysis != null) {
			throw new MprcException("Analysis cannot be built more than once");
		}
		analysis = new Analysis(scaffoldVersion, analysisDate, biologicalSamples.build());
		return analysis;
	}

	/**
	 * @return All protein sequence, identical sequence will be reported just once.
	 */
	public Collection<ProteinSequence> getProteinSequences() {
		return proteinSequencesBySequence.values();
	}

	ProteinSequence getProteinSequence(final String accessionNumber, final String databaseSources) {
		final ProteinSequence proteinSequence = proteinSequencesByAccnum.get(accessionNumber);
		if (proteinSequence == null) {
			final ProteinSequence newProteinSequence = translator.getProteinSequence(accessionNumber, databaseSources);
			ProteinSequence result = proteinSequencesBySequence.get(newProteinSequence.getSequence());
			if (result == null) {
				proteinSequencesBySequence.put(newProteinSequence.getSequence(), newProteinSequence);
				result = newProteinSequence;
			}
			proteinSequencesByAccnum.put(accessionNumber, result);
			return result;
		}
		return proteinSequence;
	}

	/**
	 * @return A list of all {@link ProteinSequenceList} objects in the Analysis, each listed only once.
	 *         The duplicities are resolved within the existing analysis.
	 */
	public Collection<ProteinSequenceList> calculateProteinSequenceLists() {
		final LinkedHashMap<ProteinSequenceList, ProteinSequenceList> map = new LinkedHashMap<ProteinSequenceList, ProteinSequenceList>();

		for (final BiologicalSample sample : getAnalysis().getBiologicalSamples()) {
			for (final SearchResult result : sample.getSearchResults()) {
				for (final ProteinGroup proteinGroup : result.getProteinGroups()) {
					final ProteinSequenceList list = proteinGroup.getProteinSequences();
					list.calculateHash();
					final ProteinSequenceList existing = map.get(list);
					if (existing == null) {
						map.put(list, list);
					} else {
						proteinGroup.setProteinSequences(existing);
					}
				}
			}
		}
		return map.values();
	}


	public ReportData getReportData() {
		return reportData;
	}

	public void setReportData(final ReportData reportData) {
		this.reportData = reportData;
	}

	public String getScaffoldVersion() {
		return scaffoldVersion;
	}

	public void setScaffoldVersion(final String scaffoldVersion) {
		this.scaffoldVersion = scaffoldVersion;
	}

	public DateTime getAnalysisDate() {
		return analysisDate;
	}

	public void setAnalysisDate(final DateTime analysisDate) {
		this.analysisDate = analysisDate;
	}

	public BiologicalSampleListBuilder getBiologicalSamples() {
		return biologicalSamples;
	}

	public ProteinSequenceTranslator getTranslator() {
		return translator;
	}

	public MassSpecDataExtractor getMassSpecDataExtractor() {
		return massSpecDataExtractor;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final AnalysisBuilder that = (AnalysisBuilder) o;

		if (analysisDate != null ? !analysisDate.equals(that.analysisDate) : that.analysisDate != null) {
			return false;
		}
		if (biologicalSamples != null ? !biologicalSamples.equals(that.biologicalSamples) : that.biologicalSamples != null) {
			return false;
		}
		if (reportData != null ? !reportData.equals(that.reportData) : that.reportData != null) {
			return false;
		}
		if (scaffoldVersion != null ? !scaffoldVersion.equals(that.scaffoldVersion) : that.scaffoldVersion != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = reportData != null ? reportData.hashCode() : 0;
		result = 31 * result + (scaffoldVersion != null ? scaffoldVersion.hashCode() : 0);
		result = 31 * result + (analysisDate != null ? analysisDate.hashCode() : 0);
		result = 31 * result + (biologicalSamples != null ? biologicalSamples.hashCode() : 0);
		return result;
	}
}
