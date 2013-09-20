package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.bulk.TempKey;
import edu.mayo.mprc.unimod.ModSpecificity;

/**
 * @author Roman Zenka
 */
public class TempLocalizedModification {
	private TempKey tempKey;
	private Integer newId;

	/**
	 * Observed modification specificity. Keep in mind that what the software reports does not have to be
	 * what actually happened - it can be a different mod with the same mass.
	 */
	private ModSpecificity modSpecificity;

	/**
	 * Position where the modification was observed. The numbering starts from 0 corresponding to the
	 * first amino acid of the sequence (starting from the N-terminus).
	 */
	private int position;

	/**
	 * The residue the modification is residing on.
	 */
	private char residue;


	public TempLocalizedModification() {
	}

	public TempLocalizedModification(final LocalizedModification localizedModification, final TempKey tempKey) {
		setModSpecificity(localizedModification.getModSpecificity());
		setPosition(localizedModification.getPosition());
		setResidue(localizedModification.getResidue());
		this.tempKey = tempKey;
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

	public ModSpecificity getModSpecificity() {
		return modSpecificity;
	}

	public void setModSpecificity(final ModSpecificity modSpecificity) {
		this.modSpecificity = modSpecificity;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(final int position) {
		this.position = position;
	}

	public char getResidue() {
		return residue;
	}

	public void setResidue(final char residue) {
		this.residue = residue;
	}
}
