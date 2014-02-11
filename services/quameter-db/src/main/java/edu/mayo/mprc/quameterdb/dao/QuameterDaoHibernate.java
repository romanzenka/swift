package edu.mayo.mprc.quameterdb.dao;

import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import org.hibernate.Query;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
@Repository("quameterDao")
public final class QuameterDaoHibernate extends DaoBase implements QuameterDao {

	private static final String MAP = "edu/mayo/mprc/quameterdb/dao/";

	private SwiftDao swiftDao;
	private SearchDbDao searchDbDao;

	public QuameterDaoHibernate() {
	}

	public QuameterDaoHibernate(final SwiftDao swiftDao, final SearchDbDao searchDbDao) {
		super();
		this.swiftDao = swiftDao;
		this.searchDbDao = searchDbDao;
	}

	@Override
	public QuameterResult addQuameterScores(final int tandemMassSpectrometrySampleId, final int fileSearchId, final Map<String, Double> values) {
		final TandemMassSpectrometrySample sample = getSearchDbDao().getTandemMassSpectrometrySampleForId(tandemMassSpectrometrySampleId);
		final FileSearch fileSearch = getSwiftDao().getFileSearchForId(fileSearchId);
		final QuameterResult result = new QuameterResult(sample, fileSearch, values);

		return save(result, quameterResultEqualityCriteria(result), false);
	}

	@Override
	public List<QuameterResult> listAllResults(final Pattern searchFilter) {
		final Query query = getSession().createQuery("from QuameterResult as q");
		final List<QuameterResult> raw = listAndCast(query);
		final List<QuameterResult> filtered = new ArrayList<QuameterResult>(Math.min(raw.size(), 1000));
		for (final QuameterResult r : raw) {
			if (searchFilter.matcher(r.getFileSearch().getExperiment()).find()) {
				filtered.add(r);
			}
		}
		return filtered;
	}

	private Criterion quameterResultEqualityCriteria(final QuameterResult result) {
		return Restrictions.conjunction()
				.add(associationEq("fileSearch", result.getFileSearch()))
				.add(associationEq("sample", result.getSample()));
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
