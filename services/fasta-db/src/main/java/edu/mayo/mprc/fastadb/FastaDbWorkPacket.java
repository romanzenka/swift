package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

/**
 * Request to load FASTA database for a curation of a given ID.
 *
 * @author Roman Zenka
 */
public final class FastaDbWorkPacket extends WorkPacketBase {

	private static final long serialVersionUID = -6506219790252689864L;
	private int curationId;

	public FastaDbWorkPacket(final String taskId, final boolean fromScratch) {
		super(fromScratch);
	}

	/**
	 * @param taskId     ID of this task.
	 * @param curationId ID of the curation to deploy.
	 */
	public FastaDbWorkPacket(final String taskId, final int curationId) {
		super(false);
		this.curationId = curationId;
	}

	public int getCurationId() {
		return curationId;
	}

	public void setCurationId(final int curationId) {
		this.curationId = curationId;
	}
}
