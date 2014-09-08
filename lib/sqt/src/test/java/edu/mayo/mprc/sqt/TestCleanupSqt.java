package edu.mayo.mprc.sqt;


import com.google.common.base.Charsets;
import edu.mayo.mprc.integration.Installer;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Roman Zenka
 */
public final class TestCleanupSqt {
	private File input;
	private File output;
	private File fasta;
	private File fastaFileFolder;

	@BeforeClass
	public void installFiles() throws IOException {
		fastaFileFolder = Installer.testFastaFiles(null, Installer.Action.INSTALL);
		fasta = new File(fastaFileFolder, "test_in.fasta");
		input = TestingUtilities.getTempFileFromResource("/edu/mayo/mprc/sqt/input.sqt", true, null);
		output = new File(input.getParentFile(), "output.sqt");
	}

	@AfterClass
	public void cleanupFiles() {
		Installer.testFastaFiles(fastaFileFolder, Installer.Action.UNINSTALL);
		FileUtilities.cleanupTempFile(input);
		FileUtilities.cleanupTempFile(output);
	}

	@Test
	public void shouldCleanupSqt() {
		CleanupSqt cleanupSqt = new CleanupSqt(input, output, fasta);
		cleanupSqt.run();
		Assert.assertEquals(FileUtilities.toString(output, Charsets.US_ASCII, 100000),
				TestingUtilities.resourceToString("edu/mayo/mprc/sqt/output.sqt"));
	}
}
