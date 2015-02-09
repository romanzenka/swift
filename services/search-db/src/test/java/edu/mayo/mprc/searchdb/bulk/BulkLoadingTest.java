package edu.mayo.mprc.searchdb.bulk;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.DaoTest;
import edu.mayo.mprc.database.SessionProvider;
import edu.mayo.mprc.dbcurator.model.impl.CurationDaoHibernate;
import edu.mayo.mprc.fastadb.FastaDbDao;
import edu.mayo.mprc.fastadb.FastaDbDaoHibernate;
import edu.mayo.mprc.fastadb.ProteinSequence;
import edu.mayo.mprc.fastadb.ProteinSequenceTranslator;
import edu.mayo.mprc.searchdb.builder.AnalysisBuilder;
import edu.mayo.mprc.searchdb.builder.DummyMassSpecDataExtractor;
import edu.mayo.mprc.searchdb.builder.SearchResultBuilder;
import edu.mayo.mprc.searchdb.builder.SearchResultListBuilder;
import edu.mayo.mprc.searchdb.dao.Analysis;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.db.SwiftDaoHibernate;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.swift.params2.ParamsDaoHibernate;
import edu.mayo.mprc.unimod.UnimodDao;
import edu.mayo.mprc.unimod.UnimodDaoHibernate;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.log.ParentLog;
import edu.mayo.mprc.utilities.log.SimpleParentLog;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDao;
import edu.mayo.mprc.workspace.WorkspaceDaoHibernate;
import org.apache.log4j.Logger;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.DatabaseSequenceFilter;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.filter.ITableFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.Transaction;
import org.hibernate.stat.Statistics;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

/**
 * @author Roman Zenka
 */
public final class BulkLoadingTest extends DaoTest {
	private static final Logger LOGGER = Logger.getLogger(BulkLoadingTest.class);
	public static final int PROTEIN_MIN = 6;
	public static final int PROTEIN_MAX = 20;
	private BulkSearchDbDao searchDbDao;
	private ParamsDao paramsDao;
	private SwiftDao swiftDao;
	private FastaDbDao fastaDbDao;
	private WorkspaceDao workspaceDao;
	private UnimodDao unimodDao;
	private SessionProvider sessionProvider;
	private Statistics stats;

	@BeforeMethod
	public void setupDb() {
		final SwiftDaoHibernate swiftDaoImpl = new SwiftDaoHibernate();
		final ParamsDaoHibernate paramsDaoImpl = new ParamsDaoHibernate();
		final FastaDbDaoHibernate fastaDbDaoImpl = new FastaDbDaoHibernate();
		final WorkspaceDaoHibernate workspaceDaoImpl = new WorkspaceDaoHibernate();
		final UnimodDaoHibernate unimodDaoImpl = new UnimodDaoHibernate();
		final CurationDaoHibernate curationDaoImpl = new CurationDaoHibernate();
		final BulkSearchDbDaoHibernate searchDbDaoImpl = new BulkSearchDbDaoHibernate(swiftDaoImpl, fastaDbDaoImpl, getDatabase());
		sessionProvider = fastaDbDaoImpl;
		initializeDatabase(Arrays.asList(swiftDaoImpl, paramsDaoImpl, fastaDbDaoImpl, workspaceDaoImpl, unimodDaoImpl, searchDbDaoImpl, curationDaoImpl));
		// loadXml();

		stats = getDatabase().getSessionFactory().getStatistics();

		searchDbDao = searchDbDaoImpl;
		swiftDao = swiftDaoImpl;
		paramsDao = paramsDaoImpl;
		fastaDbDao = fastaDbDaoImpl;
		workspaceDao = workspaceDaoImpl;
		unimodDao = unimodDaoImpl;
		paramsDao = paramsDaoImpl;
	}

	private void startStats() {
		stats.clear();
		stats.setStatisticsEnabled(true);
	}

	@AfterMethod
	public void teardown() {
		searchDbDao = null;
		Assert.assertTrue(stats.getQueryExecutionCount() < 140, "The total number of queries must be around ~100 for 100 entries being saved, was: " + stats.getQueryExecutionCount());
		stats.setStatisticsEnabled(false);

		// dumpXml();
		teardownDatabase();
	}

	private void loadXml() {
		final FileInputStream in = null;
		try {
			final Transaction transaction = sessionProvider.getSession().beginTransaction();
			final IDatabaseConnection connection = new DatabaseConnection(sessionProvider.getSession().connection());
			final IDataSet currentData = connection.createDataSet();
			final FlatXmlDataSet set = new FlatXmlDataSetBuilder().build(new File("/Users/m044910/Documents/devel/swift/services/search-db/src/test/resources/edu/mayo/mprc/searchdb/dump.xml"));
			DatabaseOperation.CLEAN_INSERT.execute(connection, set);
			transaction.commit();
		} catch (final Exception e) {
			throw new MprcException("Could not dump database XML", e);
		} finally {
			FileUtilities.closeQuietly(in);
		}
	}

	private void dumpXml() {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream("/Users/m044910/Documents/devel/swift/services/search-db/src/test/resources/edu/mayo/mprc/searchdb/dump.xml");
			final Transaction transaction = sessionProvider.getSession().beginTransaction();
			final IDatabaseConnection conn = new DatabaseConnection(sessionProvider.getSession().connection());
			final ITableFilter filter = new DatabaseSequenceFilter(conn);
			final IDataSet dataset = new FilteredDataSet(filter, conn.createDataSet());
			FlatXmlDataSet.write(dataset, out);
			transaction.commit();
		} catch (final Exception e) {
			throw new MprcException("Could not dump database XML", e);
		} finally {
			FileUtilities.closeQuietly(out);
		}
	}

	@Test
	public void shouldLoadResults() {
		final UserProgressReporter reporter = new UserProgressReporter() {
			@Override
			public void reportProgress(final ProgressInfo progressInfo) {

			}

			@Override
			public ParentLog getLog() {
				return new SimpleParentLog();
			}
		};

		searchDbDao.begin();

		User user = workspaceDao.getUserByEmail("test@test.com");
		if (user == null) {
			user = workspaceDao.addNewUser("test", "testovic", "test@test.com", new Change("test user", new DateTime()));
		}

		final List<FileSearch> inputFiles = new ArrayList<FileSearch>(1);
		inputFiles.add(new FileSearch(new File("input.mgf"), "sample", "category", "experiment", null));
		SwiftSearchDefinition searchDefinition = new SwiftSearchDefinition("test", user, new File("out"), null, null, null, inputFiles, false, false, false, new HashMap<String, String>(0));
		searchDefinition = swiftDao.addSwiftSearchDefinition(searchDefinition);
		final int searchRunId = swiftDao.fillSearchRun(searchDefinition).getId();
		final long reportDataId = swiftDao.storeReport(searchRunId, new File("test.sf3"), new DateTime(2013, 8, 20, 20, 30, 40, 50)).getId();

		nextTransaction();

		// Add all the protein sequences
		final Collection<ProteinSequence> sequences = new ArrayList<ProteinSequence>(100);
		for (int i = 0; i < 100; i++) {
			sequences.add(new ProteinSequence(getRandomSequence("AC" + i, PROTEIN_MIN, PROTEIN_MAX)));
		}
		fastaDbDao.addProteinSequences(sequences);

		nextTransaction();

		// Start building the analysis
		final AnalysisBuilder builder = new AnalysisBuilder(new DummyTranslator(), new DummyMassSpecDataExtractor(new DateTime(2013, 9, 22, 10, 20, 30, 0)));
		ReportData reportData = swiftDao.getReportForId(reportDataId);
		builder.setReportData(reportData);
		final SearchResultListBuilder searchResults = builder.getBiologicalSamples().getBiologicalSample("sample", "category").getSearchResults();
		final SearchResultBuilder tandemMassSpecResult = searchResults.getTandemMassSpecResult("test.RAW");
		for (int i = 0; i < 100; i++) {
			tandemMassSpecResult.getProteinGroups().getProteinGroup("AC" + i, "database", 1, 1, 1, 0.1, 0.2, 0.3);
		}

		nextTransaction();

		reportData = swiftDao.getReportForId(reportData.getId());

		startStats();
		final Analysis analysis = searchDbDao.addAnalysis(builder, reportData, reporter);
		searchDbDao.commit();
	}

	public static final String ACIDS = "RHKDESTNQCGPAVILMFYW";

	public static String getRandomSequence(final String accessionNumber, final int minLength, final int maxLength) {
		final Random random = new Random(accessionNumber.hashCode());
		final int length = random.nextInt(maxLength - minLength + 1) + minLength;
		final StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			builder.append(ACIDS.charAt(random.nextInt(ACIDS.length())));
		}

		return builder.toString();
	}


	private static class DummyTranslator implements ProteinSequenceTranslator {
		@Override
		public ProteinSequence getProteinSequence(final String accessionNumber, final String databaseSources) {
			final String sequence = getRandomSequence(accessionNumber, PROTEIN_MIN, PROTEIN_MAX);

			return new ProteinSequence(sequence);
		}

	}
}
