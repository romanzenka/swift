package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.mascot.MascotResultUrl;
import edu.mayo.mprc.mascot.MascotWorkPacket;
import edu.mayo.mprc.myrimatch.MyriMatchWorkPacket;
import edu.mayo.mprc.omssa.OmssaWorkPacket;
import edu.mayo.mprc.searchengine.SearchEngineResult;
import edu.mayo.mprc.sequest.SequestMGFWorkPacket;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;
import edu.mayo.mprc.xtandem.XTandemWorkPacket;

import java.io.File;

/**
 * A search on one of the saerch engines.
 */
final class EngineSearchTask extends AsyncTaskBase implements FileProducingTask {
	private final SearchEngine engine;
	private final FileProducingTask inputFile;
	private final Curation curation;
	private final DatabaseDeploymentResult deploymentResult;
	private final File paramsFile;
	private final boolean publicSearchFiles;
	private File outputFile;

	/**
	 * When true, the intermediate search files are provided for the user. In case of caching
	 * the intermediates, the file is also copied to the resulting directory. When the cache
	 * is not enabled, this parameter has no effect, as the files are already published.
	 */

	EngineSearchTask(
			final WorkflowEngine workflowEngine,
			final SearchEngine engine,
			final String searchId,
			final FileProducingTask inputFile,
			final Curation curation,
			final DatabaseDeploymentResult deploymentResult,
			final File outputFile,
			final File paramsFile,
			final boolean publicSearchFiles,
			final DaemonConnection searchEngineDaemon, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(workflowEngine, searchEngineDaemon, fileTokenFactory, fromScratch);
		this.engine = engine;
		this.outputFile = outputFile;
		this.paramsFile = paramsFile;
		this.inputFile = inputFile;
		this.curation = curation;
		this.deploymentResult = deploymentResult;
		this.publicSearchFiles = publicSearchFiles;
		setName(engine.getFriendlyName() + " search");
		setDescription(engine.getFriendlyName() + " search: " + searchId);
	}

	public File getOutputFile() {
		return outputFile;
	}

	public SearchEngine getSearchEngine() {
		return engine;
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 *         to send a work packet.
	 */
	@Override
	public WorkPacket createWorkPacket() {
		updateDescription(null);

		WorkPacket workPacket = null;
		if ("MASCOT".equalsIgnoreCase(engine.getCode())) {
			workPacket = new MascotWorkPacket(
					outputFile,
					paramsFile,
					inputFile.getResultingFile(),
					deploymentResult.getShortDbName(),
					getFullId(),
					isFromScratch(),
					publicSearchFiles);
		} else if ("SEQUEST".equalsIgnoreCase(engine.getCode())) {
			workPacket = new SequestMGFWorkPacket(
					outputFile,
					paramsFile,
					inputFile.getResultingFile(),
					getSequestDatabase(),
					publicSearchFiles,
					getFullId(),
					isFromScratch());
		} else if ("TANDEM".equalsIgnoreCase(engine.getCode())) {
			workPacket = new XTandemWorkPacket(
					inputFile.getResultingFile(),
					paramsFile,
					outputFile,
					outputFile.getParentFile(),
					curation.getCurationFile(),
					publicSearchFiles,
					getFullId(),
					isFromScratch());
		} else if ("OMSSA".equalsIgnoreCase(engine.getCode())) {
			workPacket = new OmssaWorkPacket(
					outputFile,
					paramsFile,
					inputFile.getResultingFile(),
					deploymentResult.getFastaFile(),
					deploymentResult.getGeneratedFiles(),
					publicSearchFiles,
					getFullId(),
					isFromScratch());
		} else if ("MYRIMATCH".equalsIgnoreCase(engine.getCode())) {
			workPacket = new MyriMatchWorkPacket(
					outputFile, paramsFile, inputFile.getResultingFile(),
					outputFile.getParentFile(),
					curation.getCurationFile(),
					curation.getDecoyRegex(),
					publicSearchFiles, getFullId(),
					isFromScratch()
			);
		} else {
			throw new MprcException("Unsupported engine: " + engine.getFriendlyName());
		}
		return workPacket;
	}

	/**
	 * @return Either Sequest .hdr file (when database got indexed) or the .fasta file (no deployment done).
	 */
	private File getSequestDatabase() {
		return deploymentResult.getSequestHdrFile() == null ? deploymentResult.getFastaFile() : deploymentResult.getSequestHdrFile();
	}

	private void updateDescription(final String mascotResultLink) {
		// If the string looks like a path to a file, wrap it in proper tags
		// Otherwise we just use it as-is
		final String deployedFileString = getDeployedDatabaseFile(engine, deploymentResult);
		final String deployedFileDbString;
		if (deployedFileString.contains(File.separator)) {
			File deployedFile = new File(deployedFileString);
			deployedFileDbString = fileTokenFactory.fileToTaggedDatabaseToken(deployedFile);
		} else {
			deployedFileDbString = deployedFileString;
		}

		setDescription(engine.getFriendlyName() + " search: "
				+ fileTokenFactory.fileToTaggedDatabaseToken(inputFile.getResultingFile())
				+ " params: " + fileTokenFactory.fileToTaggedDatabaseToken(paramsFile)
				+ " db: " + deployedFileDbString
				+ (mascotResultLink != null ? " <a href=\"" + mascotResultLink + "\">Open in Mascot</a>" : ""));
	}

	private String getDeployedDatabaseFile(final SearchEngine engine, final DatabaseDeploymentResult deploymentResult) {
		if ("MASCOT".equalsIgnoreCase(engine.getCode())) {
			return deploymentResult.getShortDbName();
		} else if ("SEQUEST".equalsIgnoreCase(engine.getCode())) {
			return getSequestDatabase().getAbsolutePath();
		} else if ("TANDEM".equalsIgnoreCase(engine.getCode())) {
			return curation.getShortName();
		} else if ("OMSSA".equalsIgnoreCase(engine.getCode())) {
			// TODO: OMSSA might be deploying other file types
			return deploymentResult.getShortDbName();
		} else if ("MYRIMATCH".equalsIgnoreCase(engine.getCode())) {
			return curation.getShortName();
		} else {
			throw new MprcException("Unsupported engine: " + engine.getFriendlyName());
		}
	}

	@Override
	public void onSuccess() {
		completeWhenFilesAppear(outputFile);
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
		// The search engine produced the output file at a different location than where we asked it to
		if (progressInfo instanceof SearchEngineResult) {
			final SearchEngineResult searchEngineResult = (SearchEngineResult) progressInfo;
			outputFile = searchEngineResult.getResultFile();
		} else if (progressInfo instanceof MascotResultUrl) {
			final MascotResultUrl mascotResultUrl = (MascotResultUrl) progressInfo;
			updateDescription(mascotResultUrl.getMascotUrl());
		}

	}

	@Override
	public File getResultingFile() {
		return outputFile;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof EngineSearchTask)) {
			return false;
		}

		EngineSearchTask that = (EngineSearchTask) o;

		if (publicSearchFiles != that.publicSearchFiles) {
			return false;
		}
		if (engine != null ? !engine.equals(that.engine) : that.engine != null) {
			return false;
		}
		if (inputFile != null ? !inputFile.equals(that.inputFile) : that.inputFile != null) {
			return false;
		}
		if (outputFile != null ? !outputFile.equals(that.outputFile) : that.outputFile != null) {
			return false;
		}
		if (paramsFile != null ? !paramsFile.equals(that.paramsFile) : that.paramsFile != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = engine != null ? engine.hashCode() : 0;
		result = 31 * result + (inputFile != null ? inputFile.hashCode() : 0);
		result = 31 * result + (paramsFile != null ? paramsFile.hashCode() : 0);
		result = 31 * result + (outputFile != null ? outputFile.hashCode() : 0);
		result = 31 * result + (publicSearchFiles ? 1 : 0);
		return result;
	}
}
