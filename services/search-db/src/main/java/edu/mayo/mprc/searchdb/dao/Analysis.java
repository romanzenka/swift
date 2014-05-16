package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.StarMatcher;
import edu.mayo.mprc.swift.params2.StarredProteins;
import edu.mayo.mprc.utilities.StringUtilities;
import org.joda.time.DateTime;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Extracted information about how the analysis was performed that should get stored into the LIMS.
 * <p/>
 * Analysis is an equivalent of one Scaffold execution.
 *
 * @author Roman Zenka
 */
public final class Analysis extends PersistableBase {
	private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.00%", DecimalFormatSymbols.getInstance(Locale.US));
	public static final String UNKNOWN_ACCESSION = "???";

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

	public Analysis(final String scaffoldVersion, final DateTime analysisDate, final BiologicalSampleList biologicalSamples) {
		this.scaffoldVersion = scaffoldVersion;
		this.analysisDate = analysisDate;
		this.biologicalSamples = biologicalSamples;
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
				"Biological sample category\t" +
						"Biological sample name\t" +
						"Protein identification probability\t" +
						"Number of Unique Peptides\t" +
						"Number of Unique Spectra\t" +
						"Number of Total Spectra\t" +
						"Percentage of Total Spectra\t" +
						"Percentage Sequence Coverage\n"
		);
		for (final BiologicalSample sample : getBiologicalSamples()) {
			for (final SearchResult result : sample.getSearchResults()) {
				for (final ProteinGroup proteinGroup : result.getProteinGroups()) {
					builder
							.append(sample.getCategory()).append('\t')
							.append(sample.getSampleName()).append('\t')
							.append(percent(proteinGroup.getProteinIdentificationProbability())).append('\t')
							.append(proteinGroup.getNumberOfUniquePeptides()).append('\t')
							.append(proteinGroup.getNumberOfUniqueSpectra()).append('\t')
							.append(proteinGroup.getNumberOfTotalSpectra()).append('\t')
							.append(percent(proteinGroup.getPercentageOfTotalSpectra())).append('\t')
							.append(percent(proteinGroup.getPercentageSequenceCoverage()))
							.append('\n');
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
		if (getScaffoldVersion() != null ? !getScaffoldVersion().equals(analysis.getScaffoldVersion()) : analysis.getScaffoldVersion() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = (getScaffoldVersion() != null ? getScaffoldVersion().hashCode() : 0);
		result = 31 * result + (getAnalysisDate() != null ? getAnalysisDate().hashCode() : 0);
		result = 31 * result + (getBiologicalSamples() != null ? getBiologicalSamples().hashCode() : 0);
		return result;
	}

	/**
	 * Report information about entire analysis into a given writer in HTML format.
	 */
	public void htmlReport(final Report r, final ReportData reportData, final SearchDbDao searchDbDao, final String highlight) {
		r
				.startTable("Scaffold run")
				.addKeyValueTable("Date", getAnalysisDate().toString("YYYY-MM-dd"))
				.addKeyValueTable("Scaffold Version", getScaffoldVersion())
				.endTable();

		r.startTable("Results"); // -- Results

		final SwiftSearchDefinition searchDefinition = searchDbDao.getSearchDefinition(reportData.getId());
		// TODO: Theoretically we can have different starred proteins for two different input files
		final StarredProteins starredProteins =
				searchDefinition != null && searchDefinition.getSearchParameters() != null &&
						searchDefinition.getSearchParameters().getScaffoldSettings() != null ?
						searchDefinition.getSearchParameters().getScaffoldSettings().getStarredProteins() : null;
		final boolean starredColumn = starredProteins != null;
		final StarMatcher matcher;
		if (starredColumn) {
			matcher = starredProteins.getMatcher();
		} else {
			matcher = null;
		}

		// List biological samples
		int totalColumns = 0;
		final ArrayList<String> bioSampleNames = new ArrayList<String>(getBiologicalSamples().size());
		for (final BiologicalSample sample : getBiologicalSamples()) {
			bioSampleNames.add(sample.getSampleName());
			totalColumns += sample.getSearchResults().size();
		}
		final String bioSamplePrefix = StringUtilities.longestPrefix(bioSampleNames);
		r.cell(bioSamplePrefix, 1, null);
		if (starredColumn) {
			r.cell(" ", 1, null);
		}
		final int bioSamplePrefixLength = bioSamplePrefix.length();

		for (final BiologicalSample sample : getBiologicalSamples()) {
			r.cell(sample.getSampleName().substring(bioSamplePrefixLength), sample.getSearchResults().size(), null);
		}
		r.nextRow(); // ---------------

		// List all mass-spec samples within the biological samples
		final ArrayList<String> massSpectSampleFileNames = new ArrayList<String>(20);
		for (final BiologicalSample sample : getBiologicalSamples()) {
			for (final SearchResult result : sample.getSearchResults()) {
				massSpectSampleFileNames.add(result.getMassSpecSample() != null ? result.getMassSpecSample().getFile().getName() : "<null>");
			}
		}
		final String massSpecSamplePrefix = StringUtilities.longestPrefix(massSpectSampleFileNames);
		r.cell(massSpecSamplePrefix);
		if (starredColumn) {
			r.cell(" ", 1, null);
		}
		final int massSpecSamplePrefixLength = massSpecSamplePrefix.length();

		final Iterator<String> iterator = massSpectSampleFileNames.iterator();
		for (final BiologicalSample sample : getBiologicalSamples()) {
			for (final SearchResult ignore : sample.getSearchResults()) {
				final String fileName = iterator.next();
				r.cell(fileName.substring(massSpecSamplePrefixLength));
			}
		}
		r.nextRow(); // ---------------

		/** Load all protein sequence lists */
		final TreeMap<Integer, ProteinSequenceList> allProteinGroups = searchDbDao.getAllProteinSequences(this);
		final Integer databaseId = (searchDefinition != null && searchDefinition.getSearchParameters() != null) &&
				searchDefinition.getSearchParameters().getDatabase() != null ? searchDefinition.getSearchParameters().getDatabase().getId() : null;

		final Map<
				Integer/*protein sequence id*/,
				List<String>/* accession numbers for the protein sequence  */
				>
				accnumMap = searchDbDao.getAccessionNumbersMapForProteinSequences(allProteinGroups.keySet(), databaseId);

		final List<TableRow> tableRows = collectTableRows(allProteinGroups, totalColumns, accnumMap, matcher);
		Collections.sort(tableRows);

		final StringBuilder accNums = new StringBuilder(50);
		final StringBuilder accNumsExtra = new StringBuilder(50);

		for (final TableRow row : tableRows) {
			accNums.setLength(0);
			accNumsExtra.setLength(0);
			boolean first = true;
			boolean hi = false;
			for (final String accNum : row.accnums) {
				if (accNum.equalsIgnoreCase(highlight)) {
					hi = true;
				}
			}
			for (final String accNum : row.accnums) {
				if (first) {
					accNums.append(r.esc(accNum));
				}
				accNumsExtra.append(", ");
				accNumsExtra.append(r.esc(accNum));
				first = false;
			}

			String code;
			if (accNumsExtra.length() > 0) {
				code = "<span title=\"" + accNumsExtra.substring(2) + "\">"
						+ accNums.toString() + "</span>";
			} else {
				code = accNums.toString();
			}

			if (hi) {
				r.hCellRaw(code, "highlight");
			} else {
				r.hCellRaw(code);
			}

			if (starredColumn) {
				r.cell(" ", row.isStar() ? "star" : "no-star");
			}

			for (int i = 0; i < totalColumns; i++) {
				if (row.spectra[i] > 0) {
					r.cell(String.valueOf(row.spectra[i]), "data");
				} else {
					r.cell("");
				}
			}

			r.nextRow();
		}


		r.endTable();
	}

	private List<TableRow> collectTableRows(final TreeMap<Integer, ProteinSequenceList> allProteinGroups, final int totalColumns, final Map<Integer, List<String>> accnumMap, StarMatcher matcher) {
		final List<TableRow> tableRows = new ArrayList<TableRow>(allProteinGroups.size());

		for (final ProteinSequenceList proteinSequences : allProteinGroups.values()) {
			if (proteinSequences == null) {
				// Fix issues with null sequences in the list
				continue;
			}
			final TableRow row = new TableRow();
			final Integer proteinSequencesId = proteinSequences.getId();
			row.accnums = accnumMap.get(proteinSequencesId);
			if (row.accnums == null) {
				row.accnums = new ArrayList<String>(1);
				row.accnums.add(UNKNOWN_ACCESSION);
			} else {
				if (matcher != null) {
					for (final String accnum : row.accnums) {
						if (matcher.matches(accnum)) {
							row.star = true;
							break;
						}
					}
				}
				Collections.sort(row.accnums, new AccnumComparator());
			}
			row.spectra = new int[totalColumns];
			row.totalSpectra = 0;

			int column = 0;
			for (final BiologicalSample sample : getBiologicalSamples()) {
				for (final SearchResult result : sample.getSearchResults()) {
					ProteinGroup matchingGroup = null;
					for (final ProteinGroup g : result.getProteinGroups()) {
						if (proteinSequencesId.equals(g.getProteinSequences().getId())) {
							matchingGroup = g;
							break;
						}
					}
					if (matchingGroup != null) {
						final int spectra = matchingGroup.getNumberOfTotalSpectra();
						row.spectra[column] = spectra;
						row.totalSpectra += spectra;
					} else {
						row.spectra[column] = 0;
					}
					column++;
				}
			}

			tableRows.add(row);
		}
		return tableRows;
	}

	static int compare(final int a, final int b) {
		return a < b ? -1 : a == b ? 0 : 1;
	}

	/**
	 * Rows compare this way: more spectra go before less spectra, if same spectra,
	 * compare by the first column, if same, compare by first accession number
	 */
	static final class TableRow implements Comparable<TableRow> {
		public List<String> accnums;
		public int[] spectra;
		public int totalSpectra;
		public boolean star;

		@Override
		public int compareTo(final TableRow o) {
			if (star == o.star) {
				if (totalSpectra == o.totalSpectra) {
					if (spectra[0] == o.spectra[0]) {
						final String s1 = accnums.get(0);
						final String s2 = o.accnums.get(0);
						return s1.compareTo(s2);
					} else {
						return compare(spectra[0], o.spectra[0]);
					}
				}
				return compare(o.totalSpectra, totalSpectra);
			} else {
				return star ? -1 : 1;
			}
		}

		public boolean isStar() {
			return star;
		}
	}

	private static class AccnumComparator implements Comparator<String> {
		@Override
		public int compare(String o1, String o2) {
			String reverse1 = o1.startsWith("Rev") || o1.startsWith("Random") ? "b" : "a";
			String reverse2 = o2.startsWith("Rev") || o2.startsWith("Random") ? "b" : "a";

			String human1 = o1.contains("_HUMAN") ? "a" : "b";
			String human2 = o2.contains("_HUMAN") ? "a" : "b";

			int result = reverse1.compareTo(reverse2);
			if (result == 0) {
				result = human1.compareTo(human2);
				if (result == 0) {
					result = o1.compareTo(o2);
				}
			}
			return result;
		}
	}
}
