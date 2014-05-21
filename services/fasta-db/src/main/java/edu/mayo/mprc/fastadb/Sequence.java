package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.chem.AminoAcidSet;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.PersistableBase;
import org.hibernate.criterion.Criterion;

import java.util.Locale;

/**
 * A sequence of amino acids. Will automatically normalize to all uppercase. Terminal * will be
 * removed.
 *
 * @author Roman Zenka
 */
public abstract class Sequence extends PersistableBase {
	private String sequence;
	/**
	 * Monoisotopic mass of the sequence. Can be null if the mass was not determined.
	 */
	private Double mass;

	/**
	 * Empty constructor for Hibernate.
	 */
	public Sequence() {
	}

	public Sequence(final String sequence) {
		if (sequence != null) {
			final String canonicalSequence = cleanupSequence(sequence);
			setSequence(canonicalSequence);
			setMass(AminoAcidSet.DEFAULT.getMonoisotopicMass(canonicalSequence));
		}
	}

	/**
	 * Trim, convert to uppercase, remove trailing *.
	 *
	 * @param sequence Sequence to clean.
	 * @return Sequence canonicalized to all uppercase with optional terminal * trimmed.
	 */
	private String cleanupSequence(final String sequence) {
		final String trimUpper = sequence.trim().toUpperCase(Locale.US);
		if (trimUpper.endsWith("*")) {
			return trimUpper.substring(0, trimUpper.length() - 1);
		}
		return trimUpper;
	}

	public String getSequence() {
		return sequence;
	}

	void setSequence(final String sequence) {
		this.sequence = cleanupSequence(sequence);
	}

	public Double getMass() {
		return mass;
	}

	void setMass(final Double mass) {
		this.mass = mass;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof Sequence)) {
			return false;
		}

		final Sequence sequence1 = (Sequence) o;

		if (getSequence() != null ? !getSequence().equals(sequence1.getSequence()) : sequence1.getSequence() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return getSequence() == null ? 0 : getSequence().hashCode();
	}

	@Override
	public Criterion getEqualityCriteria() {
		return DaoBase.nullSafeEq("sequence", getSequence());
	}
}
