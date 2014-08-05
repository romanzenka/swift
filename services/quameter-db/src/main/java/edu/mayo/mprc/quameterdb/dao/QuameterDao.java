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
	                                 final int identifedSpectra);

	List<QuameterResult> listAllResults(Pattern searchFilter);

	void hideQuameterResult(int quameterResultId);

	/**
	 * For given file search id and a list of categories and their corresponding proteins,
	 * count all the spectra corresponding to the protein set for the particular file search.
	 *
	 * @param fileSearchId Saved info about file search
	 * @param categoryToProteins Map of QuaMeter category name to list of proteins
	 * @return Count of spectra corresponding to proteins for given file search
	 */
	int getIdentifiedSpectra(int fileSearchId, Map<String, Pattern> categoryToProteins);
}
