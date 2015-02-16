package edu.mayo.mprc.quameterdb;

import com.google.common.collect.Lists;
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
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
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
public final class QuameterUi implements Dao, UiConfigurationProvider, Lifecycle, InstrumentNameMapper {
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

	private static final String LOW = "low";
	private static final String HIGH = "high";
	private static final String RANGE = "range";

	/**
	 * Semitryptic to tryptic ratio
	 */
	public static final String P_3 = "p_3";

	public static final List<QuameterMetric> METRICS = Lists.newArrayList(
			QuameterMetric.builder("c_1a", "C-1A", "Bleed Ratio", LOW, false, "Fraction of peptides with repeat identifications >4 min EARLIER than identification closest to the chromatographic maximum").build(),
			QuameterMetric.builder("c_1b", "C-1B", "Peak Tailing Ratio", LOW, false, "Fraction of peptides with repeat identifications >4 min LATER than identification closest to the chromatographic maximum").build(),
			QuameterMetric.builder("c_2a", "C-2A", "Retention Window", HIGH, false, "Retention time period over which the middle 50% of the identified peptides eluted (minutes)").setLink("help/metrics/retention_spread.html").build(),
			QuameterMetric.builder("duration", "Duration", "Duration", RANGE, false, "Acquisition duration (minutes)").setRange(0.0, 60.0).build(),
			QuameterMetric.builder("c_2b", "C-2B", "ID Rate", HIGH, true, "Rate of peptide identification during the C-2A time range").setLink("help/metrics/peptides_per_minute.html").build(),
			QuameterMetric.builder("c_3a", "C-3A", "Peak Width", LOW, true, "Median identified peak width").setRange(0.0, 40.0).setLink("help/metrics/peak_width.html").build(),
			QuameterMetric.builder("c_3b", "C-3B", "Peak Width Spread", LOW, true, "Interquantile range for peak widths").setRange(0.0, 40.0).setLink("help/metrics/peak_width_variability.html").build(),
			QuameterMetric.builder("c_4a", "C-4A", "Late Peak Width", LOW, false, "Median peak width over <i>last 10%</i> of the elution time").setLink("help/metrics/peak_width.html").build(),
			QuameterMetric.builder("c_4b", "C-4B", "Early Peak Width", LOW, false, "Median peak width over <i>first 10%</i> of the elution time").setLink("help/metrics/peak_width.html").build(),
			QuameterMetric.builder("c_4c", "C-4C", "Middle Peak Width", LOW, false, "Median peak width over <i>middle 10%</i> of the elution time").setLink("help/metrics/peak_width.html").build(),
			QuameterMetric.builder("ds_1a", "DS-1A", "Singly Identified", HIGH, false, "Ratio of singly to doubly identified peptide ions.").setLink("help/metrics/oversampling.html").build(),
			QuameterMetric.builder("ds_1b", "DS-1B", "Triply Identified", HIGH, false, "Ratio of doubly to triply identified peptide ions.").setLink("help/metrics/oversampling.html").build(),
			QuameterMetric.builder("ds_2a", "DS-2A", "MS1 Scans", RANGE, false, "Number of MS1 scans acquired during the C-2A time range").setLink("help/metrics/ms1_spectra.html").build(),
			QuameterMetric.builder("ds_2b", "DS-2B", "MS2 Scans", HIGH, false, "Number of MS2 scans acquired during the C-2A time range").setLink("help/metrics/ms2_spectra.html").build(),
			QuameterMetric.builder("ds_3a", "DS-3A", "Peak Sampling", LOW, false, "Median ratio of the maximum MS1 peak intensity over the MS1 intensity at the sampling time for all identified peptides. We want to capture peak at its apex.").setLink("help/metrics/trigger_point.html").build(),
			QuameterMetric.builder("ds_3b", "DS-3B", "Low Peak Sampling", LOW, false, "Median ratio of the maximum MS1 peak intensity over the MS1 intensity at the sampling time for peptides with peak intensity in bottom 50%. We want to capture peak at its apex even for low-intensity peptides.").setLink("help/metrics/low_intensity_trigger_point.html").build(),
			QuameterMetric.builder("is_1a", "IS-1A", "TIC Drop", LOW, false, "TIC dropped more than 10x in two consecutive MS1 scans (within the C-2A time range)").setLink("help/metrics/spray_instability.html").build(),
			QuameterMetric.builder("is_1b", "IS-1B", "TIC Jump", LOW, false, "TIC jumped more than 10x in two consecutive MS1 scans (within the C-2A time range)").setLink("help/metrics/spray_instability.html").build(),
			QuameterMetric.builder("is_2", "IS-2", "Precursor", RANGE, true, "Median precursor of identified peptide ions").setRange(0.0, 850.0).setLink("help/metrics/precursor_mz.html").build(),
			QuameterMetric.builder("is_3a", "IS-3A", "1+ charge", LOW, false, "Ratio of 1+/2+ identified peptides").setLink("help/metrics/percent_1h.html").build(),
			QuameterMetric.builder("is_3b", "IS-3B", "3+ charge", LOW, false, "Ratio of 3+/2+ identified peptides").setLink("help/metrics/percent_3h.html").build(),
			QuameterMetric.builder("is_3c", "IS-3C", "4+ charge", LOW, false, "Ratio of 4+/2+ identified peptides").setLink("help/metrics/percent_4h.html").build(),
			QuameterMetric.builder("ms1_1", "MS1-1", "MS1 Injection", LOW, false, "Median injection time for MS1 spectra").setLink("help/metrics/ms1_ion_inject_time.html").build(),
			QuameterMetric.builder("ms1_2a", "MS1-2A", "MS1 S/N", HIGH, true, "Ratio of maximum to median signal in MS1 spectra").setRange(0.0, null).setLink("help/metrics/ms1_signal_to_noise.html").build(),
			QuameterMetric.builder("ms1_3a", "MS1-3A", "MS1 Dynamic Range", HIGH, false, "Dynamic range - ratio of 95th and 5th percentile of MS1 maximum identities for identified peptides in C-2A time range").setLink("help/metrics/ms1_dynamic_range.html").build(),
			QuameterMetric.builder("ms1_3b", "MS1-3B", "MS1 Median", HIGH, false, "The median of maximum MS1 intensities for peptides").build(),
			QuameterMetric.builder("ms1_2b", "MS1-2B", "MS1 TIC", HIGH, true, "Median MS1 Total Ion Current").setLink("help/metrics/ms1_total_ion_current.html").build(),
			QuameterMetric.builder("ms1_5a", "MS1-5A", "AMU Error Median", HIGH, false, "Median difference between the theoretical precursor m/z and the measured precursor m/z value as reported in the scan header").setLink("help/metrics/mass_accuracy.html").build(),
			QuameterMetric.builder("ms1_5b", "MS1-5B", "AMU Error Mean", HIGH, false, "Mean absolute difference between the theoretical precursor m/z and the measured precursor m/z value as reported in the scan header").setLink("help/metrics/mass_accuracy.html").build(),
			QuameterMetric.builder("ms1_5c", "MS1-5C", "PPM Error", HIGH, true, "Median precursor mass error in PPM").setLink("help/metrics/mass_accuracy.html").build(),
			QuameterMetric.builder("ms1_5d", "MS1-5D", "PPM Error Range", HIGH, false, "Interquartile range for mass error in PPM").build(),
			QuameterMetric.builder("ms2_1", "MS2-1", "MS2 Injection", LOW, false, "Median injection time for MS2 spectra").setLink("help/metrics/ms2_ion_inject_time.html").build(),
			QuameterMetric.builder("ms2_2", "MS2-2", "MS2 S/N", HIGH, true, "Ratio of maximum to median signal in MS2 spectra").setLink("help/metrics/ms2_signal_to_noise.html").setRange(0.0, null).build(),
			QuameterMetric.builder("ms2_3", "MS2-3", "MS2 Peaks#", RANGE, true, "Median number of MS2 peaks").setRange(0.0, null).setLink("help/metrics/ms2_peaks_per_spectrum.html").build(),
			QuameterMetric.builder("ms2_4a", "MS2-4A", "MS2 ID 1", RANGE, false, "Fraction of MS2 scans identified in the 1st quartile of peptides sorted by MS1 max intensity").build(),
			QuameterMetric.builder("ms2_4b", "MS2-4B", "MS2 ID 2", RANGE, false, "Fraction of MS2 scans identified in the 2nd quartile of peptides sorted by MS1 max intensity").build(),
			QuameterMetric.builder("ms2_4c", "MS2-4C", "MS2 ID 3", RANGE, false, "Fraction of MS2 scans identified in the 3rd quartile of peptides sorted by MS1 max intensity").build(),
			QuameterMetric.builder("ms2_4d", "MS2-4D", "MS2 ID 4", RANGE, false, "Fraction of MS2 scans identified in the 4th quartile of peptides sorted by MS1 max intensity").setLink("help/metrics/ms2_low_intensity_id_rate.html").build(),
			QuameterMetric.builder("p_1", "P-1", "Search Score", HIGH, true, "Median peptide ID score").setRange(0.0, null).setLink("help/metrics/search_score.html").build(),
			QuameterMetric.builder("p_2a", "P-2A", "MS2 Tryptic Spectra", HIGH, false, "Number of MS2 spectra identifying tryptic peptide ions").setRange(0.0, null).build(),
			QuameterMetric.builder("p_2b", "P-2B", "MS2 Tryptic Ions", HIGH, false, "Number of tryptic peptide ions identified").setRange(0.0, null).build(),
			QuameterMetric.builder("p_2c", "P-2C", "Distinct Peptides", HIGH, false, "Number of distinct identified tryptic peptide sequences, ignoring modifications and charge state").setRange(0.0, null).setLink("help/metrics/peptide_count.html").build(),
			QuameterMetric.builder(P_3, "P-3", "Semitryptic Ratio", LOW, true, "Ratio of semitryptic/tryptic peptides").setLink("help/metrics/percent_semi_tryptic.html").build()

			// The protein count groups are added in code
	);

	private boolean running;

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
		final List<QuameterResult> quameterResults = quameterDao.listVisibleResults();

		final JsonWriter w = new JsonWriter(writer);
		w.setIndent("    ");

		try {
			w.beginObject();
			writeCols(w, proteinGroups);
			writeRows(w, quameterResults, proteinGroups);
			w.endObject();
		} catch (final IOException e) {
			throw new MprcException("Could not render QuaMeter data", e);
		}
	}

	public void writeMetricsJson(final Writer writer) {
		try {
			final JsonWriter jsonWriter = new JsonWriter(writer);

			jsonWriter.beginArray();
			writeMetrics(jsonWriter, METRICS);
			writeMetrics(jsonWriter, getProteinCountMetrics());
			jsonWriter.endArray();

			jsonWriter.close();
		} catch (IOException e) {
			throw new MprcException(e);
		}
	}

	private void writeMetrics(final JsonWriter writer, final List<QuameterMetric> metrics) {
		try {
			for (final QuameterMetric metric : metrics) {
				metric.write(writer);
			}
		} catch (Exception e) {
			throw new MprcException("Error writing out metric array", e);
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
		final TandemMassSpectrometrySample massSpecSample = result.getSearchResult().getMassSpecSample();
		writeValue(writer, massSpecSample.getStartTime()); // startTime
		writeValue(writer, massSpecSample.getFile().getAbsolutePath()); // path
		writeValue(writer, massSpecSample.getRunTimeInSeconds() / SEC_TO_MIN); // duration
		writeValue(writer, mapInstrument(massSpecSample.getInstrumentSerialNumber())); // instrument
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

	@Override
	public String mapInstrument(final String instrumentSerialNumber) {
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

	public QuameterDao getQuameterDao() {
		return quameterDao;
	}

	public void initialize() {
		QuameterDao dao = getQuameterDao();
		List<QuameterProteinGroup> proteins = dbWorkerConfig.getProteins();
		// On start we take our protein groups and store them in the database
		dao.begin();
		try {
			dao.updateProteinGroups(proteins);
			dao.commit();
		} catch (Exception e) {
			dao.rollback();
			throw new MprcException("Could not initialize Quameter", e);
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void start() {
		if (!isRunning()) {
			initialize();
			running = true;
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			running = false;
		}
	}

	public List<QuameterMetric> getProteinCountMetrics() {
		final List<QuameterMetric> result = Lists.newArrayList();
		final List<QuameterProteinGroup> proteins = this.dbWorkerConfig.getProteins();
		int count = 0;
		for (final QuameterProteinGroup protein : proteins) {
			count++;
			final String name = protein.getName();
			final QuameterMetric metric = QuameterMetric.builder("id_" + count, name, name + " IDs", "high", true,
					"Number of identified spectra matching proteins in category \"" + name + "\"")
					.build();
			result.add(metric);
		}
		return result;
	}

	public static String getMetricName(final String metricCode) {
		for (final QuameterMetric metric : METRICS) {
			if (metric.getCode().equals(metricCode)) {
				return metric.getName();
			}
		}
		return null;
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
