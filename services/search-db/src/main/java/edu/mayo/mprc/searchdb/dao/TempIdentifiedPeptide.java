package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.bulk.TempKey;
import edu.mayo.mprc.fastadb.PeptideSequence;

/**
 * A copy pf {@link IdentifiedPeptide} for bulk loading into a temp table.
 *
 * @author Roman Zenka
 */
public final class TempIdentifiedPeptide {

	private TempKey tempKey;
	private Integer newId;

	private PeptideSequence sequence;
	private LocalizedModBag modifications;

	public TempIdentifiedPeptide() {
	}

	public TempIdentifiedPeptide(final TempKey tempKey, final IdentifiedPeptide peptide) {
		this.tempKey = tempKey;
		setSequence(peptide.getSequence());
		setModifications(peptide.getModifications());
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

	public PeptideSequence getSequence() {
		return sequence;
	}

	public void setSequence(final PeptideSequence sequence) {
		this.sequence = sequence;
	}

	public LocalizedModBag getModifications() {
		return modifications;
	}

	public void setModifications(final LocalizedModBag modifications) {
		this.modifications = modifications;
	}
}
