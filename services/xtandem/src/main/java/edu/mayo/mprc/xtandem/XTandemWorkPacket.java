package edu.mayo.mprc.xtandem;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.searchengine.EngineWorkPacket;

import java.io.File;

public final class XTandemWorkPacket extends EngineWorkPacket {
	private static final long serialVersionUID = 20110729;

	public XTandemWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	/**
	 * Encapsulates a packet of work for X!Tandem.
	 */
	public XTandemWorkPacket(final File inputFile, final String searchParams, final File outputFile, final File databaseFile, final boolean publishSearchFiles, final String taskId, final boolean fromScratch) {
		super(inputFile, outputFile, searchParams, databaseFile, publishSearchFiles, taskId, fromScratch);
	}

	@Override
	public WorkPacket translateToCachePacket(final File cacheFolder) {
		return new XTandemWorkPacket(
				getInputFile(),
				getSearchParams(),
				canonicalOutput(cacheFolder),
				getDatabaseFile(),
				isPublishResultFiles(),
				getTaskId(),
				isFromScratch()
		);
	}
}

