/**
 * A database of all previous search results.
 * <p/>
 * The user can search for protein, peptide or specific modification and find out in which searches it was seen before.
 * <p/>
 * Ideally we would rely on previous Scaffold exports,
 * however, these might have been done using inconsistent thresholds. Instead, we invoke Scaffold Batch to load
 * the previously produced .sfd or .sf3 file and instruct it to provide spectrum report with fixed thresholds. We
 * are forced to use spectrum report since the peptide report does not contain information about peptide modifications
 * that we deem important.
 * <p/>
 * The report is then parsed back into a set of tables.
 * <p/>
 * We store each protein/peptide sequence only once.
 * We also store peptide sequence + mods combination only once. While this uses time during load, it leads
 * to more compact database that is easier to search.
 * <p/>
 * Here is the information being loaded from the Scaffold Spectrum report into various objects:
 * <h2>{@link Analysis}</h2>
 * <ul>
 * <li>Scaffold version - from header</li>
 * <li>Date created</li>
 * </ul>
 * <h2>{@link BiologicalSample}</h2>
 * <ul>
 * <li>Biological sample category</li>
 * <li>Biological sample name</li>
 * </ul>
 * <h2>{@link SearchResult}</h2>
 * <ul>
 * <li>MS/MS sample name - used to link to {@link TandemMassSpectrometrySample} with more information</li>
 * </ul>
 * <h2>{@link ProteinGroup}</h2>
 * <ul>
 * <li>Protein accession numbers</li>
 * <li>Protein identification probability</li>
 * <li>Number of unique peptides</li>
 * <li>Number of unique spectra</li>
 * <li>Number of total spectra</li>
 * <li>Percentage of total spectra</li>
 * <li>Percentage sequence coverage</li>
 * </ul>
 * <h2>{@link PeptideSpectrumMatch}</h2>
 * <ul>
 * <li>Previous amino acid</li>
 * <li>Next amino acid</li>
 * <li>Best Peptide identification probability</li>
 * <li>Best SEQUEST XCorr score</li>
 * <li>Best SEQUEST DCn score</li>
 * <li>Best Mascot Ion score</li>
 * <li>Best Mascot Identity score</li>
 * <li>Best Mascot Delta Ion Score</li>
 * <li>Best X! Tandem -log(e) score</li>
 * <li>Number of identified +1H spectra</li>
 * <li>Number of identified +2H spectra</li>
 * <li>Number of identified +3H spectra</li>
 * <li>Number of identified +4H spectra</li>
 * <li>Number of enzymatic termini</li>
 * <li>Spectrum name - not stored directly, utilized to calculate statistics</li>
 * <li>Spectrum charge - not stored directly, utilized to calculate statistics</li>
 * </ul>
 * <h2>{@link IdentifiedPeptide}</h2>
 * <ul>
 * <li>Peptide sequence</li>
 * <li>Fixed modifications identified by spectrum - stored as single mod list</li>
 * <li>Variable modifications identified by spectrum - stored as single mod list</li>
 * </ul>
 * <h2>Unprocessed columns</h2>
 * Figure out what are these good for.
 * <ul>
 * <li>Exclusive</li>
 * <li>Other Proteins</li>
 * <li>Manual validation - the user manually checked that this spectrum is OK</li>
 * <li>Assigned - (probably??) the user manually assigned a particular protein if there is grouping ambiguity</li>
 * </ul>
 */
package edu.mayo.mprc.searchdb;

