package edu.mayo.mprc.searchdb;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerBase;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.fastadb.ProteinSequenceTranslator;
import edu.mayo.mprc.fastadb.SingleDatabaseTranslator;
import edu.mayo.mprc.searchdb.builder.MapMassSpecDataExtractor;
import edu.mayo.mprc.searchdb.builder.MassSpecDataExtractor;
import edu.mayo.mprc.searchdb.builder.ScaffoldSpectraSummarizer;
import edu.mayo.mprc.searchdb.bulk.BulkSearchDbDao;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.unimod.Unimod;
import edu.mayo.mprc.unimod.UnimodDao;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

/**
 * Loads search results from a given Swift experiment into the database.
 */
public final class SearchDbWorker extends WorkerBase {
	private BulkSearchDbDao dao;
	private FastaDbDao fastaDbDao;
	private CurationDao curationDao;
	private UnimodDao unimodDao;
	private SwiftDao swiftDao;
	private Unimod databaseUnimod;
	private Unimod scaffoldUnimod;

	public static final String TYPE = "search-db";
	public static final String NAME = "Search Result Loader";
	public static final String DESC = "Loads the search results into a database for fast future queries.";

	private static final String DATABASE = "database";

	public SearchDbWorker(final BulkSearchDbDao dao, final FastaDbDao fastaDbDao, final CurationDao curationDao, final UnimodDao unimodDao, final SwiftDao swiftDao) {
		this.dao = dao;
		this.fastaDbDao = fastaDbDao;
		this.curationDao = curationDao;
		this.unimodDao = unimodDao;
		this.swiftDao = swiftDao;
		loadDatabaseUnimod();
	}

	private void loadDatabaseUnimod() {
		unimodDao.begin();
		try {
			databaseUnimod = unimodDao.load();
			unimodDao.commit();
		} catch (Exception e) {
			unimodDao.rollback();
			throw new MprcException("Could not load unimod from the database", e);
		}
	}

	private void loadScaffoldUnimod(final File scaffoldModSet) {
		scaffoldUnimod = new Unimod();
		scaffoldUnimod.parseUnimodXML(FileUtilities.getInputStream(scaffoldModSet));
	}

	@Override
	public void process(final WorkPacket wp, final UserProgressReporter reporter) {
		final SearchDbWorkPacket workPacket = (SearchDbWorkPacket) wp;
		dao.begin();
		try {
			loadScaffoldUnimod(workPacket.getScaffoldUnimod());
			final ReportData reportData = swiftDao.getReportForId(workPacket.getReportDataId());

			final ProteinSequenceTranslator translator = new SingleDatabaseTranslator(fastaDbDao, curationDao);
			final MassSpecDataExtractor dataExtractor = new MapMassSpecDataExtractor(workPacket.getFileMetaDataMap());
			final ScaffoldSpectraSummarizer summarizer = new ScaffoldSpectraSummarizer(databaseUnimod, scaffoldUnimod, translator, dataExtractor);
			summarizer.load(workPacket.getScaffoldSpectrumReport(), "3", reporter);

			dao.addAnalysis(summarizer.getAnalysisBuilder(), reportData, reporter);
			dao.commit();
		} catch (Exception e) {
			dao.rollback();
			throw new MprcException(
					"Could not load search results from ["
							+ ((SearchDbWorkPacket) wp).getScaffoldSpectrumReport().getAbsolutePath()
							+ "]", e);
		}
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("searchDbWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		private BulkSearchDbDao searchDbDao;
		private FastaDbDao fastaDbDao;
		private CurationDao curationDao;
		private UnimodDao unimodDao;
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

		public UnimodDao getUnimodDao() {
			return unimodDao;
		}

		@Resource(name = "unimodDao")
		public void setUnimodDao(final UnimodDao unimodDao) {
			this.unimodDao = unimodDao;
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
			final SearchDbWorker worker = new SearchDbWorker(getSearchDbDao(), getFastaDbDao(), getCurationDao(), getUnimodDao(), getSwiftDao());
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
