package edu.mayo.mprc.scafml;

import edu.mayo.mprc.daemon.files.FileHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ScafmlExport extends FileHolder {
	private static final long serialVersionUID = 7906225867829407626L;
	private static final Pattern SCAFFOLD_4 = Pattern.compile("^[^0-9]*4(\\..*|$)");
	// Version of Scaffold
	private String scaffoldVersion;

	private File scaffoldOutputDir;
	private String experimentName;

	// Additional exports
	private boolean exportSpectra;
	private boolean exportPeptideReport;

	// Thresholds
	private double proteinProbability;
	private double peptideProbability;
	private int minimumPeptideCount;
	private int minimumNonTrypticTerminii;

	// Protein starring
	private String starred;
	private String delimiter;
	private boolean regularExpression;

	// Retaining spectra
	private boolean saveOnlyIdentifiedSpectra;
	private boolean saveNoSpectra;

	public ScafmlExport() {
	}

	public ScafmlExport(final String scaffoldVersion, final String experimentName, final File scaffoldOutputDir, final boolean exportSpectra, final boolean exportPeptideReport, final double proteinProbability, final double peptideProbability, final int minimumPeptideCount, final int minimumNonTrypticTerminii, final String starred, final String delimiter, final boolean regularExpression, final boolean saveOnlyIdentifiedSpectra, final boolean saveNoSpectra) {
		this.scaffoldVersion = scaffoldVersion;
		this.experimentName = experimentName;
		this.scaffoldOutputDir = scaffoldOutputDir;
		this.exportSpectra = exportSpectra;
		this.exportPeptideReport = exportPeptideReport;
		this.proteinProbability = proteinProbability;
		this.peptideProbability = peptideProbability;
		this.minimumPeptideCount = minimumPeptideCount;
		this.minimumNonTrypticTerminii = minimumNonTrypticTerminii;
		this.starred = starred;
		this.delimiter = delimiter;
		this.regularExpression = regularExpression;
		this.saveOnlyIdentifiedSpectra = saveOnlyIdentifiedSpectra;
		this.saveNoSpectra = saveNoSpectra;
	}

	public boolean isScaffold4() {
		return SCAFFOLD_4.matcher(scaffoldVersion).matches();
	}

	public void appendToDocument(final StringBuilder result, final String indent) {
		result
				.append(indent)
				.append("<DisplayThresholds " +
						"name=\"Some Thresholds\" " +
						"id=\"thresh\" " +
						"proteinProbability=\"" + proteinProbability + "\" " +
						"minimumPeptideCount=\"" + minimumPeptideCount + "\" " +
						"peptideProbability=\"" + peptideProbability + "\" ");
		if (isScaffold4()) {
			result
					.append("useCharge=\"true,true,true,true\" ");
		} else {
			result
					.append("minimumNTT=\"" + minimumNonTrypticTerminii + "\" ")
					.append("useCharge=\"true,true,true\" ");
		}
		result
				.append("useMergedPeptideProbability=\"true\"")
				.append("></DisplayThresholds>\n");

		result
				.append(indent)
				.append("<Export type=\"sfd\" thresholds=\"thresh\" path=\"")
				.append(scaffoldOutputDir.getAbsolutePath()).append("\"")
				.append(" saveOnlyIdentifiedSpectra=\"").append((saveOnlyIdentifiedSpectra || saveNoSpectra) ? "true" : "false").append("\"")
				.append(" saveNoSpectra=\"").append(saveNoSpectra ? "true" : "false").append("\"")
				.append("/>\n");

		if (exportPeptideReport) {
			result
					.append(indent)
					.append("<Export type=\"peptide-report\" thresholds=\"thresh\" path=\"").append(scaffoldOutputDir.getAbsolutePath()).append("\"/>\n");
		}

		if (exportSpectra) {
			result
					.append(indent)
					.append("<Export type=\"spectrum\" thresholds=\"thresh\" path=\"").append(scaffoldOutputDir.getAbsolutePath()).append("\"/>\n");
		}

		if (starred != null && !starred.trim().isEmpty()) {
			result
					.append(indent)
					.append("<Annotation id=\"stars\">\n")
					.append(indent)
					.append("\t<Star delimiter=\"" + delimiter + "\" ")
					.append("regEx=\"" + (regularExpression ? "true" : "false") + "\">\n")
					.append(starred).append("\n")
					.append(indent)
					.append("\t</Star>\n")
					.append(indent)
					.append("</Annotation>");
		}

		result.append("\n");
	}

	/**
	 * @return A list of exported file names to be generated.
	 */
	public List<String> getExportFileList() {
		final ArrayList<String> list = new ArrayList<String>(2);
		if (exportPeptideReport) {
			list.add(experimentName + ".peptide-report.xls");
		}
		if (exportSpectra) {
			list.add(experimentName + ".spectra.txt");
		}
		return list;
	}
}

