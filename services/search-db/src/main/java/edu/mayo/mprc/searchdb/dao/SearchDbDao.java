package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.Dao;
import edu.mayo.mprc.database.QueryCallback;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;

import java.util.List;

/**
 * This dao should be implemented in an efficient manner. Typically a large amount of queries (10000x per input file)
 * is going to be run when adding peptide/protein sequences.
 */
public interface SearchDbDao extends Dao {
	/**
	 *
	 * @param analysis   Analysis to add.
	 * @param reportData The analysis is bound to this Scaffold data report (.sf3 file)
	 * @return Added analysis properly linked into Hibernate.
	 */
	Analysis addAnalysis(Analysis analysis, ReportData reportData, UserProgressReporter reporter);

	/**
	 * @param reportId Id of {@link ReportData}
	 * @return Analysis linked to a particular report (Scaffold .sf3 file).
	 */
	Analysis getAnalysis(long reportId);

	/**
	 * @param reportId Id of {@link ReportData} object.
	 * @return True if the given report has {@link Analysis} data linked to it.
	 */
	boolean hasAnalysis(long reportId);

	/**
	 * List accession numbers for a protein group.
	 *
	 * @param proteinSequenceList A list of protein sequences.
	 * @return A string describing the accession numbers for proteins within the group.
	 */
	List<String> getProteinAccessionNumbers(ProteinSequenceList proteinSequenceList);

	/**
	 * List all searches where a protein of given accession number was observed.
	 *
	 * @param accessionNumber Accession number.
	 * @return List of searches.
	 */
	List<ReportData> getSearchesForAccessionNumber(String accessionNumber);

	/**
	 * @return List of all report ids  that do not have the analysis object attached. Only reports with defined search
	 * parameters are listed (does not make sense to list them otherwise, as results cannot be loaded in that case).
	 */
	List<Long> getReportIdsWithoutAnalysis();

	/**
	 * Go through the list of all mass spectrometry samples, calling the given callback on each.
	 * @param callback Callback to be called once per each sample.
	 */
	void getTandemMassSpectrometrySamples(QueryCallback callback);

	/**
	 * Used for fixing a problem with badly parsed .RAW file information.
	 * Looks up file information in the table based on the file name and date last modified, updates all other data.
	 * @param sample Sample data to update.
	 */
	TandemMassSpectrometrySample updateTandemMassSpectrometrySample(TandemMassSpectrometrySample sample);
}
