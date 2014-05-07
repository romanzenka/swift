package edu.mayo.mprc.qa;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.io.mgf.MgfPeakListReaderFactory;
import edu.mayo.mprc.msmseval.MSMSEvalOutputReader;
import edu.mayo.mprc.peaklist.PeakListReaderFactory;
import edu.mayo.mprc.peaklist.PeakListReaders;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldQaSpectraReader;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;

public final class SpectrumInfoJoinerTest {

	@Test
	public void generateMgfStatisticsFileTest() throws IOException {

		final File tempFolder = FileUtilities.createTempFolder();

		try {
			final File mgfFile = TestingUtilities.getNamedFileFromResource("/edu/mayo/mprc/qa/test1.mgf", tempFolder);
			final File scaffoldSpectra = TestingUtilities.getNamedFileFromResource("/edu/mayo/mprc/qa/scaffoldSpectra.txt", tempFolder);
			final File referenceOutputFile = TestingUtilities.getNamedFileFromResource("/edu/mayo/mprc/qa/Out.tsv", tempFolder);
			final File rawDumpFile = TestingUtilities.getNamedFileFromResource("/edu/mayo/mprc/qa/rawdump.tsv", tempFolder);
			final File msmsEvalFile = TestingUtilities.getNamedFileFromResource("/edu/mayo/mprc/qa/msmsEval.csv", tempFolder);

			String refOut = Files.toString(referenceOutputFile, Charsets.UTF_8);
			refOut = refOut.replaceAll("<MGF>", Matcher.quoteReplacement(mgfFile.getAbsolutePath()));
			FileUtilities.writeStringToFile(referenceOutputFile, refOut, true);

			final File outputFile = new File(tempFolder, "output.tsv");

			final ScaffoldQaSpectraReader spectra = new ScaffoldQaSpectraReader();
			spectra.load(scaffoldSpectra, "2", null);
			final RawDumpReader rawDumpReader = new RawDumpReader(rawDumpFile);
			final MSMSEvalOutputReader msmsEvalReader = new MSMSEvalOutputReader(msmsEvalFile);
			final FileSpectrumInfoSink sink = new FileSpectrumInfoSink(outputFile);

			spectrumInfoJoiner().joinSpectrumData(mgfFile, spectra, rawDumpReader, msmsEvalReader, null, sink, null);

			Assert.assertEquals(TestingUtilities.compareFilesByLine(referenceOutputFile, outputFile, true), null, "Output file content is not as expected.");
		} finally {
			FileUtilities.cleanupTempFile(tempFolder);
		}
	}

	@Test
	public void shouldParseMgfTitles() {
		Assert.assertEquals(SpectrumInfoJoiner.getSpectrum("test1 scan 10 10 (test1.10.10.3.dta)"), "test1.10.10.3.dta");
		Assert.assertEquals(SpectrumInfoJoiner.getScanId("test1 scan 10 10 (test1.10.10.3.dta)"), 10L);
		Assert.assertEquals(SpectrumInfoJoiner.getScanIdFromScaffoldSpectrum("test1 scan 10 10 (test1.10.10.3.dta)"), 10L);
	}

	@Test
	public void shouldParseThermoMzMlTitles() {
		Assert.assertEquals(SpectrumInfoJoiner.getSpectrum("test1.10.10.3.dta"), "test1.10.10.3.dta");
		Assert.assertEquals(SpectrumInfoJoiner.getScanId("test1 scan 10 10 (test1.10.10.3.dta)"), 10L);
		Assert.assertEquals(SpectrumInfoJoiner.getScanIdFromScaffoldSpectrum("test1 scan 10 10 (test1.10.10.3.dta)"), 10L);
	}

	private SpectrumInfoJoiner spectrumInfoJoiner() {
		final PeakListReaders readers = new PeakListReaders(Arrays.asList((PeakListReaderFactory) new MgfPeakListReaderFactory()));
		return new SpectrumInfoJoiner(readers);
	}
}
