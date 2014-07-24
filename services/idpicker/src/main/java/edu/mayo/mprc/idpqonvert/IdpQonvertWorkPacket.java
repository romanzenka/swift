package edu.mayo.mprc.idpqonvert;

import edu.mayo.mprc.daemon.CachableWorkPacket;
import edu.mayo.mprc.daemon.WorkCache;
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

	/**
	 * Where to look for the input files referenced by the .pepXML file.
	 * <p/>
	 * The .pepXML is typically generated in a temporary location and then moved over to its final resting place.
	 * That means that paths embedded in that pepXML can be broken.
	 */
	private File referencedFileFolder;

	public IdpQonvertWorkPacket(final boolean fromScratch) {
		super(fromScratch);
	}

	/**
	 * Request to convert a .pepXML file into .idp file
	 *
	 * @param outputFile This is the desired target of the output. The cache can overwrite this to anything it sees fit.
	 *                   If that happens, a {@link edu.mayo.mprc.idpqonvert.IdpQonvertResult} class is sent back
	 *                   as progress report.
	 */
	public IdpQonvertWorkPacket(final File outputFile, final IdpQonvertSettings params, final File inputFile,
	                            final File fastaFile,
	                            final File referencedFileFolder,
	                            final boolean fromScratch) {
		super(fromScratch);

		this.inputFile = inputFile;
		this.params = params;
		this.outputFile = outputFile;
		this.fastaFile = fastaFile;
		this.referencedFileFolder = referencedFileFolder;
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

	public File getReferencedFileFolder() {
		return referencedFileFolder;
	}

	@Override
	public String getStringDescriptionOfTask() {
		final String paramString = getParams().toConfigFile();

		return "Inputs:\n" + getInputFile().getAbsolutePath() + "\n\n" +
				"Fasta:\n" + fastaFile.getAbsolutePath() + "\n\n" +
				"ReferencedFileFolder:\n" + referencedFileFolder.getAbsolutePath() + "\n\n" +
				"Parameters:\n" + paramString + "\n\n";
	}

	@Override
	public WorkPacket translateToCachePacket(final File cacheFolder) {
		final String canonicalOutput = WorkCache.getCanonicalOutput(getInputFile(), getOutputFile());
		return new IdpQonvertWorkPacket(
				new File(cacheFolder, canonicalOutput),
				getParams(),
				inputFile,
				fastaFile,
				referencedFileFolder,
				isFromScratch());
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
