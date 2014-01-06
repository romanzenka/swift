package edu.mayo.mprc.peaklist;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.mzml.MzMlPeakListReaderFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Roman Zenka
 */
public final class MzMlPeakListReaderTest {
	private MzMlPeakListReaderFactory factory = new MzMlPeakListReaderFactory();

	@Test
	public void shouldFailAtMissingCharge() {
		final File file = fileFromClasspath("mzml/small.pwiz.1.1.mzML");
		final PeakListReader reader = factory.createReader(file, true);
		try {
			while (true) {
				PeakList peakList = reader.nextPeakList();
				if (peakList == null) {
					break;
				}
			}
		} catch (MprcException e) {
			Assert.assertTrue(e.getMessage().contains("precursor charge state not specified"));
		}
	}

	@Test
	public void shouldWorkWithPossibleChargeStates() {
		final File file = fileFromClasspath("mzml/dta_example.mzML");
		final PeakListReader reader = factory.createReader(file, true);
		StringBuilder report = new StringBuilder(100);
		while (true) {
			PeakList peakList = reader.nextPeakList();
			if (peakList == null) {
				break;
			}
			report.append(peakList.getTitle())
					.append("\t")
					.append(peakList.getPepmass())
					.append("\t")
					.append(peakList.getCharge())
					.append("\n");
		}
		// Scan 10 is reported with two charge states
		Assert.assertEquals(report.toString(), "" +
				"scan=3\tPEPMASS=419.115\tCHARGE=1+\n" +
				"scan=5\tPEPMASS=460.227\tCHARGE=1+\n" +
				"scan=7\tPEPMASS=801.266\tCHARGE=1+\n" +
				"scan=10 charge=2\tPEPMASS=1082.5037\tCHARGE=2+\n" +
				"scan=10 charge=3\tPEPMASS=1082.5037\tCHARGE=3+\n" +
				"scan=12\tPEPMASS=919.14264\tCHARGE=2+\n" +
				"scan=642\tPEPMASS=474.34152\tCHARGE=3+\n" +
				"scan=647\tPEPMASS=406.26065\tCHARGE=2+\n" +
				"scan=649\tPEPMASS=948.349\tCHARGE=1+\n" +
				"scan=726\tPEPMASS=723.88763\tCHARGE=2+\n" +
				"scan=770\tPEPMASS=536.45917\tCHARGE=2+\n");
	}

	private static File fileFromClasspath(final String path) {
		try {
			final URL url = ClassLoader.getSystemResource(path);
			// Turn the resource into a File object
			return new File(url.toURI());
		} catch (URISyntaxException e) {
			throw new MprcException("Could not get file on classpath with path " + path);
		}
	}

}
