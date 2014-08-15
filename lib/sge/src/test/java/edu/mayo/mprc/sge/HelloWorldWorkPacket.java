package edu.mayo.mprc.sge;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

/**
 * @author Roman Zenka
 */
public final class HelloWorldWorkPacket extends WorkPacketBase {
	/**
	 * @param fromScratch
	 */
	public HelloWorldWorkPacket(boolean fromScratch) {
		super(fromScratch);
	}
}
