package edu.mayo.mprc.io.mgf;

import edu.mayo.mprc.utilities.FileUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;

public final class MgfCleanupTest {

	@Test
	public void shouldParseCharges() {
		Assert.assertEquals((int) MgfCleanup.parseCharge("CHARGE=2+"), 2);
		Assert.assertEquals((int) MgfCleanup.parseCharge("CHARGE=3-"), 3);
		Assert.assertEquals((int) MgfCleanup.parseCharge("CHARGE=2"), 2);
		Assert.assertEquals((int) MgfCleanup.parseCharge("CHARGE=4 #Comment"), 4);
		Assert.assertEquals((int) MgfCleanup.parseCharge("CHARGE=2+ and 3+"), 2);
	}

	@Test
	public void shouldDetectWhenCleanupNeeded() throws IOException {
		testCleanupNeeded(""
				+ "BEGIN IONS\n"
				+ "10.5\n"
				+ "END IONS\n",
				true, "Title missing");
		testCleanupNeeded(""
				+ "BEGIN IONS\n"
				+ "TITLE=hello\n"
				+ "10.5\n"
				+ "END IONS\n",
				true, "Title is of a wrong format");
		testCleanupNeeded(""
				+ "BEGIN IONS\n"
				+ "TITLE=hello (test.102.102.2.dta)\n"
				+ "10.5\n"
				+ "END IONS\n",
				false, "Correct title");
		testCleanupNeeded(""
				+ "BEGIN IONS\n"
				+ "TITLE=hello (test.dta)\n"
				+ "10.5\n"
				+ "END IONS\n",
				false, "Correct title");
		testCleanupNeeded(""
				+ "BEGIN IONS\n"
				+ "TITLE=hello1 (test1.dta)\n"
				+ "10.5\n"
				+ "END IONS\n"
				+ "BEGIN IONS\n"
				+ "TITLE=hello2 (test2.dta)\n"
				+ "10.5\n"
				+ "END IONS\n",
				false, "Correct title");
		testCleanupNeeded(""
				+ "BEGIN IONS\n"
				+ "TITLE=hello1 (test1.dta)\n"
				+ "10.5\n"
				+ "END IONS\n"
				+ "BEGIN IONS\n"
				+ "#TITLE=hello2 (test2.dta)\n"
				+ "10.5\n"
				+ "END IONS\n",
				true, "Title missing in second section");
		testCleanupNeeded(""
				+ "BEGIN IONS\n"
				+ "# TITLE=hello (test.dta)\n"
				+ "10.5\n"
				+ "END IONS\n",
				true, "Title commented out");
		testCleanupNeeded("SEARCH=MIS\n" +
				"REPTYPE=Peptide\n" +
				"BEGIN IONS\n" +
				"PEPMASS=450.75060209751\n" +
				"CHARGE=2+\n" +
				"TITLE=File: q03100602.wiff, Sample: ms0281-F  fx1 (sample number 1), Elution: 31.29 min, Period: 1, Cycle(s): 1788 (Experiment 2)\n" +
				"70.0615 0.0911\n" +
				"114.1078 0.0962\n" +
				"115.1062 0.153\n" +
				"END IONS\n" +
				"\n" +
				"BEGIN IONS\n" +
				"TITLE=File: q03100602.wiff, Sample: ms0281-F  fx1 (sample number 1), Elution: 31.89 min, Period: 1, Cycle(s): 1820 (Experiment 2)\n" +
				"PEPMASS=450.74861007249\n" +
				"CHARGE=2+\n" +
				"70.0623 0.0942\n" +
				"114.1108 0.1243\n" +
				"615.3107 0.1303\n" +
				"END IONS", true, "Sample of Kent's data"
		);

	}

	private void testCleanupNeeded(final String mgf, final boolean needed, final String why) throws IOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new StringReader(mgf));
			Assert.assertEquals(
					MgfCleanup.cleanupNeeded(reader),
					needed,
					"Cleanup is " + (needed ? "needed" : "not needed") + ": " + why);
		} finally {
			FileUtilities.closeQuietly(reader);
		}
	}

	@Test
	public void shouldCleanupProperly() throws IOException {
		testCorrectCleanup(""
				+ "BEGIN IONS\n"
				+ "10.5\n"
				+ "END IONS\n", ""

				+ "BEGIN IONS\n"
				+ "TITLE=missing title (filenameprefix.1.1.0.dta)\n"
				+ "10.5\n"
				+ "END IONS\n"
		);
		testCorrectCleanup(""
				+ "BEGIN IONS\n"
				+ "10.5\n"
				+ "END IONS\n"
				+ "BEGIN IONS\n"
				+ "10.6\n"
				+ "END IONS\n", ""

				+ "BEGIN IONS\n"
				+ "TITLE=missing title (filenameprefix.1.1.0.dta)\n"
				+ "10.5\n"
				+ "END IONS\n"
				+ "BEGIN IONS\n"
				+ "TITLE=missing title (filenameprefix.2.2.0.dta)\n"
				+ "10.6\n"
				+ "END IONS\n"
		);
		testCorrectCleanup(""
				+ "BEGIN IONS\n"
				+ "CHARGE=2+\n"
				+ "10.5\n"
				+ "END IONS\n"
				+ "BEGIN IONS\n"
				+ "CHARGE=3-\n"
				+ "10.6\n"
				+ "END IONS\n", ""

				+ "BEGIN IONS\n"
				+ "TITLE=missing title (filenameprefix.1.1.2.dta)\n"
				+ "CHARGE=2+\n"
				+ "10.5\n"
				+ "END IONS\n"
				+ "BEGIN IONS\n"
				+ "TITLE=missing title (filenameprefix.2.2.3.dta)\n"
				+ "CHARGE=3-\n"
				+ "10.6\n"
				+ "END IONS\n"
		);
		testCorrectCleanup(""
				+ "BEGIN IONS\n"
				+ "TITLE= Hello (test.dta)\n"
				+ "CHARGE=2+\n"
				+ "10.5\n"
				+ "END IONS\n"
				+ "BEGIN IONS\n"
				+ "CHARGE=3-\n"
				+ "10.6\n"
				+ "END IONS\n", ""

				+ "BEGIN IONS\n"
				+ "TITLE= Hello (test.dta)\n"
				+ "CHARGE=2+\n"
				+ "10.5\n"
				+ "END IONS\n"
				+ "BEGIN IONS\n"
				+ "TITLE=missing title (filenameprefix.1.1.3.dta)\n"
				+ "CHARGE=3-\n"
				+ "10.6\n"
				+ "END IONS\n"
		);
	}

	@Test
	public void shouldCleanupWiffOutput() throws IOException {
		testCorrectCleanup("" +
				"SEARCH=MIS\n" +
				"REPTYPE=Peptide\n" +
				"BEGIN IONS\n" +
				"PEPMASS=450.75060209751\n" +
				"CHARGE=2+\n" +
				"TITLE=File: q03100602.wiff, Sample: ms0281-F  fx1 (sample number 1), Elution: 31.29 min, Period: 1, Cycle(s): 1788 (Experiment 2)\n" +
				"70.0615 0.0911\n" +
				"114.1078 0.0962\n" +
				"115.1062 0.153\n" +
				"END IONS\n" +
				"\n" +
				"BEGIN IONS\n" +
				"PEPMASS=450.74861007249\n" +
				"CHARGE=2+\n" +
				"TITLE=File: q03100602.wiff, Sample: ms0281-F  fx1 (sample number 1), Elution: 31.89 min, Period: 1, Cycle(s): 1820 (Experiment 2)\n" +
				"70.0623 0.0942\n" +
				"114.1108 0.1243\n" +
				"615.3107 0.1303\n" +
				"END IONS\n",
				/////////////////////////
				"" +
				"SEARCH=MIS\n" +
				"REPTYPE=Peptide\n" +
				"BEGIN IONS\n" +
				"TITLE=File: q03100602.wiff, Sample: ms0281-F  fx1 (sample number 1), Elution: 31.29 min, Period: 1, Cycle(s): 1788 (Experiment 2) (filenameprefix.1.1.2.dta)\n" +
				"PEPMASS=450.75060209751\n" +
				"CHARGE=2+\n" +
				"70.0615 0.0911\n" +
				"114.1078 0.0962\n" +
				"115.1062 0.153\n" +
				"END IONS\n" +
				"\n" +
				"BEGIN IONS\n" +
				"TITLE=File: q03100602.wiff, Sample: ms0281-F  fx1 (sample number 1), Elution: 31.89 min, Period: 1, Cycle(s): 1820 (Experiment 2) (filenameprefix.2.2.2.dta)\n" +
				"PEPMASS=450.74861007249\n" +
				"CHARGE=2+\n" +
				"70.0623 0.0942\n" +
				"114.1108 0.1243\n" +
				"615.3107 0.1303\n" +
				"END IONS\n"
		);
	}

	@Test
	public void shouldCleanupProteomeDiscoverer() throws IOException {
		testCorrectCleanup("MASS=Monoisotopic\n" +
				"BEGIN IONS\n" +
				"PEPMASS=419.31540 0.00000\n" +
				"CHARGE=2+\n" +
				"TITLE=File1134 Spectrum1 scans: 228\n" +
				"RTINSECONDS=103\n" +
				"SCANS=228\n" +
				"127.160 53.0741\n" +
				"128.168 5.71123\n" +
				"129.022 14.5373\n" +
				"END IONS\n" +
				"\n" +
				"BEGIN IONS\n" +
				"TITLE=File1134 Spectrum2 scans: 441\n" +
				"PEPMASS=1031.06702 10570.94922\n" +
				"CHARGE=3+\n" +
				"RTINSECONDS=198\n" +
				"SCANS=441\n" +
				"339.192 15.6759\n" +
				"507.531 4.06305\n" +
				"END IONS",
				///////////////
				"MASS=Monoisotopic\n" +
				"BEGIN IONS\n" +
				"TITLE=File1134 Spectrum1 scans: 228 (filenameprefix.228.228.2.dta)\n" +
				"PEPMASS=419.31540 0.00000\n" +
				"CHARGE=2+\n" +
				"RTINSECONDS=103\n" +
				"SCANS=228\n" +
				"127.160 53.0741\n" +
				"128.168 5.71123\n" +
				"129.022 14.5373\n" +
				"END IONS\n" +
				"\n" +
				"BEGIN IONS\n" +
				"TITLE=File1134 Spectrum2 scans: 441 (filenameprefix.441.441.3.dta)\n" +
				"PEPMASS=1031.06702 10570.94922\n" +
				"CHARGE=3+\n" +
				"RTINSECONDS=198\n" +
				"SCANS=441\n" +
				"339.192 15.6759\n" +
				"507.531 4.06305\n" +
				"END IONS\n"
		);
	}


	private static void testCorrectCleanup(final String mgfIn, final String mgfOut) throws IOException {
		BufferedReader reader = null;
		BufferedWriter writer = null;

		try {
			reader = new BufferedReader(new StringReader(mgfIn));
			final StringWriter stringWriter = new StringWriter(mgfIn.length());
			writer = new BufferedWriter(stringWriter);
			MgfCleanup.performCleanup(reader, writer, "filenameprefix");
			Assert.assertEquals(stringWriter.toString(), mgfOut, "Cleanup of the mgf file produced unexpected result");
		} finally {
			FileUtilities.closeQuietly(reader);
			FileUtilities.closeQuietly(writer);
		}
	}

}
