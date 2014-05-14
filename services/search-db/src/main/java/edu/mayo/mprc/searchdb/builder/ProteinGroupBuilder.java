package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.fastadb.ProteinSequence;
import edu.mayo.mprc.searchdb.dao.ProteinGroup;
import edu.mayo.mprc.searchdb.dao.ProteinSequenceList;

import java.util.Locale;

/**
 * @author Roman Zenka
 */
public class ProteinGroupBuilder implements Builder<ProteinGroup> {
	private SearchResultBuilder searchResult;

	private ProteinSequenceList proteinSequences;
	private double proteinIdentificationProbability;
	private int numberOfUniquePeptides;
	private int numberOfUniqueSpectra;
	private int numberOfTotalSpectra;
	private double percentageOfTotalSpectra;
	private double percentageSequenceCoverage;

	public ProteinGroupBuilder(final SearchResultBuilder searchResult, final double proteinIdentificationProbability, final int numberOfUniquePeptides, final int numberOfUniqueSpectra, final int numberOfTotalSpectra, final double percentageOfTotalSpectra, final double percentageSequenceCoverage) {
		this.searchResult = searchResult;
		this.proteinIdentificationProbability = proteinIdentificationProbability;
		this.numberOfUniquePeptides = numberOfUniquePeptides;
		this.numberOfUniqueSpectra = numberOfUniqueSpectra;
		this.numberOfTotalSpectra = numberOfTotalSpectra;
		this.percentageOfTotalSpectra = percentageOfTotalSpectra;
		this.percentageSequenceCoverage = percentageSequenceCoverage;
	}

	@Override
	public ProteinGroup build() {
		return new ProteinGroup(proteinSequences,
				proteinIdentificationProbability, numberOfUniquePeptides,
				numberOfUniqueSpectra, numberOfTotalSpectra, percentageOfTotalSpectra,
				percentageSequenceCoverage);
	}

	public SearchResultBuilder getSearchResult() {
		return searchResult;
	}

	public ProteinSequenceList getProteinSequences() {
		return proteinSequences;
	}

	public void setProteinSequences(final ProteinSequenceList proteinSequences) {
		this.proteinSequences = proteinSequences;
	}

	public double getProteinIdentificationProbability() {
		return proteinIdentificationProbability;
	}

	public void setProteinIdentificationProbability(final double proteinIdentificationProbability) {
		this.proteinIdentificationProbability = proteinIdentificationProbability;
	}

	public int getNumberOfUniquePeptides() {
		return numberOfUniquePeptides;
	}

	public void setNumberOfUniquePeptides(final int numberOfUniquePeptides) {
		this.numberOfUniquePeptides = numberOfUniquePeptides;
	}

	public int getNumberOfUniqueSpectra() {
		return numberOfUniqueSpectra;
	}

	public void setNumberOfUniqueSpectra(final int numberOfUniqueSpectra) {
		this.numberOfUniqueSpectra = numberOfUniqueSpectra;
	}

	public int getNumberOfTotalSpectra() {
		return numberOfTotalSpectra;
	}

	public void setNumberOfTotalSpectra(final int numberOfTotalSpectra) {
		this.numberOfTotalSpectra = numberOfTotalSpectra;
	}

	public double getPercentageOfTotalSpectra() {
		return percentageOfTotalSpectra;
	}

	public void setPercentageOfTotalSpectra(final double percentageOfTotalSpectra) {
		this.percentageOfTotalSpectra = percentageOfTotalSpectra;
	}

	public double getPercentageSequenceCoverage() {
		return percentageSequenceCoverage;
	}

	public void setPercentageSequenceCoverage(final double percentageSequenceCoverage) {
		this.percentageSequenceCoverage = percentageSequenceCoverage;
	}

	public String[] getFlankingAminoAcids(final String peptideSequence) {
		final String needle = peptideSequence.toUpperCase(Locale.US);
		for (final ProteinSequence sequence : proteinSequences.getList()) {
			final String haystack = sequence.getSequence().toUpperCase(Locale.US);
			final int index = haystack.indexOf(needle);
			if (index >= 0 && needle.length() + index <= haystack.length()) {
				final int prevIndex = index - 1;
				final int nextIndex = index + needle.length();
				return new String[]{
						String.valueOf(prevIndex >= 0 ? haystack.charAt(prevIndex) : '-'),
						String.valueOf(nextIndex < haystack.length() ? haystack.charAt(nextIndex) : '-')
				};
			}
		}
		throw new MprcException(
				String.format("Could not determine flanking amino acid sequences for peptide [%s] in protein [%s]",
						needle,
						proteinSequences.getList().iterator().next().getSequence()));
	}
}
