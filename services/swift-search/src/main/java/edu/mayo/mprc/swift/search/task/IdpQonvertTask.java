package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.idpqonvert.IdpQonvertSettings;
import edu.mayo.mprc.idpqonvert.IdpQonvertWorkPacket;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.FileListener;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
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
	private EngineSearchTask searchTask;
	private final File outputFolder;
	private final SwiftSearchDefinition swiftSearchDefinition;
	private final SwiftDao swiftDao;
	private final SearchRun searchRun;

	public IdpQonvertTask(final WorkflowEngine engine,
	                      final SwiftDao swiftDao,
	                      final SearchRun searchRun,
	                      final SwiftSearchDefinition definition,
	                      final DaemonConnection idpQonvertDaemon,
	                      final EngineSearchTask searchTask,
	                      final File outputFolder, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(engine, idpQonvertDaemon, fileTokenFactory, fromScratch);
		this.swiftDao = swiftDao;
		this.searchRun = searchRun;
		swiftSearchDefinition = definition;
		this.outputFolder = outputFolder;
		this.searchTask = searchTask;
		setName("IdpQonvert");
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 *         to send a work packet.
	 */
	@Override
	public WorkPacket createWorkPacket() {
		setDescription("IdpQonvert conversion of " + fileTokenFactory.fileToTaggedDatabaseToken(searchTask.getOutputFile()));
		IdpQonvertSettings params = new IdpQonvertSettings();
		// Max FDR is set to 1- Scaffolds protein probability
		params.setMaxFDR(1.0 - swiftSearchDefinition.getSearchParameters().getScaffoldSettings().getProteinProbability());
		params.setDecoyPrefix(swiftSearchDefinition.getSearchParameters().getDatabase().getDatabaseAnnotation().getDecoyRegex());
		return new IdpQonvertWorkPacket(getResultingFile(), params, searchTask.getResultingFile(),
				swiftSearchDefinition.getSearchParameters().getDatabase().getCurationFile(),
				getFullId(), isFromScratch());
	}

	@Override
	public void onSuccess() {
		FileUtilities.waitForFile(getResultingFile(), new FileListener() {
			@Override
			public void fileChanged(Collection<File> files, boolean timeout) {
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


	@Override
	public void onProgress(final ProgressInfo progressInfo) {
	}

	public File getResultingFile() {
		final String idpDbFileName = FileUtilities.getFileNameWithoutExtension(searchTask.getResultingFile()) + ".idpDB";
		return new File(outputFolder, idpDbFileName);
	}
}

