package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import edu.mayo.mprc.comet.CometWorker;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.workflow.engine.TaskBase;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class SqtMs2CombinerTask extends TaskBase implements FileProducingTask {

	private EngineSearchTask cometTask;
	private FileProducingTask msconvertTask;

	public SqtMs2CombinerTask(final WorkflowEngine engine, final EngineSearchTask cometTask, final FileProducingTask msconvertTask) {
		super(engine);
		this.cometTask = cometTask;
		this.msconvertTask = msconvertTask;

		setName("sqt+ms2 combiner");
		setDescription("Combine .sqt and .ms2 outputs for Comet");
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(cometTask, msconvertTask);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final SqtMs2CombinerTask other = (SqtMs2CombinerTask) obj;
		return Objects.equal(this.cometTask, other.cometTask) && Objects.equal(this.msconvertTask, other.msconvertTask);
	}

	@Override
	public void run() {
		// Link the ms2 file next to Comet's SQT file.
		final File cometSqtFile = cometTask.getResultingFile();
		final File finalMs2File;
		if (cometSqtFile.getName().endsWith(CometWorker.SQT)) {
			// SQT files need .ms2 file available
			final String ms2FileName = FileUtilities.stripGzippedExtension(cometSqtFile.getName()) + CometWorker.MS2;
			finalMs2File = new File(cometSqtFile.getParentFile(), ms2FileName);
		} else {
			finalMs2File = null;
		}

		FileUtilities.linkOrCopy(msconvertTask.getResultingFile(), finalMs2File, true, false);
	}

	@Override
	public File getResultingFile() {
		return cometTask.getResultingFile();
	}

	public SearchEngine getSearchEngine() {
		return cometTask.getSearchEngine();
	}
}
