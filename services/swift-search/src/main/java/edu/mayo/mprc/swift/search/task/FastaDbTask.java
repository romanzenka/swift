package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.fastadb.FastaDbWorkPacket;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

/**
 * Loads a FASTA file into the database.
 *
 * @author Roman Zenka
 */
public final class FastaDbTask extends AsyncTaskBase {
	private final int curationIdToLoad;

	/**
	 * Does not require the curation to be loaded at the expense of having uglier description.
	 */
	public FastaDbTask(final WorkflowEngine engine, final DaemonConnection daemon, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch, final int curationIdToLoad) {
		super(engine, daemon, fileTokenFactory, fromScratch);
		this.curationIdToLoad = curationIdToLoad;
		setName("Fasta DB load");
		setDescription("Load curation #" + curationIdToLoad + " to database.");
	}

	/**
	 * See {@link AsyncTaskBase#AsyncTaskBase}
	 */
	public FastaDbTask(final WorkflowEngine engine, final DaemonConnection daemon, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch, final Curation curationToLoad) {
		super(engine, daemon, fileTokenFactory, fromScratch);
		curationIdToLoad = curationToLoad.getId();
		setName("Fasta DB load");
		setDescription("Load " + fileTokenFactory.fileToTaggedDatabaseToken(curationToLoad.getCurationFile()) + " to database.");
	}

	@Override
	public WorkPacket createWorkPacket() {
		return new FastaDbWorkPacket(curationIdToLoad);
	}

	@Override
	public void onSuccess() {
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(curationIdToLoad);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final FastaDbTask other = (FastaDbTask) obj;
		return Objects.equal(curationIdToLoad, other.curationIdToLoad);
	}
}
