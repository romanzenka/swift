package edu.mayo.mprc.searchdb.builder;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;
import edu.mayo.mprc.searchdb.dao.ProteinGroup;
import edu.mayo.mprc.searchdb.dao.ProteinGroupList;
import edu.mayo.mprc.searchdb.dao.ProteinSequenceList;

import java.util.*;

/**
 * @author Roman Zenka
 */
public class ProteinGroupListBuilder implements Builder<ProteinGroupList> {

	private SearchResultBuilder searchResult;

	private Map<String, ProteinGroupBuilder> list = new LinkedHashMap<String, ProteinGroupBuilder>();

	public ProteinGroupListBuilder(final SearchResultBuilder searchResult) {
		this.searchResult = searchResult;
	}

	@Override
	public ProteinGroupList build() {
		final List<ProteinGroup> items = new ArrayList<ProteinGroup>(list.size());
		for (final ProteinGroupBuilder builder : list.values()) {
			items.add(builder.build());
		}
		return new ProteinGroupList(items);
	}

	/**
	 * Get current protein group for a tandem mass spec sample within a biological sample.
	 * If no such group is defined yet, create a new one and add it to the {@link edu.mayo.mprc.searchdb.dao.SearchResult}.
	 * <p/>
	 * All the additional parameters should depend on the accession numbers as the primary key for the protein group.
	 * Check this for consistency and throw exceptions when the file is suspected to be corrupted.
	 *
	 * @param proteinAccessionNumbers    List of protein accession numbers. The first one is the reference, preferred one.
	 * @param databaseSources            List of all protein .fasta databases used as a source for the protein sequences. We currently expect exactly one database here.
	 * @param numberOfTotalSpectra       How many spectra in the group total.
	 * @param numberOfUniquePeptides     How many unique peptides in the group. Unique - different mods.
	 * @param numberOfUniqueSpectra      How many unique spectra - belonging to different peptides/mods or different charge.
	 * @param percentageOfTotalSpectra   How many percent of the total spectra assigned to this group (spectral counting)
	 * @param percentageSequenceCoverage How many percent of the sequence are covered.
	 * @param proteinIdentificationProbability
	 *                                   What is the calculated probability that this protein is identified correctly.
	 * @return Current protein group.
	 */
	public ProteinGroupBuilder getProteinGroup(final CharSequence proteinAccessionNumbers,
	                                           final String databaseSources,
	                                           final int numberOfTotalSpectra,
	                                           final int numberOfUniquePeptides, final int numberOfUniqueSpectra,
	                                           final double percentageOfTotalSpectra, final double percentageSequenceCoverage,
	                                           final double proteinIdentificationProbability) {
		// Canonicalize the protein accession numbers- just in case
		final String[] accNums = Iterables.toArray(ScaffoldReportReader.PROTEIN_ACCESSION_SPLITTER.split(proteinAccessionNumbers), String.class);

		Arrays.sort(accNums, String.CASE_INSENSITIVE_ORDER);
		final String canonicalizedAccNums = Joiner.on(',').join(accNums);
		final String key = canonicalizedAccNums + "\t" + databaseSources;

		final ProteinGroupBuilder proteinGroup = list.get(key);
		if (proteinGroup == null) {
			final ProteinGroupBuilder newProteinGroup = new ProteinGroupBuilder(searchResult,
					proteinIdentificationProbability, numberOfUniquePeptides, numberOfUniqueSpectra,
					numberOfTotalSpectra, percentageOfTotalSpectra, percentageSequenceCoverage);
			addProteinSequences(accNums, databaseSources, newProteinGroup);

			list.put(key, newProteinGroup);
			return newProteinGroup;
		}

		final BiologicalSampleBuilder biologicalSample = searchResult.getBiologicalSample();

		// Make sure that two consecutive lines for the same protein group have all values matching to what we already extracted
		checkConsistencyWithinSample(biologicalSample, "number of total spectra", proteinGroup.getNumberOfTotalSpectra(), numberOfTotalSpectra);
		checkConsistencyWithinSample(biologicalSample, "number of unique peptides", proteinGroup.getNumberOfUniquePeptides(), numberOfUniquePeptides);
		checkConsistencyWithinSample(biologicalSample, "number of unique spectra", proteinGroup.getNumberOfUniqueSpectra(), numberOfUniqueSpectra);
		checkConsistencyWithinSample(biologicalSample, "percentage of total spectra", proteinGroup.getPercentageOfTotalSpectra(), percentageOfTotalSpectra);
		checkConsistencyWithinSample(biologicalSample, "percentage of sequence coverage", proteinGroup.getPercentageSequenceCoverage(), percentageSequenceCoverage);
		checkConsistencyWithinSample(biologicalSample, "protein identification probability", proteinGroup.getProteinIdentificationProbability(), proteinIdentificationProbability);
		return proteinGroup;
	}

	private void addProteinSequences(final String[] accNums, final String databaseSources, final ProteinGroupBuilder newProteinGroup) {
		final AnalysisBuilder analysis = searchResult.getBiologicalSample().getAnalysis();
		final ProteinSequenceList proteinSequences = new ProteinSequenceList(accNums.length);
		for (final String accessionNumber : accNums) {
			proteinSequences.add(analysis.getProteinSequence(accessionNumber, databaseSources));
		}
		newProteinGroup.setProteinSequences(proteinSequences);
	}

	private void checkConsistencyWithinSample(final BiologicalSampleBuilder biologicalSample, final String column, final int previousValue, final int currentValue) {
		checkConsistencyWithinSample(biologicalSample, column, String.valueOf(previousValue), String.valueOf(currentValue));
	}

	private void checkConsistencyWithinSample(final BiologicalSampleBuilder biologicalSample, final String column, final double previousValue, final double currentValue) {
		checkConsistencyWithinSample(biologicalSample, column, String.valueOf(previousValue), String.valueOf(currentValue));
	}

	private void checkConsistencyWithinSample(final BiologicalSampleBuilder biologicalSample, final String column, final String previousValue, final String currentValue) {
		if (!Objects.equal(previousValue, currentValue)) {
			throw new MprcException("The protein group for biological sample [" + biologicalSample.getSampleName() + "] has conflicting " + column + " value, was previously [" + previousValue + "] now is [" + currentValue + "]");
		}
	}
}
