package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.quameter.QuameterWorkPacket;
import edu.mayo.mprc.searchengine.SearchEngineResult;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.MassUnit;
import edu.mayo.mprc.swift.params2.Tolerance;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class QuameterTask extends AsyncTaskBase {

	private final IdpQonvertTask idpQonvertTask;
	private final FileProducingTask rawFile;
	private File outputFolder;
	private final double maxFDR;
	private final boolean monoisotopic;
	private final boolean publicSearchFiles;

	public QuameterTask(final WorkflowEngine engine,
	                    final SwiftSearchDefinition definition,
	                    final DaemonConnection quaMeterDaemon,
	                    final IdpQonvertTask idpQonvertTask,
	                    final FileProducingTask rawFile,
	                    final File outputFolder,
	                    final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch,
	                    final boolean publicSearchFiles) {
		super(engine, quaMeterDaemon, fileTokenFactory, fromScratch);
		this.rawFile = rawFile;
		maxFDR = 1.0 - definition.getSearchParameters().getScaffoldSettings().getProteinProbability();
		final Tolerance fragmentTolerance = definition.getSearchParameters().getFragmentTolerance();
		if (fragmentTolerance.getUnit() == MassUnit.Da) {
			monoisotopic = fragmentTolerance.getValue() < 0.1;
		} else if (fragmentTolerance.getUnit() == MassUnit.Ppm) {
			monoisotopic = fragmentTolerance.getValue() < 100;
		} else {
			throw new MprcException("Unsupported fragment tolerance unit: " + fragmentTolerance.getUnit().getDescription());
		}
		this.outputFolder = outputFolder;
		this.idpQonvertTask = idpQonvertTask;
		this.publicSearchFiles = publicSearchFiles;
		setName("QuaMeter");
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 * to send a work packet.
	 */
	@Override
	public WorkPacket createWorkPacket() {
		setDescription("QuaMeter analysis of " + fileTokenFactory.fileToTaggedDatabaseToken(rawFile.getResultingFile())
				+ " with search results " + fileTokenFactory.fileToTaggedDatabaseToken(idpQonvertTask.getResultingFile()));

		return new QuameterWorkPacket(isFromScratch(),
				rawFile.getResultingFile(), idpQonvertTask.getResultingFile(), monoisotopic, maxFDR, getResultingFile(), publicSearchFiles);
	}

	@Override
	public void onSuccess() {
		completeWhenFilesAppear(getResultingFile());
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
		// The search engine produced the output file at a different location than where we asked it to
		if (progressInfo instanceof SearchEngineResult) {
			final SearchEngineResult searchEngineResult = (SearchEngineResult) progressInfo;
			outputFolder = searchEngineResult.getResultFile().getParentFile().getAbsoluteFile();
		}
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
		final QuameterTask other = (QuameterTask) obj;
		return Objects.equal(idpQonvertTask, other.idpQonvertTask) &&
				Objects.equal(rawFile, other.rawFile) &&
				Objects.equal(outputFolder, other.outputFolder) &&
				Objects.equal(maxFDR, other.maxFDR);
	}
}

