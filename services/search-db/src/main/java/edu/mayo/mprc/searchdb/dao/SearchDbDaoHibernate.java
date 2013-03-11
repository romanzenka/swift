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
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.utilities.progress.PercentDoneReporter;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.util.*;

/**
 * DAO for the search results stored in the database.
 * We use stateless session to speed up the access, as this dao is used for bulk data loading.
 *
 * @author Roman Zenka
 */
public final class SearchDbDaoHibernate extends DaoBase implements RuntimeInitializer, SearchDbDao {
	private SwiftDao swiftDao;
	private FastaDbDao fastaDbDao;

	private final String MAP = "edu/mayo/mprc/searchdb/dao/";
	/**
	 * Max delta for storing the protein/peptide probability.
	 */
	private static final double PROBABILITY_DELTA = 1E-4;

	public SearchDbDaoHibernate() {
	}

	public SearchDbDaoHibernate(final SwiftDao swiftDao, final FastaDbDao fastaDbDao, final DatabasePlaceholder databasePlaceholder) {
		super(databasePlaceholder);
		this.swiftDao = swiftDao;
		this.fastaDbDao = fastaDbDao;
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	public void setSwiftDao(final SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}

	public FastaDbDao getFastaDbDao() {
		return fastaDbDao;
	}

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

	private Criterion localizedModificationEqualityCriteria(final LocalizedModification mod) {
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

	private Criterion identifiedPeptideEqualityCriteria(final IdentifiedPeptide peptide) {
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

	private Criterion matchEqualityCriteria(final PeptideSpectrumMatch match) {
		return Restrictions.conjunction()
				.add(associationEq("peptide", match.getPeptide()))
				.add(nullSafeEq("previousAminoAcid", match.getPreviousAminoAcid()))
				.add(nullSafeEq("nextAminoAcid", match.getNextAminoAcid()))
				.add(doubleEq("bestPeptideIdentificationProbability", match.getBestPeptideIdentificationProbability(), PROBABILITY_DELTA))

				.add(doubleEq("bestSearchEngineScores.mascotDeltaIonScore", match.getBestSearchEngineScores().getMascotDeltaIonScore(), SearchEngineScores.DELTA))
				.add(doubleEq("bestSearchEngineScores.mascotHomologyScore", match.getBestSearchEngineScores().getMascotHomologyScore(), SearchEngineScores.DELTA))
				.add(doubleEq("bestSearchEngineScores.mascotIdentityScore", match.getBestSearchEngineScores().getMascotIdentityScore(), SearchEngineScores.DELTA))
				.add(doubleEq("bestSearchEngineScores.mascotIonScore", match.getBestSearchEngineScores().getMascotIonScore(), SearchEngineScores.DELTA))
				.add(doubleEq("bestSearchEngineScores.sequestDcnScore", match.getBestSearchEngineScores().getSequestDcnScore(), SearchEngineScores.DELTA))
				.add(doubleEq("bestSearchEngineScores.sequestPeptidesMatched", match.getBestSearchEngineScores().getSequestPeptidesMatched(), SearchEngineScores.DELTA))
				.add(doubleEq("bestSearchEngineScores.sequestSpRank", match.getBestSearchEngineScores().getSequestSpRank(), SearchEngineScores.DELTA))
				.add(doubleEq("bestSearchEngineScores.sequestSpScore", match.getBestSearchEngineScores().getSequestSpScore(), SearchEngineScores.DELTA))
				.add(doubleEq("bestSearchEngineScores.sequestXcorrScore", match.getBestSearchEngineScores().getSequestXcorrScore(), SearchEngineScores.DELTA))
				.add(doubleEq("bestSearchEngineScores.tandemHyperScore", match.getBestSearchEngineScores().getTandemHyperScore(), SearchEngineScores.DELTA))
				.add(doubleEq("bestSearchEngineScores.tandemLadderScore", match.getBestSearchEngineScores().getTandemLadderScore(), SearchEngineScores.DELTA))

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

	private Criterion proteinGroupEqualityCriteria(final ProteinGroup group) {
		return Restrictions.conjunction()
				.add(associationEq("proteinSequences", group.getProteinSequences()))
				.add(associationEq("peptideSpectrumMatches", group.getPeptideSpectrumMatches()))
				.add(nullSafeEq("proteinIdentificationProbability", group.getProteinIdentificationProbability()))
				.add(nullSafeEq("numberOfUniquePeptides", group.getNumberOfUniquePeptides()))
				.add(nullSafeEq("numberOfUniqueSpectra", group.getNumberOfUniqueSpectra()))
				.add(nullSafeEq("numberOfTotalSpectra", group.getNumberOfTotalSpectra()))
				.add(nullSafeEq("percentageOfTotalSpectra", group.getPercentageOfTotalSpectra()))
				.add(nullSafeEq("percentageSequenceCoverage", group.getPercentageSequenceCoverage()));
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
	private Criterion sampleEqualityCriteria(final TandemMassSpectrometrySample sample) {
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

	private Criterion searchResultEqualityCriteria(final SearchResult searchResult) {
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

	private Criterion biologicalSampleEqualityCriteria(final BiologicalSample biologicalSample) {
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
	public Analysis addAnalysis(final Analysis analysis, final ReportData reportData, final UserProgressReporter reporter) {

		if (analysis.getId() == null) {
			final BiologicalSampleList originalList = analysis.getBiologicalSamples();
			final PercentRangeReporter analysisRange = new PercentRangeReporter(new PercentDoneReporter(reporter, "Loading analysis into database: "), 0, 1);
			final int numBioSamples = originalList.size();
			if (originalList.getId() == null) {
				final BiologicalSampleList newList = new BiologicalSampleList(numBioSamples);
				int sampleNum = 0;
				for (final BiologicalSample sample : originalList) {
					newList.add(addBiologicalSample(sample, analysisRange.getSubset(numBioSamples, sampleNum)));
					sampleNum++;
				}
				analysis.setBiologicalSamples(addSet(newList));
				analysis.setReportData(reportData);
			}
			return save(analysis, analysisEqualityCriteria(analysis), false);
		}
		return analysis;
	}

	@Override
	public Analysis getAnalysis(final long reportId) {
		return (Analysis) getSession().createCriteria(Analysis.class).add(Restrictions.eq("reportData.id", reportId)).uniqueResult();
	}

	@Override
	public SwiftSearchDefinition getSearchDefinition(long analysisId) {
		final Object searchDefinition = getSession().createQuery("select d from SwiftSearchDefinition d, Analysis a, ReportData r, SearchRun sr where " +
				"r.searchRun = sr and sr.swiftSearch = d.id and a.reportData = r and a.id = :analysisId")
				.setLong("analysisId", analysisId)
				.uniqueResult();
		if(searchDefinition instanceof SwiftSearchDefinition) {
			return (SwiftSearchDefinition)searchDefinition;
		} else {
			ExceptionUtilities.throwCastException(searchDefinition, SwiftSearchDefinition.class);
			return null;
		}
	}

	@Override
	public boolean hasAnalysis(final long reportId) {
		return (Long) getSession().createQuery("select count(*) from Analysis a where a.reportData.id =:reportId")
				.setParameter("reportId", reportId).uniqueResult() > 0;
	}

	@Override
	public List<String> getProteinAccessionNumbers(final ProteinSequenceList proteinSequenceList) {
		return (List<String>) getSession().createQuery("select distinct e.accessionNumber.accnum from ProteinEntry e where e.sequence in (:sequences) order by e.accessionNumber.accnum")
				.setParameterList("sequences", proteinSequenceList.getList())
				.list();
	}

	@Override
	public List<ReportData> getSearchesForAccessionNumber(final String accessionNumber) {
		return (List<ReportData>) getSession().createQuery(
				"select distinct rd from " +
						" Analysis as a" +
						" inner join a.biologicalSamples as bsl" +
						" inner join bsl.list as bs" +
						" inner join bs.searchResults as srl" +
						" inner join srl.list as sr" +
						" inner join sr.proteinGroups as pgl" +
						" inner join pgl.list as pg" +
						" inner join pg.proteinSequences as psl" +
						" inner join psl.list as ps" +
						" inner join a.reportData as rd," +
						" ProteinEntry as pe" +
						" inner join pe.accessionNumber as pac" +
						" where pe.sequence = ps " +
						" and pac.accnum = :accessionNumber" +
						" order by rd.searchRun")
				.setParameter("accessionNumber", accessionNumber)
				.list();
	}

	@Override
	public List<Long> getReportIdsWithoutAnalysis() {
		return (List<Long>) getSession().createQuery("select rd.id from ReportData as rd where " +
				"rd.searchRun.hidden=0 " +
				"and rd.searchRun.swiftSearch is not null " +
				"and not exists (from Analysis as a where a.reportData=rd) order by rd.dateCreated desc").list();
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
	public Map<Integer, List<String>> getAccessionNumbersMapForProteinSequences(Set<Integer> proteinSequenceLists) {
		final HashMap<Integer, List<String>> result = Maps.newHashMap();
		final List list = getSession().createQuery("select distinct psl.id, pa.accnum from" +
				" ProteinSequenceList as psl" +
				" inner join psl.list as ps," +
				" ProteinEntry as pe," +
				" ProteinAccnum as pa" +
				" where pe.sequence = ps" +
				" and pe.accessionNumber = pa" +
				" and psl.id in (:ids)")
				.setParameterList("ids", proteinSequenceLists.toArray())
				.list();

		int lastGroup = -1;
		final Collection<String> numbers = new ArrayList<String>(20);
		for (final Object o : list) {
			final Object[] array = (Object[]) o;
			final int pgId = ((Integer) array[0]).intValue();
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
	public TreeMap<Integer, ProteinSequenceList> getAllProteinSequences(Analysis analysis) {

		final List list = getSession().createQuery("select distinct psl from" +
				" Analysis a" +
				" inner join a.biologicalSamples as bsl" +
				" inner join bsl.list as b" +
				" inner join b.searchResults as srl" +
				" inner join srl.list as r" +
				" inner join r.proteinGroups as pgl" +
				" inner join pgl.list as pg" +
				" inner join pg.proteinSequences as psl" +
				" where a=:a").setParameter("a", analysis).list();

		final TreeMap<Integer, ProteinSequenceList> allProteinGroups = new TreeMap<Integer, ProteinSequenceList>();
		for (final Object o : list) {
			final ProteinSequenceList psl = (ProteinSequenceList) o;
			allProteinGroups.put(psl.getId(), psl);
		}
		return allProteinGroups;
	}

	private Criterion analysisEqualityCriteria(final Analysis analysis) {
		return Restrictions.conjunction()
				.add(nullSafeEq("scaffoldVersion", analysis.getScaffoldVersion()))
				.add(nullSafeEq("analysisDate", analysis.getAnalysisDate()))
				.add(nullSafeEq("reportData.id", analysis.getReportData().getId()))
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
		return Arrays.asList(
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
				MAP + "TandemMassSpectrometrySample.hbm.xml"
		);
	}
}
