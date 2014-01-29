package edu.mayo.mprc.quameterdb.dao;

import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.dbmapping.FileSearch;

import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class QuameterDbDaoHibernate extends DaoBase implements QuameterDbDao {
	@Override
	public QuameterResult addQuameterScores(final TandemMassSpectrometrySample sample, final FileSearch fileSearch, final Map<String, Double> values) {
		return null;
	}
}
