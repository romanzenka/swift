package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.searchdb.dao.ProteinGroup;

import java.util.Set;

/**
 * @author Roman Zenka
 */
public class ProteinGroupBuilder implements Builder<ProteinGroup> {
	private SearchResultBuilder searchResult;

	private ProteinSequenceListBuilder proteinSequences;
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
		return new ProteinGroup(proteinSequences.build(),
				proteinIdentificationProbability, numberOfUniquePeptides,
				numberOfUniqueSpectra, numberOfTotalSpectra, percentageOfTotalSpectra,
				percentageSequenceCoverage);
	}

	public SearchResultBuilder getSearchResult() {
		return searchResult;
	}

	public void collectAccnums(Set<String> accnum) {
		accnum.addAll(proteinSequences.getAccNums());
	}

	public ProteinSequenceListBuilder getProteinSequences() {
		return proteinSequences;
	}

	public void setProteinSequences(final ProteinSequenceListBuilder proteinSequences) {
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
}
