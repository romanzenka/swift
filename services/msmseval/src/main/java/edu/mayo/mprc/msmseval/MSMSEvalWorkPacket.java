package edu.mayo.mprc.msmseval;

import edu.mayo.mprc.daemon.CachableWorkPacket;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.WorkPacketBase;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public final class MSMSEvalWorkPacket extends WorkPacketBase implements CachableWorkPacket {
	private static final long serialVersionUID = 20090402L;

	private File sourceMGFFile;
	private File msmsEvalParamFile;
	private File outputDirectory;

	/**
	 * The implemntation of this constructor calls constructor:
	 * MSMSEvalWorkPacket(File sourceMGFFile, File msmsEvalParamFile, File outputDirectory, String taskId, boolean skipIfAlreadyExecuted)
	 * and sets the skipIfAlreadyExecuted flag to a default of true.
	 *
	 * @param sourceMGFFile
	 * @param msmsEvalParamFile
	 * @param outputDirectory
	 * @param taskId
	 */
	public MSMSEvalWorkPacket(final File sourceMGFFile, final File msmsEvalParamFile, final File outputDirectory, final String taskId) {
		this(sourceMGFFile, msmsEvalParamFile, outputDirectory, taskId, false);
	}

	public MSMSEvalWorkPacket(final File sourceMGFFile, final File msmsEvalParamFile, final File outputDirectory, final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);

		this.sourceMGFFile = sourceMGFFile;
		this.outputDirectory = outputDirectory;
		this.msmsEvalParamFile = msmsEvalParamFile;
	}

	public File getSourceMGFFile() {
		return sourceMGFFile;
	}

	public File getMsmsEvalParamFile() {
		return msmsEvalParamFile;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	@Override
	public boolean isPublishResultFiles() {
		// We never publish msmsEval results to the end user - they are fine in the cache folder
		return false;
	}

	@Override
	public File getOutputFile() {
		// We do not need to provide output file as we never publish the result
		return null;
	}

	@Override
	public String getStringDescriptionOfTask() {
		final StringBuilder description = new StringBuilder();
		description
				.append("Input:")
				.append(getSourceMGFFile().getAbsolutePath())
				.append('\n')
				.append("ParamFile:")
				.append(getMsmsEvalParamFile().getAbsolutePath())
				.append('\n');
		return description.toString();
	}

	@Override
	public WorkPacket translateToWorkInProgressPacket(final File wipFolder) {
		return new MSMSEvalWorkPacket(
				getSourceMGFFile(),
				getMsmsEvalParamFile(),
				wipFolder,
				getTaskId()
		);
	}

	@Override
	public List<String> getOutputFiles() {
		return Arrays.asList(
				MSMSEvalWorker.getExpectedResultFileName(getSourceMGFFile(), new File(".")).getName(),
				MSMSEvalWorker.getExpectedEmOutputFileName(getSourceMGFFile(), new File(".")).getName()
		);
	}

	@Override
	public boolean cacheIsStale(final File subFolder, final List<String> outputFiles) {
		final long inputFileModified = getSourceMGFFile().lastModified();
		return inputFileModified > new File(subFolder, outputFiles.get(0)).lastModified() ||
				inputFileModified > new File(subFolder, outputFiles.get(1)).lastModified();
	}

	@Override
	public void reportCachedResult(final ProgressReporter reporter, final File targetFolder, final List<String> outputFiles) {
		final File outputFile = new File(targetFolder, outputFiles.get(0));
		final File emFile = new File(targetFolder, outputFiles.get(1));
		reporter.reportProgress(
				new MsmsEvalResult(
						outputFile,
						emFile));
	}

}
