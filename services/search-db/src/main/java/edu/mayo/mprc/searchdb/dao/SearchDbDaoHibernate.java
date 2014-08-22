package edu.mayo.mprc.searchdb.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.RuntimeInitializer;
import edu.mayo.mprc.database.*;
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.fastadb.ProteinSequence;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.utilities.progress.PercentProgressReporter;
import edu.mayo.mprc.utilities.progress.PercentRangeReporter;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

/**
 * DAO for the search results stored in the database.
 * We use stateless session to speed up the access, as this dao is used for bulk data loading.
 *
 * @author Roman Zenka
 */
public class SearchDbDaoHibernate extends DaoBase implements RuntimeInitializer, SearchDbDao {
	private SwiftDao swiftDao;
	private FastaDbDao fastaDbDao;

	private static final String MAP = "edu/mayo/mprc/searchdb/dao/";

	public SearchDbDaoHibernate() {
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
	public void install(Map<String, String> params) {
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
						reporter.reportProgress((float) itemsSaved / (float) size);
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
	public Analysis addAnalysis(Analysis analysis, ReportData reportData, PercentProgressReporter reporter) {
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
		}
		getSession().saveOrUpdate(reportData);
		reportData.setAnalysisId(savedAnalysis.getId());
		return savedAnalysis;
	}

	@Override
	public Analysis getAnalysis(final int analysisId) {
		return (Analysis) getSession().createCriteria(Analysis.class).add(Restrictions.eq("id", analysisId)).uniqueResult();
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
	public List<String> getProteinAccessionNumbers(final ProteinSequenceList proteinSequenceList) {
		return (List<String>) getSession().createQuery("select distinct e.accessionNumber.accnum from ProteinEntry e where e.sequence in (:sequences) order by e.accessionNumber.accnum")
				.setParameterList("sequences", proteinSequenceList.getList())
				.list();
	}

	@Override
	public List<ReportData> getSearchesForAccessionNumber(final String accessionNumber) {
		return listAndCast(getSession().createQuery(
				"select distinct rd from " +
						" Analysis as a" +
						" inner join a.biologicalSamples as bsl" +
						" inner join bsl.list as bs" +
						" inner join bs.searchResults as srl" +
						" inner join srl.list as sr" +
						" inner join sr.proteinGroups as pgl" +
						" inner join pgl.list as pg" +
						" inner join pg.proteinSequences as psl" +
						" inner join psl.list as ps, " +
						" ReportData as rd," +
						" ProteinEntry as pe" +
						" inner join pe.accessionNumber as pac" +
						" where pe.sequence = ps " +
						" and rd.analysisId = a.id " +
						" and pac.accnum = :accessionNumber")
				.setParameter("accessionNumber", accessionNumber));
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
		final Query query = getSession().createQuery("select distinct psl.id, pa.accnum from" +
				" ProteinSequenceList as psl" +
				" inner join psl.list as ps," +
				" ProteinEntry as pe," +
				" ProteinAccnum as pa" +
				" where pe.sequence = ps" +
				" and pe.accessionNumber = pa" +
				(databaseId != null ? " and pe.database.id = :databaseId" : "") +
				" and psl.id in (:ids)")
				.setParameterList("ids", proteinSequenceLists.toArray());

		if (databaseId != null) {
			query.setParameter("databaseId", databaseId);
		}
		return makeAccnumMap(query);
	}

	@Override
	public Map<Integer, List<String>> getAccessionNumbersMapForProteinGroups(Set<Integer> proteinGroupIds, Integer databaseId) {
		final Query query = getSession().createQuery("select distinct psl.id, pa.accnum from" +
				" ProteinGroup as pg " +
				" inner join pg.proteinSequences as psl" +
				" inner join psl.list as ps," +
				" ProteinEntry as pe," +
				" ProteinAccnum as pa" +
				" where pe.sequence = ps" +
				" and pe.accessionNumber = pa" +
				(databaseId != null ? " and pe.database.id = :databaseId" : "") +
				" and pg.id in (:ids)")
				.setParameterList("ids", proteinGroupIds.toArray());

		if (databaseId != null) {
			query.setParameter("databaseId", databaseId);
		}
		return makeAccnumMap(query);
	}

	/**
	 * For given query producing two columns - id and accession number, return a map from
	 * protein id to a list of all accession numbers that belong to it.
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
			if (isScaffoldReport(reportFile) && reportData.getAnalysisId() != null) {
				final Analysis analysis = getAnalysis(reportData.getAnalysisId());
				if (analysis != null) {
					for (final BiologicalSample biologicalSample : analysis.getBiologicalSamples()) {
						try {
							for (final SearchResult searchResult : biologicalSample.getSearchResults()) {
								if (inputFile.equals(searchResult.getMassSpecSample().getFile().getPath())) {
									return searchResult.getProteinGroups().size();
								}
							}
						} catch (Exception ignore) {
							// SWALLOWED
						}
					}
				}
			}
		}
		return 0;
	}

	@Override
	public TandemMassSpectrometrySample getTandemMassSpectrometrySampleForId(final int tandemMassSpectrometrySampleId) {
		try {
			final TandemMassSpectrometrySample data = (TandemMassSpectrometrySample) getSession().get(TandemMassSpectrometrySample.class, tandemMassSpectrometrySampleId);
			if (data == null) {
				throw new MprcException("tandem mass spec sample id=" + tandemMassSpectrometrySampleId + " was not found.");
			}
			return data;
		} catch (Exception t) {
			throw new MprcException("Cannot obtain tandem mass spec sample for id " + tandemMassSpectrometrySampleId, t);
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
	public TreeMap<Integer, ProteinGroup> getProteinGroupsForSample(final Analysis analysis, final TandemMassSpectrometrySample sample) {
		final List<ProteinGroup> list = listAndCast(getSession().createQuery("select distinct pg from" +
						" Analysis a" +
						" inner join a.biologicalSamples as bsl" +
						" inner join bsl.list as b" +
						" inner join b.searchResults as srl" +
						" inner join srl.list as r" +
						" inner join r.proteinGroups as pgl" +
						" inner join pgl.list as pg" +
						" where a=:a and r.massSpecSample=:sample")
						.setParameter("a", analysis)
						.setParameter("sample", sample)
		);

		final TreeMap<Integer, ProteinGroup> allProteinGroups = new TreeMap<Integer, ProteinGroup>();
		for (final ProteinGroup pg : list) {
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
