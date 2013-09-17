package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.utilities.progress.PercentDone;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;

import java.util.Collection;

/**
 * Can load .FASTA file into a database for easy lookups of protein sequences.
 * <p/>
 * Since storage of peptides is very related to storage of proteins, the peptide handling methods
 * are provided as well.
 *
 * @author Roman Zenka
 */
public interface FastaDbDao {
	/**
	 * Look up given protein sequence in the database.
	 * If the sequence does not exist in the database, it is added.
	 * ID of the protein sequence is updated to match the database id.
	 *
	 * @param proteinSequence Sequence to add.
	 */
	ProteinSequence addProteinSequence(ProteinSequence proteinSequence);

	/**
	 * Return a protein sequence for given ID.
	 *
	 * @param proteinId Id of the sequence to return.
	 * @return Sequence from the database.
	 */
	ProteinSequence getProteinSequence(int proteinId);

	/**
	 * Get a protein sequence for a given accession number.
	 *
	 * @param database        A fasta database from which the accession numbers come.
	 * @param accessionNumber Accession number of the protein.
	 * @return Protein sequence corresponding to the accession number.
	 */
	ProteinSequence getProteinSequence(Curation database, String accessionNumber);

	/**
	 * Find a matching protein description for given accession number.
	 *
	 * @param database        Database to use.
	 * @param accessionNumber Protein accession number.
	 * @return Matching protein description. Throws an exception if accession number was not found.
	 */
	String getProteinDescription(Curation database, String accessionNumber);

	/**
	 * Look up given peptide sequence in the database.
	 * If the sequence does not exist in the database, it is added.
	 * ID of the peptide sequence is updated to match the database id.
	 *
	 * @param peptideSequence Sequence to add.
	 */
	PeptideSequence addPeptideSequence(PeptideSequence peptideSequence);

	/**
	 * Return a protein sequence for given ID.
	 *
	 * @param peptideId Id of the sequence to return.
	 * @return Sequence from the database.
	 */
	PeptideSequence getPeptideSequence(int peptideId);

	/**
	 * @param database FASTA database to check.
	 * @return How many accession numbers are associated with given curation.
	 */
	long countDatabaseEntries(Curation database);

	/**
	 * Add data from a given FASTA file into the database.
	 *
	 * @param database         Database to load data for.
	 * @param progressReporter The {@link PercentDone} message will be set periodically using {@link UserProgressReporter#reportProgress}. If null, no progress is reported.
	 */
	void addFastaDatabase(Curation database, UserProgressReporter progressReporter);

	/**
	 * Add all protein sequences as fast as possible.
	 *
	 * @param proteinSequences List of protein sequences to add.
	 */
	void addProteinSequences(Collection<ProteinSequence> proteinSequences);

	/**
	 * Add all peptide sequences as fast as possible.
	 *
	 * @param peptideSequences List of protein sequences to add.
	 */
	void addPeptideSequences(Collection<PeptideSequence> peptideSequences);

	/**
	 * @return New job to start.
	 */
	BulkLoadJob startNewJob();

	/**
	 * Remove the running job.
	 *
	 * @param job Job to be removed.
	 */
	void endJob(BulkLoadJob job);
}
