package edu.mayo.mprc.myrimatch;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.searchengine.EngineWorkPacket;

import java.io.File;

public final class MyriMatchWorkPacket extends EngineWorkPacket {

	private static final long serialVersionUID = 20110711;

	private String decoySequencePrefix;

	public MyriMatchWorkPacket(final boolean fromScratch) {
		super(fromScratch);
	}

	/**
	 * Encapsulates a packet of work for MyriMatch.
	 */
	public MyriMatchWorkPacket(final File outputFile, final String searchParams, final File inputFile, final File databaseFile,
	                           final String decoySequencePrefix, final boolean publishSearchFiles, final boolean fromScratch) {
		super(inputFile, outputFile, searchParams, databaseFile, publishSearchFiles, fromScratch);

		if (inputFile == null) {
			throw new MprcException("MyriMatch request cannot be created: The .mgf file was null");
		}
		if (searchParams == null) {
			throw new MprcException("MyriMatch request cannot be created: The search params have to be set");
		}
		if (outputFile == null) {
			throw new MprcException("MyriMatch request cannot be created: The resulting file was null");
		}
		if (databaseFile == null) {
			throw new MprcException("MyriMatch request cannot be created: Path to fasta file was null");
		}

		this.decoySequencePrefix = decoySequencePrefix;
	}

	public String getDecoySequencePrefix() {
		return decoySequencePrefix;
	}

	@Override
	public WorkPacket translateToCachePacket(final File cacheFolder) {
		return new MyriMatchWorkPacket(
				canonicalOutput(cacheFolder),
				getSearchParams(),
				getInputFile(),
				getDatabaseFile(),
				getDecoySequencePrefix(),
				isPublishResultFiles(),
				isFromScratch()
		);
	}
}
