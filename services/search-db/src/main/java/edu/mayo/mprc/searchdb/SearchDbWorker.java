package edu.mayo.mprc.searchdb;

import com.google.common.collect.Lists;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerBase;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.dbcurator.model.CurationDao;
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.fastadb.ProteinSequenceTranslator;
import edu.mayo.mprc.fastadb.SingleDatabaseTranslator;
import edu.mayo.mprc.searchdb.builder.MapMassSpecDataExtractor;
import edu.mayo.mprc.searchdb.builder.MassSpecDataExtractor;
import edu.mayo.mprc.searchdb.builder.ScaffoldSpectraSummarizer;
import edu.mayo.mprc.searchdb.bulk.BulkSearchDbDao;
import edu.mayo.mprc.searchdb.dao.Analysis;
import edu.mayo.mprc.searchdb.dao.BiologicalSample;
import edu.mayo.mprc.searchdb.dao.SearchResult;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

/**
 * Loads search results from a given Swift experiment into the database.
 */
public final class SearchDbWorker extends WorkerBase {
	private BulkSearchDbDao dao;
	private FastaDbDao fastaDbDao;
	private CurationDao curationDao;
	private SwiftDao swiftDao;

	public static final String TYPE = "search-db";
	public static final String NAME = "Search Result Loader";
	public static final String DESC = "Loads the search results into a database for fast future queries.";

	private static final String DATABASE = "database";

	public SearchDbWorker(final BulkSearchDbDao dao, final FastaDbDao fastaDbDao, final CurationDao curationDao, final SwiftDao swiftDao) {
		this.dao = dao;
		this.fastaDbDao = fastaDbDao;
		this.curationDao = curationDao;
		this.swiftDao = swiftDao;
	}

	@Override
	public void process(final WorkPacket wp, final File tempWorkFolder, final UserProgressReporter reporter) {
		final SearchDbWorkPacket workPacket = (SearchDbWorkPacket) wp;
		dao.begin();
		try {
			final ReportData reportData = swiftDao.getReportForId(workPacket.getReportDataId());

			final ProteinSequenceTranslator translator = new SingleDatabaseTranslator(fastaDbDao, curationDao);
			final MassSpecDataExtractor dataExtractor = new MapMassSpecDataExtractor(workPacket.getFileMetaDataMap());
			final ScaffoldSpectraSummarizer summarizer = new ScaffoldSpectraSummarizer(translator, dataExtractor);
			summarizer.load(workPacket.getScaffoldSpectrumReport(), "3", reporter);

			final Analysis analysis = dao.addAnalysis(summarizer.getAnalysisBuilder(), reportData, reporter);

			final List<SearchDbResultEntry> searchDbResultList = getSavedSearchResults(analysis);

			dao.commit();
			reporter.reportProgress(new SearchDbResult(searchDbResultList, analysis.getId()));
		} catch (Exception e) {
			dao.rollback();
			throw new MprcException(
					"Could not load search results from ["
							+ ((SearchDbWorkPacket) wp).getScaffoldSpectrumReport().getAbsolutePath()
							+ "]", e);
		}
	}

	/**
	 * No temp files created, it is all going into the database.
	 */
	@Override
	public File createTempWorkFolder() {
		return null;
	}

	/**
	 * @param analysis Saved analysis
	 * @return a map from .RAW file to ID of the saved {@link edu.mayo.mprc.searchdb.dao.SearchResult} for that file.
	 */
	private List<SearchDbResultEntry> getSavedSearchResults(final Analysis analysis) {
		final List<SearchDbResultEntry> searchResultList = Lists.newArrayList();
		for (BiologicalSample biologicalSample : analysis.getBiologicalSamples()) {
			for (SearchResult searchResult : biologicalSample.getSearchResults()) {
				if (searchResult.getId() == null) {
					throw new MprcException("Programmer error - the search result should have been already saved.");
				}
				searchResultList.add(new SearchDbResultEntry(searchResult.getMassSpecSample().getFile(), searchResult.getId()));
			}
		}
		return searchResultList;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("searchDbWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		private BulkSearchDbDao searchDbDao;
		private FastaDbDao fastaDbDao;
		private CurationDao curationDao;
		private SwiftDao swiftDao;

		public BulkSearchDbDao getSearchDbDao() {
			return searchDbDao;
		}

		@Resource(name = "searchDbDao")
		public void setSearchDbDao(final BulkSearchDbDao searchDbDao) {
			this.searchDbDao = searchDbDao;
		}

		public FastaDbDao getFastaDbDao() {
			return fastaDbDao;
		}

		@Resource(name = "fastaDbDao")
		public void setFastaDbDao(final FastaDbDao fastaDbDao) {
			this.fastaDbDao = fastaDbDao;
		}

		public CurationDao getCurationDao() {
			return curationDao;
		}

		@Resource(name = "curationDao")
		public void setCurationDao(final CurationDao curationDao) {
			this.curationDao = curationDao;
		}

		public SwiftDao getSwiftDao() {
			return swiftDao;
		}

		@Resource(name = "swiftDao")
		public void setSwiftDao(final SwiftDao swiftDao) {
			this.swiftDao = swiftDao;
		}

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final SearchDbWorker worker = new SearchDbWorker(getSearchDbDao(), getFastaDbDao(), getCurationDao(), getSwiftDao());
			return worker;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private Database.Config database;

		public Database.Config getDatabase() {
			return database;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(DATABASE, writer.save(getDatabase()));
		}

		@Override
		public void load(final ConfigReader reader) {
			database = (Database.Config) reader.getObject(DATABASE);
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			final Database.Config database = (Database.Config) daemon.firstResourceOfType(Database.Config.class);

			builder.property(DATABASE, "Database", "Database we will be storing data into")
					.reference(Database.Factory.TYPE, UiBuilder.NONE_TYPE)
					.defaultValue(database);
		}
	}
}
