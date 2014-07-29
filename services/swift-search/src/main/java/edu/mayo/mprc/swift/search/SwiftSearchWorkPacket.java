package edu.mayo.mprc.swift.search;

import edu.mayo.mprc.daemon.worker.WorkPacketBase;

public final class SwiftSearchWorkPacket extends WorkPacketBase {
	private static final long serialVersionUID = 20110901;
	private int swiftSearchId;
	private int previousSearchRunId; // For reruns

	public SwiftSearchWorkPacket(final int swiftSearchId,
	                             final String taskId,
	                             final boolean fromScratch,
	                             final int previousSearchRunId) {
		super(fromScratch);
		this.swiftSearchId = swiftSearchId;
		this.previousSearchRunId = previousSearchRunId;
	}

	public int getSwiftSearchId() {
		return swiftSearchId;
	}

	public int getPreviousSearchRunId() {
		return previousSearchRunId;
	}
}