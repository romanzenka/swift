package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.mgf2mgf.MgfTitleCleanupResult;
import edu.mayo.mprc.mgf2mgf.MgfTitleCleanupWorkPacket;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

final class MgfTitleCleanupTask extends AsyncTaskBase implements FileProducingTask {

	private boolean cleanupPerformed;
	private final File mgfToCleanup;
	private final File cleanedMgf;
	private static final AtomicInteger TASK_ID = new AtomicInteger(0);

	public MgfTitleCleanupTask(final DaemonConnection daemon, final File mgfToCleanup, final File cleanedMgf, final FileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(daemon, fileTokenFactory, fromScratch);
		this.cleanupPerformed = false;
		this.cleanedMgf = cleanedMgf;
		this.mgfToCleanup = mgfToCleanup;
		this.setName("Mgf cleanup");

		this.setDescription(".mgf cleanup " + fileTokenFactory.fileToTaggedDatabaseToken(mgfToCleanup));
	}

	public synchronized WorkPacket createWorkPacket() {
		if (!isFromScratch() && cleanedMgf.exists()) {
			cleanupPerformed = true;
			return null;
		}
		return new MgfTitleCleanupWorkPacket(mgfToCleanup, cleanedMgf, "Mgf Cleanup #" + TASK_ID.incrementAndGet(), false);
	}

	public void onSuccess() {
		// Nothing to do.
	}

	public synchronized void onProgress(final ProgressInfo progressInfo) {
		if (progressInfo instanceof MgfTitleCleanupResult) {
			final MgfTitleCleanupResult result = (MgfTitleCleanupResult) progressInfo;
			cleanupPerformed = result.isCleanupPerformed();
		}
	}

	public synchronized File getResultingFile() {
		if (cleanupPerformed) {
			return cleanedMgf;
		} else {
			return mgfToCleanup;
		}
	}
}
