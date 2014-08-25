package edu.mayo.mprc.quameterdb;

import com.google.gson.stream.JsonWriter;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.FactoryDescriptor;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.SimpleRunner;
import edu.mayo.mprc.daemon.UiConfigurationProvider;
import edu.mayo.mprc.database.Dao;
import edu.mayo.mprc.quameterdb.dao.QuameterDao;
import edu.mayo.mprc.quameterdb.dao.QuameterProteinGroup;
import edu.mayo.mprc.quameterdb.dao.QuameterResult;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class QuameterUi implements Dao, UiConfigurationProvider {
	public static final String TYPE = "quameterUi";
	public static final String NAME = "QuaMeter User Interface";
	public static final String DESC = "Specialized interface for browsing QuaMeter database";

	private static final String QUAMETER_DB_WORKER = "quameterDb";
	public static final double SEC_TO_MIN = 60.0;

	private final QuameterDao quameterDao;
	private final QuameterDbWorker.Config dbWorkerConfig;
	private final Map<String, String> instrumentMap;
	private static final DateTimeFormatter DATE_FORMAT_1 = DateTimeFormat.forPattern("'Date('yyyy, ").withLocale(Locale.US);
	private static final DateTimeFormatter DATE_FORMAT_2 = DateTimeFormat.forPattern(", d, H, m, s, S')'").withLocale(Locale.US);

	/**
	 * Use this constant to get to a list of quameter categories from the user interface
	 */
	public static final String UI_QUAMETER_CATEGORIES = "swift.quameter.categories";

	public QuameterUi(final QuameterDao quameterDao,
	                  final QuameterDbWorker.Config dbWorkerConfig) {
		this.quameterDao = quameterDao;
		this.dbWorkerConfig = dbWorkerConfig;
		this.instrumentMap = dbWorkerConfig.getInstrumentNameMap();
	}

	@Override
	public void begin() {
		quameterDao.begin();
	}

	@Override
	public void commit() {
		quameterDao.commit();
	}

	@Override
	public void rollback() {
		quameterDao.rollback();
	}

	@Override
	public String qualifyTableName(final String table) {
		return quameterDao.qualifyTableName(table);
	}

	public void dataTableJson(final Writer writer) {

		final List<QuameterProteinGroup> proteinGroups = quameterDao.listProteinGroups();
		final List<QuameterResult> quameterResults = quameterDao.listAllResults();

		final JsonWriter w = new JsonWriter(writer);
		w.setIndent("   ");

		try {
			w.beginObject();
			writeCols(w, proteinGroups);
			writeRows(w, quameterResults, proteinGroups);
			w.endObject();
		} catch (final IOException e) {
			throw new MprcException("Could not render QuaMeter data", e);
		}
	}

	private void writeCols(final JsonWriter writer, final List<QuameterProteinGroup> proteinGroups) throws IOException {
		writer.name("cols");
		writer.beginArray();

		writeCol(writer, "id", "ID", "number"); // Id of the entry
		writeCol(writer, "startTime", "Start Time", "datetime");
		writeCol(writer, "path", "Path", "string");
		writeCol(writer, "duration", "Duration (min)", "number");
		writeCol(writer, "instrument", "Instrument", "string");
		writeCol(writer, "category", "Category", "string");
		writeCol(writer, "searchParameters", "Search parameters ID", "number");
		writeCol(writer, "transaction", "Transaction ID", "number");

		for (final QuameterResult.QuameterColumn column : QuameterResult.QuameterColumn.values()) {
			writeCol(writer, column.name(), QuameterResult.getColumnName(column), "number");
		}

		int proteinGroupId = 1;
		for (final QuameterProteinGroup proteinGroup : proteinGroups) {
			writeCol(writer, "id_" + proteinGroupId, proteinGroup.getName(), "number");
			proteinGroupId++;
		}

		writer.endArray();
	}

	private void writeRow(final JsonWriter writer, final QuameterResult result, final List<QuameterProteinGroup> proteinGroups) throws IOException {
		writer.beginObject()
				.name("c");
		writer.beginArray();

		writeValue(writer, result.getId()); // Id of the entry (for hiding)
		writeValue(writer, result.getSample().getStartTime()); // startTime
		writeValue(writer, result.getSample().getFile().getAbsolutePath()); // path
		writeValue(writer, result.getSample().getRunTimeInSeconds() / SEC_TO_MIN); // duration
		writeValue(writer, mapInstrument(result.getSample().getInstrumentSerialNumber())); // instrument
		writeValue(writer, result.getCategory());
		final SearchEngineParameters parameters = result.getFileSearch().getSearchParameters();
		writeValue(writer, parameters != null ? parameters.getId() : 0); // search parameters id
		writeValue(writer, result.getTransaction());

		for (final QuameterResult.QuameterColumn column : QuameterResult.QuameterColumn.values()) {
			writeValue(writer, result.getValue(column));
		}

		final Map<QuameterProteinGroup, Integer> identifiedSpectra = result.getIdentifiedSpectra();
		for (final QuameterProteinGroup proteinGroup : proteinGroups) {
			final Integer numSpectra = identifiedSpectra.get(proteinGroup);
			writeValue(writer, numSpectra != null ? numSpectra : 0);
		}

		writer.endArray();
		writer.endObject();
	}

	private String mapInstrument(final String instrumentSerialNumber) {
		final String result = instrumentMap.get(instrumentSerialNumber);
		return result == null ? instrumentSerialNumber : result;
	}

	private void writeCol(final JsonWriter writer, final String id, final String label, final String type) throws IOException {
		writer.beginObject()
				.name("id").value(id)
				.name("label").value(label)
				.name("type").value(type)
				.endObject();
	}

	private void writeRows(final JsonWriter writer, final List<QuameterResult> results, final List<QuameterProteinGroup> proteinGroups) throws IOException {
		writer.name("rows");
		writer.beginArray();

		for (final QuameterResult result : results) {
			writeRow(writer, result, proteinGroups);
		}

		writer.endArray();
	}

	private void writeValue(final JsonWriter writer, final double value) throws IOException {
		writer.beginObject().name("v").value(value).endObject();
	}

	private void writeValue(final JsonWriter writer, final String value) throws IOException {
		writer.beginObject().name("v").value(value).endObject();
	}

	private void writeValue(final JsonWriter writer, final int value) throws IOException {
		writer.beginObject().name("v").value(value).endObject();
	}

	private void writeValue(final JsonWriter writer, final DateTime value) throws IOException {
		writeValue(writer, value.toString(DATE_FORMAT_1) + (value.getMonthOfYear() - 1) + value.toString(DATE_FORMAT_2));
	}

	@Override
	public void provideConfiguration(final Map<String, String> currentConfiguration) {
		currentConfiguration.put(UI_QUAMETER_CATEGORIES, dbWorkerConfig.getCategories());
	}

	public static final class Config implements ResourceConfig {
		private ServiceConfig quameterConfig;

		public Config() {
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(QUAMETER_DB_WORKER, writer.save(quameterConfig));
		}

		@Override
		public void load(final ConfigReader reader) {
			quameterConfig = (ServiceConfig) reader.getObject(QUAMETER_DB_WORKER);
		}

		@Override
		public int getPriority() {
			return 0;
		}

		public void setQuameterConfig(final QuameterDbWorker.Config quameterConfig) {
			this.quameterConfig = new ServiceConfig("quameter", new SimpleRunner.Config(quameterConfig));
		}

		public QuameterDbWorker.Config getQuameterConfig() {
			return (QuameterDbWorker.Config) quameterConfig.getRunner().getWorkerConfiguration();
		}
	}

	@Component("quameterUiFactory")
	public static final class Factory extends FactoryBase<Config, QuameterUi> implements FactoryDescriptor {
		@Resource(name = "quameterDao")
		private QuameterDao quameterDao;

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public String getUserName() {
			return NAME;
		}

		@Override
		public String getDescription() {
			return DESC;
		}

		@Override
		public Class<? extends ResourceConfig> getConfigClass() {
			return Config.class;
		}

		@Override
		public ServiceUiFactory getServiceUiFactory() {
			return new Ui();
		}

		@Override
		public QuameterUi create(final Config config, final DependencyResolver dependencies) {
			final QuameterDbWorker.Config dbConfig = config.getQuameterConfig();
			return new QuameterUi(getQuameterDao(), dbConfig);
		}

		public QuameterDao getQuameterDao() {
			return quameterDao;
		}

		public void setQuameterDao(final QuameterDao quameterDao) {
			this.quameterDao = quameterDao;
		}
	}

	public static final class Ui implements ServiceUiFactory {

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(QUAMETER_DB_WORKER, "Quamered Db", "Reference to the worker that loads QuaMeter data to the database")
					.reference(QuameterDbWorker.TYPE, UiBuilder.NONE_TYPE);
		}
	}

}
