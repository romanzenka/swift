package edu.mayo.mprc.searchdb;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.fastadb.ProteinSequence;
import edu.mayo.mprc.fastadb.ProteinSequenceTranslator;
import edu.mayo.mprc.searchdb.builder.AnalysisBuilder;
import edu.mayo.mprc.searchdb.dao.*;
import edu.mayo.mprc.unimod.IndexedModSet;
import edu.mayo.mprc.unimod.MockUnimodDao;
import edu.mayo.mprc.unimod.Unimod;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ResourceUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Tests the summary report for the scaffold spectra file.
 *
 * @author Roman Zenka
 */
public class TestScaffoldSpectraSummarizer {

	private static final String SINGLE = "classpath:edu/mayo/mprc/searchdb/single.tsv";
	private static final String SINGLE_EXPECTED = "/edu/mayo/mprc/searchdb/expected_single_report.tsv";

	private static final String MULTIPLE = "classpath:edu/mayo/mprc/searchdb/multiple.tsv";
	private static final String MULTIPLE_EXPECTED = "/edu/mayo/mprc/searchdb/expected_multiple_report.tsv";

	private static final String LARGE = "classpath:edu/mayo/mprc/searchdb/large.tsv";
	private static final String LARGE_EXPECTED = "/edu/mayo/mprc/searchdb/expected_large_report.tsv";

	private static final String SCAFFOLD4 = "classpath:edu/mayo/mprc/searchdb/scaffold4.tsv";
	private static final String SCAFFOLD4_EXPECTED = "/edu/mayo/mprc/searchdb/expected_scaffold4_report.tsv";

	private static final String BUG_2205 = "classpath:edu/mayo/mprc/searchdb/pyroCmc_bug_2205.tsv";
	private static final String BUG_2205_EXPECTED = "/edu/mayo/mprc/searchdb/expected_pyroCmc_bug_2205.tsv";

	static final String SINGLE_MISSING_DB = "classpath:edu/mayo/mprc/searchdb/single_missing_db.tsv";
	// Should parse to identical result as SINGLE
	static final String SINGLE_MISSING_DB_EXPECTED = SINGLE_EXPECTED;

	private static final double EPSILON = 1E-6;

	private final MockUnimodDao mockUnimodDao = new MockUnimodDao();
	private Unimod unimod;
	private Unimod scaffoldUnimod;

	@BeforeClass
	public void setup() {
		unimod = mockUnimodDao.load();
		scaffoldUnimod = new Unimod();
		scaffoldUnimod.parseUnimodXML(ResourceUtilities.getStream("classpath:edu/mayo/mprc/searchdb/scaffold_unimod.xml", Unimod.class));
	}

	@Test
	public void shouldParseDoubles() {
		Assert.assertEquals("100.0", String.valueOf(ScaffoldSpectraSummarizer.parseDouble("100.0%")));
		Assert.assertEquals("94.7", String.valueOf(ScaffoldSpectraSummarizer.parseDouble("94.7%")));
		Assert.assertEquals("0.948", String.valueOf(ScaffoldSpectraSummarizer.parseDouble("94.8%") / 100.0));
	}

	/**
	 * Should load a report generated from single file experiment.
	 */
	@Test
	public void shouldLoadSingleReport() throws IOException {
		final InputStream stream = ResourceUtilities.getStream(SINGLE, TestScaffoldSpectraSummarizer.class);
		try {
			final ScaffoldSpectraSummarizer summarizer = makeSummarizer();

			summarizer.load(stream, -1, SINGLE, "3", null);
			final AnalysisBuilder analysisBuilder = summarizer.getAnalysisBuilder();
			final Analysis analysis = analysisBuilder.build();
			Assert.assertEquals(analysisBuilder.getAnalysisDate(), new DateTime(2011, 12, 16, 0, 0, 0, 0), "Report date");
			Assert.assertEquals(analysisBuilder.getScaffoldVersion(), "Scaffold_3.3.1", "Scaffold version");

			Assert.assertEquals(analysis.getBiologicalSamples().size(), 1, "Biological samples");

			final BiologicalSample biologicalSample = analysis.getBiologicalSamples().iterator().next();
			Assert.assertEquals(biologicalSample.getSampleName(), "test1", "Sample name");
			Assert.assertEquals(biologicalSample.getSearchResults().size(), 1, "Total msms searches");

			final SearchResult searchResult = biologicalSample.getSearchResults().iterator().next();
			Assert.assertEquals(searchResult.getProteinGroups().size(), 5, "Total protein groups");
			final Iterator<ProteinGroup> iterator = searchResult.getProteinGroups().iterator();
			iterator.next();
			iterator.next();
			iterator.next();
			final ProteinGroup teraBovinGroup = iterator.next();
			Assert.assertEquals(teraBovinGroup.getNumberOfTotalSpectra(), 26, "Total spectra");
			Assert.assertEquals(teraBovinGroup.getNumberOfUniquePeptides(), 10, "Unique peptides");
			Assert.assertEquals(teraBovinGroup.getNumberOfUniqueSpectra(), 13, "Unique spectra");
			assertNear(teraBovinGroup.getPercentageSequenceCoverage(), 0.145, "Sequence coverage");
			assertNear(teraBovinGroup.getPercentageOfTotalSpectra(), 0.351, "Percentage total");
			assertNear(teraBovinGroup.getProteinIdentificationProbability(), 1.0, "Id probability");
			Assert.assertEquals(teraBovinGroup.getProteinSequences().size(), 1, "One protein only");
			Assert.assertEquals(teraBovinGroup.getPeptideSpectrumMatches().size(), 10, "Peptides assigned to protein");
			final PeptideSpectrumMatch firstPsm = teraBovinGroup.getPeptideSpectrumMatches().iterator().next();
			Assert.assertEquals(firstPsm.getPeptide().getSequence().getSequence(), "AHVIVMAATNRPNSIDPALR");
			Assert.assertEquals(firstPsm.getPeptide().getModifications().size(), 0);
			Assert.assertEquals(firstPsm.getSpectrumIdentificationCounts().getNumberOfIdentifiedSpectra(), 3);
			Assert.assertEquals(firstPsm.getSpectrumIdentificationCounts().getNumberOfIdentified1HSpectra(), 0);
			Assert.assertEquals(firstPsm.getSpectrumIdentificationCounts().getNumberOfIdentified2HSpectra(), 1);
			Assert.assertEquals(firstPsm.getSpectrumIdentificationCounts().getNumberOfIdentified3HSpectra(), 2);
			Assert.assertEquals(firstPsm.getSpectrumIdentificationCounts().getNumberOfIdentified4HSpectra(), 0);

			checkAnalysisMatch(analysis, SINGLE_EXPECTED);

		} finally {
			FileUtilities.closeQuietly(stream);
		}
	}

	private ScaffoldSpectraSummarizer makeSummarizer(final IndexedModSet scaffoldUnimod) {
		return new ScaffoldSpectraSummarizer(
				unimod, scaffoldUnimod,
				new DummyTranslator(),
				new DummyMassSpecDataExtractor(new DateTime()));
	}

	private ScaffoldSpectraSummarizer makeSummarizer() {
		return makeSummarizer(scaffoldUnimod);
	}

	/**
	 * Multiple experiments in one report should be loaded correctly.
	 */
	@Test
	public void shouldLoadMultipleReports() {
		final InputStream stream = ResourceUtilities.getStream(MULTIPLE, TestScaffoldSpectraSummarizer.class);
		try {
			final ScaffoldSpectraSummarizer summarizer = makeSummarizer();
			summarizer.load(stream, -1, MULTIPLE, "3", null);
			final Analysis analysis = summarizer.getAnalysisBuilder().build();
			Assert.assertEquals(analysis.getAnalysisDate(), new DateTime(2011, 12, 28, 0, 0, 0, 0), "Report date");
			Assert.assertEquals(analysis.getScaffoldVersion(), "Scaffold_3.3.1", "Scaffold version");

			Assert.assertEquals(analysis.getBiologicalSamples().size(), 4, "Biological samples");

			checkAnalysisMatch(analysis, MULTIPLE_EXPECTED);
		} finally {
			FileUtilities.closeQuietly(stream);
		}
	}

	/**
	 * Large report should be loaded correctly.
	 */
	@Test
	public void shouldLoadLargeReport() {
		final InputStream stream = ResourceUtilities.getStream(LARGE, TestScaffoldSpectraSummarizer.class);
		try {
			final ScaffoldSpectraSummarizer summarizer = makeSummarizer();
			summarizer.load(stream, -1, LARGE, "3", null);
			final Analysis analysis = summarizer.getAnalysisBuilder().build();
			Assert.assertEquals(analysis.getAnalysisDate(), new DateTime(2011, 10, 18, 0, 0, 0, 0), "Report date");
			Assert.assertEquals(analysis.getScaffoldVersion(), "Scaffold_3.2.0", "Scaffold version");

			Assert.assertEquals(analysis.getBiologicalSamples().size(), 9, "Biological samples");

			checkAnalysisMatch(analysis, LARGE_EXPECTED);
		} finally {
			FileUtilities.closeQuietly(stream);
		}
	}

	/**
	 * Scaffold 4 report should be loaded correctly.
	 */
	@Test
	public void shouldLoadScaffold4Report() {
		final InputStream stream = ResourceUtilities.getStream(SCAFFOLD4, TestScaffoldSpectraSummarizer.class);
		try {
			final ScaffoldSpectraSummarizer summarizer = makeSummarizer();
			summarizer.load(stream, -1, SCAFFOLD4, "4.0.4", null);
			final Analysis analysis = summarizer.getAnalysisBuilder().build();
			Assert.assertEquals(analysis.getAnalysisDate(), new DateTime(2013, 04, 23, 0, 0, 0, 0), "Report date");
			Assert.assertEquals(analysis.getScaffoldVersion(), "Scaffold_4.0.4", "Scaffold version");

			Assert.assertEquals(analysis.getBiologicalSamples().size(), 2, "Biological samples");

			checkAnalysisMatch(analysis, SCAFFOLD4_EXPECTED);
		} finally {
			FileUtilities.closeQuietly(stream);
		}
	}

	/**
	 * A report with missing source database field should be loaded correctly.
	 */
	@Test
	public void shouldLoadMissingDatabaseReport() {
		final InputStream stream = ResourceUtilities.getStream(SINGLE_MISSING_DB, TestScaffoldSpectraSummarizer.class);
		try {
			final ScaffoldSpectraSummarizer summarizer = makeSummarizer();
			summarizer.load(stream, -1, SINGLE_MISSING_DB, "3", null);
			final Analysis analysis = summarizer.getAnalysisBuilder().build();
			Assert.assertEquals(analysis.getAnalysisDate(), new DateTime(2011, 12, 16, 0, 0, 0, 0), "Report date");
			Assert.assertEquals(analysis.getScaffoldVersion(), "Scaffold_3.3.1", "Scaffold version");

			Assert.assertEquals(analysis.getBiologicalSamples().size(), 1, "Biological samples");

			checkAnalysisMatch(analysis, SINGLE_MISSING_DB_EXPECTED);
		} finally {
			FileUtilities.closeQuietly(stream);
		}
	}

	/**
	 * X!Tandem's n-Term pyroCmc was causing issues when parsing.
	 */
	@Test
	public void shouldProcessPyroCmc() {
		final InputStream stream = ResourceUtilities.getStream(BUG_2205, TestScaffoldSpectraSummarizer.class);
		try {
			Unimod scaffoldUnimod = new Unimod();
			scaffoldUnimod.parseUnimodXML(ResourceUtilities.getStream("classpath:edu/mayo/mprc/searchdb/scaffold_3.6.2_unimod.xml", Unimod.class));

			final ScaffoldSpectraSummarizer summarizer = makeSummarizer(scaffoldUnimod);
			summarizer.load(stream, -1, BUG_2205, "3", null);
			final Analysis analysis = summarizer.getAnalysisBuilder().build();
			Assert.assertEquals(analysis.getAnalysisDate(), new DateTime(2013, 06, 14, 0, 0, 0, 0), "Report date");
			Assert.assertEquals(analysis.getScaffoldVersion(), "Scaffold_3.6.2", "Scaffold version");

			Assert.assertEquals(analysis.getBiologicalSamples().size(), 1, "Biological samples");

			checkAnalysisMatch(analysis, BUG_2205_EXPECTED);
		} finally {
			FileUtilities.closeQuietly(stream);
		}
	}


	/**
	 * The report from a given analysis has to match the expected values.
	 *
	 * @param analysis         Analysis to check.
	 * @param expectedResource Resource to load the expected value from.
	 */
	private void checkAnalysisMatch(final Analysis analysis, final String expectedResource) {
		final String actual;
		final String expected;
		try {
			actual = analysis.peptideReport();
			expected = Resources.toString(Resources.getResource(getClass(), expectedResource), Charsets.ISO_8859_1);
		} catch (Exception e) {
			throw new MprcException("Failed when matching analysis", e);
		}
		Assert.assertEquals(TestingUtilities.compareStringsByLine(actual, expected, true), null, "The peptide report does not match");
	}

	public static void assertNear(final double d1, final double d2, final String message) {
		final double delta = d1 - d2;
		Assert.assertTrue(-EPSILON < delta && delta < EPSILON, message);
	}

	private static class DummyTranslator implements ProteinSequenceTranslator {

		@Override
		public ProteinSequence getProteinSequence(final String accessionNumber, final String databaseSources) {
			if (Strings.isNullOrEmpty(databaseSources)) {
				throw new MprcException("Database was null");
			}
			if (Strings.isNullOrEmpty(accessionNumber)) {
				throw new MprcException("Unspecified accession number");
			}
			return new ProteinSequence("CAT");
		}
	}
}
