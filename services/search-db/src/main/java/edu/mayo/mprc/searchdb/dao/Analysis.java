package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.searchdb.Report;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import org.joda.time.DateTime;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Extracted information about how the analysis was performed that should get stored into the LIMS.
 * <p/>
 * Analysis is an equivalent of one Scaffold execution.
 *
 * @author Roman Zenka
 */
public final class Analysis extends PersistableBase {
	private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.00%", DecimalFormatSymbols.getInstance(Locale.US));

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
	private BiologicalSampleList biologicalSamples;

	/**
	 * Empty constructor for Hibernate.
	 */
	public Analysis() {
	}

	public Analysis(final ReportData reportData, final String scaffoldVersion, final DateTime analysisDate, final BiologicalSampleList biologicalSamples) {
		this.reportData = reportData;
		this.scaffoldVersion = scaffoldVersion;
		this.analysisDate = analysisDate;
		this.biologicalSamples = biologicalSamples;
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

	public BiologicalSampleList getBiologicalSamples() {
		return biologicalSamples;
	}

	public void setBiologicalSamples(final BiologicalSampleList biologicalSamples) {
		this.biologicalSamples = biologicalSamples;
	}

	/**
	 * Emulates the Scaffold's peptide report.
	 *
	 * @return String similar to Scaffold's peptide report. For testing mostly.
	 */
	public String peptideReport() {
		final StringBuilder builder = new StringBuilder();
		builder.append(
				"Experiment name\t" +
						"Biological sample category\t" +
						"Biological sample name\t" +
						"MS/MS sample name\t" +
						"Protein name\t" +
						"Protein accession numbers\t" +
						"Database sources\t" +
						"Protein molecular weight (Da)\t" +
						"Protein identification probability\t" +
						"Number of Unique Peptides\t" +
						"Number of Unique Spectra\t" +
						"Number of Total Spectra\t" +
						"Percentage of Total Spectra\t" +
						"Percentage Sequence Coverage\t" +
						"Peptide Sequence\t" +
						"Peptide Modifications\t" +
						"Previous Amino Acid\t" +
						"Next Amino Acid\t" +
						"Best Peptide Identification Probability\t" +
						"Best SEQUEST XCorr score\t" +
						"Best SEQUEST DCn score\t" +
						"Best Mascot Ion score\t" +
						"Best Mascot Identity score\t" +
						"Best Mascot Delta Ion score\t" +
						"Best X! Tandem -log(e) score\t" +
						"Number of identified +1H spectra\t" +
						"Number of identified +2H spectra\t" +
						"Number of identified +3H spectra\t" +
						"Number of identified +4H spectra\t" +
						"Number of enzymatic termini\n");
		for (final BiologicalSample sample : getBiologicalSamples()) {
			for (final SearchResult result : sample.getSearchResults()) {
				for (final ProteinGroup proteinGroup : result.getProteinGroups()) {
					for (final PeptideSpectrumMatch psm : proteinGroup.getPeptideSpectrumMatches()) {
						builder
								.append("").append('\t')
								.append(sample.getCategory()).append('\t')
								.append(sample.getSampleName()).append('\t')
								.append("").append('\t')
								.append("").append('\t')
								.append("").append('\t')
								.append("").append('\t')
								.append("").append('\t')
								.append(percent(proteinGroup.getProteinIdentificationProbability())).append('\t')
								.append(proteinGroup.getNumberOfUniquePeptides()).append('\t')
								.append(proteinGroup.getNumberOfUniqueSpectra()).append('\t')
								.append(proteinGroup.getNumberOfTotalSpectra()).append('\t')
								.append(percent(proteinGroup.getPercentageOfTotalSpectra())).append('\t')
								.append(percent(proteinGroup.getPercentageSequenceCoverage())).append('\t')
								.append(psm.getPeptide().getSequence().getSequence()).append('\t')
								.append(psm.getPeptide().getModificationsAsString()).append('\t')
								.append(psm.getPreviousAminoAcid()).append('\t')
								.append(psm.getNextAminoAcid()).append('\t')
								.append(psm.getBestPeptideIdentificationProbability()).append('\t')
								.append(psm.getBestSearchEngineScores().getSequestXcorrScore()).append('\t')
								.append(psm.getBestSearchEngineScores().getSequestDcnScore()).append('\t')
								.append(psm.getBestSearchEngineScores().getMascotIonScore()).append('\t')
								.append(psm.getBestSearchEngineScores().getMascotIdentityScore()).append('\t')
								.append(psm.getBestSearchEngineScores().getMascotDeltaIonScore()).append('\t')
								.append(psm.getBestSearchEngineScores().getTandemHyperScore()).append('\t')
								.append(psm.getSpectrumIdentificationCounts().getNumberOfIdentified1HSpectra()).append('\t')
								.append(psm.getSpectrumIdentificationCounts().getNumberOfIdentified2HSpectra()).append('\t')
								.append(psm.getSpectrumIdentificationCounts().getNumberOfIdentified3HSpectra()).append('\t')
								.append(psm.getSpectrumIdentificationCounts().getNumberOfIdentified4HSpectra()).append('\t')
								.append(psm.getNumberOfEnzymaticTerminii()).append('\n');
					}
				}
			}
		}
		return builder.toString();
	}

	private String percent(final double percent) {
		return PERCENT_FORMAT.format(percent);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof Analysis)) {
			return false;
		}

		final Analysis analysis = (Analysis) o;

		if (getAnalysisDate() != null ? !getAnalysisDate().equals(analysis.getAnalysisDate()) : analysis.getAnalysisDate() != null) {
			return false;
		}
		if (getBiologicalSamples() != null ? !getBiologicalSamples().equals(analysis.getBiologicalSamples()) : analysis.getBiologicalSamples() != null) {
			return false;
		}
		if (getReportData() != null ? !getReportData().equals(analysis.getReportData()) : analysis.getReportData() != null) {
			return false;
		}
		if (getScaffoldVersion() != null ? !getScaffoldVersion().equals(analysis.getScaffoldVersion()) : analysis.getScaffoldVersion() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getReportData() != null ? getReportData().hashCode() : 0;
		result = 31 * result + (getScaffoldVersion() != null ? getScaffoldVersion().hashCode() : 0);
		result = 31 * result + (getAnalysisDate() != null ? getAnalysisDate().hashCode() : 0);
		result = 31 * result + (getBiologicalSamples() != null ? getBiologicalSamples().hashCode() : 0);
		return result;
	}

	/**
	 * Report information about entire analysis into a given writer in HTML format.
	 */
	public void htmlReport(final Report r, final SearchDbDao searchDbDao, final String highlight) {
		r
				.startTable("Scaffold run")
				.addKeyValueTable("Date", getAnalysisDate())
				.addKeyValueTable("Scaffold Version", getScaffoldVersion())
				.endTable();

		r.startTable("Results") // -- Results
				.cell("", 1);

		final TreeMap<Integer, ProteinSequenceList> allProteinGroups = new TreeMap<Integer, ProteinSequenceList>();

		// List biological samples
		for (final BiologicalSample sample : getBiologicalSamples()) {
			r.cell(sample.getSampleName(), sample.getSearchResults().size());

			for (final SearchResult result : sample.getSearchResults()) {
				for (final ProteinGroup group : result.getProteinGroups()) {
					allProteinGroups.put(group.getProteinSequences().getId(), group.getProteinSequences());
				}
			}
		}
		r.nextRow(); // ---------------

		r.cell("");

		// List all mass-spec samples within the biological samples
		for (final BiologicalSample sample : getBiologicalSamples()) {
			for (final SearchResult result : sample.getSearchResults()) {
				r.cell(result.getMassSpecSample() != null ? result.getMassSpecSample().getFile().getName() : "<null>");
			}
		}
		r.nextRow(); // ---------------

		final StringBuilder accNums = new StringBuilder(50);
		final Map<Integer/*protein sequence id*/, List<String>/* accession numbers for the protein sequence  */> accnumMap
				= searchDbDao.getAccessionNumbersMapForAnalysis(this);
		for (final ProteinSequenceList proteinSequences : allProteinGroups.values()) {
			final List<String> proteinAccessionNumbers = accnumMap.get(proteinSequences.getId());
			accNums.setLength(0);
			for (final String accNum : proteinAccessionNumbers) {
				if (accNum.equalsIgnoreCase(highlight)) {
					accNums.append("<span class=\"highlight\">" + r.esc(accNum) + "</span>, ");
				} else {
					accNums.append(r.esc(accNum)).append(", ");

				}
			}
			r.hCellRaw(accNums.substring(0, 2 <= accNums.length() ? accNums.length() - 2 : accNums.length()));

			for (final BiologicalSample sample : getBiologicalSamples()) {
				for (final SearchResult result : sample.getSearchResults()) {
					ProteinGroup matchingGroup = null;
					for (final ProteinGroup g : result.getProteinGroups()) {
						if (proteinSequences.getId().equals(g.getProteinSequences().getId())) {
							matchingGroup = g;
							break;
						}
					}
					if (matchingGroup != null) {
						r.cell(String.valueOf(matchingGroup.getNumberOfTotalSpectra()));
					} else {
						r.cell("");
					}
				}
			}

			r.nextRow();
		}


		r.endTable();
	}
}
