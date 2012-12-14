package edu.mayo.mprc.fastadb;

/**
 * Can translate given accession number to protein sequence within a context of a particular database.
 *
 * @author Roman Zenka
 */
public interface ProteinSequenceTranslator {
	/**
	 * @param accessionNumber Accession number of the protein.
	 * @param databaseSources A comma delimited list of .fasta database that are used as a context for translating the given accession number to sequence.
	 *                        Currently only a single database is supported.
	 * @return {@link ProteinSequence} corresponding to the accession number within a context of a particular database. If no such sequence is found,
	 *         null is returned.
	 */
	ProteinSequence getProteinSequence(String accessionNumber, String databaseSources);
}
