package edu.mayo.mprc.quameterdb;

import com.google.common.base.Splitter;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerBase;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.quameterdb.dao.QuameterDao;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Roman Zenka
 */
public final class QuameterDbWorker extends WorkerBase {
	private QuameterDao dao;

	public static final String TYPE = "quameter-db";
	public static final String NAME = "QuaMeter Result Loader";
	public static final String DESC = "Loads the QuaMeter results into a database.";

	private static final String DATABASE = "database";

	public QuameterDbWorker(final QuameterDao quameterDao) {
		this.dao = quameterDao;
	}

	@Override
	public void process(final WorkPacket wp, final File tempWorkFolder, final UserProgressReporter reporter) {
		final QuameterDbWorkPacket workPacket = (QuameterDbWorkPacket) wp;
		dao.begin();
		try {
			final Map<String, Double> map = loadQuameterResultFile(workPacket.getQuameterResultFile());
			dao.addQuameterScores(workPacket.getTandemMassSpectrometrySampleId(),
					workPacket.getFileSearchId(),
					map);
			dao.commit();
		} catch (Exception e) {
			dao.rollback();
			throw new MprcException(
					"Could not load QuaMeter results from ["
							+ workPacket.getQuameterResultFile()
							+ "]", e);
		}
	}

	/**
	 * No output files are being published.
	 */
	@Override
	public File createTempWorkFolder() {
		return null;
	}

	static Map<String, Double> loadQuameterResultFile(final File quameterFile) {
		return loadQuameterResultFile(FileUtilities.getReader(quameterFile));
	}

	static Map<String, Double> loadQuameterResultFile(final Reader reader) {
		final Map<String, Double> map = new TreeMap<String, Double>();
		final BufferedReader bufferedReader = new BufferedReader(reader);

		try {
			final String header = bufferedReader.readLine();
			final String body = bufferedReader.readLine();
			final Iterator<String> headerSplit = Splitter.on("\t").split(header).iterator();
			final Iterator<String> bodySplit = Splitter.on("\t").split(body).iterator();
			while (headerSplit.hasNext()) {
				final String key = headerSplit.next();
				if (!bodySplit.hasNext()) {
					throw new MprcException("Malformed quameter file - no value for key [" + key + "]");
				}
				final String value = bodySplit.next();
				if ("Filename".equalsIgnoreCase(key) || "StartTimeStamp".equalsIgnoreCase(key)) {
					continue;
				}
				try {
					final double valueDouble = Double.parseDouble(value);
					map.put(key, valueDouble);
				} catch (NumberFormatException e) {
					throw new MprcException("Value for key [" + key + "] is not numeric: [" + value + "]", e);
				}
			}

		} catch (IOException e) {
			throw new MprcException("Could not read QuaMeter data", e);
		} finally {
			FileUtilities.closeQuietly(bufferedReader);
		}
		return map;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("quameterDbWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		private QuameterDao quameterDao;

		public QuameterDao getQuameterDao() {
			return quameterDao;
		}

		@Resource(name = "quameterDao")
		public void setQuameterDao(final QuameterDao quameterDao) {
			this.quameterDao = quameterDao;
		}

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			return new QuameterDbWorker(getQuameterDao());
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
