package edu.mayo.mprc.quameterdb.dao;

import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
@Repository("quameterDao")
public final class QuameterDaoHibernate extends DaoBase implements QuameterDao {
	private static final Logger LOGGER = Logger.getLogger(QuameterDaoHibernate.class);
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
	public QuameterResult addQuameterScores(
			final int tandemMassSpectrometrySampleId, final int fileSearchId,
			final Map<String, Double> values,
			final int identifiedSpectra) {
		final TandemMassSpectrometrySample sample = getSearchDbDao().getTandemMassSpectrometrySampleForId(tandemMassSpectrometrySampleId);
		final FileSearch fileSearch = getSwiftDao().getFileSearchForId(fileSearchId);
		final QuameterResult result = new QuameterResult(sample, fileSearch, values, identifiedSpectra);

		return save(result, false);
	}

	@Override
	public List<QuameterResult> listAllResults(final Pattern searchFilter) {
		final Query query = getSession().createSQLQuery("" +
				"SELECT {q.*}, m.metadata_value AS v, r.transaction_id AS ti" +
				" FROM `" + swiftDao.qualifyTableName("transaction") + "` AS r, " +
				" " + swiftDao.qualifyTableName("file_search") + " AS f, " +
				" " + swiftDao.qualifyTableName("quameter_result") + " AS q, " +
				" " + swiftDao.qualifyTableName("swift_search_definition") + " AS d," +
				" " + swiftDao.qualifyTableName("search_metadata") + " AS m," +
				" " + swiftDao.qualifyTableName("tandem_mass_spec_sample") + " AS t" +
				" WHERE " +
				" q.hidden=0 AND " +
				" r.hidden=0 AND " +
				" r.swift_search = d.swift_search_definition_id AND " +
				" f.swift_search_definition_id = d.swift_search_definition_id AND " +
				" q.file_search_id = f.file_search_id AND " +
				" m.swift_search_definition_id = d.swift_search_definition_id AND " +
				" m.metadata_key='quameter.category' AND" +
				" t.tandem_mass_spec_sample_id = q.sample_id" +
				" ORDER BY t.start_time")
				.addEntity("q", QuameterResult.class)
				.addScalar("v", Hibernate.STRING)
				.addScalar("ti", Hibernate.INTEGER);
		final List raw = query.list();
		final List<QuameterResult> filtered = new ArrayList<QuameterResult>(Math.min(raw.size(), 1000));
		for (final Object o : raw) {
			final Object[] array = (Object[]) o;
			final QuameterResult r = (QuameterResult) array[0];
			final String category = (String) array[1];
			r.setCategory(category);
			final Integer transactionId = (Integer) array[2];
			r.setTransaction(transactionId);
			if (r.resultMatches(searchFilter)) {
				filtered.add(r);
			}
		}
		return filtered;
	}

	@Override
	public void hideQuameterResult(final int quameterResultId) {
		final QuameterResult quameterResult = (QuameterResult) getSession().get(QuameterResult.class, quameterResultId);
		quameterResult.setHidden(true);
		getSession().saveOrUpdate(quameterResult);
	}

	@Override
	public List<QuameterAnnotation> listAnnotations() {
		// Only list annotations that belong to non-hidden quameter results
		return listAndCast(getSession().createQuery("select q " +
				"from QuameterAnnotation as q, QuameterResult as r " +
				"WHERE q.quameterResultId = r.id AND r.hidden = false"));
	}

	@Override
	public QuameterAnnotation addAnnotation(QuameterAnnotation annotation) {
		return save(annotation, false);
	}

	@Override
	public int getIdentifiedSpectra(int fileSearchId, Map<String, Pattern> categoryToProteins) {
		final FileSearch fileSearch = swiftDao.getFileSearchForId(fileSearchId);
		final SwiftSearchDefinition swiftSearchDefinition = swiftDao.getSwiftSearchDefinition(fileSearch.getSwiftSearchDefinitionId());
		final SearchEngineParameters searchParameters = fileSearch.getSearchParametersWithDefault(swiftSearchDefinition.getSearchParameters());

		final String category = swiftSearchDefinition.getMetadata().get("quameter.category");
		if (category == null) {
			LOGGER.warn("No category defined for file search id " + fileSearchId);
			return 0;
		}

		return 0;  // TODO: Implement this method
	}

	@Override
	public Collection<String> getHibernateMappings() {
		final List<String> list = new ArrayList<String>(Arrays.asList(
				MAP + "QuameterResult.hbm.xml",
				MAP + "QuameterAnnotation.hbm.xml"
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
