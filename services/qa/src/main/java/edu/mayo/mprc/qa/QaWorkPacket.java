package edu.mayo.mprc.qa;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

import java.io.File;
import java.util.List;

/**
 * QA task work packet.
 */
public final class QaWorkPacket extends WorkPacketBase {

	private static final long serialVersionUID = 20101025L;

	private List<ExperimentQa> experimentQaTokens;
	private File qaReportFolderFile;
	private File reportFile;
	private String decoyRegex;

	/**
	 * Create a new work packet.
	 *
	 * @param experimentQas      A list of all experiment QA information objects, one per each Scaffold file produced.
	 * @param qaReportFolderFile A folder to put the QA files into (the images and extracted data files)
	 * @param reportFile         Name of the master report file (.html)
	 * @param decoyRegex         The prefix the database uses to mark reverse entries.
	 * @param taskId             Id of this task, see {@link WorkPacketBase}.
	 */
	public QaWorkPacket(final List<ExperimentQa> experimentQas, final File qaReportFolderFile, final File reportFile, final String decoyRegex, final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);

		experimentQaTokens = experimentQas;

		this.qaReportFolderFile = qaReportFolderFile;
		this.reportFile = reportFile;
		this.decoyRegex = decoyRegex;
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
