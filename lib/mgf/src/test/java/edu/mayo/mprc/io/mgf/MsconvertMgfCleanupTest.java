package edu.mayo.mprc.io.mgf;

import edu.mayo.mprc.utilities.FileUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;

public final class MsconvertMgfCleanupTest {

	@Test
	public void shouldCleanupTrivial() throws IOException {
		testCorrectCleanup(""
						+ "BEGIN IONS\n"
						+ "TITLE=title\n"
						+ "RTINSECONDS=1000\n"
						+ "CHARGE=2+\n"
						+ "10.5 20.3\n"
						+ "END IONS\n", ""
						////
						+ "BEGIN IONS\n"
						+ "TITLE=title\n"
						+ "CHARGE=2+\n"
						+ "RTINSECONDS=1000\n"
						+ "10.5 20.3\n"
						+ "END IONS\n",
				1
		);
	}

	@Test
	public void shouldCleanupWindows() throws IOException {
		testCorrectCleanup(""
						+ "BEGIN IONS\r\n"
						+ "TITLE=title\r\n"
						+ "RTINSECONDS=1000\r\n"
						+ "CHARGE=2+\r\n"
						+ "10.5 20.3\r\n"
						+ "END IONS\r\n", ""
						////
						+ "BEGIN IONS\n"
						+ "TITLE=title\n"
						+ "CHARGE=2+\n"
						+ "RTINSECONDS=1000\n"
						+ "10.5 20.3\n"
						+ "END IONS\n",
				1
		);
	}

	@Test
	public void shouldCleanupMultipleCharges() throws IOException {
		testCorrectCleanup(""
						+ "BEGIN IONS\n"
						+ "TITLE=title (file.1.1..dta)\n"
						+ "RTINSECONDS=1000\n"
						+ "CHARGE=2+ and 3+\n"
						+ "10.5 20.3\n"
						+ "END IONS\n", ""
						////
						+ "BEGIN IONS\n"
						+ "TITLE=title (file.1.1.2.dta)\n"
						+ "CHARGE=2+\n"
						+ "RTINSECONDS=1000\n"
						+ "10.5 20.3\n"
						+ "END IONS\n"
						+ "BEGIN IONS\n"
						+ "TITLE=title (file.1.1.3.dta)\n"
						+ "CHARGE=3+\n"
						+ "RTINSECONDS=1000\n"
						+ "10.5 20.3\n"
						+ "END IONS\n",
				1
		);
	}

	@Test
	public void shouldRemoveEmptySpectra() throws IOException {
		testCorrectCleanup(""
						+ "BEGIN IONS\n"
						+ "TITLE=title (file.1.1..dta)\n"
						+ "RTINSECONDS=1000\n"
						+ "CHARGE=2+ and 3+\n"
						+ "END IONS\n", ""
						////
						+ "",
				1
		);
	}

	@Test
	public void shouldRemoveSimpleEmptySpectra() throws IOException {
		testCorrectCleanup(""
						+ "BEGIN IONS\n" // Empty, gets dropped
						+ "TITLE=title (file.1.1..dta)\n"
						+ "RTINSECONDS=1000\n"
						+ "CHARGE=2+ and 3+\n"
						+ "10.5 20.3\n"
						+ "END IONS\n"
						+ "BEGIN IONS\n" // Gets copied
						+ "TITLE=title\n"
						+ "RTINSECONDS=1000\n"
						+ "CHARGE=2+\n"
						+ "10.5 20.3\n"
						+ "11.5 22.3\n"
						+ "END IONS\n", ""
						////
						+ "BEGIN IONS\n"
						+ "TITLE=title\n"
						+ "CHARGE=2+\n"
						+ "RTINSECONDS=1000\n"
						+ "10.5 20.3\n"
						+ "11.5 22.3\n"
						+ "END IONS\n",
				2
		);
	}


	private static void testCorrectCleanup(final String mgfIn, final String mgfOut, int minPeaksPerSpectrum) throws IOException {
		BufferedReader reader = null;
		BufferedWriter writer = null;

		try {
			reader = new BufferedReader(new StringReader(mgfIn));
			final StringWriter stringWriter = new StringWriter(mgfIn.length());
			writer = new BufferedWriter(stringWriter);
			MsconvertMgfCleanup cleanup = new MsconvertMgfCleanup(null, minPeaksPerSpectrum); // One peak must be present
			cleanup.performCleanup(reader, writer);
			Assert.assertEquals(stringWriter.toString(), mgfOut, "Cleanup of the mgf file produced unexpected result");
		} finally {
			FileUtilities.closeQuietly(reader);
			FileUtilities.closeQuietly(writer);
		}
	}

}
