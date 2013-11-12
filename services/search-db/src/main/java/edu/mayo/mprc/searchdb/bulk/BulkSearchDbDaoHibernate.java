package edu.mayo.mprc.searchdb.bulk;

import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.searchdb.builder.AnalysisBuilder;
import edu.mayo.mprc.searchdb.dao.*;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.utilities.progress.PercentDoneReporter;
import edu.mayo.mprc.utilities.progress.PercentProgressReporter;
import edu.mayo.mprc.utilities.progress.PercentRangeReporter;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * @author Roman Zenka
 */
@Repository("searchDbDao")
public final class BulkSearchDbDaoHibernate extends SearchDbDaoHibernate implements BulkSearchDbDao {
	/**
	 * How many percent of the time does the bulk loading part take.
	 */
	private static final float BULK_PERCENT = 0.8f;

	public BulkSearchDbDaoHibernate() {
	}

	public BulkSearchDbDaoHibernate(SwiftDao swiftDao, FastaDbDao fastaDbDao, Database database) {
		super(swiftDao, fastaDbDao, database);
	}

	@Override
	public Analysis addAnalysis(final AnalysisBuilder analysisBuilder, final ReportData reportData, final UserProgressReporter reporter) {
		final Analysis analysis = analysisBuilder.build();
		bulkLoad(analysisBuilder, new PercentRangeReporter(new PercentDoneReporter(reporter, "Loading bulk of analysis data into database: "), 0.0f, BULK_PERCENT));
		PercentProgressReporter remainingReporter = new PercentRangeReporter(new PercentDoneReporter(reporter, "Loading remaining analysis data into database: "), BULK_PERCENT, 1.0f);
		return addAnalysis(analysis, reportData, remainingReporter);
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
		reporter.reportProgress((float) 0 / (float) totalSteps);
		getFastaDbDao().addProteinSequences(analysisBuilder.getProteinSequences());
		reporter.reportProgress((float) 1 / (float) totalSteps);
		getFastaDbDao().addPeptideSequences(analysisBuilder.getPeptideSequences());
		reporter.reportProgress((float) 2 / (float) totalSteps);
		addLocalizedModifications(analysisBuilder.getLocalizedModifications());
		reporter.reportProgress((float) 3 / (float) totalSteps);
		addLocalizedModBags(analysisBuilder.calculateLocalizedModBags());
		reporter.reportProgress((float) 4 / (float) totalSteps);
		addIdentifiedPeptides(analysisBuilder.getIdentifiedPeptides());
		reporter.reportProgress((float) 5 / (float) totalSteps);
		addPeptideSpectrumMatches(analysisBuilder.getPeptideSpectrumMatches());
		reporter.reportProgress((float) 6 / (float) totalSteps);
		addPsmLists(analysisBuilder.calculatePsmLists());
		reporter.reportProgress((float) 7 / (float) totalSteps);
		addProteinSequenceLists(analysisBuilder.calculateProteinSequenceLists());
		reporter.reportProgress((float) 8 / (float) totalSteps);
	}

	private void addProteinSequenceLists(final Collection<ProteinSequenceList> lists) {
		final ProteinSequenceListLoader loader = new ProteinSequenceListLoader(getFastaDbDao(), this);
		loader.addObjects(lists);
	}

	private void addPsmLists(final Collection<PsmList> lists) {
		final PsmListLoader loader = new PsmListLoader(getFastaDbDao(), this);
		loader.addObjects(lists);
	}

	private void addPeptideSpectrumMatches(final Collection<PeptideSpectrumMatch> peptideSpectrumMatches) {
		final PeptideSpectrumMatchLoader loader = new PeptideSpectrumMatchLoader(getFastaDbDao(), this);
		loader.addObjects(peptideSpectrumMatches);
	}

	private void addLocalizedModBags(final Collection<LocalizedModBag> localizedModBags) {
		for (final LocalizedModBag bag : localizedModBags) {
			addBag(bag);
		}
	}

	private void addIdentifiedPeptides(final Collection<IdentifiedPeptide> identifiedPeptides) {
		final IdentifiedPeptideLoader loader = new IdentifiedPeptideLoader(getFastaDbDao(), this);
		loader.addObjects(identifiedPeptides);
	}

	private void addLocalizedModifications(final Collection<LocalizedModification> localizedModifications) {
		final LocalizedModificationLoader loader = new LocalizedModificationLoader(getFastaDbDao(), this);
		loader.addObjects(localizedModifications);
	}
}
