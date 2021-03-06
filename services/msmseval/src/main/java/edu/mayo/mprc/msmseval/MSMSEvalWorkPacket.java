package edu.mayo.mprc.msmseval;

import edu.mayo.mprc.daemon.CachableWorkPacket;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public final class MSMSEvalWorkPacket extends WorkPacketBase implements CachableWorkPacket {
	public static final String OUTPUT_FILE_SUFFIX = "_eval.mod.csv";
	public static final String MZXML_OUTPUT_FILE_EXTENTION = ".mzxml";
	public static final String SCORE_FILE_SUFFIX = "_eval.csv";
	public static final String EM_FILE_SUFFIX = "_em.csv";
	private static final long serialVersionUID = 20090402L;

	private File sourceFile;
	private File msmsEvalParamFile;
	private File outputFile;
	private File outputEmFile;

	public MSMSEvalWorkPacket(final File sourceFile, final File msmsEvalParamFile,
	                          final File outputFile,
	                          final File outputEmFile,
	                          final boolean fromScratch) {
		super(fromScratch);

		this.sourceFile = sourceFile;
		this.outputFile = outputFile;
		this.outputEmFile = outputEmFile;
		this.msmsEvalParamFile = msmsEvalParamFile;
	}

	static File getExpectedMzXMLOutputFileName(final File sourceMGFFile, final File outputDirectory) {
		return new File(outputDirectory, FileUtilities.getFileNameWithoutExtension(sourceMGFFile) + MZXML_OUTPUT_FILE_EXTENTION);
	}

	static File getExpectedMsmsEvalOutputFileName(final File sourceMGFFile, final File outputDirectory) {
		return new File(outputDirectory, getExpectedMzXMLOutputFileName(sourceMGFFile, outputDirectory).getName() + SCORE_FILE_SUFFIX);
	}

	/**
	 * File with information about expectation maximization parameters.
	 */
	public static File getExpectedEmOutputFileName(final File sourceMGFFile, final File outputDirectory) {
		return new File(outputDirectory, getExpectedMzXMLOutputFileName(sourceMGFFile, outputDirectory).getName() + EM_FILE_SUFFIX);
	}

	/**
	 * File with list of spectra (original spectrum numbers) + their msmsEval information.
	 */
	public static File getExpectedResultFileName(final File sourceMGFFile, final File outputDirectory) {
		return new File(outputDirectory, getExpectedMzXMLOutputFileName(sourceMGFFile, outputDirectory).getName() + OUTPUT_FILE_SUFFIX);
	}

	public File getSourceFile() {
		return sourceFile;
	}

	public File getMsmsEvalParamFile() {
		return msmsEvalParamFile;
	}

	@Override
	public File getOutputFile() {
		return outputFile;
	}

	public File getOutputEmFile() {
		return outputEmFile;
	}

	@Override
	public boolean isPublishResultFiles() {
		// We never publish msmsEval results to the end user - they are fine in the cache folder
		return false;
	}

	@Override
	public String getStringDescriptionOfTask() {
		final StringBuilder description = new StringBuilder();
		description
				.append("Input:")
				.append(getSourceFile().getAbsolutePath())
				.append('\n')
				.append("ParamFile:")
				.append(getMsmsEvalParamFile().getAbsolutePath())
				.append('\n');
		return description.toString();
	}

	@Override
	public WorkPacket translateToCachePacket(final File cacheFolder) {
		return new MSMSEvalWorkPacket(
				getSourceFile(),
				getMsmsEvalParamFile(),
				new File(cacheFolder, getExpectedResultFileName(getSourceFile(), cacheFolder).getName()),
				new File(cacheFolder, getExpectedEmOutputFileName(getSourceFile(), cacheFolder).getName()),
				isFromScratch()
		);
	}

	@Override
	public List<String> getOutputFiles() {
		return Arrays.asList(
				getExpectedResultFileName(getSourceFile(), new File(".")).getName(),
				getExpectedEmOutputFileName(getSourceFile(), new File(".")).getName()
		);
	}

	@Override
	public boolean cacheIsStale(final File subFolder, final List<String> outputFiles) {
		final long inputFileModified = getSourceFile().lastModified();
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
