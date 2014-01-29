package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.quameterdb.QuameterDbWorkPacket;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

/**
 * @author Roman Zenka
 */
public final class QuameterDbTask extends AsyncTaskBase {
	private SearchDbTask searchDbTask;
	private QuaMeterTask quaMeterTask;
	private FileSearch fileSearch;

	public QuameterDbTask(WorkflowEngine engine, DaemonConnection daemon, DatabaseFileTokenFactory fileTokenFactory, boolean fromScratch, SearchDbTask searchDbTask, QuaMeterTask quaMeterTask, FileSearch fileSearch) {
		super(engine, daemon, fileTokenFactory, fromScratch);
		this.searchDbTask = searchDbTask;
		this.fileSearch = fileSearch;
		this.quaMeterTask = quaMeterTask;
		setName("quameter");
		setDescription("Load QuaMeter metadata from " + fileTokenFactory.fileToTaggedDatabaseToken(fileSearch.getInputFile()));
	}

	@Override
	public WorkPacket createWorkPacket() {
		return new QuameterDbWorkPacket(getFullId(),
				isFromScratch(),
				searchDbTask.getLoadedTandemFileMetadata().get(fileSearch.getInputFile().getName()),
				fileSearch.getId(),
				quaMeterTask.getResultingFile()
		);
	}

	@Override
	public void onSuccess() {
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(searchDbTask, quaMeterTask, fileSearch.getId());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final QuameterDbTask other = (QuameterDbTask) obj;
		return Objects.equal(this.searchDbTask, other.searchDbTask)
				&& Objects.equal(this.quaMeterTask, other.quaMeterTask)
				&& Objects.equal(this.fileSearch.getId(), other.fileSearch.getId());
	}
}
