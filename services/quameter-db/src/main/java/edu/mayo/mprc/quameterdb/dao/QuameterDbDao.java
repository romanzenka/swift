package edu.mayo.mprc.quameterdb.dao;

import edu.mayo.mprc.database.Dao;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.dbmapping.FileSearch;

import java.util.Map;

/**
 * @author Roman Zenka
 */
public interface QuameterDbDao extends Dao {
	/**
	 * Add Quameter scores for given file + search combo.
	 *
	 * @param sample     Metadata for the (usually) .raw file the scores refer to.
	 * @param fileSearch Information about how the file was searched.
	 * @param values     Quameter scores
	 */
	public QuameterResult addQuameterScores(final TandemMassSpectrometrySample sample, final FileSearch fileSearch, final Map<String, Double> values);
}
