package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
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
public class FastaDbTask extends AsyncTaskBase {
	private int curationIdToLoad;

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

	public int getCurationIdToLoad() {
		return curationIdToLoad;
	}

	public void setCurationIdToLoad(final int curationIdToLoad) {
		this.curationIdToLoad = curationIdToLoad;
	}

	@Override
	public WorkPacket createWorkPacket() {
		return new FastaDbWorkPacket(getFullId(), curationIdToLoad);
	}

	@Override
	public void onSuccess() {
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
	}
}
