package edu.mayo.mprc.quameterdb.dao;

import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author Roman Zenka
 */
@Repository("quameterDao")
public final class QuameterDaoHibernate extends DaoBase implements QuameterDao {

	private static final String MAP = "edu/mayo/mprc/quameterdb/dao/";

	private SwiftDao swiftDao;
	private SearchDbDao searchDbDao;

	@Override
	public QuameterResult addQuameterScores(final int tandemMassSpectrometrySampleId, final int fileSearchId, final Map<String, Double> values) {
		final TandemMassSpectrometrySample sample = getSearchDbDao().getTandemMassSpectrometrySampleForId(tandemMassSpectrometrySampleId);
		final FileSearch fileSearch = getSwiftDao().getFileSearchForId(fileSearchId);
		QuameterResult result = new QuameterResult(sample, fileSearch, values);

		return save(result, quameterResultEqualityCriteria(result), false);
	}

	private Criterion quameterResultEqualityCriteria(final QuameterResult result) {
		return Restrictions.conjunction()
				.add(associationEq("fileSearch", result.getFileSearch()))
				.add(associationEq("sample", result.getSample()))
				.add(nullSafeEq("jsonData", result.getJsonData()));
	}

	@Override
	public Collection<String> getHibernateMappings() {
		final List<String> list = new ArrayList<String>(Arrays.asList(
				MAP + "QuameterResult.hbm.xml"
		));
		list.addAll(super.getHibernateMappings());
		return list;
	}

	public SearchDbDao getSearchDbDao() {
		return searchDbDao;
	}

	@Resource(name = "searchDbDao")
	public void setSearchDbDao(final SearchDbDao searchDbDao) {
		this.searchDbDao = searchDbDao;
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	@Resource(name = "swiftDao")
	public void setSwiftDao(final SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}
}
