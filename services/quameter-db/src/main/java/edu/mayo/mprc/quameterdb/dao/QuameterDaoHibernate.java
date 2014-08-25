package edu.mayo.mprc.quameterdb.dao;

import com.google.common.collect.*;
import edu.mayo.mprc.config.RuntimeInitializer;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.searchdb.dao.Analysis;
import edu.mayo.mprc.searchdb.dao.ProteinGroup;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.type.StandardBasicTypes;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author Roman Zenka
 */
@Repository("quameterDao")
public final class QuameterDaoHibernate extends DaoBase implements QuameterDao, RuntimeInitializer {
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
			final Map<QuameterProteinGroup, Integer> identifiedSpectra) {
		final TandemMassSpectrometrySample sample = getSearchDbDao().getTandemMassSpectrometrySampleForId(tandemMassSpectrometrySampleId);
		final FileSearch fileSearch = getSwiftDao().getFileSearchForId(fileSearchId);
		final QuameterResult result = new QuameterResult(sample, fileSearch, values, identifiedSpectra);

		return save(result, false);
	}

	public List<QuameterProteinGroup> listProteinGroups() {
		return listAll(QuameterProteinGroup.class);
	}

	@Override
	public List<QuameterResult> listAllResults() {
		final List<QuameterProteinGroup> activeProteinGroups = listProteinGroups();

		final Query query = getSession().createSQLQuery("" +
				"SELECT {q.*}, m.metadata_value AS v, r.transaction_id AS ti" +
				" FROM `" + swiftDao.qualifyTableName("transaction") + "` AS r, "
				+ swiftDao.qualifyTableName("file_search") + " AS f, "
				+ swiftDao.qualifyTableName("quameter_result") + " AS q, "
				+ swiftDao.qualifyTableName("swift_search_definition") + " AS d, "
				+ swiftDao.qualifyTableName("search_metadata") + " AS m, "
				+ swiftDao.qualifyTableName("tandem_mass_spec_sample") + " AS t" +
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
				.addScalar("v", StandardBasicTypes.STRING)
				.addScalar("ti", StandardBasicTypes.INTEGER);
		final List raw = query.list();
		final List<QuameterResult> filtered = new ArrayList<QuameterResult>(Math.min(raw.size(), 1000));
		for (final Object o : raw) {
			final Object[] array = (Object[]) o;
			final QuameterResult r = (QuameterResult) array[0];
			final String category = (String) array[1];
			r.setCategory(category);
			final Integer transactionId = (Integer) array[2];
			r.setTransaction(transactionId);

			final ImmutableMap.Builder<QuameterProteinGroup, Integer> builder = new ImmutableMap.Builder<QuameterProteinGroup, Integer>();
			for (final QuameterProteinGroup group : activeProteinGroups) {
				// Fake some data up
				builder.put(group, (int) (Math.random() * 60.0));
			}
			r.setIdentifiedSpectra(builder.build());
			filtered.add(r);
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
	public QuameterAnnotation addAnnotation(final QuameterAnnotation annotation) {
		return save(annotation, false);
	}

	@Override
	public Map<QuameterProteinGroup, Integer> getIdentifiedSpectra(
			final int analysisId,
			final int fileSearchId,
			final int tandemMassSpectrometrySampleId,
			final List<QuameterProteinGroup> quameterProteinGroups) {
		final FileSearch fileSearch = swiftDao.getFileSearchForId(fileSearchId);
		final SwiftSearchDefinition swiftSearchDefinition = fileSearch.getSwiftSearchDefinition();
		// Now we know the database in our context

		final TandemMassSpectrometrySample sample = searchDbDao.getTandemMassSpectrometrySampleForId(tandemMassSpectrometrySampleId);
		final Analysis analysis = searchDbDao.getAnalysis(analysisId);
		final TreeMap<Integer, ProteinGroup> proteinGroups = searchDbDao.getProteinGroupsForSample(analysis, sample);
		final Integer databaseId = swiftSearchDefinition.getSearchParameters().getDatabase().getId();
		final Map<Integer, List<String>> accnumsForGroups = searchDbDao.getAccessionNumbersMapForProteinGroups(proteinGroups.keySet(), databaseId);

		// We know protein groups + its accession numbers
		// .. Match this against quameter protein groups and sum spectra appropriately.

		final int[] counts = new int[quameterProteinGroups.size()];

		for (final Map.Entry<Integer, ProteinGroup> entry : proteinGroups.entrySet()) {
			final Integer groupId = entry.getKey();
			final ProteinGroup proteinGroup = entry.getValue();
			final List<String> accnums = accnumsForGroups.get(groupId);
			for (int i = 0; i < counts.length; i++) {
				final QuameterProteinGroup quameterGroup = quameterProteinGroups.get(i);
				if (quameterGroup.matches(accnums)) {
					counts[i] += proteinGroup.getNumberOfUniqueSpectra();
				}
			}
		}


		final ImmutableMap.Builder<QuameterProteinGroup, Integer> resultBuilder = new ImmutableBiMap.Builder<QuameterProteinGroup, Integer>();
		for (int i = 0; i < counts.length; i++) {
			resultBuilder.put(quameterProteinGroups.get(i), counts[i]);
		}
		return resultBuilder.build();
	}

	@Override
	public List<QuameterProteinGroup> updateProteinGroups(final List<QuameterProteinGroup> groups) {
		final List<QuameterProteinGroup> oldGroups = listAndCast(allCriteria(QuameterProteinGroup.class));
		final Set<QuameterProteinGroup> current = new ImmutableSet.Builder<QuameterProteinGroup>().addAll(oldGroups).build();
		final Set<QuameterProteinGroup> future = new ImmutableSet.Builder<QuameterProteinGroup>().addAll(groups).build();

		final Sets.SetView<QuameterProteinGroup> toAdd = Sets.difference(future, current);
		final Sets.SetView<QuameterProteinGroup> toRemove = Sets.difference(current, future);
		final Sets.SetView<QuameterProteinGroup> unchanged = Sets.intersection(current, future);

		final List<QuameterProteinGroup> result = new ArrayList<QuameterProteinGroup>(groups.size());

		if (toAdd.size() == 0 && toRemove.size() == 0) {
			LOGGER.debug("The quameter protein groups are unchanged from the previous run");
		} else {
			LOGGER.debug(String.format("Quameter protein groups changed. Adding %d group(s), removing %d group(s)", toAdd.size(), toRemove.size()));

			final Change change = new Change("Updating the protein group list", new DateTime());

			for (final QuameterProteinGroup adding : toAdd) {
				result.add(save(adding, change, true));
			}
			for (final QuameterProteinGroup removing : toRemove) {
				delete(removing, change);
			}

			recalculateProteinCounts();
		}
		result.addAll(unchanged);
		return result;
	}

	@Override
	public void recalculateProteinCounts() {
		// LOGGER.info("Recalculating the protein counts as the protein groups changed. This might take a long time");
	}

	@Override
	public Collection<String> getHibernateMappings() {
		final Collection<String> list = Lists.newArrayList(
				MAP + "QuameterResult.hbm.xml",
				MAP + "QuameterAnnotation.hbm.xml",
				MAP + "QuameterProteinGroup.hbm.xml"
		);
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

	@Override
	public String check() {
		return null;  // TODO: Implement this method
	}

	@Override
	public void install(Map<String, String> params) {
		swiftDao.install(params);
		searchDbDao.install(params);
	}
}
