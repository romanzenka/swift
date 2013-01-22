package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.idpicker.IdpQonvertSettings;
import edu.mayo.mprc.idpicker.IdpickerWorkPacket;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;

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

	public IdpickerTask(final SwiftSearchDefinition definition,
	                    final DaemonConnection idpickerDaemon,
	                    final EngineSearchTask searchTask,
	                    final DatabaseDeployment dbDeployment,
	                    final File outputFolder, final FileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(idpickerDaemon, fileTokenFactory, fromScratch);
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
		setDescription("IdpQonvert conversion of " + searchTask.getOutputFile().getAbsolutePath());
		IdpQonvertSettings params = new IdpQonvertSettings();
		// Max FDR is set to 1- Scaffolds protein probability
		params.setMaxFDR(1.0 - swiftSearchDefinition.getSearchParameters().getScaffoldSettings().getProteinProbability());
		params.setDecoyPrefix(swiftSearchDefinition.getSearchParameters().getDatabase().getDatabaseAnnotation().getDecoyRegex());
		return new IdpickerWorkPacket(getResultingFile(), params, searchTask.getResultingFile(), dbDeployment.getFastaFile(),
				getFullId(), isFromScratch());
	}

	public void onSuccess() {
		completeWhenFilesAppear(getResultingFile());
	}

	public void onProgress(final ProgressInfo progressInfo) {
	}

	public File getResultingFile() {
		final String idpDbFileName = FileUtilities.getFileNameWithoutExtension(searchTask.getResultingFile()) + ".idpDB";
		return new File(outputFolder, idpDbFileName);
	}
}

