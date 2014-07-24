package edu.mayo.mprc.daemon;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

import java.util.UUID;

/**
 * When a cache gets this packet, it removes its cached data.
 *
 * @author Roman Zenka
 */
public final class CleanupCacheWorkPacket extends WorkPacketBase {
	/**
	 * @param taskId      Task identifier for logging.
	 * @param fromScratch
	 */
	public CleanupCacheWorkPacket(UUID taskId, boolean fromScratch) {
		super(fromScratch);
	}
}
