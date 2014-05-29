package edu.mayo.mprc.mascot;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.searchengine.EngineWorkPacket;
import edu.mayo.mprc.searchengine.SearchEngineResult;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Defines a Mascot search.
 */
public final class MascotWorkPacket extends EngineWorkPacket {
	private static final Logger LOGGER = Logger.getLogger(MascotWorkPacket.class);

	private static final long serialVersionUID = 20090402L;
	private String shortDbName;

	// USERNAME, USEREMAIL and LICENSE fields do not influence the result
	private static final Pattern DELETE_HEADERS = Pattern.compile("(?<=\n)(LICENSE|USERNAME|USEREMAIL)=[^\n]*\n");

	public static final String MASCOT_URL_FILENAME = "mascot_url.txt";

	public MascotWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	public MascotWorkPacket(final File outputFile, final String searchParams, final File inputFile, final String shortDbName, final String taskId, final boolean fromScratch, final boolean publishSearchFiles) {
		super(inputFile, outputFile, searchParams, null, publishSearchFiles, taskId, fromScratch);

		assert inputFile != null : "Mascot request cannot be created: The input file was null";
		assert shortDbName != null : "Mascot request cannot be created: Short database name was null";
		assert outputFile != null : "Mascot request cannot be created: The output file was null";
		assert searchParams != null : "Mascot request cannot be created: The search params have to be set";

		this.shortDbName = shortDbName;
	}

	public String getShortDbName() {
		return shortDbName;
	}

	@Override
	public String getStringDescriptionOfTask() {
		final StringBuilder description = new StringBuilder(100);
		String paramString = getSearchParams();
		paramString = DELETE_HEADERS.matcher(paramString).replaceAll("");
		description
				.append("Input:")
				.append(getInputFile().getAbsolutePath())
				.append('\n')
				.append("Database:")
				.append(getShortDbName())
				.append('\n')
				.append("ParamFile:\n---\n")
				.append(paramString)
				.append("---\n");
		return description.toString();
	}

	@Override
	public WorkPacket translateToWorkInProgressPacket(final File wipFolder) {
		return new MascotWorkPacket(
				new File(wipFolder, getOutputFile().getName()),
				getSearchParams(),
				getInputFile(),
				getShortDbName(),
				getTaskId(),
				isFromScratch(),
				false);
	}

	@Override
	public List<String> getOutputFiles() {
		return Arrays.asList(getOutputFile().getName(), MASCOT_URL_FILENAME);
	}

	@Override
	public void reportCachedResult(final ProgressReporter reporter, final File targetFolder, final List<String> outputFiles) {
		final File mascotUrlFile = new File(targetFolder, outputFiles.get(1));
		if (mascotUrlFile.exists()) {
			try {
				final String mascotUrl = Files.toString(mascotUrlFile, Charsets.UTF_8);
				reporter.reportProgress(new MascotResultUrl(mascotUrl));
			} catch (IOException ignore) {
				// SWALLOWED: not a big deal if we cannot report the mascot url
				LOGGER.warn("Cache could not find mascot URL information: " + mascotUrlFile.getAbsolutePath());
			}
		}

		reporter.reportProgress(new SearchEngineResult(new File(targetFolder, outputFiles.get(0))));
	}


}
