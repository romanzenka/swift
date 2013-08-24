package edu.mayo.mprc.dbundeploy;

import edu.mayo.mprc.daemon.WorkPacketBase;
import edu.mayo.mprc.dbcurator.model.Curation;

import java.io.File;

/**
 * Database undeployer request.
 */
public final class DatabaseUndeployerWorkPacket extends WorkPacketBase {
	private static final long serialVersionUID = 20101221L;

	/**
	 * Curation object representing database to be undeployed.
	 */
	private Curation dbToUndeploy;
	private File curationFile;

	/**
	 * @param dbToUndeploy
	 * @param taskId
	 */
	public DatabaseUndeployerWorkPacket(final Curation dbToUndeploy, final String taskId) {
		super(taskId, true);

		this.dbToUndeploy = dbToUndeploy;
		curationFile = dbToUndeploy.getCurationFile();
	}

	public Curation getDbToUndeploy() {
		dbToUndeploy.setCurationFile(curationFile);
		return dbToUndeploy;
	}
}
