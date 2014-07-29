package edu.mayo.mprc.xtandem;

import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.searchengine.EngineWorkPacket;

import java.io.File;

public final class XTandemWorkPacket extends EngineWorkPacket {
	private static final long serialVersionUID = 20110729;

	public XTandemWorkPacket(final boolean fromScratch) {
		super(fromScratch);
	}

	/**
	 * Encapsulates a packet of work for X!Tandem.
	 */
	public XTandemWorkPacket(final File inputFile, final String searchParams, final File outputFile, final File databaseFile, final boolean publishSearchFiles, final boolean fromScratch) {
		super(inputFile, outputFile, searchParams, databaseFile, publishSearchFiles, fromScratch);
	}

	@Override
	public WorkPacket translateToCachePacket(final File cacheFolder) {
		return new XTandemWorkPacket(
				getInputFile(),
				getSearchParams(),
				canonicalOutput(cacheFolder),
				getDatabaseFile(),
				isPublishResultFiles(),
				isFromScratch()
		);
	}
}

