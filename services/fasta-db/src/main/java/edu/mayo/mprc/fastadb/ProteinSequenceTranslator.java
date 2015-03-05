package edu.mayo.mprc.fastadb;

import java.util.Collection;
import java.util.Map;

/**
 * Can translate given accession number to protein sequence within a context of a particular database.
 *
 * @author Roman Zenka
 */
public interface ProteinSequenceTranslator {
	/**
	 * @param accessionNumbers Accession numbers of the protein.
	 * @param databaseSources  A comma delimited list of .fasta database that are used as a context for translating the given accession number to sequence.
	 *                         Currently only a single database is supported.
	 * @return Map from accession number to {@link ProteinSequence} corresponding to the accession number within a context of a particular database.
	 */
	Map<String, ProteinSequence> getProteinSequence(Collection<String> accessionNumbers, String databaseSources);
}
