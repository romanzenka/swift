package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.scaffold.report.ScaffoldReportWorkPacket;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.List;

final class ScaffoldReportTask extends AsyncTaskBase {

	private static final Logger LOGGER = Logger.getLogger(ScaffoldReportTask.class);

	private final List<File> scaffoldOutputFiles;
	private final File peptideReportFile;
	private final File proteinReportFile;

	public static final String TASK_NAME = "ScaffoldReport";

	ScaffoldReportTask(final WorkflowEngine engine, final DaemonConnection daemon, final List<File> scaffoldOutputFiles, final File peptideReportFile, final File proteinReportFile, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(engine, daemon, fileTokenFactory, fromScratch);
		this.scaffoldOutputFiles = scaffoldOutputFiles;
		this.peptideReportFile = peptideReportFile;
		this.proteinReportFile = proteinReportFile;

		setName(TASK_NAME);
		setDescription("Scaffold reports: " + fileTokenFactory.fileToTaggedDatabaseToken(peptideReportFile) + ", " + fileTokenFactory.fileToTaggedDatabaseToken(proteinReportFile));
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 *         to send a work packet.
	 */
	@Override
	public WorkPacket createWorkPacket() {
		if (!isFromScratch() && peptideReportFile.exists() && peptideReportFile.length() > 0 &&
				proteinReportFile.exists() && proteinReportFile.length() > 0) {
			LOGGER.info("Skipping scaffold report task because report output files, " + peptideReportFile.getAbsolutePath() + " and " + proteinReportFile.getAbsolutePath() + ", already exist.");
			return null;
		}

		return new ScaffoldReportWorkPacket(scaffoldOutputFiles, peptideReportFile, proteinReportFile, isFromScratch());
	}

	@Override
	public void onSuccess() {
		completeWhenFilesAppear(proteinReportFile);
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
		//Do nothing
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(peptideReportFile, proteinReportFile);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ScaffoldReportTask other = (ScaffoldReportTask) obj;
		return Objects.equal(peptideReportFile, other.peptideReportFile) && Objects.equal(proteinReportFile, other.proteinReportFile);
	}
}
