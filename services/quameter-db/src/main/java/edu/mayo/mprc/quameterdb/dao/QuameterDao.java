package edu.mayo.mprc.quameterdb.dao;

import edu.mayo.mprc.database.Dao;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.dbmapping.FileSearch;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
public interface QuameterDao extends Dao {
	/**
	 * Add Quameter scores for given file + search combo.
	 *
	 * @param tandemMassSpectrometrySampleId Id of the serialized {@link TandemMassSpectrometrySample} object. Metadata for the (usually) .raw file the scores refer to.
	 * @param fileSearchId                   Id of the serialized {@link FileSearch} object. Information about how the file was searched.
	 * @param values                         Quameter scores
	 * @param identifedSpectra               Number of identified spectra for specific list of proteins
	 */
	QuameterResult addQuameterScores(final int tandemMassSpectrometrySampleId,
	                                 final int fileSearchId,
	                                 final Map<String, Double> values,
	                                 final Map<QuameterProteinGroup, Integer> identifedSpectra);

	/**
	 * @param searchFilter Filter the search names by this regex.
	 * @return All the Quameter results that match given search name filter, with all values populated.
	 */
	List<QuameterResult> listAllResults(Pattern searchFilter);

	void hideQuameterResult(int quameterResultId);

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
	 * @param analysisId                     Id of the loaded analysis (Scaffold report) that contains the file.
	 * @param fileSearchId                   Saved info about file search that was used to process the input file
	 * @param tandemMassSpectrometrySampleId Direct link to the mass spectrometry metadata for the file.
	 * @param proteinGroups                  Currently defined protein groups
	 * @return Count of spectra corresponding to protein groups for given file search
	 */
	Map<QuameterProteinGroup, Integer> getIdentifiedSpectra(int analysisId, int fileSearchId, int tandemMassSpectrometrySampleId, List<QuameterProteinGroup> proteinGroups);

	/**
	 * Take a list of protein groups as it came from the config.
	 * <p/>
	 * Make sure that our database contains only the listed protein groups and nothing else.
	 * <p/>
	 * Return the serialized protein groups as currently in the database.
	 *
	 * @param groups List of groups to set the database to.
	 * @return Serialized protein groups.
	 */
	List<QuameterProteinGroup> updateProteinGroups(List<QuameterProteinGroup> groups);


	/**
	 * @return List of currently active protein groups.
	 */
	List<QuameterProteinGroup> listProteinGroups();

	/**
	 * When the user changes definitions of the protein groups, the numbers of proteins become inconsistent.
	 * <p/>
	 * The old protein counts would refer to old definitions of the protein groups.
	 * <p/>
	 * At this point we need to recalculate the protein counts on the entire database, to bring all the counts
	 * up to date.
	 */
	void recalculateProteinCounts();

}
