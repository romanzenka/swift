package edu.mayo.mprc.myrimatch;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.searchengine.EngineWorkPacket;

import java.io.File;

public final class MyrimatchWorkPacket extends EngineWorkPacket {

	private static final long serialVersionUID = 20110711;

	private File workFolder;
	private long numForwardEntries;
	private String decoySequencePrefix;

	public MyrimatchWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	/**
	 * Encapsulates a packet of work for Myrimatch.
	 *
	 * @param workFolder Myrmimatch work folder (the param file will be generated in there).
	 */
	public MyrimatchWorkPacket(final File outputFile, final File searchParamsFile, final File inputFile, final File workFolder, final File databaseFile,
	                           final long numForwardEntries, final String decoySequencePrefix, final boolean publishSearchFiles, final String taskId, final boolean fromScratch) {
		super(inputFile, outputFile, searchParamsFile, databaseFile, publishSearchFiles, taskId, fromScratch);

		if (inputFile == null) {
			throw new MprcException("X!Tandem request cannot be created: The .mgf file was null");
		}
		if (searchParamsFile == null) {
			throw new MprcException("X!Tandem request cannot be created: The search params file has to be set");
		}
		if (outputFile == null) {
			throw new MprcException("X!Tandem request cannot be created: The resulting file was null");
		}
		if (workFolder == null) {
			throw new MprcException("X!Tandem request cannot be created: The work folder was null");
		}
		if (databaseFile == null) {
			throw new MprcException("X!Tandem request cannot be created: Path to fasta file was null");
		}

		this.workFolder = workFolder;
		this.numForwardEntries = numForwardEntries;
		this.decoySequencePrefix = decoySequencePrefix;
	}

	public File getWorkFolder() {
		return workFolder;
	}

	public long getNumForwardEntries() {
		return numForwardEntries;
	}

	public String getDecoySequencePrefix() {
		return decoySequencePrefix;
	}

	@Override
	public WorkPacket translateToWorkInProgressPacket(final File wipFolder) {
		return new MyrimatchWorkPacket(
				new File(wipFolder, getOutputFile().getName()), getSearchParamsFile(), getInputFile(),
				wipFolder,
				getDatabaseFile(),
				getNumForwardEntries(),
				getDecoySequencePrefix(),
				isPublishResultFiles(),
				getTaskId(),
				isFromScratch()
		);
	}
}
