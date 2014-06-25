package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.idpqonvert.IdpQonvertSettings;
import edu.mayo.mprc.idpqonvert.IdpQonvertWorkPacket;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.FileListener;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.Task;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;
import java.util.Collection;

/**
 * @author Roman Zenka
 */
public final class IdpQonvertTask extends AsyncTaskBase {

	/**
	 * Key: Input file search specification.
	 * Value: List of searches performed on the file.
	 */
	private final EngineSearchTask searchTask;
	private final File outputFolder;
	private final String decoyPrefix;
	private final File curationFile;

	private boolean embedSpectrumScanTimes;
	private double maxFDR;

	private final SwiftDao swiftDao;
	private final SearchRun searchRun;

	/**
	 * Only when true the resulting idpDB file gets published.
	 * This is done to switch off publishing of results created solely for QuaMeter's purpose.
	 */
	private boolean publishResult;

	public IdpQonvertTask(final WorkflowEngine engine,
	                      final SwiftDao swiftDao,
	                      final SearchRun searchRun,
	                      final SwiftSearchDefinition definition,
	                      final DaemonConnection idpQonvertDaemon,
	                      final EngineSearchTask searchTask,
	                      final File outputFolder,
	                      final boolean publishResult,
	                      final DatabaseFileTokenFactory fileTokenFactory,
	                      final boolean fromScratch) {
		super(engine, idpQonvertDaemon, fileTokenFactory, fromScratch);
		this.swiftDao = swiftDao;
		this.searchRun = searchRun;
		maxFDR = 1.0 - definition.getSearchParameters().getScaffoldSettings().getProteinProbability();
		curationFile = definition.getSearchParameters().getDatabase().getCurationFile();
		decoyPrefix = definition.getSearchParameters().getDatabase().getDatabaseAnnotation().getDecoyRegex();
		this.outputFolder = outputFolder;
		this.searchTask = searchTask;
		this.publishResult = publishResult;
		this.embedSpectrumScanTimes = false;
		setName("IdpQonvert");
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 * to send a work packet.
	 */
	@Override
	public WorkPacket createWorkPacket() {
		setDescription("IdpQonvert conversion of " + fileTokenFactory.fileToTaggedDatabaseToken(searchTask.getOutputFile()));
		final IdpQonvertSettings params = new IdpQonvertSettings();
		// Max FDR is set to 1- Scaffolds protein probability
		params.setMaxFDR(getMaxFDR());
		params.setEmbedSpectrumScanTimes(isEmbedSpectrumScanTimes());
		params.setDecoyPrefix(decoyPrefix);

		return new IdpQonvertWorkPacket(
				getResultingFile(), params,
				searchTask.getResultingFile(),
				curationFile,
				searchTask.getInputFile().getResultingFile(),
				getFullId(),
				isFromScratch());
	}

	@Override
	public void onSuccess() {
		FileUtilities.waitForFile(getResultingFile(), new FileListener() {
			@Override
			public void fileChanged(final Collection<File> files, final boolean timeout) {
				if (!timeout) {
					storeReportFile();
				}
				completeWhenFilesAppear(getResultingFile());
			}
		});
	}

	/**
	 * Store information into the database that we produced a particular report file.
	 * This has to happen whenever Scaffold successfully finished (be it because it ran,
	 * or if it was done previously).
	 */
	private void storeReportFile() {
		if (publishResult) {
			swiftDao.begin();
			try {
				// Scaffold finished. Store the resulting file.
				swiftDao.storeReport(searchRun.getId(), getResultingFile());
				swiftDao.commit();
			} catch (Exception t) {
				swiftDao.rollback();
				throw new MprcException("Could not store change in task information", t);
			}
		}
	}


	@Override
	public void onProgress(final ProgressInfo progressInfo) {
	}

	public File getResultingFile() {
		final String idpDbFileName = FileUtilities.getFileNameWithoutExtension(searchTask.getResultingFile()) + ".idpDB";
		return new File(outputFolder, idpDbFileName);
	}

	public void upgrade(final Task other) {
		if (other instanceof IdpQonvertTask) {
			if (((IdpQonvertTask) other).publishResult) {
				publishResult = true;
			}
		}
	}

	public double getMaxFDR() {
		return maxFDR;
	}

	/**
	 * Override maxFDR that was taken from the search definition.
	 *
	 * @param maxFDR FDR to override the value with.
	 */
	public void setMaxFDR(double maxFDR) {
		this.maxFDR = maxFDR;
	}

	public boolean isEmbedSpectrumScanTimes() {
		return embedSpectrumScanTimes;
	}

	public void setEmbedSpectrumScanTimes(boolean embedSpectrumScanTimes) {
		this.embedSpectrumScanTimes = embedSpectrumScanTimes;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(searchTask, outputFolder, maxFDR, embedSpectrumScanTimes, decoyPrefix, curationFile);
	}

	/**
	 * Two idpicker tasks are considered equal even if they do not have the publishResults set.
	 * One is "subset" of the other.
	 * <p/>
	 * That is why we define the method {@link #upgrade} that takes the other tasks that we are equal to and ensures
	 * that we set publish to true when needed.
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final IdpQonvertTask other = (IdpQonvertTask) obj;
		return Objects.equal(searchTask, other.searchTask)
				&& Objects.equal(outputFolder, other.outputFolder)
				&& Objects.equal(maxFDR, other.maxFDR)
				&& Objects.equal(embedSpectrumScanTimes, other.embedSpectrumScanTimes)
				&& Objects.equal(decoyPrefix, other.decoyPrefix)
				&& Objects.equal(curationFile, other.curationFile);
	}

	@Override
	public String toString() {
		return "maxFDR=" + maxFDR;
	}
}

