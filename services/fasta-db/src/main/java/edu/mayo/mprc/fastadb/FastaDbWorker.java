package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerBase;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.database.DatabaseFactory;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Wrapper around the {@link FastaDbDao#addFastaDatabase} API.
 * <p/>
 * This worker is separate module so we can:
 * <ul>
 * <li>run the database load long before we have search results</li>
 * <li>can run the load on the same node as the database to minimize network traffic</li>
 * </ul>
 *
 * @author Roman Zenka
 */
public class FastaDbWorker extends WorkerBase {
	public static final String TYPE = "fasta-db";
	public static final String NAME = "FASTA Database Loader";
	public static final String DESC = "Loads the FASTA files into a database for easier management.";

	private static final String DATABASE = "database";

	private FastaDbDao fastaDbDao;
	private CurationDao curationDao;

	public FastaDbWorker(final FastaDbDao fastaDbDao, final CurationDao curationDao) {
		this.fastaDbDao = fastaDbDao;
		this.curationDao = curationDao;
	}

	public void process(final WorkPacket wp, final UserProgressReporter reporter) {
		final FastaDbWorkPacket workPacket = (FastaDbWorkPacket) wp;
		curationDao.begin();
		try {
			final Curation database = curationDao.getCuration(workPacket.getCurationId());
			if (database == null) {
				throw new MprcException("Curation #" + workPacket.getCurationId() + " is not in the database.");
			}
			fastaDbDao.addFastaDatabase(database, reporter);
			curationDao.commit();
		} catch (Exception e) {
			curationDao.rollback();
			throw new MprcException("Could not load curation #" + workPacket.getCurationId() + " into the database", e);
		}
	}

	@Override
	public void check() {
		// As long as the database works, this should be ok
		// TODO: Check the database for being functional
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("fastaDbFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		private FastaDbDao fastaDbDao;
		private CurationDao curationDao;

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

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final FastaDbWorker worker = new FastaDbWorker(fastaDbDao, curationDao);
			return worker;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private DatabaseFactory.Config database;

		public DatabaseFactory.Config getDatabase() {
			return database;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(DATABASE, writer.save(getDatabase()));
		}

		@Override
		public void load(final ConfigReader reader) {
			database = (DatabaseFactory.Config) reader.getObject(DATABASE);
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			//TODO: implement me
		}
	}

}
