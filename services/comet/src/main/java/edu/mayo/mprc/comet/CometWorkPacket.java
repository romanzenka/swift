package edu.mayo.mprc.comet;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.worker.CoreRequirements;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.searchengine.EngineWorkPacket;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CometWorkPacket extends EngineWorkPacket implements CoreRequirements {
	private static final long serialVersionUID = 20110729;
	private static final Pattern PEPXML_LINE = Pattern.compile("^(\\s*output_pepxmlfile\\s*=\\s*)(\\d+)", Pattern.MULTILINE);
	private static final Pattern SQT_LINE = Pattern.compile("^(\\s*output_sqtfile\\s*=\\s*)(\\d+)", Pattern.MULTILINE);

	public CometWorkPacket(final boolean fromScratch) {
		super(fromScratch);
	}

	/**
	 * Encapsulates a packet of work for Comet.
	 */
	public CometWorkPacket(final File inputFile, final String searchParams, final File outputFile, final File databaseFile, final boolean publishSearchFiles, final boolean fromScratch) {
		super(inputFile, outputFile, fixSearchParams(searchParams, outputFile), databaseFile, publishSearchFiles, fromScratch);
	}

	static String fixSearchParams(final String searchParams, final File outputFile) {
		// The search params need to be tweaked based on the extension of the output file.
		final String extension = getOutputExtension(outputFile);
		if ("pep.xml".equals(extension)) {
			final Matcher matcher = PEPXML_LINE.matcher(searchParams);
			if (!matcher.find()) {
				throw new MprcException("Malformed Comet parameter file - missing output_pepxmlfile option");
			}
			final String result = matcher.replaceFirst(matcher.group(1) + "1");
			return result;
		} else if ("sqt".equals(extension)) {
			final Matcher matcher = SQT_LINE.matcher(searchParams);
			if (!matcher.find()) {
				throw new MprcException("Malformed Comet parameter file - missing output_sqtfile option");
			}
			final String result = matcher.replaceFirst(matcher.group(1) + "1");
			return result;
		} else {
			throw new MprcException(String.format("Unsupported extension [%s] for Comet output file [%s]", extension, outputFile.getAbsolutePath()));
		}

	}

	/**
	 * We need to take care of .pep.xml extension that does not register as a file extension otherwise (only .xml gets chopped off)
	 */
	@Override
	public File canonicalOutput(final File cacheFolder) {
		final String name = FileUtilities.stripGzippedExtension(getInputFile().getName())
				+ "."
				+ getOutputExtension(getOutputFile());
		return new File(cacheFolder, name);
	}

	private static String getOutputExtension(final File outputFile) {
		return FileUtilities.getGzippedExtension(outputFile.getName(), new String[]{"pep.xml"});
	}

	@Override
	public WorkPacket translateToCachePacket(final File cacheFolder) {
		return new CometWorkPacket(
				getInputFile(),
				getSearchParams(),
				canonicalOutput(cacheFolder),
				getDatabaseFile(),
				isPublishResultFiles(),
				isFromScratch()
		);
	}

	@Override
	public int getNumRequiredCores() {
		return 10;
	}
}

