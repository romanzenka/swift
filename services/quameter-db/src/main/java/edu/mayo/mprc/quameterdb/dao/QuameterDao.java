package edu.mayo.mprc.quameterdb.dao;

import edu.mayo.mprc.database.Dao;
import edu.mayo.mprc.searchdb.dao.SearchResult;
import edu.mayo.mprc.swift.dbmapping.FileSearch;

import java.util.List;
import java.util.Map;

/**
 * @author Roman Zenka
 */
public interface QuameterDao extends Dao {
	/**
	 * Add Quameter scores for given file + search combo.
	 *
	 * @param searchResultId   Id of the serialized {@link SearchResult} object. Metadata for the (usually) .raw file the scores refer to.
	 * @param fileSearchId     Id of the serialized {@link FileSearch} object. Information about how the file was searched.
	 * @param values           Quameter scores
	 * @param identifedSpectra Number of identified spectra for specific list of proteins
	 */
	QuameterResult addQuameterScores(final int searchResultId,
	                                 final int fileSearchId,
	                                 final Map<String, Double> values,
	                                 final Map<QuameterProteinGroup, Integer> identifedSpectra);

	/**
	 * @return All Quameter results that are not hidden. Limit to last 1 year
	 */
	List<QuameterResult> listVisibleResults();

	/**
	 * @return Only hidden Quameter results.
	 */
	List<QuameterResult> listHiddenResults();

	void hideQuameterResult(int quameterResultId);

	void unhideQuameterResult(int quameterResultId);


	/**
	 * Add an annotation. The annotation is uniquely identified by its
	 * data point + metric. Adding an annotation for the same metric+data point
	 * rewrites the old annotation.
	 *
	 * @param annotation Annotation to add.
	 * @return Saved version of the annotation. You can typically ignore this return value.
	 */
	QuameterAnnotation addAnnotation(QuameterAnnotation annotation);

	/**
	 * List all annotations present in the system.
	 *
	 * @return List of all annotations.
	 */
	List<QuameterAnnotation> listAnnotations();

	/**
	 * For given file search id and a list of categories and their corresponding proteins,
	 * count all the spectra corresponding to the protein set for the particular file search.
	 *
	 * @param fileSearchId   Saved info about file search that was used to process the input file
	 * @param searchResultId Direct link to the search results  for the file.
	 * @param proteinGroups  Currently defined protein groups
	 * @return Count of spectra corresponding to protein groups for given file search.
	 * We use {@link edu.mayo.mprc.searchdb.dao.ProteinGroup#getNumberOfTotalSpectra()}, so the value can be larger
	 * than it should be.
	 */
	Map<QuameterProteinGroup, Integer> getIdentifiedSpectra(int fileSearchId, int searchResultId, List<QuameterProteinGroup> proteinGroups);

	/**
	 * Take a list of protein groups as it came from the config.
	 * <p/>
	 * Make sure that our database contains only the listed protein groups and nothing else.
	 * <p/>
	 * Return the serialized protein groups as currently in the database.
	 * This function will also recalculate the protein counts in case protein groups changed,
	 * so it can run for a long time.
	 *
	 * @param groups List of groups to set the database to.
	 * @return Serialized protein groups.
	 */
	List<QuameterProteinGroup> updateProteinGroups(List<QuameterProteinGroup> groups);

	/**
	 * @return List of currently active protein groups.
	 */
	List<QuameterProteinGroup> listProteinGroups();
}
