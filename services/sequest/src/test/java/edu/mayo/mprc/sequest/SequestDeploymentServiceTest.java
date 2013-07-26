package edu.mayo.mprc.sequest;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonWorkerTester;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ResourceUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;

import static org.testng.Assert.*;

@Test(groups = {"sequest"}, sequential = true)
public final class SequestDeploymentServiceTest {
	private static final Logger LOGGER = Logger.getLogger(SequestDeploymentServiceTest.class);

	@BeforeClass
	public void init() {
	}

	@DataProvider(name = "fastaResources")
	public Object[][] getResources() {
		return new Object[][]{
				{"uncompressed", "/test_in.fasta"},
//				{"compressed", "/test_in.fasta.gz"},
		};
	}


	class FakeDeploymentTest {
		public File sharedTestFolder;
		public File fakeFASTAFileToDeploy;
		public DeploymentRequest request;

		private static final String sequestparams = "/edu/mayo/mprc/swift/params/Orbitrap_Sprot_Latest_CarbC_OxM/sequest.params";
		private static final String makedbparams = "/edu/mayo/mprc/swift/params/Orbitrap_Sprot_Latest_CarbC_OxM/makedb.params";

		private void init(final String resourcePath) {
			try {
				sharedTestFolder = null;//TestApplicationContext.getSharedTestFolder();
				fakeFASTAFileToDeploy = TestingUtilities.getTempFileFromResource(this.getClass(), resourcePath, sharedTestFolder);
				final File fakeParamsFile = writeToFile(sequestparams, sharedTestFolder, "tmp_sequest.params");

				final String shortName = FileUtilities.stripExtension(fakeFASTAFileToDeploy.getName());

				final Curation toDeploy = new Curation();
				toDeploy.setShortName(shortName);
				toDeploy.setCurationFile(fakeFASTAFileToDeploy);

				request = new DeploymentRequest(this.getClass().getSimpleName(), toDeploy.getFastaFile());
				request.addProperty(SequestDeploymentService.SEQUEST_PARAMS_FILE, fakeParamsFile);
			} catch (IOException e) {
				throw new MprcException(e);
			}
		}

		public File writeToFile(final String resourceOnClasspath, final File directoryToWriteTo, final String name) {
			final InputStream is = this.getClass().getResourceAsStream(resourceOnClasspath);
			final File file = new File(directoryToWriteTo, name);
			try {
				FileUtilities.writeStreamToFile(is, file);
				file.deleteOnExit();
			} catch (IOException e) {
				throw new MprcException(e);
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch (IOException e) {
					//SWALLOWED: unnecessary...
				}
			}
			return file;
		}

		public void writeMakedbParams(final String testName) {
			writeToFile(makedbparams, fakeFASTAFileToDeploy.getParentFile(), FileUtilities.stripExtension(testName) + ".makedb.params");
		}

		public FakeDeploymentTest(final String resourcePath) {
			init(resourcePath);
		}
	}

	@Test(enabled = false, groups = {"linux"}, dataProvider = "fastaResources")
	public void performDeployment_Test(final String testName, final String resourcePath) throws IOException {

		LOGGER.info("Running SequesetDST for " + testName + "file.");

		final SequestDeploymentService service = null; //getConfiguredSequestDeploymentService();

		final FakeDeploymentTest fdt = new FakeDeploymentTest(resourcePath);

		final File fakeFASTAFileToDeploy = fdt.fakeFASTAFileToDeploy;

		final DeploymentRequest request = fdt.request;

		final SequestDeploymentResult[] results = new SequestDeploymentResult[1];

		final DaemonWorkerTester tester = new DaemonWorkerTester(service);
		try {
			final Object workToken = tester.sendWork(request, new ProgressListener() {

				public void requestEnqueued(final String hostString) {
					LOGGER.debug("SequestDS request enqueued at " + hostString);
				}

				public void requestProcessingStarted(final String hostString) {
					LOGGER.debug("SequestDS processing started at " + hostString);
				}

				public void requestProcessingFinished() {
					LOGGER.debug("SequestDS processing finished");
				}

				public void requestTerminated(final Exception e) {
					fail("Request terminated", e);
				}

				public void userProgressInformation(final ProgressInfo progressInfo) {
					if (progressInfo instanceof SequestDeploymentResult) {
						results[0] = (SequestDeploymentResult) progressInfo;
					} else {
						LOGGER.info(progressInfo.toString());
					}
				}
			});

			while (!tester.isDone(workToken)) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					fail("Interupted thread", e);
				}
			}

			final SequestDeploymentResult result = results[0];

			assertNotNull(result, "No result was returned to the deployment failed.");

			for (final String message : result.getMessages()) {
				LOGGER.info(message);
			}

			if (result.getCompositeException() != null) {
				LOGGER.error(result.getCompositeException());
			}

			File dgtFile = null;
			for (final File sharedFile : result.getGeneratedFiles()) {
				if (sharedFile.getName().contains(".dgt")) {
					dgtFile = sharedFile;
				}
			}

			//perform assertions but we need to make sure that generated files are deleted.
			try {
				final File hdrFile = result.getFileToSearchAgainst();
				assertNull(result.getCompositeException(), "There was at least one exception in performing sequest deployment");
				assertNotNull(result.getDeployedFile(), "No deployed file was returned.");
				assertNotNull(hdrFile);
				assertTrue(hdrFile.length() > 0);
				assertNotNull(dgtFile);
				assertTrue(dgtFile.length() > fakeFASTAFileToDeploy.length());
			} finally {
				for (final File sharedFile : result.getGeneratedFiles()) {
					LOGGER.debug("deleting: " + sharedFile.getAbsolutePath());
					FileUtilities.cleanupTempFile(sharedFile);
				}
			}
		} finally {
			tester.close();
		}
	}

	private DeploymentRequest getFakeDeploymentRequest(final File sharedTestFolder, final File fakeFASTAFileToDeploy) throws IOException {
		final InputStream is = this.getClass().getResourceAsStream("/edu/mayo/mprc/swift/params/Orbitrap_Sprot_Latest_CarbC_OxM/sequest.params");
		final File fakeParamsFile = new File(sharedTestFolder, "tmp_sequest.params");
		try {
			FileUtilities.writeStreamToFile(is, fakeParamsFile);
			fakeParamsFile.deleteOnExit();
		} finally {
			if (is != null) {
				is.close();
			}
		}

		final String shortName = FileUtilities.stripExtension(fakeFASTAFileToDeploy.getName());

		final Curation toDeploy = new Curation();
		toDeploy.setShortName(shortName);
		toDeploy.setCurationFile(fakeFASTAFileToDeploy);

		final DeploymentRequest request = new DeploymentRequest(this.getClass().getSimpleName(), toDeploy.getFastaFile());

		request.addProperty(SequestDeploymentService.SEQUEST_PARAMS_FILE, fakeParamsFile);
		return request;
	}


	private static final String INPUT_SEQUEST_PARAMS = "classpath:edu/mayo/mprc/sequest/sequest.params";
	private static final String FASTA_NAME = "foo.fasta";
	private static final String EXPECTED_MAKEDB_PARAMS = "classpath:edu/mayo/mprc/sequest/makedb.params.nocomments";
	//TODO make SequestToMakeDBConverter preserve comments.

	@Test(enabled = true)
	public void testConvertSequestToMakedb() throws Throwable {
		final SequestMappings pic = new SequestMappings();
		pic.read(ResourceUtilities.getReader(INPUT_SEQUEST_PARAMS, this.getClass()));
		final SequestToMakeDBConverter converter = new SequestToMakeDBConverter();
		final StringBuilder outpic = converter.convertSequestToMakeDB(pic, new File(FASTA_NAME));
		String expected = FileUtilities.readIntoString(ResourceUtilities.getReader(EXPECTED_MAKEDB_PARAMS, pic), 1024 * 1024);
		expected = expected.replaceAll("database_name = /Users/cmason/mprc/maven/core/foo.fasta", Matcher.quoteReplacement("database_name = " + new File("foo.fasta").getAbsolutePath()));
		final String obtained = outpic.toString();
		Assert.assertEquals(obtained, expected, "Generated makedb parameters do not match the expected ones");
	}

	@Test
	public void testSpecifiesNoEnzyme() throws IOException {
		final File nsParamsFile = TestingUtilities.getTempFileFromResource(this.getClass(), "/edu/mayo/mprc/sequest/nonspecific_sequest.params", true, null);

		final SequestDeploymentService service = new SequestDeploymentService();
		service.setConverter(new SequestToMakeDBConverter());

		final boolean result = service.specifiesNoEnzyme(nsParamsFile);

		Assert.assertTrue(result, "Did not properly detect that a params file was non-specific enzyme.");

	}


}
