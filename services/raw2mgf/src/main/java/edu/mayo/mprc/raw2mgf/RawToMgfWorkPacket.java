package edu.mayo.mprc.raw2mgf;

import edu.mayo.mprc.daemon.CachableWorkPacket;
import edu.mayo.mprc.daemon.WorkCache;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * One task for batch converter.
 * Defines who searches, using what params, what file and where to put outputs.
 * The packet is now in the format used by messaging. Eventually there will be an unified system for both grid engine
 * and daemon messaging in place.
 */
public final class RawToMgfWorkPacket extends WorkPacketBase implements CachableWorkPacket {
	private static final long serialVersionUID = 20071220L;
	private String params;
	private File outputFile;
	private boolean skipIfExists;
	private File inputFile;
	private boolean publicAccess;

	public RawToMgfWorkPacket(final boolean fromScratch) {
		super(fromScratch);
	}

	/**
	 * Request to convert a .RAW file to .mgf file.
	 *  @param outputFile   This is the desired target of the output. The cache can overwrite this to anything it sees fit.
	 *                     If that happens, a {@link RawToMgfResult} class is sent back
	 *                     as progress report.
	 * @param publicAccess If the .mgf caching is enabled, the files will never be visible to the end user.
	 */
	public RawToMgfWorkPacket(final String params,
	                          final File outputFile,
	                          final boolean bSkipIfExists,
	                          final File inputFile,
	                          final boolean fromScratch,
	                          final boolean publicAccess) {
		super(fromScratch);

		assert params != null : "Raw2MGF request cannot be created: parameters are null";
		assert outputFile != null : "Raw2MGF request cannot be created: output file is null";
		assert inputFile != null : "Raw2MGF request cannot be created: input file is null";

		this.params = params;
		this.outputFile = outputFile;
		skipIfExists = bSkipIfExists;
		this.inputFile = inputFile;
		this.publicAccess = publicAccess;
	}

	public String getParams() {
		return params;
	}

	@Override
	public boolean isPublishResultFiles() {
		return isPublicAccess();
	}

	@Override
	public File getOutputFile() {
		return outputFile;
	}

	public boolean isSkipIfExists() {
		return skipIfExists;
	}

	public File getInputFile() {
		return inputFile;
	}

	public boolean isPublicAccess() {
		return publicAccess;
	}

	@Override
	public String getStringDescriptionOfTask() {
		final File sourceFile = getInputFile();
		final String params = getParams();
		return "Input:" + sourceFile.getAbsolutePath() + "\nParams:" + params;
	}

	@Override
	public WorkPacket translateToCachePacket(final File cacheFolder) {
		final String canonicalOutput = WorkCache.getCanonicalOutput(getInputFile(), getOutputFile());
		return new RawToMgfWorkPacket(
				getParams(),
				new File(cacheFolder, canonicalOutput),
				isSkipIfExists(),
				getInputFile(),
				isFromScratch(),
				/*public access*/false);
	}

	@Override
	public List<String> getOutputFiles() {
		return Arrays.asList(getOutputFile().getName());
	}

	@Override
	public boolean cacheIsStale(final File subFolder, final List<String> outputFiles) {
		return getInputFile().lastModified() > new File(subFolder, outputFiles.get(0)).lastModified();
	}

	@Override
	public void reportCachedResult(final ProgressReporter reporter, final File targetFolder, final List<String> outputFiles) {
		final File cachedMgf = new File(targetFolder, outputFiles.get(0));
		reporter.reportProgress(new RawToMgfResult(cachedMgf));
	}
}
