package edu.mayo.mprc.xtandem;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.searchengine.EngineWorkPacket;

import java.io.File;

public final class XTandemWorkPacket extends EngineWorkPacket {
	private static final long serialVersionUID = 20110729;

	private File workFolder;

	public XTandemWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	/**
	 * Encapsulates a packet of work for X!Tandem.
	 *
	 * @param searchParamsFile Parameter template that is used to generate specific X!Tandem input file.
	 * @param outputFile       Where should X!Tandem put the results to.
	 * @param workFolder       X!Tandem work folder (the param file will be generated in there).
	 */
	public XTandemWorkPacket(final File inputFile, final File searchParamsFile, final File outputFile, final File workFolder, final File databaseFile, final boolean publishSearchFiles, final String taskId, final boolean fromScratch) {
		super(inputFile, outputFile, searchParamsFile, databaseFile, publishSearchFiles, taskId, fromScratch);

		if (inputFile == null) {
			throw new MprcException("X!Tandem request cannot be created: The input file was null");
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
	}

	public File getWorkFolder() {
		return workFolder;
	}

	@Override
	public WorkPacket translateToWorkInProgressPacket(final File wipFolder) {
		return new XTandemWorkPacket(
				getInputFile(),
				getSearchParamsFile(),
				new File(wipFolder, getOutputFile().getName()),
				wipFolder,
				getDatabaseFile(),
				isPublishResultFiles(),
				getTaskId(),
				isFromScratch()
		);
	}
}

