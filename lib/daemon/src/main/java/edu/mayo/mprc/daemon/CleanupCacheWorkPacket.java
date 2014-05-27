package edu.mayo.mprc.daemon;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

/**
 * When a cache gets this packet, it removes its cached data.
 *
 * @author Roman Zenka
 */
public final class CleanupCacheWorkPacket extends WorkPacketBase {
	/**
	 * @param taskId      Task identifier to be used for nested diagnostic context when logging.
	 * @param fromScratch
	 */
	public CleanupCacheWorkPacket(String taskId, boolean fromScratch) {
		super(taskId, fromScratch);
	}
}
