package edu.mayo.mprc.searchengine;

import edu.mayo.mprc.daemon.CachableWorkPacket;
import edu.mayo.mprc.daemon.WorkCache;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * A task for a search engine, containing as the minimum the following:
 * <ul>
 * <li>an input file to be processed</li>
 * <li>an output file to be generated that contains results of the processing</li>
 * <li>A parameter string. This contains all the search engine settings, the idea is that if you run the same engine on the
 * same input file with the same parameters, you should get the same output. The string will be typically written to a
 * file prior to invoking the search engine, it can be modified (e.g. for tandem we use the locally available cores).
 * However, for purposes of checking if two engines did the same work, only the original string representation is used.</li>
 * <li>a link to the FASTA database file to do the processing with (can be omitted for engines like Mascot)</li>
 * <li>a flag whether to publish the resulting files or keep them in a cache. This is ignored by the search engine itself,
 * the caches act upon this information.</li>
 * </ul>
 * <p/>
 * The concept of the parameter string can quite confusing. We are basically splitting the search engine implementation into two parts:
 * <ul>
 * <li>the first part creates the parameter string using one of the Mappings classes.</li>
 * <li>the search engine gets this pre-chewed information in a form of a parameter string and just does the search</li>
 * </ul>
 * <p/>
 * The parameter string helps us implement some functionality, such as caching (where parameters need to be compared).
 */
public abstract class EngineWorkPacket extends WorkPacketBase implements CachableWorkPacket {
	private static final long serialVersionUID = 20090402L;

	private File outputFile; //will use the parent File as the work folder
	private String searchParams;
	private File databaseFile;
	private File inputFile;
	private boolean publishResultFiles;

	public EngineWorkPacket(final boolean fromScratch) {
		super(fromScratch);
	}

	public EngineWorkPacket(final File inputFile, final File outputFile, final String searchParams, final File databaseFile, final boolean publishResultFiles, final boolean fromScratch) {
		super(fromScratch);

		assert outputFile != null : "output file was null.";
		assert inputFile != null : "input file was null";

		this.outputFile = outputFile;
		this.searchParams = searchParams;
		this.inputFile = inputFile;
		this.databaseFile = databaseFile;
		this.publishResultFiles = publishResultFiles;
	}

	public File getDatabaseFile() {
		return databaseFile;
	}

	public File getInputFile() {
		return inputFile;
	}

	@Override
	public File getOutputFile() {
		return outputFile;
	}

	public String getSearchParams() {
		return searchParams;
	}

	@Override
	public String getStringDescriptionOfTask() {
		final StringBuilder description = new StringBuilder();
		String paramString = searchParams;
		description
				.append("Input:")
				.append(getInputFile().getAbsolutePath())
				.append('\n');

		if (getDatabaseFile() != null) {
			description.append("Database:")
					.append(getDatabaseFile().getAbsolutePath())
					.append('\n');
		}

		description.append("Params:\n---\n")
				.append(paramString)
				.append("---\n");
		return description.toString();
	}

	@Override
	public List<String> getOutputFiles() {
		return Arrays.asList(getOutputFile().getName());
	}

	@Override
	public boolean cacheIsStale(final File subFolder, final List<String> outputFiles) {
		final long outputFileModified = new File(subFolder, outputFiles.get(0)).lastModified();
		return getInputFile().lastModified() > outputFileModified
				||
				getDatabaseFile() != null && getDatabaseFile().lastModified() > outputFileModified;
	}

	@Override
	public void reportCachedResult(final ProgressReporter reporter, final File targetFolder, final List<String> outputFiles) {
		reporter.reportProgress(new SearchEngineResult(new File(targetFolder, outputFiles.get(0))));
	}

	public File canonicalOutput(final File cacheFolder) {
		return new File(cacheFolder, WorkCache.getCanonicalOutput(getInputFile(), getOutputFile()));
	}

	@Override
	public boolean isPublishResultFiles() {
		return publishResultFiles;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof EngineWorkPacket)) {
			return false;
		}

		final EngineWorkPacket that = (EngineWorkPacket) o;

		if (publishResultFiles != that.publishResultFiles) {
			return false;
		}
		if (databaseFile != null ? !databaseFile.equals(that.databaseFile) : that.databaseFile != null) {
			return false;
		}
		if (inputFile != null ? !inputFile.equals(that.inputFile) : that.inputFile != null) {
			return false;
		}
		if (outputFile != null ? !outputFile.equals(that.outputFile) : that.outputFile != null) {
			return false;
		}
		if (searchParams != null ? !searchParams.equals(that.searchParams) : that.searchParams != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = outputFile != null ? outputFile.hashCode() : 0;
		result = 31 * result + (searchParams != null ? searchParams.hashCode() : 0);
		result = 31 * result + (databaseFile != null ? databaseFile.hashCode() : 0);
		result = 31 * result + (inputFile != null ? inputFile.hashCode() : 0);
		result = 31 * result + (publishResultFiles ? 1 : 0);
		return result;
	}

	public String toString() {
		return "\n\tinput file: " + getInputFile()
				+ "\n\tfasta file: " + getDatabaseFile()
				+ "\n\toutput file: " + getOutputFile();
	}
}
