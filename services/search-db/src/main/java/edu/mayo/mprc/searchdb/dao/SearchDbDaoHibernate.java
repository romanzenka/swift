package edu.mayo.mprc.searchdb.dao;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.*;
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.fastadb.ProteinSequence;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.utilities.progress.DummyProgressReporter;
import edu.mayo.mprc.utilities.progress.PercentProgressReporter;
import edu.mayo.mprc.utilities.progress.PercentRangeReporter;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

/**
 * DAO for the search results stored in the database.
 * We use stateless session to speed up the access, as this dao is used for bulk data loading.
 *
 * @author Roman Zenka
 */
public class SearchDbDaoHibernate extends DaoBase implements SearchDbDao {
	public static final int IDS_AT_A_TIME = 1000;
	private SwiftDao swiftDao;
	private FastaDbDao fastaDbDao;

	private static final String MAP = "edu/mayo/mprc/searchdb/dao/";

	public SearchDbDaoHibernate() {
	}

	public SearchDbDaoHibernate(final SwiftDao swiftDao, final FastaDbDao fastaDbDao) {
		this.swiftDao = swiftDao;
		this.fastaDbDao = fastaDbDao;
	}

	public SearchDbDaoHibernate(final SwiftDao swiftDao, final FastaDbDao fastaDbDao, final Database database) {
		super(database);
		this.swiftDao = swiftDao;
		this.fastaDbDao = fastaDbDao;
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	@Resource(name = "swiftDao")
	public void setSwiftDao(final SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}

	public FastaDbDao getFastaDbDao() {
		return fastaDbDao;
	}

	@Resource(name = "fastaDbDao")
	public void setFastaDbDao(final FastaDbDao fastaDbDao) {
		this.fastaDbDao = fastaDbDao;
	}

	@Override
	public String check() {
		return null;
	}

	@Override
	public void install(final Map<String, String> params) {
		swiftDao.install(params);
		fastaDbDao.install(params);
		if (params.containsKey("test")) {

			// Build results of two sample analyses
			// Make them 1 month old so they do not retire from filters
			DateTime now = new DateTime();

			final TandemMassSpectrometrySample sample1 = addTandemMassSpectrometrySample(
					new TandemMassSpectrometrySample(
							new File("test.RAW"),
							new DateTime(now.getYear(), 1, 10, 9, 10, 11, 0),
							100,
							1000,
							0,
							"instrument",
							"Orbi123",
							new DateTime(now.getYear(), 1, 10, 10, 20, 30, 0),
							60.0,
							"comment",
							"sample Information")
			);

			final TandemMassSpectrometrySample sample2 = addTandemMassSpectrometrySample(
					new TandemMassSpectrometrySample(
							new File("test2.RAW"),
							new DateTime(now.getYear(), 2, 12, 11, 20, 30, 40),
							200,
							2000,
							0,
							"instrument",
							"Orbi123",
							new DateTime(now.getYear(), 2, 12, 12, 21, 22, 23),
							120.0,
							"comment 2",
							"sample Information 2")
			);


			final ProteinSequenceList sequenceList1 = new ProteinSequenceList();
			// Sequence of K1C10_BOVIN from ShortTest.fasta
			final ProteinSequence sequence1 = new ProteinSequence("MSVRYSSSKQYSSSRSGGGGGGGSSLRISSSKGSLGGGYSSGGFSGGSFSRGSSAGGCFGGSSSIYGGGLGSGFGGGYGS" +
					"SFGGSYGGSFGGGYGGGGFGGGSFGGGSFGGGLGGGFGDGGLISGNQKITMQNLNDRLASYLDKVRALEESNYELEVKIK" +
					"EWYEKYGNSRQREPRDYSKYYQTIDDLKNQIFNLTTDNANILIQVDNARLAADDFRLKYENEVTLRQSVEADINGLRRVL" +
					"DELTLTKTDLEMQIESLTEELAYLKKNHEEEMRDLQNVSTGDVNVEMNAAPGVDLTELLNNMRSQYEQLAEKNRRDAEAW" +
					"FNEKSKELTTEINSNLEQVSSHKSEITELRRTIQGLEIELQSQLALKQSLEASLAETEGRYCVQLSQIQSQISSLEEQLQ" +
					"QIRAETECQNAEYQQLLDIKIRLENEIQTYRSLLEGEGSSGGGSYGGGRGYGGSSGGGGGGYGGGSSSGGYGGGSSSGGG" +
					"HGGSSGGSYGGGSSSGGGHGGGSSSGGHKSTTTGSVGESSSKGPRY");
			sequenceList1.add(sequence1);

			final ProteinSequenceList sequenceList2 = new ProteinSequenceList();
			// Sequence of CONTAM_AAH69161 from ShortTest.fasta
			final ProteinSequence sequence2 = new ProteinSequence("MTCCQTSFCGYPSCSTSGTCGSSCCQPSCCETSCCQPSCCQTSFCGFPSFSTSGTCSSSCCQPSCCETSCCQPSCCQTSS" +
					"CGTGCGIGGGIGYGQEGSSGAVSTRIRWCRPDCRVEGTCLPPCCVVSCTPPTCCQLHHAEASCCRPSYCGQSCCRPVCCC" +
					"YSCEPTC"
			);
			sequenceList2.add(sequence2);

			final ProteinGroup proteinGroup1 = new ProteinGroup(sequenceList1, 0.95, 1, 2, 3, 0.3, 0.2);
			final ProteinGroupList proteinGroups1 = new ProteinGroupList();
			proteinGroups1.add(proteinGroup1);

			final ProteinGroup proteinGroup2 = new ProteinGroup(sequenceList2, 0.90, 4, 5, 6, 0.4, 0.5);
			final ProteinGroupList proteinGroups2 = new ProteinGroupList();
			proteinGroups2.add(proteinGroup2);

			final SearchResult searchResult1 = new SearchResult(sample1, proteinGroups1);
			final SearchResult searchResult2 = new SearchResult(sample2, proteinGroups2);

			final SearchResultList searchResultList1 = new SearchResultList();
			searchResultList1.add(searchResult1);
			searchResultList1.add(searchResult2);

			final BiologicalSample biologicalSample1 = new BiologicalSample("Sample 1", "none", searchResultList1);
			final BiologicalSampleList biologicalSampleList = new BiologicalSampleList();
			biologicalSampleList.add(biologicalSample1);

			final Analysis a = new Analysis("Scaffold_4.3.3", new DateTime(), biologicalSampleList);

			final SearchRun searchRun = swiftDao.getSearchRunForId(1);
			final ReportData reportData = new ReportData(new File("."), new DateTime(), searchRun);
			addAnalysis(a, reportData, new DummyProgressReporter());
		}
	}

	public ProteinGroup addProteinGroup(final ProteinGroup group, final PercentRangeReporter reporter) {
		if (group.getId() == null) {
			final int size = group.getProteinSequences().size();
			int itemsSaved = 0;
			{
				final ProteinSequenceList originalList = group.getProteinSequences();
				if (originalList.getId() == null) {
					final ProteinSequenceList newList = new ProteinSequenceList(size);
					for (final ProteinSequence item : originalList) {
						newList.add(fastaDbDao.addProteinSequence(item));
						if (reporter != null) {
							reporter.reportProgress((float) itemsSaved / (float) size);
						}
						itemsSaved++;
					}
					group.setProteinSequences(addSet(newList));
				}
			}

			return save(group, false);
		}
		return group;
	}

	public TandemMassSpectrometrySample addTandemMassSpectrometrySample(final TandemMassSpectrometrySample sample) {
		if (sample == null) {
			return null;
		}
		if (sample.getId() == null) {
			return save(sample, false);
		}
		return sample;
	}

	public SearchResult addSearchResult(final SearchResult searchResult, final PercentRangeReporter reporter) {
		if (searchResult.getId() == null) {
			searchResult.setMassSpecSample(addTandemMassSpectrometrySample(searchResult.getMassSpecSample()));
			final ProteinGroupList originalList = searchResult.getProteinGroups();
			if (originalList.getId() == null) {
				final int size = originalList.size();
				final ProteinGroupList newList = new ProteinGroupList(size);
				int groupNum = 0;
				for (final ProteinGroup item : originalList) {
					newList.add(addProteinGroup(item, reporter.getSubset(size, groupNum)));
					groupNum++;
				}
				searchResult.setProteinGroups(addSet(newList));
			}
			return save(searchResult, false);
		}
		return searchResult;
	}

	public BiologicalSample addBiologicalSample(final BiologicalSample biologicalSample, final PercentRangeReporter reporter) {
		if (biologicalSample.getId() == null) {
			final SearchResultList originalList = biologicalSample.getSearchResults();
			final int totalResults = originalList.size();
			if (originalList.getId() == null) {
				final SearchResultList newList = new SearchResultList(totalResults);
				int resultNum = 0;
				for (final SearchResult item : originalList) {
					newList.add(addSearchResult(item, reporter.getSubset(totalResults, resultNum)));
					resultNum++;
				}
				biologicalSample.setSearchResults(addSet(newList));
			}
			return save(biologicalSample, false);
		}
		return biologicalSample;
	}

	@Override
	public List<SearchRun> getSearchRunList(final SearchRunFilter filter, final boolean withReports) {
		try {
			final Criteria criteria = filter.makeCriteria(getSession());
			if (withReports) {
				criteria.setFetchMode("reports", FetchMode.JOIN);
				criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			}
			criteria.setCacheable(true)
					.setReadOnly(true);

			return criteria.list();
		} catch (final Exception t) {
			throw new MprcException("Cannot obtain search run status list for filter: " + filter, t);
		}
	}

	@Override
	public Analysis addAnalysis(final Analysis analysis, final ReportData reportData, final PercentProgressReporter reporter) {
		Analysis savedAnalysis = analysis;
		if (analysis.getId() == null) {
			final BiologicalSampleList originalList = analysis.getBiologicalSamples();
			final PercentRangeReporter analysisRange = new PercentRangeReporter(reporter, 0.0f, 1.0f);
			final int numBioSamples = originalList.size();
			if (originalList.getId() == null) {
				final BiologicalSampleList newList = new BiologicalSampleList(numBioSamples);
				int sampleNum = 0;
				for (final BiologicalSample sample : originalList) {
					newList.add(addBiologicalSample(sample, analysisRange.getSubset(numBioSamples, sampleNum)));
					sampleNum++;
				}
				analysis.setBiologicalSamples(addSet(newList));
			}
			savedAnalysis = save(analysis, false);
			analysis.getReports().add(reportData);
			reportData.setAnalysisId(analysis.getId());
		}
		return savedAnalysis;
	}

	@Override
	public Analysis getAnalysis(final ReportData reportData) {
		return (Analysis) getSession()
				.createQuery("from Analysis a where :report in elements(a.reports)")
				.setParameter("report", reportData)
				.uniqueResult();
	}

	@Override
	public SwiftSearchDefinition getSearchDefinition(final long reportId) {
		final Object searchDefinition = getSession().createQuery("select d from SwiftSearchDefinition d, ReportData r, SearchRun sr where " +
				"r.searchRun = sr and sr.swiftSearch = d.id and r.id = :reportId")
				.setLong("reportId", reportId)
				.uniqueResult();
		if (searchDefinition instanceof SwiftSearchDefinition) {
			return (SwiftSearchDefinition) searchDefinition;
		} else {
			ExceptionUtilities.throwCastException(searchDefinition, SwiftSearchDefinition.class);
			return null;
		}
	}

	@Override
	public List<ReportData> getSearchesForAccessionNumber(final String accessionNumber) {
		return listAndCast(getSession().createQuery(
				"select distinct a.reports from " +
						" Analysis as a" +
						" inner join a.biologicalSamples as bsl" +
						" inner join bsl.list as bs" +
						" inner join bs.searchResults as srl" +
						" inner join srl.list as sr" +
						" inner join sr.proteinGroups as pgl" +
						" inner join pgl.list as pg" +
						" inner join pg.proteinSequences as psl" +
						" inner join psl.list as ps, " +
						" ProteinEntry as pe" +
						" inner join pe.accessionNumber as pac" +
						" where pe.sequence = ps " +
						" and pac.accnum = :accessionNumber")
				.setParameter("accessionNumber", accessionNumber));
	}

	public List<SearchRun> fillInInstrumentSerialNumbers(List<SearchRun> searchRuns) {
		if (searchRuns.size() > 0) {
			final Integer[] ids = DatabaseUtilities.getIdList(searchRuns);

			final List runToInstruments = getSession().createQuery(
					"select distinct rd.searchRun.id, ms.instrumentSerialNumber from" +
							" Analysis as a" +
							" inner join a.biologicalSamples as bsl" +
							" inner join bsl.list as bs" +
							" inner join bs.searchResults as srl" +
							" inner join srl.list as sr" +
							" inner join sr.massSpecSample as ms," +
							" ReportData as rd where" +
							" rd.analysisId = a.id and " +
							" rd.searchRun.id in (:ids)"
			).setParameterList("ids", ids)
					.list();

			final SortedSetMultimap<Integer, String> runToInstrumentsMap = TreeMultimap.create();
			for (final Object o : runToInstruments) {
				if (o instanceof Object[]) {
					final Object[] array = (Object[]) o;
					final Integer searchRunId = (Integer) array[0];
					final String instrumentName = (String) array[1];
					runToInstrumentsMap.put(searchRunId, instrumentName);
				}
			}

			for (final SearchRun searchRun : searchRuns) {
				final SortedSet<String> instruments = runToInstrumentsMap.get(searchRun.getId());
				final String instrumentsString = Joiner.on(", ").join(instruments);
				searchRun.setInstruments(instrumentsString);
			}
		}

		return searchRuns;
	}

	@Override
	public List<String> listAllInstrumentSerialNumbers() {
		List<String> serialNumbers = listAndCast(getSession()
				.createQuery("select distinct t.instrumentSerialNumber" +
						" from Analysis as a join" +
						" a.biologicalSamples as bs join" +
						" bs.list as bsl join" +
						" bsl.searchResults as sr join" +
						" sr.list as srl join" +
						" srl.massSpecSample as t" +
						" order by t.instrumentSerialNumber"));

		return serialNumbers;
	}

	@Override
	public List<Long> getReportIdsWithoutAnalysis() {
		return listAndCast(getSession().createQuery("select rd.id from ReportData as rd where " +
				"rd.searchRun.hidden=0 " +
				"and rd.searchRun.swiftSearch is not null " +
				"and rd.analysisId is null order by rd.dateCreated desc"));
	}

	@Override
	public void getTandemMassSpectrometrySamples(final QueryCallback queryCallback) {
		scrollQuery("from TandemMassSpectrometrySample", queryCallback);
	}

	@Override
	public TandemMassSpectrometrySample updateTandemMassSpectrometrySample(final TandemMassSpectrometrySample sample) {
		// Our adding function happens to do exactly what we need
		return addTandemMassSpectrometrySample(sample);
	}

	@Override
	public Map<Integer, List<String>> getAccessionNumbersMapForProteinSequences(final Set<Integer> proteinSequenceLists, final Integer databaseId) {
		final Object[] ids = proteinSequenceLists.toArray();
		final Map<Integer, List<String>> completeMap = Maps.newHashMapWithExpectedSize(ids.length);
		for (int i = 0; i < ids.length; i += IDS_AT_A_TIME) {
			final int endIndex = Math.min(ids.length, i + IDS_AT_A_TIME);
			final Query query = getSession().createQuery("select distinct psl.id, pa.accnum from" +
					" ProteinSequenceList as psl" +
					" inner join psl.list as ps," +
					" ProteinEntry as pe," +
					" ProteinAccnum as pa" +
					" where pe.sequence = ps" +
					" and pe.accessionNumber = pa" +
					(databaseId != null ? " and pe.database.id = :databaseId" : "") +
					" and psl.id in (:ids)")
					.setParameterList("ids", Arrays.copyOfRange(ids, i, endIndex));

			if (databaseId != null) {
				query.setParameter("databaseId", databaseId);
			}
			completeMap.putAll(makeAccnumMap(query));
		}
		return completeMap;
	}

	@Override
	public Map<Integer, List<String>> getAccessionNumbersMapForProteinGroups(final Set<Integer> proteinGroupIds, final Integer databaseId) {
		if (proteinGroupIds.isEmpty()) {
			return Maps.newHashMap();
		}

		final Object[] ids = proteinGroupIds.toArray();

		final Map<Integer, List<String>> completeMap = new HashMap<Integer, List<String>>(ids.length);

		for (int i = 0; i < ids.length; i += IDS_AT_A_TIME) {
			final int endIndex = Math.min(ids.length, i + IDS_AT_A_TIME);
			final Query query = getSession().createQuery("select distinct pg.id, pa.accnum from" +
					" ProteinGroup as pg " +
					" inner join pg.proteinSequences as psl" +
					" inner join psl.list as ps," +
					" ProteinEntry as pe," +
					" ProteinAccnum as pa" +
					" where pe.sequence = ps" +
					" and pe.accessionNumber = pa" +
					(databaseId != null ? " and pe.database.id = :databaseId" : "") +
					" and pg.id in (:ids)")
					.setParameterList("ids", Arrays.copyOfRange(ids, i, endIndex));

			if (databaseId != null) {
				query.setParameter("databaseId", databaseId);
			}
			completeMap.putAll(makeAccnumMap(query));
		}

		return completeMap;
	}

	/**
	 * For given query producing two columns - id and accession number, return a map from
	 * protein id to a list of all accession numbers that belong to it.
	 * <p/>
	 * The results are sorted by the accession number, so whenever that number changes,
	 * we emit a new group.
	 *
	 * @param query Query to execute
	 * @return Resulting map.
	 */
	private static Map<Integer, List<String>> makeAccnumMap(final Query query) {
		final List<Object> list = listAndCast(query);

		final Map<Integer, List<String>> result = Maps.newHashMap();

		int lastGroup = -1;
		final Collection<String> numbers = new ArrayList<String>(20);
		for (final Object o : list) {
			final Object[] array = (Object[]) o;
			final int pgId = (Integer) array[0];
			if (lastGroup == -1) {
				lastGroup = pgId;
			}
			final String accNum = (String) array[1];
			if (pgId != lastGroup) {
				result.put(lastGroup, Lists.newArrayList(numbers));
				numbers.clear();
				lastGroup = pgId;
			}
			numbers.add(accNum);
		}
		if (!list.isEmpty()) {
			result.put(lastGroup, Lists.newArrayList(numbers));
			numbers.clear();
		}
		return result;
	}

	@Override
	public int getScaffoldProteinGroupCount(final String inputFile, final Iterable<ReportData> reports) {
		for (final ReportData reportData : reports) {
			final File reportFile = reportData.getReportFile();
			if (isScaffoldReport(reportFile)) {
				final Analysis analysis = getAnalysis(reportData);
				if (analysis != null) {
					for (final BiologicalSample biologicalSample : analysis.getBiologicalSamples()) {
						try {
							for (final SearchResult searchResult : biologicalSample.getSearchResults()) {
								if (inputFile.equals(searchResult.getMassSpecSample().getFile().getPath())) {
									return searchResult.getProteinGroups().size();
								}
							}
						} catch (final Exception ignore) {
							// SWALLOWED
						}
					}
				}
			}
		}
		return 0;
	}

	@Override
	public SearchResult getSearchResult(final int searchResultId) {
		try {
			final SearchResult data = (SearchResult) getSession().get(SearchResult.class, searchResultId);
			if (data == null) {
				throw new MprcException(String.format("search result id %d was not found.", searchResultId));
			}
			return data;
		} catch (final Exception t) {
			throw new MprcException(String.format("Cannot obtain search result for id %d", searchResultId), t);
		}
	}

	private static boolean isScaffoldReport(final File reportFile) {
		final String extension = FileUtilities.getExtension(reportFile.getName());
		return "sf3".equalsIgnoreCase(extension) || "sfd".equalsIgnoreCase(extension);
	}

	@Override
	public TreeMap<Integer, ProteinSequenceList> getAllProteinSequences(final Analysis analysis) {

		final List<ProteinSequenceList> list = listAndCast(getSession().createQuery("select distinct psl from" +
				" Analysis a" +
				" inner join a.biologicalSamples as bsl" +
				" inner join bsl.list as b" +
				" inner join b.searchResults as srl" +
				" inner join srl.list as r" +
				" inner join r.proteinGroups as pgl" +
				" inner join pgl.list as pg" +
				" inner join pg.proteinSequences as psl" +
				" where a=:a").setParameter("a", analysis));

		final TreeMap<Integer, ProteinSequenceList> allProteinGroups = new TreeMap<Integer, ProteinSequenceList>();
		for (final ProteinSequenceList psl : list) {
			allProteinGroups.put(psl.getId(), psl);
		}
		return allProteinGroups;
	}

	@Override
	public TreeMap<Integer, ProteinGroup> getProteinGroups(final SearchResult searchResult) {
		final TreeMap<Integer, ProteinGroup> allProteinGroups = new TreeMap<Integer, ProteinGroup>();
		for (final ProteinGroup pg : searchResult.getProteinGroups()) {
			allProteinGroups.put(pg.getId(), pg);
		}
		return allProteinGroups;
	}

	/**
	 * Save any kind of set into the database.
	 *
	 * @param bag List to save.
	 * @param <T> Type of the list, must extend {@link PersistableBagBase}
	 * @return Saved list (or the same one in case it was saved already).
	 */
	protected <T extends PersistableHashedBagBase<?>> T addBag(final T bag) {
		if (bag.getId() == null) {
			return updateHashedBag(bag);
		}
		return bag;
	}

	@Override
	public Collection<String> getHibernateMappings() {
		final List<String> list = new ArrayList<String>(Arrays.asList(
				MAP + "Analysis.hbm.xml",
				MAP + "BiologicalSample.hbm.xml",
				MAP + "BiologicalSampleList.hbm.xml",
				MAP + "ProteinGroup.hbm.xml",
				MAP + "ProteinGroupList.hbm.xml",
				MAP + "ProteinSequenceList.hbm.xml",
				MAP + "SearchResult.hbm.xml",
				MAP + "SearchResultList.hbm.xml",
				MAP + "TandemMassSpectrometrySample.hbm.xml"
		));
		list.addAll(super.getHibernateMappings());
		return list;
	}
}
