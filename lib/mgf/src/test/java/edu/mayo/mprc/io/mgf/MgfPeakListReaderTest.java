package edu.mayo.mprc.io.mgf;

import edu.mayo.mprc.peaklist.PeakList;
import edu.mayo.mprc.peaklist.PeakListReader;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.proteomecommons.io.Peak;
import org.proteomecommons.io.mgf.MascotGenericFormatPeakList;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Roman Zenka
 */
public final class MgfPeakListReaderTest {

	private File file;

	@BeforeClass
	public void setup() throws IOException {
		file = TestingUtilities.getTempFileFromResource(MgfPeakListReaderTest.class, "/edu/mayo/mprc/io/mgf/test.mgf", true, null);
	}

	@AfterClass
	public void teardown() {
		FileUtilities.cleanupTempFile(file);
	}

	@Test
	public void genericPeakListShouldWork() {
		MascotGenericFormatPeakList peakList = new MascotGenericFormatPeakList();
	}

	@Test
	public void shouldReadPeaks() throws IOException {
		MgfPeakListReaderFactory factory = new MgfPeakListReaderFactory();
		PeakListReader reader = factory.createReader(file);
		reader.setReadPeaks(true);

		int i = 0;
		while (true) {
			PeakList peakList = reader.nextPeakList();
			if (peakList == null) {
				break;
			}

			if (i == 0) {
				Assert.assertEquals(peakList.getPepmass(), "PEPMASS=506.60531764");
				Assert.assertEquals(peakList.getCharge(), "CHARGE=3+");
				Peak[] peaks = peakList.getPeaks();
				Assert.assertEquals(peaks.length, 1);
				Assert.assertEquals(peaks[0].getMassOverCharge(), 145.946);
				Assert.assertEquals(peaks[0].getIntensity(), 10.0);
				Assert.assertEquals(peaks[0].getMonoisotopic(), Integer.MIN_VALUE);
				Assert.assertEquals(peaks[0].getAveraged(), Integer.MIN_VALUE);
				Assert.assertEquals(peaks[0].getCentroided(), Integer.MIN_VALUE);
			}

			i++;
		}

		reader.close();
	}
}
