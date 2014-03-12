package edu.mayo.mprc.quameterdb.dao;

import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import org.hibernate.Hibernate;
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
		final Query query = getSession().createSQLQuery("" +
				"SELECT {q.*}, m.metadata_value AS v " +
				" FROM `transaction` AS r, " +
				" file_search AS f, " +
				" quameter_result AS q, " +
				" swift_search_definition AS d," +
				" search_metadata AS m," +
				" tandem_mass_spectrometry_sample AS t" +
				" WHERE " +
				" r.hidden=0 AND " +
				" r.swift_search = d.swift_search_definition_id AND " +
				" f.input_files_id = d.swift_search_definition_id AND " +
				" q.file_search_id = f.file_search_id AND " +
				" m.swift_search_definition_id = d.swift_search_definition_id AND " +
				" m.metadata_key='quameter.category' AND" +
				" t.tandem_mass_spec_sample_id = q.tandem_mass_spec_sample_id" +
				" ORDER BY t.start_time")
				.addEntity("q", QuameterResult.class)
				.addScalar("v", Hibernate.STRING);
		final List raw = query.list();
		final List<QuameterResult> filtered = new ArrayList<QuameterResult>(Math.min(raw.size(), 1000));
		for (final Object o : raw) {
			final Object[] array = (Object[]) o;
			final QuameterResult r = (QuameterResult) array[0];
			final String category = (String) array[1];
			r.setCategory(category);
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
