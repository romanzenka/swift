package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.idpicker.IdpQonvertSettings;
import edu.mayo.mprc.idpicker.IdpickerWorkPacket;
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
public final class IdpickerTask extends AsyncTaskBase {

	/**
	 * Key: Input file search specification.
	 * Value: List of searches performed on the file.
	 */
	private EngineSearchTask searchTask;
	private DatabaseDeployment dbDeployment;
	private final File outputFolder;
	private final SwiftSearchDefinition swiftSearchDefinition;
	private final SwiftDao swiftDao;
	private final SearchRun searchRun;

	public IdpickerTask(final WorkflowEngine engine,
	                    final SwiftDao swiftDao,
	                    final SearchRun searchRun,
	                    final SwiftSearchDefinition definition,
	                    final DaemonConnection idpickerDaemon,
	                    final EngineSearchTask searchTask,
	                    final DatabaseDeployment dbDeployment,
	                    final File outputFolder, final FileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(engine, idpickerDaemon, fileTokenFactory, fromScratch);
		this.swiftDao = swiftDao;
		this.searchRun = searchRun;
		this.swiftSearchDefinition = definition;
		this.outputFolder = outputFolder;
		this.searchTask = searchTask;
		this.dbDeployment = dbDeployment;
		setName("IdpQonvert");
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 *         to send a work packet.
	 */
	public WorkPacket createWorkPacket() {
		setDescription("IdpQonvert conversion of " + fileTokenFactory.fileToTaggedDatabaseToken(searchTask.getOutputFile()));
		IdpQonvertSettings params = new IdpQonvertSettings();
		// Max FDR is set to 1- Scaffolds protein probability
		params.setMaxFDR(1.0 - swiftSearchDefinition.getSearchParameters().getScaffoldSettings().getProteinProbability());
		params.setDecoyPrefix(swiftSearchDefinition.getSearchParameters().getDatabase().getDatabaseAnnotation().getDecoyRegex());
		return new IdpickerWorkPacket(getResultingFile(), params, searchTask.getResultingFile(), dbDeployment.getFastaFile(),
				getFullId(), isFromScratch());
	}

	public void onSuccess() {
		FileUtilities.waitForFile(getResultingFile(), new FileListener() {
			@Override
			public void fileChanged(Collection<File> files, boolean timeout) {
				storeReportFile();
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


	public void onProgress(final ProgressInfo progressInfo) {
	}

	public File getResultingFile() {
		final String idpDbFileName = FileUtilities.getFileNameWithoutExtension(searchTask.getResultingFile()) + ".idpDB";
		return new File(outputFolder, idpDbFileName);
	}
}

