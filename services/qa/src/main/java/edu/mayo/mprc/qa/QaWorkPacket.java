package edu.mayo.mprc.qa;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

import java.io.File;
import java.util.List;

/**
 * QA task work packet.
 */
public final class QaWorkPacket extends WorkPacketBase {

	private static final long serialVersionUID = 20140507L;

	private String searchRunName;
	private List<ExperimentQa> experimentQaTokens;
	private File qaReportFolderFile;
	private File reportFile;
	private String decoyRegex;

	/**
	 * Create a new work packet.
	 *  @param searchRunName      Name of the entire search run. This will be used for naming the summary spreadsheet to prevent confusion.
	 * @param experimentQas      A list of all experiment QA information objects, one per each Scaffold file produced.
	 * @param qaReportFolderFile A folder to put the QA files into (the images and extracted data files)
	 * @param reportFile         Name of the master report file (.html)
	 * @param decoyRegex         The prefix the database uses to mark reverse entries.
	 */
	public QaWorkPacket(final String searchRunName, final List<ExperimentQa> experimentQas, final File qaReportFolderFile, final File reportFile, final String decoyRegex, final boolean fromScratch) {
		super(fromScratch);

		this.searchRunName = searchRunName;
		experimentQaTokens = experimentQas;

		this.qaReportFolderFile = qaReportFolderFile;
		this.reportFile = reportFile;
		this.decoyRegex = decoyRegex;
	}

	public String getSearchRunName() {
		return searchRunName;
	}

	public List<ExperimentQa> getExperimentQas() {
		return experimentQaTokens;
	}

	/**
	 * Null map values are not valid.
	 *
	 * @return
	 */
	public File getQaReportFolder() {
		return qaReportFolderFile;
	}

	public File getReportFile() {
		return reportFile;
	}

	public String getDecoyRegex() {
		return decoyRegex;
	}
}
