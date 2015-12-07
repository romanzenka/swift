package edu.mayo.mprc.swift.report;

import com.google.common.base.Joiner;
import edu.mayo.mprc.database.QueryCallback;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.searchdb.dao.SearchDbDaoBlank;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Check the methods that produce sample reports.
 *
 * @author Roman Zenka
 */
public final class SampleReportTest {

	public static final String SAMPLE_INFO = "Calibration File:\t\n" +
			"Comment:\tyeast, 5 uL inj fr 1ug in 100uL in 5Ang samp buffer\n" +
			"Data Path:\tC:\\XCALIBUR\\DATA\\09SEPTEMBER2010\\\n" +
			"Dilution Factor:\t1\n" +
			"Injection Volume:\t5\n" +
			"Instrument Method:\tC:\\Xcalibur\\methods\\orbi24_3_40_50top5ltq80ms_75min_300_0101_mono_3p.meth\n" +
			"ISTD Amount:\t0\n" +
			"Level Name:\t\n" +
			"Row:\t1\n" +
			"Processing Method:\t\n" +
			"Raw File Name:\to63_10sep22_03_75min_yeast1.RAW\n" +
			"Sample ID:\tnew ion transfer tube, new tee; new LTQ cali\n" +
			"Sample Name:\t\n" +
			"Sample Type:\tUnknown\n" +
			"Sample Volume:\t0\n" +
			"Sample Weight:\t0\n" +
			"User Label 1:\tDate\n" +
			"User Text 1:\t092210\n" +
			"User Label 2:\tTrap\n" +
			"User Text 2:\tUC 0.25uL OptiPak, Michrom C8, 10sep19\n" +
			"User Label 3:\tColumn\n" +
			"User Text 3:\tnew 27cm x 75 um Magic C18AQ 3um, 200A,  2010Sep19 Picofrit\n" +
			"User Label 4:\tMob. Phase\n" +
			"User Text 4:\t\n" +
			"User Label 5:\tPhone\n" +
			"User Text 5:\t\n" +
			"Vial:\tF2\n" +
			"";

	public static final String SAMPLE2_INFO = "Test:\ttest2\nVial:\tB3\n";

	/**
	 * Given a sample from the database, should extract a proper list of headers in proper order.
	 */
	@Test
	public void shouldParseSampleHeaders() {
		final List<String> headers = SampleReportData.parseSampleHeaders(SAMPLE_INFO);
		Assert.assertNotNull(headers);
		Assert.assertEquals(Joiner.on(',').join(headers), "Calibration File,Comment,Data Path,Dilution Factor,Injection Volume," +
				"Instrument Method,ISTD Amount,Level Name,Row,Processing Method,Raw File Name,"
				+ "Sample ID,Sample Name,Sample Type,Sample Volume,Sample Weight,User Date,User Trap," +
				"User Column,User Mob. Phase,User Phone,Vial", "Headers must be in correct order");
	}

	@Test
	public void shouldDealWithEmptyHeader() {
		Assert.assertEquals(SampleReportData.parseSampleHeaders("  ").size(), 0);
		Assert.assertEquals(SampleReportData.parseSampleHeaders("").size(), 0);
		Assert.assertEquals(SampleReportData.parseSampleHeaders(null).size(), 0);
	}

	@Test
	public void shouldConsolidateHeaders() {
		final List<TandemMassSpectrometrySample> samples = getTwoSamples();
		SampleReportData.SampleHeaderCollector headerCollector = new SampleReportData.SampleHeaderCollector();
		for (TandemMassSpectrometrySample sample : samples) {
			headerCollector.process(new Object[]{sample});
		}

		Assert.assertEquals(Joiner.on(',').join(headerCollector.getHeaders()), "Calibration File,Comment,Data Path,Dilution Factor,Injection Volume," +
				"Instrument Method,ISTD Amount,Level Name,Row,Processing Method,Raw File Name,"
				+ "Sample ID,Sample Name,Sample Type,Sample Volume,Sample Weight,User Date,User Trap," +
				"User Column,User Mob. Phase,User Phone,Vial,Test", "Headers must be in correct order");
	}

	@Test
	public void shouldProduceCsv() throws IOException {
		StringWriter stringWriter = new StringWriter(1000);
		SearchDbDao dao = new MySearchDbDao(getTwoSamples());
		SampleReportData.writeCsv(stringWriter, dao);

		String result = stringWriter.getBuffer().toString();
		stringWriter.close();
		Assert.assertEquals(TestingUtilities.compareStringToResourceByLine(result, "edu/mayo/mprc/swift/report/report.csv"), null);
	}

	private List<TandemMassSpectrometrySample> getTwoSamples() {
		TandemMassSpectrometrySample sample1 = new TandemMassSpectrometrySample(
				new File("/file/test1.RAW"), new DateTime(2011, 1, 2, 3, 4, 5, 0), 10, 20, 30, "instrument", "serial",
				new DateTime(2011, 2, 3, 10, 20, 30, 0), 20 * 60, "Test File 1", SAMPLE_INFO, 100);
		TandemMassSpectrometrySample sample2 = new TandemMassSpectrometrySample(
				new File("/file/test2.RAW"), new DateTime(2012, 1, 2, 3, 4, 5, 0), 11, 21, 31, "instrument 2", "serial 2",
				new DateTime(2012, 2, 3, 10, 20, 30, 0), 1234, "Test File 2", SAMPLE2_INFO, 200);
		return Arrays.asList(sample1, sample2);
	}

	private static class MySearchDbDao extends SearchDbDaoBlank {
		private final List<TandemMassSpectrometrySample> samples;

		private MySearchDbDao(List<TandemMassSpectrometrySample> samples) {
			this.samples = samples;
		}

		@Override
		public void getTandemMassSpectrometrySamples(QueryCallback callback) {
			for (TandemMassSpectrometrySample sample : samples) {
				callback.process(new Object[]{sample});
			}
		}
	}
}
