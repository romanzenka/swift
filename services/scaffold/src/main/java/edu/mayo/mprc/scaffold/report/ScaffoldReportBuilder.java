package edu.mayo.mprc.scaffold.report;

import edu.mayo.mprc.utilities.FileUtilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Builds the peptide and protein reports for Scaffold.
 */
final class ScaffoldReportBuilder {


	private ScaffoldReportBuilder() {
	}

	/**
	 * Builds an easy to read XLS output from multiple scaffold peptide reports.
	 * <p/>
	 * The output is grouped by biological sample name. Each sample is separated by a blank line from other samples.
	 * Within a sample, the rows are ordered by
	 * <ol>
	 * <li>number of unique peptides, descending</li>
	 * <li>peptide accession number, ascending</li>
	 * <li>peptide sequence, ascending</li>
	 * </ol>
	 *
	 * @param inputReports        Input files (Scaffold peptide report).
	 * @param outputPeptideReport Output .xls file for peptide-level list (actually tab-separated).
	 * @param outputProteinReport Output .xls file for protein-level list (actually tab-separated) - differs from peptide-level by omitting the peptide sequence.
	 */
	public static void buildReport(final List<File> inputReports, final File outputPeptideReport, final File outputProteinReport) throws IOException {

		BufferedWriter peptideWriter = null;
		BufferedWriter proteinWriter = null;

		try {
			peptideWriter = new BufferedWriter(new FileWriter(outputPeptideReport));
			proteinWriter = new BufferedWriter(new FileWriter(outputProteinReport));
			boolean first = true;
			for (final File inputReport : inputReports) {
				//Leave a empty line between tables.
				if (!first) {
					peptideWriter.newLine();
					proteinWriter.newLine();
				}

				final ReportBuildingReader reader = new ReportBuildingReader();
				reader.load(inputReport, "3", null);

				peptideWriter.write(reader.getPeptideReport(first));
				proteinWriter.write(reader.getProteinReport(first));

				first = false;
			}

		} finally {
			FileUtilities.closeQuietly(peptideWriter);
			FileUtilities.closeQuietly(proteinWriter);
			FileUtilities.restoreUmaskRights(outputPeptideReport, false);
			FileUtilities.restoreUmaskRights(outputProteinReport, false);
		}
	}

}
