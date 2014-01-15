package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.workflow.engine.TaskBase;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;
import edu.mayo.mprc.workflow.persistence.TaskState;

import java.io.File;

/**
 * Checks that the externally provided mzML file is all right for Swift to process.
 * If not, cleans the file up so it goes through our pipeline unharmed.
 *
 * @author Roman Zenka
 */
public final class MzMlCleanupTask extends TaskBase implements FileProducingTask {
	private final File mzMlFile;

	public MzMlCleanupTask(WorkflowEngine engine, File mzMlFile, final DatabaseFileTokenFactory fileTokenFactory) {
		super(engine);
		this.mzMlFile = mzMlFile;
		setName("mzML cleanup");

		setDescription(".mzML cleanup " + fileTokenFactory.fileToTaggedDatabaseToken(mzMlFile));
	}

	@Override
	public File getResultingFile() {
		return mzMlFile;
	}

	@Override
	public void run() {
		// We do not check anything right now, hoping the file is a-ok
		setState(TaskState.COMPLETED_SUCCESFULLY);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(mzMlFile);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final MzMlCleanupTask other = (MzMlCleanupTask) obj;
		return Objects.equal(mzMlFile, other.mzMlFile);
	}
}
