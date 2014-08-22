package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.Dao;
import edu.mayo.mprc.database.QueryCallback;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.progress.PercentProgressReporter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This dao should be implemented in an efficient manner. Typically a large amount of queries (10000x per input file)
 * is going to be run when adding peptide/protein sequences.
 */
public interface SearchDbDao extends Dao {
	/**
	 * Add analysis.
	 *
	 * @param analysis   Analysis to load.
	 * @param reportData The analysis is bound to this Scaffold data report (.sf3 file)
	 * @param reporter   Progress is reported here
	 * @return Added analysis properly linked into Hibernate.
	 */
	Analysis addAnalysis(Analysis analysis, ReportData reportData, PercentProgressReporter reporter);

	/**
	 * @param analysisId Id of {@link Analysis}
	 * @return Analysis of given id.
	 */
	Analysis getAnalysis(int analysisId);

	/**
	 * Get parameters for Swift search that belongs to a particular report.
	 *
	 * @param reportId ID of the {@link ReportData} to get the parameters for.
	 * @return Swift saerch definition.
	 */
	SwiftSearchDefinition getSearchDefinition(long reportId);

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
	 *
	 * @param callback Callback to be called once per each sample.
	 */
	void getTandemMassSpectrometrySamples(QueryCallback callback);

	/**
	 * Used for fixing a problem with badly parsed .RAW file information.
	 * Looks up file information in the table based on the file name and date last modified, updates all other data.
	 *
	 * @param sample Sample data to update.
	 */
	TandemMassSpectrometrySample updateTandemMassSpectrometrySample(TandemMassSpectrometrySample sample);

	/**
	 * Get a map of protein sequence list id -> {@link edu.mayo.mprc.searchdb.dao.ProteinSequenceList}
	 * for every single protein sequence list identified in given analysis.
	 *
	 * @param analysis Analysis to load protein sequences from.
	 * @return List of protein sequences from the analysis.
	 */
	TreeMap<Integer, ProteinSequenceList> getAllProteinSequences(Analysis analysis);

	/**
	 * Get a map of protein sequence list id -> {@link edu.mayo.mprc.searchdb.dao.ProteinGroup}
	 * for every single protein group identified in given analysis associated with a given sample file.
	 *
	 * @param analysis Analysis to load protein sequences from.
	 * @param sample   Mass spec sample to limit ourselves to.
	 * @return List of protein groups associated with the given mass spec sample for given analysis.
	 */
	TreeMap<Integer, ProteinGroup> getProteinGroupsForSample(Analysis analysis, TandemMassSpectrometrySample sample);

	/**
	 * Look at all proteins given {@link ProteinSequenceList} ids. For each protein sequence list
	 * load all accession numbers associated with it and put them in a map keyed by their id.
	 * Only limit yourself to accession numbers from a given database.
	 *
	 * @param proteinSequenceLists Ids of protein sequence lists to load the ids for.
	 * @param databaseId           Database to get the  accession numbers from. If null, all matching accnums are returned.
	 * @return Map from {@link ProteinSequenceList} id to list of accession numbers for that group.
	 */
	Map<Integer, List<String>> getAccessionNumbersMapForProteinSequences(Set<Integer> proteinSequenceLists, Integer databaseId);

	/**
	 * Look at all proteins given {@link edu.mayo.mprc.searchdb.dao.ProteinGroup} ids. For each protein group
	 * load all accession numbers associated with it and put them in a map keyed by the group id.
	 * Only limit yourself to accession numbers from a given database.
	 *
	 * @param proteinGroupIds Ids of protein sequence lists to load the ids for.
	 * @param databaseId           Database to get the  accession numbers from. If null, all matching accnums are returned.
	 * @return Map from {@link ProteinSequenceList} id to list of accession numbers for that group.
	 */
	Map<Integer, List<String>> getAccessionNumbersMapForProteinGroups(Set<Integer> proteinGroupIds, Integer databaseId);


	/**
	 * Return the number of protein groups identified for a specified raw file
	 * for a list of reports.
	 *
	 * @param inputFile File to check
	 * @param reports   Reports to check
	 * @return How many protein groups were identified for the given input file in the first Scaffold report
	 * that contains the input file (there should be just one).
	 */
	int getScaffoldProteinGroupCount(String inputFile, Iterable<ReportData> reports);

	TandemMassSpectrometrySample getTandemMassSpectrometrySampleForId(int tandemMassSpectrometrySampleId);
}
