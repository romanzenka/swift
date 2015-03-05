package edu.mayo.mprc.searchdb.builder;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;
import edu.mayo.mprc.searchdb.dao.ProteinGroup;
import edu.mayo.mprc.searchdb.dao.ProteinGroupList;

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
	 * @param proteinAccessionNumbers          List of protein accession numbers. The first one is the reference, preferred one.
	 * @param numberOfTotalSpectra             How many spectra in the group total.
	 * @param numberOfUniquePeptides           How many unique peptides in the group. Unique - different mods.
	 * @param numberOfUniqueSpectra            How many unique spectra - belonging to different peptides/mods or different charge.
	 * @param percentageOfTotalSpectra         How many percent of the total spectra assigned to this group (spectral counting)
	 * @param percentageSequenceCoverage       How many percent of the sequence are covered.
	 * @param proteinIdentificationProbability What is the calculated probability that this protein is identified correctly.
	 * @return Current protein group.
	 */
	public ProteinGroupBuilder getProteinGroup(final CharSequence proteinAccessionNumbers,
	                                           final int numberOfTotalSpectra,
	                                           final int numberOfUniquePeptides, final int numberOfUniqueSpectra,
	                                           final double percentageOfTotalSpectra, final double percentageSequenceCoverage,
	                                           final double proteinIdentificationProbability) {
		// Canonicalize the protein accession numbers- just in case
		final ArrayList<String> listAccNums = Lists.newArrayList(ScaffoldReportReader.PROTEIN_ACCESSION_SPLITTER.split(proteinAccessionNumbers));
		Collections.sort(listAccNums, String.CASE_INSENSITIVE_ORDER);
		final String canonicalizedAccNums = Joiner.on(',').join(listAccNums);

		final ProteinGroupBuilder proteinGroup = list.get(canonicalizedAccNums);
		if (proteinGroup == null) {
			final ProteinGroupBuilder newProteinGroup = new ProteinGroupBuilder(searchResult,
					proteinIdentificationProbability, numberOfUniquePeptides, numberOfUniqueSpectra,
					numberOfTotalSpectra, percentageOfTotalSpectra, percentageSequenceCoverage);
			addProteinSequences(listAccNums, newProteinGroup);

			list.put(canonicalizedAccNums, newProteinGroup);
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

	public void collectAccnums(final Set<String> accnums) {
		for (final ProteinGroupBuilder builder : list.values()) {
			builder.collectAccnums(accnums);
		}
	}

	private void addProteinSequences(final Collection<String> accNums, final ProteinGroupBuilder newProteinGroup) {
		// This kills performance as we do a ton of database queries to translate the sequences
		final AnalysisBuilder analysis = searchResult.getBiologicalSample().getAnalysis();
		final ProteinSequenceListBuilder proteinSequences = new ProteinSequenceListBuilder(accNums, analysis);
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
