package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.quameter.QuaMeterWorkPacket;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class QuaMeterTask extends AsyncTaskBase {

	private final IdpQonvertTask idpQonvertTask;
	private final File rawFile;
	private final File outputFolder;
	private final double maxFDR;

	public QuaMeterTask(final WorkflowEngine engine,
	                    final SwiftSearchDefinition definition,
	                    final DaemonConnection quaMeterDaemon,
	                    final IdpQonvertTask idpQonvertTask,
	                    final File rawFile,
	                    final File outputFolder,
	                    final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(engine, quaMeterDaemon, fileTokenFactory, fromScratch);
		this.rawFile = rawFile;
		maxFDR = 1.0 - definition.getSearchParameters().getScaffoldSettings().getProteinProbability();
		this.outputFolder = outputFolder;
		this.idpQonvertTask = idpQonvertTask;
		setName("QuaMeter");
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 *         to send a work packet.
	 */
	@Override
	public WorkPacket createWorkPacket() {
		setDescription("QuaMeter analysis of " + fileTokenFactory.fileToTaggedDatabaseToken(rawFile)
				+ " with search results " + fileTokenFactory.fileToTaggedDatabaseToken(idpQonvertTask.getResultingFile()));

		return new QuaMeterWorkPacket(getFullId(), isFromScratch(),
				rawFile, idpQonvertTask.getResultingFile(), true, maxFDR, getResultingFile());
	}

	@Override
	public void onSuccess() {
		completeWhenFilesAppear(getResultingFile());
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
	}

	public File getResultingFile() {
		final String quaMeterFile = FileUtilities.getFileNameWithoutExtension(idpQonvertTask.getResultingFile()) + ".qual.txt";
		return new File(outputFolder, quaMeterFile);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(idpQonvertTask, rawFile, outputFolder, maxFDR);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final QuaMeterTask other = (QuaMeterTask) obj;
		return Objects.equal(idpQonvertTask, other.idpQonvertTask) && Objects.equal(rawFile, other.rawFile) && Objects.equal(outputFolder, other.outputFolder) && Objects.equal(maxFDR, other.maxFDR);
	}
}

