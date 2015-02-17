package edu.mayo.mprc.quameterdb;

import com.google.common.base.Splitter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
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
import edu.mayo.mprc.quameterdb.dao.QuameterProteinGroup;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

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
	private SwiftDao swiftDao;

	public static final String TYPE = "quameter-db";
	public static final String NAME = "QuaMeter Result Loader";
	public static final String DESC = "Loads the QuaMeter results into a database.";

	private static final String DATABASE = "database";
	public static final String CATEGORIES = "categories";
	public static final String PROTEINS = "proteins";
	public static final String INSTRUMENT_NAME_MAP = "instrumentNameMap";

	public QuameterDbWorker(final QuameterDao quameterDao, final SwiftDao swiftDao) {
		dao = quameterDao;
		this.swiftDao = swiftDao;
	}

	@Override
	public void process(final WorkPacket wp, final File tempWorkFolder, final UserProgressReporter reporter) {
		final QuameterDbWorkPacket workPacket = (QuameterDbWorkPacket) wp;
		dao.begin();
		try {
			final FileSearch fileSearch = swiftDao.getFileSearchForId(workPacket.getFileSearchId());
			final List<QuameterProteinGroup> proteins = dao.listProteinGroups();
			final String msmsSampleName = FileUtilities.getFileNameWithoutExtension(fileSearch.getInputFile());
			final Map<String, Double> map = loadQuameterResultFile(workPacket.getQuameterResultFile());

			final Map<QuameterProteinGroup, Integer> identifiedSpectra = dao.getIdentifiedSpectra(
					workPacket.getFileSearchId(),
					workPacket.getSearchResultId(),
					proteins);

			final double semiTryptic = getSemiTrypticRatio(workPacket.getScaffoldSpectraFile(), msmsSampleName, reporter);
			map.put(QuameterUi.P_3, semiTryptic);

			dao.addQuameterScores(workPacket.getSearchResultId(),
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

	public static double getSemiTrypticRatio(final File scaffoldSpectraFile, final String msmsSampleName, UserProgressReporter reporter) {
		final SemitrypticRatioScaffoldReader reader = new SemitrypticRatioScaffoldReader(msmsSampleName);
		reader.load(scaffoldSpectraFile, "4.0", reporter);
		return reader.getSemiTrypticRatio();
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
		private SwiftDao swiftDao;

		public QuameterDao getQuameterDao() {
			return quameterDao;
		}

		@Resource(name = "quameterDao")
		public void setQuameterDao(final QuameterDao quameterDao) {
			this.quameterDao = quameterDao;
		}

		public SwiftDao getSwiftDao() {
			return swiftDao;
		}

		@Resource(name = "swiftDao")
		public void setSwiftDao(SwiftDao swiftDao) {
			this.swiftDao = swiftDao;
		}

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final QuameterDbWorker quameterDbWorker = new QuameterDbWorker(getQuameterDao(), getSwiftDao());
			return quameterDbWorker;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private Database.Config database;
		private String categories;
		private String proteins;
		private String instrumentNameMap;

		public Config() {
		}

		public Config(final Database.Config database, final String categories, final String proteins, final String instrumentNameMap) {
			this.database = database;
			this.categories = categories;
			this.proteins = proteins;
			this.instrumentNameMap = instrumentNameMap;
		}

		public static Map<String, String> parseInstrumentNameMap(final String instrumentNameMap) {
			try {
				return parseMap(instrumentNameMap);
			} catch (final Exception e) {
				throw new MprcException("Could not parse instrument name map", e);
			}
		}

		public static List<QuameterProteinGroup> parseConfigProteins(final String proteins) {
			try {
				final Map<String, String> proteinMap = parseMap(proteins);
				final List<QuameterProteinGroup> groups = new ArrayList<QuameterProteinGroup>(proteinMap.size());
				for (final Map.Entry<String, String> entry : proteinMap.entrySet()) {
					final String key = entry.getKey();
					final String value = entry.getValue();
					checkEntry(key, value);
					groups.add(new QuameterProteinGroup(key, value));
				}
				return groups;
			} catch (final Exception e) {
				throw new MprcException("Could not parse protein map", e);
			}
		}

		private static void checkEntry(final String key, final String value) {
			try {
				Pattern.compile(value);
			} catch (final Exception e) {
				throw new MprcException(String.format("Bad pattern for key [%s]", key), e);
			}
		}

		/**
		 * Parse a map stored as key=value, comma separated.
		 * Backslash serves as an escape sequence for = or commas that have to be embedded in the values.
		 *
		 * @param map Map to parse.
		 * @return Parsed map.
		 */
		private static Map<String, String> parseMap(final String map) {
			final Map<String, String> result = new LinkedHashMap<String, String>();
			final JsonElement parse;
			try {
				parse = new JsonParser().parse(map);
			} catch (final JsonSyntaxException e) {
				throw new MprcException("Could not parse map from given JSON string", e);
			}

			if (parse.isJsonNull()) {
				return result;
			}
			if (!parse.isJsonObject()) {
				throw new MprcException("We expected a JSON map");
			}
			final JsonObject jsonObject = parse.getAsJsonObject();
			for (final Map.Entry<String, JsonElement> entries : jsonObject.entrySet()) {
				final String key = entries.getKey();
				final JsonElement jsonValue = entries.getValue();
				if (!jsonValue.isJsonPrimitive() || !jsonValue.getAsJsonPrimitive().isString()) {
					throw new MprcException(String.format("The map value for key [%s] is not a string", key));
				}
				final String value = jsonValue.getAsJsonPrimitive().getAsString();
				result.put(key, value);
			}
			return result;
		}

		public Database.Config getDatabase() {
			return database;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(DATABASE, writer.save(getDatabase()));
			writer.put(CATEGORIES, categories);
			writer.put(PROTEINS, proteins);
			writer.put(INSTRUMENT_NAME_MAP, instrumentNameMap);
		}

		@Override
		public void load(final ConfigReader reader) {
			database = (Database.Config) reader.getObject(DATABASE);
			categories = reader.get(CATEGORIES);
			proteins = reader.get(PROTEINS);
			instrumentNameMap = reader.get(INSTRUMENT_NAME_MAP);
		}

		@Override
		public int getPriority() {
			return 0;
		}

		public String getCategories() {
			return categories;
		}

		public List<QuameterProteinGroup> getProteins() {
			return parseConfigProteins(proteins);
		}

		public Map<String, String> getInstrumentNameMap() {
			return parseInstrumentNameMap(instrumentNameMap);
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
							"to filter all the results. The categories are comma-separated. Use a dash (or multiple dashes) in front of a category to form sub-categories." +
							"An asterisk <tt>*</tt> after the category name designates a default category.</p>" +
							"<p>Example: <tt>animal,-cat,--siamese,-dog,--chihuahua</tt></p>")
					.defaultValue("no-category")

					.property(PROTEINS, "Proteins", "You can define multiple patterns for matching proteins." +
							" This enables QuaMeter display to list number of spectra assigned to proteins of interest." +
							" <p>" +
							" The patterns are named. A pattern named after a category is associated with that category." +
							" A pattern with a different name will produce additional graphs in the QuaMeter user interface," +
							" that are independent on categories.</p>" +
							"" +
							" <p>The map is stored in JSON format as map of string->string. " +
							" You can use a <a href=\"http://jsonformatter.curiousconcept.com/\">validator</a> to check your syntax." +
							" Keep in mind that backslashes, for instance <tt>\\d</tt> need to be doubled: <tt>\\\\d</t></p>" +
							" " +
							"<p>Example: <tt>{\"cat\":\"CAT1|CAT2|CATS_.*\", \"siamese\":\"SIAMESE.*\", \"dog\":\"DOG1|DOG2\", \"contaminants\":\"ALBU_\\\\d*\"}</tt> will" +
							" turn into <tt>cat, siamese, dog</tt> graphs associated with the categories, and <tt>contaminants</tt> graph" +
							" that will be always visible.</p>")
					.defaultValue("")

					.property(INSTRUMENT_NAME_MAP, "Instrument Name Map", "You can rename instruments as stored in .RAW files using this map. " +
							"The instruments not listed will retain their names." +
							"<p>The instrument map is stored in JSON format as map of string->string." +
							"<p>Example: <tt>{\"01475B\":\"Orbi\", \"Exactive Serie 3093\":\"QE1\"}</tt></p>")
					.defaultValue("");

		}
	}

}
