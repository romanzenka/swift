package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.fastadb.TempKey;

/**
 * @author Roman Zenka
 */
public final class TempPeptideSpectrumMatch {
	private TempKey tempKey;
	private Integer newId;

	private IdentifiedPeptide peptide;
	private char previousAminoAcid;
	private char nextAminoAcid;
	private double bestPeptideIdentificationProbability;
	private SpectrumIdentificationCounts spectrumIdentificationCounts = new SpectrumIdentificationCounts();
	private int numberOfEnzymaticTerminii;

	public TempPeptideSpectrumMatch() {
	}

	public TempPeptideSpectrumMatch(final TempKey key, final PeptideSpectrumMatch value) {
		this.tempKey = key;
		setPeptide(value.getPeptide());
		setPreviousAminoAcid(value.getPreviousAminoAcid());
		setNextAminoAcid(value.getNextAminoAcid());
		setBestPeptideIdentificationProbability(value.getBestPeptideIdentificationProbability());
		setSpectrumIdentificationCounts(value.getSpectrumIdentificationCounts());
		setNumberOfEnzymaticTerminii(value.getNumberOfEnzymaticTerminii());
	}

	public TempKey getTempKey() {
		return tempKey;
	}

	public void setTempKey(final TempKey tempKey) {
		this.tempKey = tempKey;
	}

	public Integer getNewId() {
		return newId;
	}

	public void setNewId(final Integer newId) {
		this.newId = newId;
	}

	public IdentifiedPeptide getPeptide() {
		return peptide;
	}

	public void setPeptide(final IdentifiedPeptide peptide) {
		this.peptide = peptide;
	}

	public char getPreviousAminoAcid() {
		return previousAminoAcid;
	}

	public void setPreviousAminoAcid(final char previousAminoAcid) {
		this.previousAminoAcid = previousAminoAcid;
	}

	public char getNextAminoAcid() {
		return nextAminoAcid;
	}

	public void setNextAminoAcid(final char nextAminoAcid) {
		this.nextAminoAcid = nextAminoAcid;
	}

	public double getBestPeptideIdentificationProbability() {
		return bestPeptideIdentificationProbability;
	}

	public void setBestPeptideIdentificationProbability(final double bestPeptideIdentificationProbability) {
		this.bestPeptideIdentificationProbability = bestPeptideIdentificationProbability;
	}

	public SpectrumIdentificationCounts getSpectrumIdentificationCounts() {
		return spectrumIdentificationCounts;
	}

	public void setSpectrumIdentificationCounts(final SpectrumIdentificationCounts spectrumIdentificationCounts) {
		this.spectrumIdentificationCounts = spectrumIdentificationCounts;
	}

	public int getNumberOfEnzymaticTerminii() {
		return numberOfEnzymaticTerminii;
	}

	public void setNumberOfEnzymaticTerminii(final int numberOfEnzymaticTerminii) {
		this.numberOfEnzymaticTerminii = numberOfEnzymaticTerminii;
	}
}
