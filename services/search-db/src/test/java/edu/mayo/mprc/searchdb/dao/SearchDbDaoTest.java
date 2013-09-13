package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.DaoTest;
import edu.mayo.mprc.database.DummyFileTokenTranslator;
import edu.mayo.mprc.database.FileType;
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.fastadb.FastaDbDaoHibernate;
import edu.mayo.mprc.fastadb.ProteinSequence;
import edu.mayo.mprc.fastadb.ProteinSequenceTranslator;
import edu.mayo.mprc.searchdb.DummyMassSpecDataExtractor;
import edu.mayo.mprc.searchdb.ScaffoldModificationFormat;
import edu.mayo.mprc.searchdb.builder.*;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.db.SwiftDaoHibernate;
import edu.mayo.mprc.swift.dbmapping.*;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.swift.params2.ParamsDaoHibernate;
import edu.mayo.mprc.unimod.Unimod;
import edu.mayo.mprc.unimod.UnimodDao;
import edu.mayo.mprc.unimod.UnimodDaoHibernate;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDao;
import edu.mayo.mprc.workspace.WorkspaceDaoHibernate;
import org.apache.log4j.Logger;
import org.hibernate.stat.Statistics;
import org.joda.time.DateTime;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

/**
 * @author Roman Zenka
 */
public final class SearchDbDaoTest extends DaoTest {
	private static final Logger LOGGER = Logger.getLogger(SearchDbDaoTest.class);
	public static final int PROTEIN_MIN = 6;
	public static final int PROTEIN_MAX = 20;
	private SearchDbDao searchDbDao;
	private ParamsDao paramsDao;
	private SwiftDao swiftDao;
	private FastaDbDao fastaDbDao;
	private WorkspaceDao workspaceDao;
	private UnimodDao unimodDao;
	private Statistics stats;

	@BeforeMethod
	public void setupDb() {
		FileType.initialize(new DummyFileTokenTranslator());
		SwiftDaoHibernate swiftDaoImpl = new SwiftDaoHibernate();
		ParamsDaoHibernate paramsDaoImpl = new ParamsDaoHibernate();
		FastaDbDaoHibernate fastaDbDaoImpl = new FastaDbDaoHibernate();
		WorkspaceDaoHibernate workspaceDaoImpl = new WorkspaceDaoHibernate();
		final UnimodDaoHibernate unimodDaoImpl = new UnimodDaoHibernate();
		final SearchDbDaoHibernate searchDbDaoImpl = new SearchDbDaoHibernate(swiftDaoImpl, fastaDbDaoImpl, getDatabasePlaceholder());
		initializeDatabase(Arrays.asList(swiftDaoImpl, paramsDaoImpl, fastaDbDaoImpl, workspaceDaoImpl, unimodDaoImpl, searchDbDaoImpl));

		stats = getDatabasePlaceholder().getSessionFactory().getStatistics();

		searchDbDao = searchDbDaoImpl;
		swiftDao = swiftDaoImpl;
		paramsDao = paramsDaoImpl;
		fastaDbDao = fastaDbDaoImpl;
		workspaceDao = workspaceDaoImpl;
		unimodDao = unimodDaoImpl;
	}

	private void startStats() {
		stats.clear();
		stats.setStatisticsEnabled(true);
	}

	@AfterMethod
	public void teardown() {
		searchDbDao = null;
		teardownDatabase();
	}

	@Test
	public void shouldLoadResults() {
		UserProgressReporter reporter = new UserProgressReporter() {
			@Override
			public void reportProgress(ProgressInfo progressInfo) {

			}
		};
		searchDbDao.begin();
		User user = workspaceDao.addNewUser("test", "testovic", "test@test.com", new Change("test user", new DateTime()));

		List<FileSearch> inputFiles = new ArrayList<FileSearch>(1);
		inputFiles.add(new FileSearch(new File("input.mgf"), "sample", "category", "experiment", new EnabledEngines(), null));
		SwiftSearchDefinition searchDefinition = new SwiftSearchDefinition("test", user, new File("out"), null, null, null, inputFiles, false, false, false);
		searchDefinition = swiftDao.addSwiftSearchDefinition(searchDefinition);

		SearchRun searchRun = swiftDao.fillSearchRun(searchDefinition);

		ReportData reportData = swiftDao.storeReport(1, new File("test.sf3"));

		Unimod defaultUnimod = unimodDao.getDefaultUnimod();
		ScaffoldModificationFormat format = new ScaffoldModificationFormat(defaultUnimod, defaultUnimod);

		// Add all the protein sequences
		Collection<ProteinSequence> sequences = new ArrayList<ProteinSequence>(100);
		for (int i = 0; i < 100; i++) {
			sequences.add(new ProteinSequence(getRandomSequence("AC" + i, PROTEIN_MIN, PROTEIN_MAX)));
		}
		fastaDbDao.addProteinSequences(sequences);


		searchDbDao.commit();
		searchDbDao.begin();

		// Start building the analysis
		AnalysisBuilder builder = new AnalysisBuilder(format, new DummyTranslator(), new DummyMassSpecDataExtractor(new DateTime()));
		builder.setReportData(reportData);
		SearchResultListBuilder searchResults = builder.getBiologicalSamples().getBiologicalSample("sample", "category").getSearchResults();
		SearchResultBuilder tandemMassSpecResult = searchResults.getTandemMassSpecResult("test.RAW");
		for (int i = 0; i < 100; i++) {
			ProteinGroupBuilder group = tandemMassSpecResult.getProteinGroups().getProteinGroup("AC" + i, "database", 1, 1, 1, 0.1, 0.2, 0.3);
			PsmListBuilder peptideSpectrumMatches = group.getPeptideSpectrumMatches();
			PeptideSpectrumMatchBuilder peptideSpectrumMatch = peptideSpectrumMatches.getPeptideSpectrumMatch(getRandomSequence("AC" + i, 3, 10), "", "", 'R', 'S', 0);
			peptideSpectrumMatch.recordSpectrum("spectrum" + i, 2, 0.95);
		}

		searchDbDao.commit();

		searchDbDao.begin();
		startStats();
		Analysis analysis = searchDbDao.addAnalysis(builder, reportData, reporter);
		searchDbDao.commit();
	}

	public static final String ACIDS = "RHKDESTNQCGPAVILMFYW";

	public static String getRandomSequence(String accessionNumber, int minLength, int maxLength) {
		Random random = new Random(accessionNumber.hashCode());
		int length = random.nextInt(maxLength - minLength + 1) + minLength;
		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			builder.append(ACIDS.charAt(random.nextInt(ACIDS.length())));
		}

		return builder.toString();
	}


	private static class DummyTranslator implements ProteinSequenceTranslator {
		@Override
		public ProteinSequence getProteinSequence(String accessionNumber, String databaseSources) {
			String sequence = getRandomSequence(accessionNumber, PROTEIN_MIN, PROTEIN_MAX);

			return new ProteinSequence(sequence);
		}

	}
}
