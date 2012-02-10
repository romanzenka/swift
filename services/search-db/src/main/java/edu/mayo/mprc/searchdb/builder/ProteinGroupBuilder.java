package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.searchdb.dao.ProteinGroup;
import edu.mayo.mprc.searchdb.dao.ProteinSequenceList;

/**
 * @author Roman Zenka
 */
public class ProteinGroupBuilder implements Builder<ProteinGroup> {
    private SearchResultBuilder searchResult;

    private ProteinSequenceList proteinSequences;
    private PsmListBuilder peptideSpectrumMatches;
    private double proteinIdentificationProbability;
    private int numberOfUniquePeptides;
    private int numberOfUniqueSpectra;
    private int numberOfTotalSpectra;
    private double percentageOfTotalSpectra;
    private double percentageSequenceCoverage;

    public ProteinGroupBuilder(SearchResultBuilder searchResult, double proteinIdentificationProbability, int numberOfUniquePeptides, int numberOfUniqueSpectra, int numberOfTotalSpectra, double percentageOfTotalSpectra, double percentageSequenceCoverage) {
        this.searchResult = searchResult;
        this.peptideSpectrumMatches = new PsmListBuilder(this);
        this.proteinIdentificationProbability = proteinIdentificationProbability;
        this.numberOfUniquePeptides = numberOfUniquePeptides;
        this.numberOfUniqueSpectra = numberOfUniqueSpectra;
        this.numberOfTotalSpectra = numberOfTotalSpectra;
        this.percentageOfTotalSpectra = percentageOfTotalSpectra;
        this.percentageSequenceCoverage = percentageSequenceCoverage;
    }

    @Override
    public ProteinGroup build() {
        return new ProteinGroup(proteinSequences, peptideSpectrumMatches.build(),
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

    public void setProteinSequences(ProteinSequenceList proteinSequences) {
        this.proteinSequences = proteinSequences;
    }

    public PsmListBuilder getPeptideSpectrumMatches() {
        return peptideSpectrumMatches;
    }

    public double getProteinIdentificationProbability() {
        return proteinIdentificationProbability;
    }

    public void setProteinIdentificationProbability(double proteinIdentificationProbability) {
        this.proteinIdentificationProbability = proteinIdentificationProbability;
    }

    public int getNumberOfUniquePeptides() {
        return numberOfUniquePeptides;
    }

    public void setNumberOfUniquePeptides(int numberOfUniquePeptides) {
        this.numberOfUniquePeptides = numberOfUniquePeptides;
    }

    public int getNumberOfUniqueSpectra() {
        return numberOfUniqueSpectra;
    }

    public void setNumberOfUniqueSpectra(int numberOfUniqueSpectra) {
        this.numberOfUniqueSpectra = numberOfUniqueSpectra;
    }

    public int getNumberOfTotalSpectra() {
        return numberOfTotalSpectra;
    }

    public void setNumberOfTotalSpectra(int numberOfTotalSpectra) {
        this.numberOfTotalSpectra = numberOfTotalSpectra;
    }

    public double getPercentageOfTotalSpectra() {
        return percentageOfTotalSpectra;
    }

    public void setPercentageOfTotalSpectra(double percentageOfTotalSpectra) {
        this.percentageOfTotalSpectra = percentageOfTotalSpectra;
    }

    public double getPercentageSequenceCoverage() {
        return percentageSequenceCoverage;
    }

    public void setPercentageSequenceCoverage(double percentageSequenceCoverage) {
        this.percentageSequenceCoverage = percentageSequenceCoverage;
    }
}
