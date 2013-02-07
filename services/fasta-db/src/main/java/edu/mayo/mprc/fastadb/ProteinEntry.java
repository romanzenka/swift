package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.dbcurator.model.Curation;

/**
 * @author Roman Zenka
 */
public final class ProteinEntry extends PersistableBase {
	/**
	 * Database this entry belongs to.
	 */
	private Curation database;

	/**
	 * Accession number the entry belongs to.
	 */
	private ProteinAccnum accessionNumber;

	/**
	 * Description of the database entry.
	 */
	private ProteinDescription description;

	/**
	 * The protein sequence of the entry.
	 */
	private ProteinSequence sequence;

	public ProteinEntry() {
	}

	public ProteinEntry(final Curation database, final ProteinAccnum accessionNumber, final ProteinDescription description, final ProteinSequence sequence) {
		setDatabase(database);
		setAccessionNumber(accessionNumber);
		setDescription(description);
		setSequence(sequence);
	}

	public Curation getDatabase() {
		return database;
	}

	void setDatabase(final Curation database) {
		this.database = database;
	}

	public ProteinAccnum getAccessionNumber() {
		return accessionNumber;
	}

	void setAccessionNumber(ProteinAccnum accessionNumber) {
		this.accessionNumber = accessionNumber;
	}

	public ProteinDescription getDescription() {
		return description;
	}

	void setDescription(ProteinDescription description) {
		this.description = description;
	}

	public ProteinSequence getSequence() {
		return sequence;
	}

	void setSequence(final ProteinSequence sequence) {
		this.sequence = sequence;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (null == o || !(o instanceof ProteinEntry)) {
			return false;
		}

		final ProteinEntry that = (ProteinEntry) o;

		if (null != getAccessionNumber() ? !getAccessionNumber().equals(that.getAccessionNumber()) : null != that.getAccessionNumber()) {
			return false;
		}
		if (null != getDatabase() ? !getDatabase().equals(that.getDatabase()) : null != that.getDatabase()) {
			return false;
		}
		if (null != getDescription() ? !getDescription().equals(that.getDescription()) : null != that.getDescription()) {
			return false;
		}
		if (null != getSequence() ? !getSequence().equals(that.getSequence()) : null != that.getSequence()) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = null != getDatabase() ? getDatabase().hashCode() : 0;
		result = 31 * result + (null != getAccessionNumber() ? getAccessionNumber().hashCode() : 0);
		result = 31 * result + (null != getDescription() ? getDescription().hashCode() : 0);
		result = 31 * result + (null != getSequence() ? getSequence().hashCode() : 0);
		return result;
	}

}
