package edu.mayo.mprc.quameterdb;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
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
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.quameterdb.dao.QuameterDao;
import edu.mayo.mprc.quameterdb.dao.QuameterProteinGroup;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
public final class QuameterDbWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(QuameterDbWorker.class);
	private QuameterDao dao;

	public static final String TYPE = "quameter-db";
	public static final String NAME = "QuaMeter Result Loader";
	public static final String DESC = "Loads the QuaMeter results into a database.";

	private static final String DATABASE = "database";
	public static final String CATEGORIES = "categories";
	public static final String PROTEINS = "proteins";

	private final List<QuameterProteinGroup> proteins;

	public QuameterDbWorker(final QuameterDao quameterDao,
	                        final List<QuameterProteinGroup> proteins) {
		this.dao = quameterDao;
		this.proteins = proteins;
	}

	@Override
	public void process(final WorkPacket wp, final File tempWorkFolder, final UserProgressReporter reporter) {
		final QuameterDbWorkPacket workPacket = (QuameterDbWorkPacket) wp;
		dao.begin();
		try {
			final Map<String, Double> map = loadQuameterResultFile(workPacket.getQuameterResultFile());

			final Map<QuameterProteinGroup, Integer> identifiedSpectra = dao.getIdentifiedSpectra(workPacket.getFileSearchId(), proteins);

			dao.addQuameterScores(workPacket.getTandemMassSpectrometrySampleId(),
					workPacket.getFileSearchId(),
					map, identifiedSpectra);
			dao.commit();
		} catch (final Exception e) {
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
		LOGGER.info(String.format("Loading Quameter result file from %s", quameterFile.getAbsolutePath()));
		final Map<String, Double> stringDoubleMap = loadQuameterResultFile(FileUtilities.getReader(quameterFile));
		LOGGER.info(String.format("Loaded %d key-value pairs", stringDoubleMap.size()));
		return stringDoubleMap;
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
				} catch (final NumberFormatException e) {
					throw new MprcException("Value for key [" + key + "] is not numeric: [" + value + "]", e);
				}
			}

		} catch (final IOException e) {
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
			return new QuameterDbWorker(getQuameterDao(),
					parseConfigProteins(config.getCategories(), config.getProteins()));
		}

		private List<QuameterProteinGroup> parseConfigProteins(final String categories, final String proteins) {
			final Iterable<String> categorySplit = Splitter.on(',').trimResults().split(categories);

//			final Iterable<String> split = Splitter.on(',').trimResults().split(proteins);
//			final Function<String, Pattern> stringPatternFunction = new Function<String, Pattern>() {
//				@Override
//				public Pattern apply(final String s) {
//					return Pattern.compile(s);
//				}
//			};
//			return Lists.newArrayList(Iterables.transform(split, stringPatternFunction));
			return new ArrayList<QuameterProteinGroup>(0);
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private Database.Config database;
		private String categories;
		private String proteins;

		public Database.Config getDatabase() {
			return database;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(DATABASE, writer.save(getDatabase()));
			writer.put(CATEGORIES, categories);
			writer.put(PROTEINS, proteins);
		}

		@Override
		public void load(final ConfigReader reader) {
			database = (Database.Config) reader.getObject(DATABASE);
			categories = reader.get(CATEGORIES);
			proteins = reader.get(PROTEINS);
		}

		@Override
		public int getPriority() {
			return 0;
		}

		public String getCategories() {
			return categories;
		}

		public String getProteins() {
			return proteins;
		}
	}

	public static final class Ui implements ServiceUiFactory {

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			final Database.Config database = (Database.Config) daemon.firstResourceOfType(Database.Config.class);

			builder.property(DATABASE, "Database", "Database we will be storing data into")
					.reference(Database.Factory.TYPE, UiBuilder.NONE_TYPE)
					.defaultValue(database)

					.property(CATEGORIES, "Categories", "Categories for the different QuaMeter quality checks." +
							"<p>You can assign a category to each search. The QuaMeter user interface will then allow you to pick a category" +
							"to filter all the results. The categories are comma-separated. Use a dash in front of a category to form sub-categories.</p>" +
							"<p>Example: <tt>animal,-cat,--siamese,-dog,--chihuahua</tt></p>")
					.defaultValue("no-category")

					.property(PROTEINS, "Proteins", "Each category from the upper list needs a regular expression that selects" +
							" protein accessions from that category. The regular expressions are separated by commas." +
							" This enables QuaMeter display to list number of spectra assigned to proteins of interest" +
							"<p>Example: <tt>ANIMAL1|ANIMAL2,CAT1|CAT2|CATS_.*,SIAMESE.*,DOG1|DOG2,CHIHUAHUA_\\d+</tt>")
					.defaultValue(".*");

		}
	}

}
