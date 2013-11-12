package edu.mayo.mprc.idpqonvert;

import edu.mayo.mprc.daemon.CachableWorkPacket;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * A task for idpQonvert to convert .pepXML into .idp files.
 */
public final class IdpQonvertWorkPacket extends WorkPacketBase implements CachableWorkPacket {
	private static final long serialVersionUID = 20121109;

	/**
	 * Input .pepXML file
	 */
	private File inputFile;
	/**
	 * Output .idp file
	 */
	private File outputFile;
	/**
	 * Settings for idpQonvert.
	 */
	private IdpQonvertSettings params;

	/**
	 * The FASTA file to be used.
	 */
	private File fastaFile;

	public IdpQonvertWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	/**
	 * Request to convert a .pepXML file into .idp file
	 *
	 * @param outputFile This is the desired target of the output. The cache can overwrite this to anything it sees fit.
	 *                   If that happens, a {@link IdpQonvertResult} class is sent back
	 *                   as progress report.
	 */
	public IdpQonvertWorkPacket(final File outputFile, final IdpQonvertSettings params, final File inputFile,
	                            final File fastaFile,
	                            final String taskId,
	                            final boolean fromScratch) {
		super(taskId, fromScratch);

		this.inputFile = inputFile;
		this.params = params;
		this.outputFile = outputFile;
		this.fastaFile = fastaFile;
	}

	@Override
	public boolean isPublishResultFiles() {
		return true;
	}

	public File getInputFile() {
		return inputFile;
	}

	@Override
	public File getOutputFile() {
		return outputFile;
	}

	public IdpQonvertSettings getParams() {
		return params;
	}

	public File getFastaFile() {
		return fastaFile;
	}

	@Override
	public String getStringDescriptionOfTask() {
		final String paramString = getParams().toConfigFile();

		return "Inputs:\n" + getInputFile().getAbsolutePath() + "\n\n" +
				"Output:\n" + outputFile.getAbsolutePath() + "\n\n" +
				"Fasta:\n" + fastaFile.getAbsolutePath() + "\n\n" +
				"Parameters:\n" + paramString + "\n\n";
	}

	@Override
	public WorkPacket translateToWorkInProgressPacket(final File wipFolder) {
		return new IdpQonvertWorkPacket(new File(wipFolder, getOutputFile().getName()), getParams(), inputFile,
				fastaFile, getTaskId(), isFromScratch());
	}

	@Override
	public List<String> getOutputFiles() {
		return Arrays.asList(getOutputFile().getName());
	}

	@Override
	public boolean cacheIsStale(final File subFolder, final List<String> outputFiles) {
		final long outputModifiedTime = new File(subFolder, outputFiles.get(0)).lastModified();
		if (inputFile.lastModified() > outputModifiedTime || fastaFile.lastModified() > outputModifiedTime) {
			return true;
		}
		return false;
	}

	@Override
	public void reportCachedResult(final ProgressReporter reporter, final File targetFolder, final List<String> outputFiles) {
		final File cachedFile = new File(targetFolder, outputFiles.get(0));
		reporter.reportProgress(new IdpQonvertResult(cachedFile));
	}
}
