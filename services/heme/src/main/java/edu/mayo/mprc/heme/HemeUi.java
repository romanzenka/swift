package edu.mayo.mprc.heme;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.FactoryDescriptor;
import edu.mayo.mprc.config.ui.ResourceConfigBase;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.database.Dao;
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.heme.dao.HemeDao;
import edu.mayo.mprc.heme.dao.HemeTest;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.swift.params2.SavedSearchEngineParameters;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import edu.mayo.mprc.swift.search.SearchInput;
import edu.mayo.mprc.swift.search.SwiftSearcherCaller;
import edu.mayo.mprc.utilities.FileUtilities;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Heme UI functionality.
 *
 * @author Roman Zenka
 */
public final class HemeUi implements Dao {
	public static final String TYPE = "hemeUi";
	public static final String NAME = "HemePathology User Interface";
	public static final String DESC = "Specialized interface for Heme Pathology";

	private static final String TRYPSIN_SUFFIX = "_T.d";
	private static final String CHYMO_SUFFIX = "_CT.d";
	private static final Pattern DATE_PATTERN = Pattern.compile("(\\d\\d\\d\\d)(\\d\\d)(\\d\\d)");
	private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormat.forPattern("yyyyMMdd");

	/**
	 * Where do the test data go
	 */
	private static final String DATA_PATH = "dataPath";

	/**
	 * Where do the test results go
	 */
	private static final String RESULT_PATH = "resultPath";

	private static final String TRYPSIN_PARAM_SET_NAME = "trypsinParamSetName";
	private static final String CHYMO_PARAM_SET_NAME = "chymoParamSetName";
	private static final String USER_EMAIL = "userEmail";
	private static final String SEARCH_ENGINES = "searchEngines";
	public static final String SPECTRA_EXTENSION = ".spectra.txt";
	public static final double DEFAULT_MASS_DELTA_TOLERANCE = 0.5;
	public static final String[] NO_ENGINES = new String[0];

	private final File data;
	private final File results;
	private final HemeDao hemeDao;
	private final SwiftDao swiftDao;
	private final FastaDbDao fastaDbDao;
	private final ParamsDao paramsDao;
	private SwiftSearcherCaller swiftSearcherCaller;
	private final String trypsinParameterSetName;
	private final String chymoParameterSetName;

	/**
	 * Id of {@link SearchEngineParameters} for the {@link #TRYPSIN_SUFFIX} searches.
	 */
	private int trypsinParameterSetId;

	/**
	 * Id of {@link SearchEngineParameters} for the {@link #CHYMO_SUFFIX} searches.
	 */
	private int chymoParameterSetId;
	private String userEmail;
	private String[] searchEngines;

	public HemeUi(final File data, final File results, final HemeDao hemeDao, final SwiftDao swiftDao,
	              final FastaDbDao fastaDbDao,
	              final ParamsDao paramsDao,
	              final SwiftSearcherCaller swiftSearcherCaller,
	              final String trypsinParameterSetName, final String chymoParameterSetName, final String userEmail,
	              final String[] searchEngines) {
		this.data = data;
		this.results = results;
		this.hemeDao = hemeDao;
		this.swiftDao = swiftDao;
		this.fastaDbDao = fastaDbDao;
		this.paramsDao = paramsDao;
		this.swiftSearcherCaller = swiftSearcherCaller;
		this.trypsinParameterSetName = trypsinParameterSetName;
		this.chymoParameterSetName = chymoParameterSetName;
		this.userEmail = userEmail;
		this.searchEngines = searchEngines == null ? NO_ENGINES : searchEngines.clone();
	}

	@Override
	public void begin() {
		getHemeDao().begin();
	}

	@Override
	public void commit() {
		getHemeDao().commit();
	}

	@Override
	public void rollback() {
		getHemeDao().rollback();
	}

	public List<HemeEntry> getCurrentEntries() {
		final List<HemeEntry> result = new ArrayList<HemeEntry>((int) getHemeDao().countTests());
		for (final HemeTest test : getHemeDao().getAllTests()) {
			result.add(new HemeEntry(test));
		}
		return result;
	}

	/**
	 * Discover all new folders within the filesystem, add them to the database
	 */
	public void scanFileSystem() {
		final List<File> dirs = new ArrayList<File>(100);
		final List<File> patients = new ArrayList<File>(10);
		FileUtilities.listFolderContents(data, null, dirs, null);

		final ImmutableMap<String, HemeTest> existingTestsByPath = getTestsByPath();

		// We only add each test once, but it will correspond to multiple folders (Trypsin, Chymotrypsin)
		// Only the first one makes it, the latter is skipped
		final Set<String/*path*/> alreadyAdded = new HashSet<String>();

		for (final File dateDir : dirs) {
			// The directories must be date strings
			if (DATE_PATTERN.matcher(dateDir.getName()).matches()) {
				final Date date;
				try {
					date = YYYY_MM_DD.parseDateTime(dateDir.getName()).toDate();
				} catch (Exception ignore) {
					// We skip incorrect dates
					continue;
				}

				// The directories have _CT.d or _T.d extension for chymotrypsin/trypsin
				FileUtilities.listFolderContents(dateDir, null, patients, null);
				for (final File patient : patients) {
					final String name = extractPatientName(patient);
					if (name != null) {
						final String path = dateDir.getName() + "/" + name;
						if (!existingTestsByPath.containsKey(path) && !alreadyAdded.contains(path)) {
							final HemeTest test = new HemeTest(name, date, path, 0.0, DEFAULT_MASS_DELTA_TOLERANCE);
							getHemeDao().addTest(test);
							alreadyAdded.add(path);
						}
					}
				}
			}
		}
	}

	/**
	 * Run a Swift search for test of given ID.
	 *
	 * @param testId Test to run
	 */
	public long startSwiftSearch(final int testId) {
		final SearchInput searchInput = new SearchInput();
		begin();
		final HemeTest test;
		try {
			findParameterSetIds();

			test = getHemeDao().getTestForId(testId);

			final String title = test.getName();
			searchInput.setTitle(title);
			searchInput.setUserEmail(getUserEmail());
			searchInput.setOutputFolderName(relativeToBrowseRoot(new File(results, test.getPath())));
			searchInput.setParamSetId(trypsinParameterSetId); // Trypsin is the primary one

			searchInput.setInputFilePaths(new String[]{
					relativeToBrowseRoot(new File(data, test.getPath() + TRYPSIN_SUFFIX)),
					relativeToBrowseRoot(new File(data, test.getPath() + CHYMO_SUFFIX))
			});

			searchInput.setBiologicalSamples(new String[]{
					title,
					title
			});

			searchInput.setCategoryNames(new String[]{
					"none",
					"none"
			});

			searchInput.setExperiments(new String[]{
					title,
					title
			});

			searchInput.setEnabledEngines(getSearchEngines());

			searchInput.setParamSetIds(new int[]{
					trypsinParameterSetId,
					chymoParameterSetId
			});

			searchInput.setPeptideReport(false);
			searchInput.setFromScratch(false);
			searchInput.setLowPriority(false);
			searchInput.setPublicMgfFiles(false);
			searchInput.setPublicSearchFiles(false);
			commit();

		} catch (Exception e) {
			rollback();
			throw new MprcException("Could not start search", e);
		}

		final long searchId = swiftSearcherCaller.startSearchRestful(searchInput);

		begin();
		try {
			getHemeDao().saveOrUpdate(test);
			final SearchRun searchRun = getSwiftDao().getSearchRunForId((int) searchId);
			test.setSearchRun(searchRun);
			commit();
		} catch (Exception e) {
			rollback();
			throw new MprcException("Could not save the started search id with the test database entry", e);
		}

		return searchId;
	}

	private void findParameterSetIds() {
		try {
			trypsinParameterSetId = getIdForParameterSet(trypsinParameterSetName);
			chymoParameterSetId = getIdForParameterSet(chymoParameterSetName);
		} catch (Exception e) {
			throw new MprcException("Could not translate saved parameter sets", e);
		}
	}

	private String relativeToBrowseRoot(final File file) {
		return FileUtilities.getRelativePath(
				swiftSearcherCaller.getBrowseRoot().getPath(), file.getPath());
	}

	/**
	 * Save the mass delta for given {@link HemeTest} id.
	 *
	 * @param testId    Id of the {@link HemeTest} object.
	 * @param massDelta New delta.
	 */
	public void setMassDelta(final int testId, final double massDelta) {
		final HemeTest test = getHemeDao().getTestForId(testId);
		test.setMassDelta(massDelta);
	}

	/**
	 * Save the mass delta tolerance for given {@link HemeTest} id.
	 *
	 * @param testId             Id of the {@link HemeTest} object.
	 * @param massDeltaTolerance New delta tolerance.
	 */
	public void setMassDeltaTolerance(final int testId, final double massDeltaTolerance) {
		final HemeTest test = getHemeDao().getTestForId(testId);
		test.setMassDeltaTolerance(massDeltaTolerance);
	}

	/**
	 * Load all the protein groups from the report.
	 *
	 * @param testId The id of the test to load.
	 * @return Data ready to be passed to the report viewer.
	 */
	public HemeReport createReport(final int testId) {
		final HemeTest test = getHemeDao().getTestForId(testId);
		final String path = test.getPath();
		final File resultFolder = new File(getResults(), path);
		final File scaffoldFolder = new File(resultFolder, "scaffold");
		final File scaffoldFile = new File(scaffoldFolder, test.getName() + SPECTRA_EXTENSION);
		final SwiftSearchDefinition swiftSearchDefinition = swiftDao.getSwiftSearchDefinition(test.getSearchRun().getSwiftSearch());
		final HemeScaffoldReader reader = new HemeScaffoldReader(fastaDbDao, swiftSearchDefinition.getSearchParameters().getDatabase());
		reader.load(scaffoldFile, "3", null);
		final Collection<HemeReportEntry> entries = reader.getEntries();

		// We split all entries into three:
		// These are within the specified range (at least one hit):
		final List<HemeReportEntry> withinRange = new ArrayList<HemeReportEntry>();
		// These have mass delta specified (are some kind of hemoglobin), but they are not within the threshold:
		final List<HemeReportEntry> haveMassDelta = new ArrayList<HemeReportEntry>();
		// Everyone else (contaminants?):
		final List<HemeReportEntry> allOthers = new ArrayList<HemeReportEntry>();
		for (final HemeReportEntry entry : entries) {
			boolean isInRange = false;
			boolean hasMassDelta = false;
			for (final ProteinId proteinId : entry.getProteinIds()) {
				if (proteinId.getMassDelta() != null) {
					hasMassDelta = true;
					if (Math.abs(proteinId.getMassDelta() - test.getMassDelta()) <= test.getMassDeltaTolerance()) {
						isInRange = true;
						break;
					}
				}
			}
			if (isInRange) {
				withinRange.add(entry);
			} else if (hasMassDelta) {
				haveMassDelta.add(entry);
			} else {
				allOthers.add(entry);
			}
		}
		return new HemeReport(test, withinRange, haveMassDelta, allOthers);
	}

	private String extractPatientName(final File patient) {
		String name = null;
		if (patient.getName().endsWith(CHYMO_SUFFIX)) {
			name = patient.getName().substring(0, patient.getName().length() - CHYMO_SUFFIX.length());
		} else if (patient.getName().endsWith(TRYPSIN_SUFFIX)) {
			name = patient.getName().substring(0, patient.getName().length() - TRYPSIN_SUFFIX.length());
		}
		return name;
	}

	private ImmutableMap<String, HemeTest> getTestsByPath() {
		return Maps.uniqueIndex(getHemeDao().getAllTests(), new Function<HemeTest, String>() {
			@Override
			public String apply(@Nullable final HemeTest from) {
				Preconditions.checkNotNull(from, "Programmer error - the heme test was null");
				return from.getPath();
			}
		});
	}

	private int getIdForParameterSet(final String name) {
		final SavedSearchEngineParameters parameters = paramsDao.findSavedSearchEngineParameters(name);
		if (parameters == null) {
			throw new MprcException("HemeUi cannot find parameter set named " + name);
		}
		return parameters.getId();
	}

	public File getData() {
		return data;
	}

	public File getResults() {
		return results;
	}

	public HemeDao getHemeDao() {
		return hemeDao;
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	public SwiftSearcherCaller getSwiftSearcherCaller() {
		return swiftSearcherCaller;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public String[] getSearchEngines() {
		return searchEngines;
	}

	@Component("hemeUiFactory")
	public static final class Factory extends FactoryBase<Config, HemeUi> implements FactoryDescriptor {
		private HemeDao hemeDao;
		private ParamsDao paramsDao;
		private SwiftDao swiftDao;
		private FastaDbDao fastaDbDao;
		private SwiftSearcherCaller swiftSearcherCaller;
		private RunningApplicationContext runningApplicationContext;

		public HemeDao getHemeDao() {
			return hemeDao;
		}

		@Resource(name = "hemeDao")
		public void setHemeDao(final HemeDao hemeDao) {
			this.hemeDao = hemeDao;
		}

		public SwiftDao getSwiftDao() {
			return swiftDao;
		}

		@Resource(name = "swiftDao")
		public void setSwiftDao(final SwiftDao swiftDao) {
			this.swiftDao = swiftDao;
		}

		public ParamsDao getParamsDao() {
			return paramsDao;
		}

		@Resource(name = "paramsDao")
		public void setParamsDao(final ParamsDao paramsDao) {
			this.paramsDao = paramsDao;
		}

		public FastaDbDao getFastaDbDao() {
			return fastaDbDao;
		}

		@Resource(name = "fastaDbDao")
		public void setFastaDbDao(FastaDbDao fastaDbDao) {
			this.fastaDbDao = fastaDbDao;
		}

		public SwiftSearcherCaller getSwiftSearcherCaller() {
			return swiftSearcherCaller;
		}

		@Resource(name = "swiftSearcherCaller")
		public void setSwiftSearcherCaller(final SwiftSearcherCaller swiftSearcherCaller) {
			this.swiftSearcherCaller = swiftSearcherCaller;
		}

		public RunningApplicationContext getRunningApplicationContext() {
			return runningApplicationContext;
		}

		@Resource(name = "swiftEnvironment")
		public void setRunningApplicationContext(final RunningApplicationContext context) {
			this.runningApplicationContext = context;
		}

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
		public HemeUi create(final Config config, final DependencyResolver dependencies) {
			final String rootDir = getRunningApplicationContext().getDaemonConfig().getSharedFileSpacePath();
			final File dataDir = new File(rootDir, config.get(DATA_PATH));
			FileUtilities.ensureFolderExists(dataDir);
			final File resultDir = new File(rootDir, config.get(RESULT_PATH));
			FileUtilities.ensureFolderExists(resultDir);

			final String[] engines = splitEngineString(config.get(SEARCH_ENGINES));

			return new HemeUi(dataDir,
					resultDir,
					getHemeDao(),
					getSwiftDao(),
					getFastaDbDao(),
					getParamsDao(),
					getSwiftSearcherCaller(),
					config.get(TRYPSIN_PARAM_SET_NAME),
					config.get(CHYMO_PARAM_SET_NAME),
					config.get(USER_EMAIL),
					engines);
		}

		private String[] splitEngineString(final String engines) {
			final ArrayList<String> strings = Lists.newArrayList(Splitter.on(' ').omitEmptyStrings().trimResults().split(engines));
			final String[] result = new String[strings.size()];
			return strings.toArray(result);
		}
	}

	public static final class Config extends ResourceConfigBase {
		public Config() {
		}
	}

	public static final class Ui implements ServiceUiFactory {

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(DATA_PATH, "Data path", "Folder containing the heme pathology test data. Every sub-folder in this folder will be displayed")
					.required()
					.existingDirectory()
					.defaultValue("data")

					.property(RESULT_PATH, "Result path", "Folder where the search results will be stored")
					.required()
					.existingDirectory()
					.defaultValue("results")

					.property(TRYPSIN_PARAM_SET_NAME, "Trypsin parameter set name", "Name of the saved parameter set for Trypsin (_T.d samples)")
					.required()

					.property(CHYMO_PARAM_SET_NAME, "Chymotrypsin parameter set name", "Name of the saved parameter set for Chymotrypsin (_CT.d samples)")
					.required()

					.property(USER_EMAIL, "User email", "Email of the user to run searches as. Identifies the user uniquely. See http://&lt;swift url&gt;/service/users.xml for a list.")
					.required()

					.property(SEARCH_ENGINES, "Search engines", "Space separated list of search engine codes in <code>[engine]-[version]</code> format. See http://&lt;swift url&gt;/service/engines.xml for a list.")
					.required()
					.defaultValue("MASCOT-2.4 SEQUEST-v.27 TANDEM-2013.06.15 SCAFFOLD-4.0.7");
		}
	}

}
