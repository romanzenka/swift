package edu.mayo.mprc.quameterdb;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.RuntimeInitializer;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.quameterdb.dao.*;
import edu.mayo.mprc.searchdb.dao.ProteinGroup;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.searchdb.dao.SearchResult;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.StringUtilities;
import edu.mayo.mprc.utilities.progress.PercentDoneReporter;
import edu.mayo.mprc.utilities.progress.PercentProgressReporter;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.type.StandardBasicTypes;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
@Repository("quameterDao")
public final class QuameterDaoHibernate extends DaoBase implements QuameterDao, RuntimeInitializer {
	private static final Logger LOGGER = Logger.getLogger(QuameterDaoHibernate.class);
	private static final String MAP = "edu/mayo/mprc/quameterdb/dao/";
	/**
	 * Files that have _Pre or _Post after the standard prefix (copath, patient, date) are ignored.
	 */
	public static final Pattern PRE_POST = Pattern.compile("^.{14}.*_(Pre|Post).*$", Pattern.CASE_INSENSITIVE);

	private SwiftDao swiftDao;
	private SearchDbDao searchDbDao;

	/**
	 * What items to list
	 */
	private enum ListItems {
		ALL,
		HIDDEN,
		SHOWN,
	}

	public QuameterDaoHibernate() {
	}

	public QuameterDaoHibernate(final SwiftDao swiftDao, final SearchDbDao searchDbDao) {
		this.swiftDao = swiftDao;
		this.searchDbDao = searchDbDao;
	}

	@Override
	public QuameterResult addQuameterScores(
			final int searchResultId, final int fileSearchId,
			final Map<String, Double> values,
			final Map<QuameterProteinGroup, Integer> identifiedSpectra) {
		final SearchResult searchResult = getSearchDbDao().getSearchResult(searchResultId);
		final FileSearch fileSearch = getSwiftDao().getFileSearchForId(fileSearchId);
		final QuameterResult result = new QuameterResult(searchResult, fileSearch, values, identifiedSpectra);

		return save(result, false);
	}

	public List<QuameterProteinGroup> listProteinGroups() {
		return listAll(QuameterProteinGroup.class);
	}

	@Override
	public Collection<QuameterTag> getQuameterTags() {
		final DateTime lowerDateLimit = new DateTime().minusYears(1);

		// Only list annotations that belong to non-hidden quameter results
		final List results = getSession().createQuery("select q, ms " +
				"from QuameterAnnotation as q, " +
				" QuameterResult as r, " +
				" SearchResult as sr, " +
				" TandemMassSpectrometrySample as ms " +
				" WHERE " +
				" q.quameterResultId = r.id " +
				" AND r.searchResult = sr " +
				" AND sr.massSpecSample = ms " +
				" AND r.hidden = false " +
				" AND ms.startTime >= :lowerDateLimit " +
				"ORDER BY r.id, q.metricCode")
				.setParameter("lowerDateLimit", lowerDateLimit)
				.list();

		final ArrayList<QuameterTag> quameterTags = new ArrayList<QuameterTag>(results.size());

		for (final Object o : results) {
			if (o instanceof Object[]) {
				QuameterAnnotation annotation = (QuameterAnnotation) ((Object[]) o)[0];
				TandemMassSpectrometrySample massSpectrometrySample = (TandemMassSpectrometrySample) ((Object[]) o)[1];

				final String metricName = QuameterUi.getMetricName(annotation.getMetricCode());
				if (metricName != null) {
					final File file = massSpectrometrySample.getFile();
					final QuameterTag quameterTag = new QuameterTag(
							StringUtilities.getDirectoryString(file),
							file.getName(),
							massSpectrometrySample.getInstrumentSerialNumber(),
							metricName,
							annotation.getText());
					quameterTags.add(quameterTag);
				}
			}
		}

		return quameterTags;
	}

	@Override
	public List<QuameterResult> listAllResults() {
		return listResults(ListItems.ALL, true);
	}

	@Override
	public List<QuameterResult> listVisibleResults() {
		return listResults(ListItems.SHOWN, true);
	}

	@Override
	public List<QuameterResult> listVisibleResultsAllTime() {
		return listResults(ListItems.SHOWN, false);
	}

	@Override
	public List<QuameterResult> listHiddenResults() {
		return listResults(ListItems.HIDDEN, true);
	}

	/**
	 * @param listedItems Shown/hidden items?
	 * @param timeLimit   Apply the 1-year time limit
	 * @return List of all results matching the criteria
	 */
	private List<QuameterResult> listResults(final ListItems listedItems, final boolean timeLimit) {
		final String hiddenQuery;
		switch (listedItems) {
			case ALL:
				hiddenQuery = "";
				break;
			case HIDDEN:
				hiddenQuery = "q.hidden=1 AND ";
				break;
			case SHOWN:
				hiddenQuery = "q.hidden=0 AND ";
				break;
			default:
				throw new MprcException(String.format("Programmer error - unknown ListItems parameter [%s]", listedItems));
		}

		// We only care about 1 year old data max
		final DateTime lowerDateLimit = new DateTime().minusYears(1);
		final Query query = getSession().createSQLQuery("" +
				"SELECT {q.*}, {t.*}, " +
				" m.metadata_value AS v," +
				" r.search_run_id AS ti," +
				" a.annotation_text as an," +
				" t.sample_file as sf," +
				" d.search_parameters as sp " +
				" FROM " + swiftDao.qualifyTableName("search_run") + " AS r, "
				+ swiftDao.qualifyTableName("file_search") + " AS f, "
				+ swiftDao.qualifyTableName("swift_search_definition") + " AS d, "
				+ swiftDao.qualifyTableName("search_metadata") + " AS m, "
				+ swiftDao.qualifyTableName("tandem_mass_spec_sample") + " AS t, "
				+ swiftDao.qualifyTableName("search_result") + " AS sr, "
				+ swiftDao.qualifyTableName("quameter_result") + " AS q "
				+ " LEFT JOIN "
				+ swiftDao.qualifyTableName("quameter_annotation") + " AS a" +
				" ON a.quameter_result_id = q.quameter_result_id" +
				" AND a.metric_code='hidden' "
				+ " WHERE "
				+ hiddenQuery
				+ " r.hidden=0 AND"
				+ " q.file_search_id = f.file_search_id AND"
				+ " q.search_result_id = sr.search_result_id AND"
				+ " f.swift_search_definition_id = d.swift_search_definition_id AND"
				+ " d.swift_search_definition_id = m.swift_search_definition_id AND"
				+ " d.swift_search_definition_id = r.swift_search AND"
				+ " m.metadata_key='quameter.category' AND"
				+ " t.tandem_mass_spec_sample_id = sr.tandem_mass_spec_sample_id"
				+ (timeLimit ? " AND t.start_time >= :timeStart " : "")
				+ " ORDER BY t.start_time")
				.addEntity("q", QuameterResult.class)
				.addEntity("t", TandemMassSpectrometrySample.class)
				.addScalar("v", StandardBasicTypes.STRING)
				.addScalar("ti", StandardBasicTypes.INTEGER)
				.addScalar("an", StandardBasicTypes.STRING)
				.addScalar("sf", StandardBasicTypes.STRING)
				.addScalar("sp", StandardBasicTypes.INTEGER)
				.setReadOnly(true);

		if (timeLimit) {
			query.setParameter("timeStart", lowerDateLimit.toDate(), StandardBasicTypes.DATE);
		}

		final List raw = query.list();
		final Map<Integer, QuameterResult> filtered = new LinkedHashMap<Integer, QuameterResult>(Math.min(raw.size(), 1000));
		for (final Object o : raw) {
			final Object[] array = (Object[]) o;
			final QuameterResult q = (QuameterResult) array[0];
			final TandemMassSpectrometrySample t = (TandemMassSpectrometrySample) array[1];
			q.setMassSpectrometrySample(t);
			final String category = (String) array[2];
			q.setCategory(category);
			final Integer transactionId = (Integer) array[3];
			q.setTransaction(transactionId);
			final String hideComment = (String) array[4];
			q.setHiddenReason(Strings.nullToEmpty(hideComment));
			final File inputFile = new File((String) array[5]);
			q.setSearchParametersId((Integer) array[6]);

			if (!PRE_POST.matcher(inputFile.getName()).find()) {
				q.initializeReadOnlyIdentifiedSpectra();
				filtered.put(q.getId(), q);
			}
		}

		// Load all the protein groups
		final List<QuameterProteinGroup> proteinGroups = listProteinGroups();
		final Map<Integer, QuameterProteinGroup> proteinGroupMap = new HashMap<Integer, QuameterProteinGroup>(proteinGroups.size());
		for (final QuameterProteinGroup group : proteinGroups) {
			proteinGroupMap.put(group.getId(), group);
		}

		// Load all the protein group counts
		final List groupCounts = getSession().createSQLQuery(""
				+ "SELECT quameter_result_id, quameter_pg_id, unique_spectra FROM "
				+ swiftDao.qualifyTableName("quameter_spectra"))
				.setReadOnly(true)
				.list();

		for (final Object o : groupCounts) {
			final Object[] array = (Object[]) o;
			final Integer quameterResultId = (Integer) array[0];
			final Integer proteinGroupId = (Integer) array[1];
			final Integer uniqueSpectra = (Integer) array[2];

			final QuameterResult result = filtered.get(quameterResultId);
			if (result != null) {
				QuameterProteinGroup pg = proteinGroupMap.get(proteinGroupId);
				if (pg == null) {
					continue;
				}

				result.getReadOnlyIdentifiedSpectra().put(pg, uniqueSpectra);
			}
		}

		return new ArrayList<QuameterResult>(filtered.values());
	}

	@Override
	public void hideQuameterResult(final int quameterResultId, final String hideReason) {
		final QuameterResult quameterResult = (QuameterResult) getSession().get(QuameterResult.class, quameterResultId);
		addAnnotation(new QuameterAnnotation("hidden", quameterResultId, hideReason));
		quameterResult.setHidden(true);
		getSession().saveOrUpdate(quameterResult);
	}

	@Override
	public void unhideQuameterResult(final int quameterResultId, final String unhideReason) {
		final QuameterResult quameterResult = (QuameterResult) getSession().get(QuameterResult.class, quameterResultId);
		addAnnotation(new QuameterAnnotation("hidden", quameterResultId, unhideReason));
		quameterResult.setHidden(false);
		getSession().saveOrUpdate(quameterResult);
	}

	@Override
	public List<QuameterAnnotation> listAnnotations() {
		return listAnnotations(false);
	}

	@Override
	public List<QuameterAnnotation> listHiddenAnnotations() {
		return listAnnotations(true);
	}

	public List<QuameterAnnotation> listAnnotations(boolean hidden) {
		// Only list annotations that belong to non-hidden quameter results
		final List results = getSession().createQuery("select q " +
				"from QuameterAnnotation as q, QuameterResult as r " +
				"WHERE q.quameterResultId = r.id AND r.hidden = :hidden " +
				"ORDER BY r.id, q.metricCode")
				.setParameter("hidden", hidden)
				.list();

		final List<QuameterAnnotation> finalResults = Lists.newArrayListWithCapacity(results.size());

		for (final Object o : results) {
			QuameterAnnotation annotation = (QuameterAnnotation) o;
			finalResults.add(annotation);
		}
		return finalResults;
	}

	@Override
	public QuameterAnnotation addAnnotation(final QuameterAnnotation annotation) {
		if ("".equals(annotation.getText().trim())) {
			getSession()
					.createQuery("delete from QuameterAnnotation a where " +
							"a.metricCode = :metricCode " +
							"and a.quameterResultId = :qrId")
					.setParameter("metricCode", annotation.getMetricCode())
					.setParameter("qrId", annotation.getQuameterResultId())
					.executeUpdate();
			return null;
		} else {
			return save(annotation, false);
		}
	}

	@Override
	public Map<QuameterProteinGroup, Integer> getIdentifiedSpectra(
			final int fileSearchId,
			final int searchResultId,
			final List<QuameterProteinGroup> quameterProteinGroups) {
		final FileSearch fileSearch = swiftDao.getFileSearchForId(fileSearchId);
		final SwiftSearchDefinition swiftSearchDefinition = fileSearch.getSwiftSearchDefinition();
		// Now we know the database in our context

		final SearchResult searchResult = searchDbDao.getSearchResult(searchResultId);
		final TreeMap<Integer, ProteinGroup> proteinGroups = searchDbDao.getProteinGroups(searchResult);
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
					counts[i] += proteinGroup.getNumberOfTotalSpectra();
				}
			}
		}


		final ImmutableMap.Builder<QuameterProteinGroup, Integer> resultBuilder = new ImmutableMap.Builder<QuameterProteinGroup, Integer>();
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

			recalculateProteinCounts(Lists.newArrayList(toAdd));
		}
		result.addAll(unchanged);
		return result;
	}

	/**
	 * When the user changes definitions of the protein groups, the numbers of proteins become inconsistent.
	 * <p/>
	 * The old protein counts would refer to old definitions of the protein groups.
	 * <p/>
	 * At this point we need to recalculate the protein counts on the entire database, to bring all the counts
	 * up to date.
	 * This function is designed to be idempotent. You can freely call it on Swift startup and it should
	 * do nothing if no work is needed.
	 *
	 * @param toAdd - the protein groups that need their numbers calculated. This speeds up the process.
	 */

	public void recalculateProteinCounts(final List<QuameterProteinGroup> toAdd) {
		LOGGER.info("Recalculating the protein counts as the protein groups changed. This might take a long time");

		final List<QuameterResult> quameterResults = listResults(ListItems.ALL, false);
		int step = 0;
		final PercentProgressReporter reporter = new PercentDoneReporter(null, "Updating quameter results: ");
		for (final QuameterResult result : quameterResults) {
			final Map<QuameterProteinGroup, Integer> identifiedSpectra = getIdentifiedSpectra(
					result.getFileSearch().getId(),
					result.getSearchResult().getId(),
					toAdd);
			if (result.getIdentifiedSpectra() == null) {
				result.setIdentifiedSpectra(identifiedSpectra);
			} else {
				for (final QuameterProteinGroup group : toAdd) {
					result.getIdentifiedSpectra().put(group, identifiedSpectra.get(group));
				}
			}
			step++;
			reporter.reportProgress((float) step / (float) quameterResults.size());
			if (step % 100 == 0) {
				getSession().flush();
			}
		}
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
		return null;
	}

	@Override
	public void install(final Map<String, String> params) {
		swiftDao.install(params);
		searchDbDao.install(params);
	}
}
