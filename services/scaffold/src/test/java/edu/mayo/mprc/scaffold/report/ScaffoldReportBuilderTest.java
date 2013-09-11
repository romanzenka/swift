package edu.mayo.mprc.scaffold.report;

import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ScaffoldReportBuilderTest {

	@Test
	public void shouldProduceProperOutput() throws IOException {
		final List<File> inputFiles = new ArrayList<File>();
		final File parentFolder = FileUtilities.createTempFolder();
		final File file1 = TestingUtilities.getTempFileFromResource("/edu/mayo/mprc/scaffold/output/test1.txt", true, parentFolder);
		final File file2 = TestingUtilities.getTempFileFromResource("/edu/mayo/mprc/scaffold/output/test2.txt", true, parentFolder);
		final File file3 = TestingUtilities.getTempFileFromResource("/edu/mayo/mprc/scaffold/output/test3.txt", true, parentFolder);
		final File result_peptide = TestingUtilities.getTempFileFromResource("/edu/mayo/mprc/scaffold/output/testresult_peptide.txt", true, parentFolder);
		final File result_protein = TestingUtilities.getTempFileFromResource("/edu/mayo/mprc/scaffold/output/testresult_protein.txt", true, parentFolder);

		inputFiles.add(file1);
		inputFiles.add(file2);
		inputFiles.add(file3);

		final File outputPeptideFile = File.createTempFile("scaffoldReportTest_peptide", ".xls", parentFolder);
		final File outputProteinFile = File.createTempFile("scaffoldReportTest_protein", ".xls", parentFolder);

		ScaffoldReportBuilder.buildReport(inputFiles, outputPeptideFile, outputProteinFile);

		Assert.assertEquals(TestingUtilities.compareFilesByLine(outputPeptideFile, result_peptide, true), null, "Peptide scaffold report file does not match");
		Assert.assertEquals(TestingUtilities.compareFilesByLine(outputProteinFile, result_protein, true), null, "Protein scaffold report file does not match");

		FileUtilities.cleanupTempFile(parentFolder);
	}

}
