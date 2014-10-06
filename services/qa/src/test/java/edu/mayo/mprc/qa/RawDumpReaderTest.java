package edu.mayo.mprc.qa;

import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public final class RawDumpReaderTest {

	@Test
	public void shouldReadSimpleFile() throws IOException {
		final File resource = TestingUtilities.getTempFileFromResource(RawDumpReader.class, "/edu/mayo/mprc/qa/rawdump.tsv", true, null);
		final RawDumpReader r = new RawDumpReader(resource);
		Assert.assertEquals(r.getLineForKey("10"), "1010\t0.010\t2\t0\t210\t1.01\t1.201\t0.011\t1.201\t1\t-1\t0\t0\t40000000.010\t-9000000.010\t47498204.010\t-69569781.010\tcid");
		// Scan ID is ommitted
		Assert.assertEquals(r.getHeaderLine(), "TIC\tRT\tMS Level\tChild Scans\tIon Injection Time\tCycle Time\tElapsed Time\tDead Time\tTime To Next Scan\tLock Mass Found\tLock Mass Shift\tConversion Parameter I\tConversion Parameter A\tConversion Parameter B\tConversion Parameter C\tConversion Parameter D\tConversion Parameter E\tDissociation Type");

		int i = 1;
		for (final String spectrum : r) {
			Assert.assertEquals(spectrum, String.valueOf(i), "Spectra are not loaded in proper order");
			i++;
		}
		Assert.assertEquals(i, 11, "Not all spectra were loaded");
		FileUtilities.quietDelete(resource);
	}

	@Test
	public void shouldGetRtFromLine() {
		RawDumpReader rawDumpReader = new RawDumpReader(null);
		Assert.assertEquals(
				rawDumpReader.getRtFromLine("1\t2\t3\t4\n"),
				"3", "RT is column #3");
	}
}
