package edu.mayo.mprc.comet;

import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.searchengine.EngineWorkPacket;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;

public final class CometWorkPacket extends EngineWorkPacket {
	private static final long serialVersionUID = 20110729;

	public CometWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	/**
	 * Encapsulates a packet of work for Comet.
	 */
	public CometWorkPacket(final File inputFile, final String searchParams, final File outputFile, final File databaseFile, final boolean publishSearchFiles, final String taskId, final boolean fromScratch) {
		super(inputFile, outputFile, searchParams, databaseFile, publishSearchFiles, taskId, fromScratch);
	}

	/**
	 * We need to take care of .pep.xml extension that does not register as a file extension otherwise (only .xml gets chopped off)
	 */
	@Override
	public File canonicalOutput(final File cacheFolder) {
		final String name = FileUtilities.stripGzippedExtension(getInputFile().getName())
				+ "."
				+ FileUtilities.getGzippedExtension(getOutputFile().getName(), new String[]{"pep.xml"});
		return new File(cacheFolder, name);
	}

	@Override
	public WorkPacket translateToCachePacket(final File cacheFolder) {
		return new CometWorkPacket(
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

