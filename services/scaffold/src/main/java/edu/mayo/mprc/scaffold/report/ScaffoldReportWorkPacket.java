package edu.mayo.mprc.scaffold.report;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

import java.io.File;
import java.util.List;

public final class ScaffoldReportWorkPacket extends WorkPacketBase {

	private static final long serialVersionUID = 1L;

	private List<File> scaffoldOutputFiles;
	private File peptideReportFile;
	private File proteinReportFile;

	public ScaffoldReportWorkPacket(final boolean fromScratch) {
		super(fromScratch);
	}

	/**
	 * @param scaffoldOutputFiles
	 */
	public ScaffoldReportWorkPacket(final List<File> scaffoldOutputFiles, final File peptideReportFile, final File proteinReportFile, final boolean fromScratch) {
		super(fromScratch);

		this.scaffoldOutputFiles = scaffoldOutputFiles;
		this.peptideReportFile = peptideReportFile;
		this.proteinReportFile = proteinReportFile;
	}

	public List<File> getScaffoldOutputFiles() {
		return scaffoldOutputFiles;
	}

	public File getPeptideReportFile() {
		return peptideReportFile;
	}

	public File getProteinReportFile() {
		return proteinReportFile;
	}
}
