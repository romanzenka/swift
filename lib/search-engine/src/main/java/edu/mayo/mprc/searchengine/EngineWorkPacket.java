package edu.mayo.mprc.searchengine;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.CachableWorkPacket;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * A task for a search engine, containing as the minimum the following:
 * <ul>
 * <li>an input file to be processed</li>
 * <li>an output file to be generated that contains results of the processing</li>
 * <li>A parameter file. This contains all the search engine settings, the idea is that if you run the same engine on the
 * same input file with the same parameter file, you should get the same output.</li>
 * <li>a link to the FASTA database file to do the processing with (can be omitted for engines like Mascot)</li>
 * <li>a flag whether to publish the resulting files or keep them in a cache. This is ignored by the search engine itself,
 * the caches act upon this information.</li>
 * </ul>
 * <p/>
 * The concept of the parameter file can quite confusing. We are basically splitting the search engine implementation into two parts:
 * <ul>
 * <li>the first part creates the parameter file using one of the Mappings classes.</li>
 * <li>the search engine gets this pre-chewed information in a form of a parameter file and just does the search</li>
 * </ul>
 * Why is this done this way? Much simpler solution would be to actually send the input parameters directly to the search engine.
 * <p/>
 * The reasons are following:
 * <ul>
 * <li>Parameter files are very important to the scientist. They document exactly what was the search engine told to do.
 * Because of that, the files are available even before the search starts, so they can be reviewed.</li>
 * <li>Having the parameters serialized before search is started enables a generic caching mechanism to check whether
 * the work was already done before or not. The cache needs to store the parameters in a serialized form anyway.</li>
 * </ul>
 */
public abstract class EngineWorkPacket extends WorkPacketBase implements CachableWorkPacket {
	private static final long serialVersionUID = 20090402L;

	private File outputFile; //will use the parent File as the work folder
	private File searchParamsFile;
	private File databaseFile;
	private File inputFile;
	private boolean publishResultFiles;

	public EngineWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	public EngineWorkPacket(final File inputFile, final File outputFile, final File searchParamsFile, final File databaseFile, final boolean publishResultFiles, final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);

		assert outputFile != null : "output file was null.";
		assert inputFile != null : "input file was null";

		this.outputFile = outputFile;
		this.searchParamsFile = searchParamsFile;
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

	@Override
	public String getStringDescriptionOfTask() {
		final StringBuilder description = new StringBuilder();
		String paramString = "";
		try {
			paramString = Files.toString(getSearchParamsFile(), Charsets.UTF_8);
		} catch (IOException e) {
			throw new MprcException("Could not read parameter file: " + getSearchParamsFile().getAbsolutePath(), e);
		}
		description
				.append("Input:")
				.append(getInputFile().getAbsolutePath())
				.append('\n');

		if (getDatabaseFile() != null) {
			description.append("Database:")
					.append(getDatabaseFile().getAbsolutePath())
					.append('\n');
		}

		description.append("ParamFile:\n---\n")
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

	public File getSearchParamsFile() {
		return searchParamsFile;
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
		if (searchParamsFile != null ? !searchParamsFile.equals(that.searchParamsFile) : that.searchParamsFile != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = outputFile != null ? outputFile.hashCode() : 0;
		result = 31 * result + (searchParamsFile != null ? searchParamsFile.hashCode() : 0);
		result = 31 * result + (databaseFile != null ? databaseFile.hashCode() : 0);
		result = 31 * result + (inputFile != null ? inputFile.hashCode() : 0);
		result = 31 * result + (publishResultFiles ? 1 : 0);
		return result;
	}

	public String toString() {
		return "\n\tinput file: " + getInputFile()
				+ "\n\tfasta file: " + getDatabaseFile()
				+ "\n\toutput file: " + getOutputFile()
				+ "\n\tsearch params: " + getSearchParamsFile();
	}
}
