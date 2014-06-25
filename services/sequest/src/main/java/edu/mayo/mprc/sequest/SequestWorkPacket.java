package edu.mayo.mprc.sequest;

import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.searchengine.EngineWorkPacket;

import java.io.File;

/**
 * All information Sequest needs to run a search.
 */
public final class SequestWorkPacket extends EngineWorkPacket {
	private static final long serialVersionUID = 20101221L;

	public SequestWorkPacket(final File outputFile, final String searchParams, final File inputFile, final File databaseFile, final boolean publishSearchFiles, final String taskId, final boolean fromScratch) {
		super(inputFile, outputFile, searchParams, databaseFile, publishSearchFiles, taskId, fromScratch);
	}

	@Override
	public WorkPacket translateToCachePacket(final File cacheFolder) {
		return new SequestWorkPacket(
				canonicalOutput(cacheFolder),
				getSearchParams(),
				getInputFile(),
				getDatabaseFile(),
				isPublishResultFiles(),
				getTaskId(),
				isFromScratch()
		);
	}
}
