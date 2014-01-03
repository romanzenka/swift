package edu.mayo.mprc.msmseval;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.io.mgf.MgfPeakListReaderFactory;
import edu.mayo.mprc.io.mgf.MzXmlConverter;
import edu.mayo.mprc.peaklist.PeakListReaderFactory;
import edu.mayo.mprc.peaklist.PeakListReaders;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

@Test(sequential = true)
public final class MSMSEvalTest {

	private static final Logger LOGGER = Logger.getLogger(MSMSEvalTest.class);

	private static final String INPUT_MGF = "/edu/mayo/mprc/msmseval/input.mgf";
	private static final String INPUT_PARAMS = "/edu/mayo/mprc/msmseval/msmsEval_orbi.params";
	private static final String RESULT_WIN = "/edu/mayo/mprc/msmseval/output_win.mzxml_eval.mod.csv";
	// TODO: This is a huge problem - two different flavors of linux machines produce very different output! We test both, at least one has to match.
	private static final String RESULT_LINUX_1 = "/edu/mayo/mprc/msmseval/output_linux.mzxml_eval.mod.csv";
	private static final String RESULT_LINUX_2 = "/edu/mayo/mprc/msmseval/output_linux_2.mzxml_eval.mod.csv";

	private File[] outputFiles;
	private File mgfFile;
	private File mzxmlFile;
	private File tempDirectory;
	private File paramFile;
	private Map<Integer, String> scanToTitleMapping;
	private MSMSEval msmsEval;

	// TODO: make this configurable
	public static File getMsmsEvalExecutable() {
		if (FileUtilities.isWindowsPlatform()) {
			return new File("c:\\svn\\src\\java\\swift\\install\\swift\\bin\\msmseval\\win\\msmsEval.exe");
		} else {
			return new File("/mnt/raid1/software/swift/prod/bin/msmseval/linux_x86_64/msmsEval");
		}
	}

	private void setup() {
		LOGGER.info("Creating source and parameter files.");

		tempDirectory = FileUtilities.createTempFolder();
		LOGGER.debug("Temporary folder: " + tempDirectory.getAbsolutePath());

		try {
			mgfFile = TestingUtilities.getTempFileFromResource(INPUT_MGF, false, tempDirectory, ".mgf");
			paramFile = TestingUtilities.getTempFileFromResource(INPUT_PARAMS, false, tempDirectory);

			//MSMSEval output file differs a bite between the Windows OS and others.
			if (FileUtilities.isWindowsPlatform()) {
				outputFiles = new File[]{TestingUtilities.getTempFileFromResource(RESULT_WIN, false, tempDirectory)};
			} else {
				outputFiles = new File[]{
						TestingUtilities.getTempFileFromResource(RESULT_LINUX_1, false, tempDirectory),
						TestingUtilities.getTempFileFromResource(RESULT_LINUX_2, false, tempDirectory)};
			}

		} catch (IOException e) {
			throw new MprcException(e);
		}

		LOGGER.info("Files created:\n" +
				mgfFile.getAbsolutePath() +
				"\n" +
				paramFile.getAbsolutePath());
	}

	@Test(enabled = true)
	public void mgf2MzXMLConversionlTest() {
		setup();
		mzxmlFile = new File(mgfFile.getAbsolutePath() + ".mzxml");
		final MzXmlConverter converter = getMgfSupportingMzXmlConverter();
		scanToTitleMapping = converter.convert(mgfFile, mzxmlFile, true);
		Assert.assertEquals(31, scanToTitleMapping.size(), "Conversion from mgf to mzxml failed.");
	}

	private static MzXmlConverter getMgfSupportingMzXmlConverter() {
		final Collection<PeakListReaderFactory> factories = Arrays.asList((PeakListReaderFactory) new MgfPeakListReaderFactory());
		final PeakListReaders readers = new PeakListReaders(factories);
		return new MzXmlConverter(readers);
	}

	@Test(dependsOnMethods = {"mgf2MzXMLConversionlTest"}, enabled = true)
	public void msmsEvalTest() throws IOException {
		LOGGER.info("Executing msmsEval command.");

		// TODO: Add TestAppContext support
		final File msmsEvalExecutable = getMsmsEvalExecutable();
		msmsEval = new MSMSEval(mzxmlFile, paramFile, msmsEvalExecutable);
		msmsEval.execute(true);

		LOGGER.info("Command msmsEval executed.");
	}

	@Test(dependsOnMethods = {"msmsEvalTest"}, enabled = true)
	public void msmsEvalOutputFormatTest() throws IOException {
		final File formattedMsmsOutputFile = MSMSEvalOutputFileFormatter.replaceMzXMLScanIdsWithMgfNumbers(new File(msmsEval.getMsmsEvalOutputFileName())
				, new File(msmsEval.getMsmsEvalOutputFileName().substring(0, msmsEval.getMsmsEvalOutputFileName().lastIndexOf('.')) + ".mod.csv"), scanToTitleMapping);


		String difference = null;
		for (final File outputFile : outputFiles) {
			difference = TestingUtilities.compareFilesByLine(formattedMsmsOutputFile, outputFile);
			if (difference == null) {
				break;
			}
		}

		LOGGER.info("Formatted file name: " + formattedMsmsOutputFile.getAbsolutePath());

		if (difference != null) {
			LOGGER.info(difference);
		}

		Assert.assertNull(difference, "MSMSEval Output file content is not valid.");
	}

	@Test(dependsOnMethods = "msmsEvalOutputFormatTest")
	public void teardown() {
		LOGGER.info("Deleting test generated files and temp directory.");
		FileUtilities.cleanupTempFile(tempDirectory);
		LOGGER.info("Test generated files and temp directory deleted.");
	}

}
