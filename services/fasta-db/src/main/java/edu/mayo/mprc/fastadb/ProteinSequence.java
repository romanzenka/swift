package edu.mayo.mprc.fastadb;

/**
 * A protein sequence. Immutable, stored in the database only once with unique ID.
 *
 * @author Roman Zenka
 */
public final class ProteinSequence extends Sequence {
	public ProteinSequence() {
	}

	public ProteinSequence(final String sequence) {
		super(sequence);
	}
}
