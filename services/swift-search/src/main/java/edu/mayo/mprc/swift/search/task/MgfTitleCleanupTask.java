package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.mgf2mgf.MgfTitleCleanupResult;
import edu.mayo.mprc.mgf2mgf.MgfTitleCleanupWorkPacket;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

final class MgfTitleCleanupTask extends AsyncTaskBase implements FileProducingTask {

	private final File mgfToCleanup;
	private File cleanedMgf;

	private boolean cleanupPerformed;

	private static final AtomicInteger TASK_ID = new AtomicInteger(0);

	MgfTitleCleanupTask(final WorkflowEngine engine, final DaemonConnection daemon, final File mgfToCleanup, final File cleanedMgf, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(engine, daemon, fileTokenFactory, fromScratch);
		cleanupPerformed = false;
		this.cleanedMgf = cleanedMgf;
		this.mgfToCleanup = mgfToCleanup;
		setName("Mgf cleanup");

		setDescription(".mgf cleanup " + fileTokenFactory.fileToTaggedDatabaseToken(mgfToCleanup));
	}

	@Override
	public synchronized WorkPacket createWorkPacket() {
		if (!isFromScratch() && cleanedMgf.exists()) {
			cleanupPerformed = true;
			return null;
		}
		return new MgfTitleCleanupWorkPacket(mgfToCleanup, cleanedMgf, false);
	}

	@Override
	public void onSuccess() {
		// Nothing to do.
	}

	void setCleanedMgf(final File cleanedMgf) {
		this.cleanedMgf = cleanedMgf;
	}

	@Override
	public synchronized void onProgress(final ProgressInfo progressInfo) {
		if (progressInfo instanceof MgfTitleCleanupResult) {
			final MgfTitleCleanupResult result = (MgfTitleCleanupResult) progressInfo;
			cleanupPerformed = result.isCleanupPerformed();
		}
	}

	@Override
	public synchronized File getResultingFile() {
		if (cleanupPerformed) {
			return cleanedMgf;
		} else {
			return mgfToCleanup;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(mgfToCleanup);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final MgfTitleCleanupTask other = (MgfTitleCleanupTask) obj;
		return Objects.equal(mgfToCleanup, other.mgfToCleanup);
	}
}
