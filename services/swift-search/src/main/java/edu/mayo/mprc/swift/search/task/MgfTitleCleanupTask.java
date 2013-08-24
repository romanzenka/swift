package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.mgf2mgf.MgfTitleCleanupResult;
import edu.mayo.mprc.mgf2mgf.MgfTitleCleanupWorkPacket;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

final class MgfTitleCleanupTask extends AsyncTaskBase implements FileProducingTask {

	private boolean cleanupPerformed;
	private final File mgfToCleanup;
	private final File cleanedMgf;
	private static final AtomicInteger TASK_ID = new AtomicInteger(0);

	public MgfTitleCleanupTask(final WorkflowEngine engine, final DaemonConnection daemon, final File mgfToCleanup, final File cleanedMgf, final FileTokenFactory fileTokenFactory, final boolean fromScratch) {
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
		return new MgfTitleCleanupWorkPacket(mgfToCleanup, cleanedMgf, "Mgf Cleanup #" + TASK_ID.incrementAndGet(), false);
	}

	@Override
	public void onSuccess() {
		// Nothing to do.
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
}
