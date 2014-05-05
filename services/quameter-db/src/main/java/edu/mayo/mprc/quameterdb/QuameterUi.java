package edu.mayo.mprc.quameterdb;

import com.google.common.base.Strings;
import com.google.gson.stream.JsonWriter;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.FactoryBase;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.*;
import edu.mayo.mprc.daemon.UiConfigurationProvider;
import edu.mayo.mprc.database.Dao;
import edu.mayo.mprc.quameterdb.dao.QuameterDao;
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Roman Zenka
 */
public final class QuameterUi implements Dao, UiConfigurationProvider {
	public static final String TYPE = "quameterUi";
	public static final String NAME = "QuaMeter User Interface";
	public static final String DESC = "Specialized interface for browsing QuaMeter database";

	private static final String SEARCH_FILTER = "searchFilter";
	public static final double SEC_TO_MIN = 60.0;

	private final QuameterDao quameterDao;
	private final Pattern searchFilter;
	private final String categories;
	private static final DateTimeFormatter DATE_FORMAT_1 = DateTimeFormat.forPattern("'Date('yyyy, ").withLocale(Locale.US);
	private static final DateTimeFormatter DATE_FORMAT_2 = DateTimeFormat.forPattern(", d, H, m, s, S')'").withLocale(Locale.US);

	public static final String CATEGORIES = "categories";

	/**
	 * Use this constant to get to a list of quameter categories from the user interface
	 */
	public static final String UI_QUAMETER_CATEGORIES = "swift.quameter.categories";

	public QuameterUi(final QuameterDao quameterDao,
	                  final Pattern searchFilter,
	                  final String categories) {
		this.quameterDao = quameterDao;
		this.searchFilter = searchFilter;
		this.categories = categories;
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
		final List<QuameterResult> quameterResults = quameterDao.listAllResults(searchFilter);

		final JsonWriter w = new JsonWriter(writer);
		w.setIndent("");

		try {
			w.beginObject();
			writeCols(w);
			writeRows(w, quameterResults);
			w.endObject();
		} catch (IOException e) {
			throw new MprcException("Could not render QuaMeter data", e);
		}
	}

	private void writeCols(final JsonWriter writer) throws IOException {
		writer.name("cols");
		writer.beginArray();

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

		writer.endArray();
	}

	private void writeRow(final JsonWriter writer, final QuameterResult result) throws IOException {
		writer.beginObject()
				.name("c");
		writer.beginArray();

		writeValue(writer, result.getSample().getStartTime()); // startTime
		writeValue(writer, result.getSample().getFile().getAbsolutePath()); // path
		writeValue(writer, result.getSample().getRunTimeInSeconds() / SEC_TO_MIN); // duration
		writeValue(writer, result.getSample().getInstrumentSerialNumber()); // instrument
		writeValue(writer, result.getCategory());
		writeValue(writer, result.getTransaction());
		final SearchEngineParameters parameters = result.getFileSearch().getSearchParameters();
		writeValue(writer, parameters != null ? parameters.getId() : 0); // search parameters id

		for (final QuameterResult.QuameterColumn column : QuameterResult.QuameterColumn.values()) {
			writeValue(writer, result.getValue(column));
		}


		writer.endArray();
		writer.endObject();
	}

	private void writeCol(final JsonWriter writer, final String id, final String label, final String type) throws IOException {
		writer.beginObject()
				.name("id").value(id)
				.name("label").value(label)
				.name("type").value(type)
				.endObject();
	}

	private void writeRows(final JsonWriter writer, final List<QuameterResult> results) throws IOException {
		writer.name("rows");
		writer.beginArray();

		for (final QuameterResult result : results) {
			writeRow(writer, result);
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
	public void provideConfiguration(Map<String, String> currentConfiguration) {
		currentConfiguration.put(UI_QUAMETER_CATEGORIES, categories);
	}

	public static final class Config extends ResourceConfigBase {
		public Config() {
		}

		void setSearchFilter(final String searchFilter) {
			put(SEARCH_FILTER, searchFilter);
		}

		public String getCategories() {
			return get(CATEGORIES);
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
			final String filterString = config.get(SEARCH_FILTER);
			final String categories = config.get(CATEGORIES);
			final Pattern filter = compileFilter(filterString);
			return new QuameterUi(getQuameterDao(), filter, categories);
		}

		public QuameterDao getQuameterDao() {
			return quameterDao;
		}

		public void setQuameterDao(final QuameterDao quameterDao) {
			this.quameterDao = quameterDao;
		}
	}

	private static Pattern compileFilter(final String filterString) {
		final Pattern filter;
		try {
			filter = Pattern.compile(Strings.isNullOrEmpty(filterString) ? ".*" : filterString);
		} catch (PatternSyntaxException e) {
			throw new MprcException("The pattern syntax [" + filterString + "] is incorrect", e);
		}
		return filter;
	}

	public static final class Ui implements ServiceUiFactory {

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(SEARCH_FILTER, "Search filter", "Only searches whose name matches this regular expression will be considered")
					.required()
					.defaultValue(".*")
					.addChangeListener(new PropertyChangeListener() {
						@Override
						public void propertyChanged(final ResourceConfig config, final String propertyName, final String newValue, final UiResponse response, final boolean validationRequested) {
							if (propertyName.equals(SEARCH_FILTER) && validationRequested) {
								try {
									compileFilter(newValue);
								} catch (MprcException e) {
									response.displayPropertyError(config, propertyName, MprcException.getDetailedMessage(e));
								}
							}
						}

						@Override
						public void fixError(final ResourceConfig config, final String propertyName, final String action) {
						}
					})

					.property(CATEGORIES, "Categories", "Categories for the different QuaMeter quality checks." +
							"<p>You can assign a category to each search. The QuaMeter user interface will then allow you to pick a category" +
							"to filter all the results. The categories are comma-separated. Use a dash in front of a category to form sub-categories.</p>" +
							"<p>Example: <tt>animal,-cat,--siamese,-dog,--chihuahua</tt></p>")
					.defaultValue("no-category");

		}
	}

}
