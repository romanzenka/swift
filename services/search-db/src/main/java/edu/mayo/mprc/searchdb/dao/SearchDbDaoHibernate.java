package edu.mayo.mprc.searchdb.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.RuntimeInitializer;
import edu.mayo.mprc.database.*;
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.fastadb.ProteinSequence;
import edu.mayo.mprc.searchdb.builder.AnalysisBuilder;
import edu.mayo.mprc.searchdb.dao.bulk.*;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.utilities.progress.PercentDoneReporter;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.hibernate.Query;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

/**
 * DAO for the search results stored in the database.
 * We use stateless session to speed up the access, as this dao is used for bulk data loading.
 *
 * @author Roman Zenka
 */
@Repository("searchDbDao")
public final class SearchDbDaoHibernate extends DaoBase implements RuntimeInitializer, SearchDbDao {
	/**
	 * How many percent of the time does the bulk loading part take.
	 */
	private static final float BULK_PERCENT = 0.8f;
	private SwiftDao swiftDao;
	private FastaDbDao fastaDbDao;

	private static final String MAP = "edu/mayo/mprc/searchdb/dao/";
	/**
	 * Max delta for storing the protein/peptide probability.
	 */
	private static final double PROBABILITY_DELTA = 1E-4;

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
	public String check(final Map<String, String> params) {
		return null;
	}

	@Override
	public void initialize(final Map<String, String> params) {
	}

	public LocalizedModification addLocalizedModification(final LocalizedModification mod) {
		if (mod.getId() == null) {
			return save(mod, localizedModificationEqualityCriteria(mod), false);
		}
		return mod;
	}

	private static Criterion localizedModificationEqualityCriteria(final LocalizedModification mod) {
		return Restrictions.conjunction()
				.add(nullSafeEq("position", mod.getPosition()))
				.add(nullSafeEq("residue", mod.getResidue()))
				.add(associationEq("modSpecificity", mod.getModSpecificity()));
	}

	public IdentifiedPeptide addIdentifiedPeptide(final IdentifiedPeptide peptide) {
		if (peptide.getId() == null) {
			final LocalizedModBag originalList = peptide.getModifications();
			if (originalList.getId() == null) {
				final LocalizedModBag newList = new LocalizedModBag(originalList.size());
				for (final LocalizedModification item : originalList) {
					newList.add(addLocalizedModification(item));
				}
				peptide.setModifications(addBag(newList));
			}

			peptide.setSequence(fastaDbDao.addPeptideSequence(peptide.getSequence()));
			try {
				return save(peptide, identifiedPeptideEqualityCriteria(peptide), false);
			} catch (Exception e) {
				throw new MprcException("Could not add identified peptide", e);
			}
		}
		return peptide;
	}

	private static Criterion identifiedPeptideEqualityCriteria(final IdentifiedPeptide peptide) {
		return Restrictions.conjunction()
				.add(associationEq("sequence", peptide.getSequence()))
				.add(associationEq("modifications", peptide.getModifications()));
	}

	public PeptideSpectrumMatch addPeptideSpectrumMatch(final PeptideSpectrumMatch match) {
		if (match.getId() == null) {
			match.setPeptide(addIdentifiedPeptide(match.getPeptide()));
			return save(match, matchEqualityCriteria(match), false);
		}
		return match;
	}

	private static Criterion matchEqualityCriteria(final PeptideSpectrumMatch match) {
		return Restrictions.conjunction()
				.add(associationEq("peptide", match.getPeptide()))
				.add(nullSafeEq("previousAminoAcid", match.getPreviousAminoAcid()))
				.add(nullSafeEq("nextAminoAcid", match.getNextAminoAcid()))
				.add(doubleEq("bestPeptideIdentificationProbability", match.getBestPeptideIdentificationProbability(), PROBABILITY_DELTA))

				.add(Restrictions.eq("spectrumIdentificationCounts.numberOfIdentifiedSpectra", match.getSpectrumIdentificationCounts().getNumberOfIdentifiedSpectra()))
				.add(Restrictions.eq("spectrumIdentificationCounts.numberOfIdentified1HSpectra", match.getSpectrumIdentificationCounts().getNumberOfIdentified1HSpectra()))
				.add(Restrictions.eq("spectrumIdentificationCounts.numberOfIdentified2HSpectra", match.getSpectrumIdentificationCounts().getNumberOfIdentified2HSpectra()))
				.add(Restrictions.eq("spectrumIdentificationCounts.numberOfIdentified3HSpectra", match.getSpectrumIdentificationCounts().getNumberOfIdentified3HSpectra()))
				.add(Restrictions.eq("spectrumIdentificationCounts.numberOfIdentified4HSpectra", match.getSpectrumIdentificationCounts().getNumberOfIdentified4HSpectra()))

				.add(nullSafeEq("spectrumIdentificationCounts", match.getSpectrumIdentificationCounts()))
				.add(nullSafeEq("numberOfEnzymaticTerminii", match.getNumberOfEnzymaticTerminii()));

	}

	public ProteinGroup addProteinGroup(final ProteinGroup group, final PercentRangeReporter reporter) {
		if (group.getId() == null) {
			final int size = group.getProteinSequences().size() + group.getPeptideSpectrumMatches().size();
			int itemsSaved = 0;
			{
				final ProteinSequenceList originalList = group.getProteinSequences();
				if (originalList.getId() == null) {
					final ProteinSequenceList newList = new ProteinSequenceList(size);
					for (final ProteinSequence item : originalList) {
						newList.add(fastaDbDao.addProteinSequence(item));
						reporter.reportDone(size, itemsSaved);
						itemsSaved++;
					}
					group.setProteinSequences(addSet(newList));
				}
			}

			{
				final PsmList originalList = group.getPeptideSpectrumMatches();
				if (originalList.getId() == null) {
					final PsmList newList = new PsmList(originalList.size());
					for (final PeptideSpectrumMatch item : originalList) {
						newList.add(addPeptideSpectrumMatch(item));
						itemsSaved++;
						reporter.reportDone(size, itemsSaved);
					}
					group.setPeptideSpectrumMatches(addSet(newList));
				}
			}

			return save(group, proteinGroupEqualityCriteria(group), false);
		}
		return group;
	}

	private static Criterion proteinGroupEqualityCriteria(final ProteinGroup group) {
		return Restrictions.conjunction()
				.add(associationEq("proteinSequences", group.getProteinSequences()))
				.add(associationEq("peptideSpectrumMatches", group.getPeptideSpectrumMatches()))
				.add(nullSafeEq("proteinIdentificationProbability", group.getProteinIdentificationProbability()))
				.add(nullSafeEq("numberOfUniquePeptides", group.getNumberOfUniquePeptides()))
				.add(nullSafeEq("numberOfUniqueSpectra", group.getNumberOfUniqueSpectra()))
				.add(nullSafeEq("numberOfTotalSpectra", group.getNumberOfTotalSpectra()))
				.add(doubleEq("percentageOfTotalSpectra", group.getPercentageOfTotalSpectra(), ProteinGroup.PERCENT_TOLERANCE))
				.add(doubleEq("percentageSequenceCoverage", group.getPercentageSequenceCoverage(), ProteinGroup.PERCENT_TOLERANCE));
	}

	public TandemMassSpectrometrySample addTandemMassSpectrometrySample(final TandemMassSpectrometrySample sample) {
		if (sample == null) {
			return null;
		}
		if (sample.getId() == null) {
			return save(sample, sampleEqualityCriteria(sample), false);
		}
		return sample;
	}

	/**
	 * Two {@link TandemMassSpectrometrySample} objects are considered identical if they point to the same file.
	 * This way it is possible to update an older extraction of metadata for a file.
	 */
	private static Criterion sampleEqualityCriteria(final TandemMassSpectrometrySample sample) {
		return Restrictions.conjunction()
				.add(nullSafeEq("file", sample.getFile()))
				.add(nullSafeEq("lastModified", sample.getLastModified()));
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
			return save(searchResult, searchResultEqualityCriteria(searchResult), false);
		}
		return searchResult;
	}

	private static Criterion searchResultEqualityCriteria(final SearchResult searchResult) {
		return Restrictions.conjunction()
				.add(associationEq("massSpecSample", searchResult.getMassSpecSample()))
				.add(associationEq("proteinGroups", searchResult.getProteinGroups()));
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
				reporter.reportDone();
			}
			return save(biologicalSample, biologicalSampleEqualityCriteria(biologicalSample), false);
		}
		return biologicalSample;
	}

	private static Criterion biologicalSampleEqualityCriteria(final BiologicalSample biologicalSample) {
		return Restrictions.conjunction()
				.add(nullSafeEq("sampleName", biologicalSample.getSampleName()))
				.add(nullSafeEq("category", biologicalSample.getCategory()))
				.add(associationEq("searchResults", biologicalSample.getSearchResults()));
	}

	/**
	 * Reports percent of a task done within a given range.
	 */
	private final class PercentRangeReporter {
		private final PercentDoneReporter reporter;
		private final float percentFrom;
		private final float percentTo;

		PercentRangeReporter(final PercentDoneReporter reporter, final float percentFrom, final float percentTo) {
			this.reporter = reporter;
			this.percentFrom = percentFrom;
			this.percentTo = percentTo;
		}

		public void reportDone() {
			reporter.reportProgress(percentTo);
		}

		public void reportDone(final int totalChunks, final int chunkNumber) {
			reporter.reportProgress(percentFrom + (percentTo - percentFrom) / totalChunks * chunkNumber);
		}

		/**
		 * Get a percent range by splitting the current range into equally sized chunks and returning a chunk of a given numer.
		 *
		 * @param totalChunks How many chunks.
		 * @param chunkNumber Which chunk we want range for.
		 * @return a reporter going over the specified chunk
		 */
		public PercentRangeReporter getSubset(final int totalChunks, final int chunkNumber) {
			final float chunkPercent = (percentTo - percentFrom) / totalChunks;
			return new PercentRangeReporter(reporter, percentFrom + chunkPercent * chunkNumber, percentFrom + chunkPercent * (chunkNumber + 1));
		}
	}

	@Override
	public Analysis addAnalysis(final AnalysisBuilder analysisBuilder, final ReportData reportData, final UserProgressReporter reporter) {
		final Analysis analysis = analysisBuilder.build();
		bulkLoad(analysisBuilder, new PercentRangeReporter(new PercentDoneReporter(reporter, "Loading bulk of analysis data into database: "), 0.0f, BULK_PERCENT));
		Analysis savedAnalysis = analysis;
		if (analysis.getId() == null) {
			final BiologicalSampleList originalList = analysis.getBiologicalSamples();
			final PercentRangeReporter analysisRange = new PercentRangeReporter(new PercentDoneReporter(reporter, "Loading remaining analysis data into database: "), BULK_PERCENT, 1.0f);
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
			savedAnalysis = save(analysis, analysisEqualityCriteria(analysis), false);
		}
		getSession().saveOrUpdate(reportData);
		reportData.setAnalysisId(savedAnalysis.getId());
		return savedAnalysis;
	}

	/**
	 * This is a mere optimization of the database loading.
	 * The code should work the same even if this entire function is not called at all.
	 * It would just run slower, as it would produce more "select-insert" pairs of queries,
	 * as we never insert the same value twice.
	 *
	 * @param analysisBuilder The analysis to load
	 */
	private void bulkLoad(final AnalysisBuilder analysisBuilder, final PercentRangeReporter reporter) {
		// The order of these operations matters
		// We are bulk-saving the lower level objects before the higher-level ones get saved
		// This way we have always all the data available (like ids of child objects)
		// The reason for this work is to speed the database communication. We want to avoid
		// select / insert call pairs that occur if we update object at a time
		final int totalSteps = 8;
		reporter.reportDone(totalSteps, 0);
		fastaDbDao.addProteinSequences(analysisBuilder.getProteinSequences());
		reporter.reportDone(totalSteps, 1);
		fastaDbDao.addPeptideSequences(analysisBuilder.getPeptideSequences());
		reporter.reportDone(totalSteps, 2);
		addLocalizedModifications(analysisBuilder.getLocalizedModifications());
		reporter.reportDone(totalSteps, 3);
		addLocalizedModBags(analysisBuilder.calculateLocalizedModBags());
		reporter.reportDone(totalSteps, 4);
		addIdentifiedPeptides(analysisBuilder.getIdentifiedPeptides());
		reporter.reportDone(totalSteps, 5);
		addPeptideSpectrumMatches(analysisBuilder.getPeptideSpectrumMatches());
		reporter.reportDone(totalSteps, 6);
		addPsmLists(analysisBuilder.calculatePsmLists());
		reporter.reportDone(totalSteps, 7);
		addProteinSequenceLists(analysisBuilder.calculateProteinSequenceLists());
		reporter.reportDone(totalSteps, 8);
	}

	private void addProteinSequenceLists(final Collection<ProteinSequenceList> lists) {
		final ProteinSequenceListLoader loader = new ProteinSequenceListLoader(fastaDbDao, this);
		loader.addObjects(lists);
	}

	private void addPsmLists(final Collection<PsmList> lists) {
		final PsmListLoader loader = new PsmListLoader(fastaDbDao, this);
		loader.addObjects(lists);
	}

	private void addPeptideSpectrumMatches(final Collection<PeptideSpectrumMatch> peptideSpectrumMatches) {
		final PeptideSpectrumMatchLoader loader = new PeptideSpectrumMatchLoader(fastaDbDao, this);
		loader.addObjects(peptideSpectrumMatches);
	}

	private void addLocalizedModBags(final Collection<LocalizedModBag> localizedModBags) {
		for (final LocalizedModBag bag : localizedModBags) {
			addBag(bag);
		}
	}

	private void addIdentifiedPeptides(final Collection<IdentifiedPeptide> identifiedPeptides) {
		final IdentifiedPeptideLoader loader = new IdentifiedPeptideLoader(fastaDbDao, this);
		loader.addObjects(identifiedPeptides);
	}

	private void addLocalizedModifications(final Collection<LocalizedModification> localizedModifications) {
		final LocalizedModificationLoader loader = new LocalizedModificationLoader(fastaDbDao, this);
		loader.addObjects(localizedModifications);
	}

	@Override
	public Analysis getAnalysis(final int analysisId) {
		return (Analysis) getSession().createCriteria(Analysis.class).add(Restrictions.eq("id", analysisId)).uniqueResult();
	}

	@Override
	public SwiftSearchDefinition getSearchDefinition(final long analysisId) {
		final Object searchDefinition = getSession().createQuery("select d from SwiftSearchDefinition d, ReportData r, SearchRun sr where " +
				"r.searchRun = sr and sr.swiftSearch = d.id and r.analysisId = :analysisId")
				.setLong("analysisId", analysisId)
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
		final HashMap<Integer, List<String>> result = Maps.newHashMap();
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
		final List<Object> list = listAndCast(query);

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

	private static Criterion analysisEqualityCriteria(final Analysis analysis) {
		return Restrictions.conjunction()
				.add(nullSafeEq("scaffoldVersion", analysis.getScaffoldVersion()))
				.add(nullSafeEq("analysisDate", analysis.getAnalysisDate()))
				.add(associationEq("biologicalSamples", analysis.getBiologicalSamples()));
	}

	/**
	 * Save any kind of set into the database.
	 *
	 * @param bag List to save.
	 * @param <T> Type of the list, must extend {@link PersistableBagBase}
	 * @return Saved list (or the same one in case it was saved already).
	 */
	private <T extends PersistableHashedBagBase<?>> T addBag(final T bag) {
		if (bag.getId() == null) {
			return updateHashedBag(bag);
		}
		return bag;
	}

	/**
	 * Save any kind of set into the database.
	 *
	 * @param set List to save.
	 * @param <T> Type of the list, must extend {@link PersistableBagBase}
	 * @return Saved list (or the same one in case it was saved already).
	 */
	private <T extends PersistableHashedSetBase<?>> T addSet(final T set) {
		if (set.getId() == null) {
			return updateHashedSet(set);
		}
		return set;
	}

	@Override
	public Collection<String> getHibernateMappings() {
		final List<String> list = new ArrayList<String>(Arrays.asList(
				MAP + "Analysis.hbm.xml",
				MAP + "BiologicalSample.hbm.xml",
				MAP + "BiologicalSampleList.hbm.xml",
				MAP + "IdentifiedPeptide.hbm.xml",
				MAP + "LocalizedModification.hbm.xml",
				MAP + "LocalizedModList.hbm.xml",
				MAP + "PeptideSpectrumMatch.hbm.xml",
				MAP + "ProteinGroup.hbm.xml",
				MAP + "ProteinGroupList.hbm.xml",
				MAP + "ProteinSequenceList.hbm.xml",
				MAP + "PsmList.hbm.xml",
				MAP + "SearchResult.hbm.xml",
				MAP + "SearchResultList.hbm.xml",
				MAP + "TandemMassSpectrometrySample.hbm.xml",
				MAP + "TempLocalizedModification.hbm.xml",
				MAP + "TempIdentifiedPeptide.hbm.xml",
				MAP + "TempPeptideSpectrumMatch.hbm.xml"
		));
		list.addAll(super.getHibernateMappings());
		return list;
	}
}
