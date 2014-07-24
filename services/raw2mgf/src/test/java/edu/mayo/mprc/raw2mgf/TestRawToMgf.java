package edu.mayo.mprc.raw2mgf;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ResourceFactory;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.integration.Installer;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.log.ParentLog;
import edu.mayo.mprc.utilities.log.SimpleParentLog;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class TestRawToMgf {
	private static final Logger LOGGER = Logger.getLogger(TestRawToMgf.class);
	private File tempRootDir;
	private File extractMsnFolder;
	private File unixXvfbWrapper;
	private File rawFolder;

	@BeforeClass()
	public void setUp() throws IOException {
		extractMsnFolder = Installer.getDirectory("SWIFT_TEST_EXTRACT_MSN_FOLDER", "extract_msn.exe and supporting files");

		tempRootDir = FileUtilities.createTempFolder();
		unixXvfbWrapper = Installer.xvfbWrapper(null, Installer.Action.INSTALL);
		rawFolder = Installer.rawFiles(null, Installer.Action.INSTALL);
	}

	@AfterClass
	public void tearDown() {
		Installer.xvfbWrapper(unixXvfbWrapper, Installer.Action.UNINSTALL);
		Installer.rawFiles(rawFolder, Installer.Action.UNINSTALL);
	}

	@Test
	public void runRaw2MgfWorker() {
		runRawToMgf(10000);
	}

	@Test
	public void runRaw2MgfWorkerSmallBatch() {
		runRawToMgf(10);
	}

	private void runRawToMgf(final int spectrumBatchSize) {
		File raw2MgfTemp = null;

		final File mgfFile = new File(tempRootDir, "result.mgf");
		try {
			raw2MgfTemp = new File(tempRootDir, "raw2mgf");

			FileUtilities.ensureFolderExists(raw2MgfTemp);

			final File rawFile = new File(rawFolder, "test.RAW");
			FileUtilities.ensureFileExists(rawFile);
			LOGGER.info("Raw file created. Raw file [" + rawFile.getAbsolutePath() + "]");

			final RawToMgfWorker.Config raw2mgfConfig = new RawToMgfWorker.Config();
			raw2mgfConfig.put(RawToMgfWorker.TEMP_FOLDER, "tempFolder");
			raw2mgfConfig.put(RawToMgfWorker.WRAPPER_SCRIPT, "wineconsole");
			raw2mgfConfig.put(RawToMgfWorker.XVFB_WRAPPER_SCRIPT, unixXvfbWrapper.getAbsolutePath());
			raw2mgfConfig.put(RawToMgfWorker.EXTRACT_MSN_EXECUTABLE, new File(extractMsnFolder, "extract_msn.exe").getAbsolutePath());
			raw2mgfConfig.put(RawToMgfWorker.TEMP_FOLDER, FileUtilities.DEFAULT_TEMP_DIRECTORY.getAbsolutePath());
			// The config comes pre-set for linux, on Windows we switch the wrappers off
			if (FileUtilities.isWindowsPlatform()) {
				raw2mgfConfig.put(RawToMgfWorker.WRAPPER_SCRIPT, null);
				raw2mgfConfig.put(RawToMgfWorker.XVFB_WRAPPER_SCRIPT, "");
			}
			final ResourceFactory<RawToMgfWorker.Config, Worker> factory = new RawToMgfWorker.Factory();
			final RawToMgfWorker simpleDaemonWorker = (RawToMgfWorker) factory.create(raw2mgfConfig, null);
			simpleDaemonWorker.setSpectrumBatchSize(spectrumBatchSize);

			final String params = "-Z -V -MP100.00 -F1 -L20000 -EA100 -S1 -I10 -G1";

			final RawToMgfWorkPacket workPacket = new RawToMgfWorkPacket(params, mgfFile, false, rawFile, false, false);
			WorkPacketBase.simulateTransfer(workPacket);

			simpleDaemonWorker.processRequest(workPacket, new ProgressReporter() {

				@Override
				public void reportStart(final String hostString) {
					LOGGER.info("Started processing on " + hostString);
				}

				@Override
				public void reportProgress(final ProgressInfo progressInfo) {
					LOGGER.info(progressInfo);
				}

				@Override
				public void reportSuccess() {
					Assert.assertTrue(mgfFile.exists(), "MGF file was not created by raw to mgf converter worker.");
					Assert.assertTrue(mgfFile.length() > 0, "MGF file was created, but it is empty.");
				}

				@Override
				public void reportFailure(final Throwable t) {
					throw new MprcException("Raw2Mgf worker failed to process work packet.", t);
				}

				@Override
				public ParentLog getParentLog() {
					return new SimpleParentLog();
				}
			});
		} catch (Exception e) {
			throw new MprcException("Raw to Mgf worker test failed.", e);
		} finally {
			FileUtilities.cleanupTempFile(raw2MgfTemp);
			if (mgfFile.exists()) {
				FileUtilities.cleanupTempFile(mgfFile);
			}
		}
	}

	@Test
	public void singleDtaTest() throws IOException {
		final File mgfFile = new File(tempRootDir, "singleDta.mgf");
		final File dtaFile = new File(tempRootDir, "test.10.20.30.dta");
		Files.write("10 20", dtaFile, Charsets.US_ASCII);
		final File[] files = new File[]{dtaFile};

		DTAToMGFConverter.convert(mgfFile, files, false);

		FileUtilities.cleanupTempFile(mgfFile);
		FileUtilities.cleanupTempFile(dtaFile);
	}

	@Test
	public void zeroDtaTest() throws MprcException, IOException {
		final File mgfFile = new File(tempRootDir, "singleDta.mgf");
		try {
			final File[] files = new File[]{};

			DTAToMGFConverter.convert(mgfFile, files, false);
		} catch (MprcException e) {
			Assert.assertTrue(
					e.getMessage().contains("MS2") &&
							e.getMessage().contains("missing") &&
							e.getMessage().contains("threshold"), "Exception message does not mention potential causes of the issue: " + e.getMessage());
		} finally {
			final boolean mgfCreated = mgfFile.exists();
			FileUtilities.quietDelete(mgfFile);
			Assert.assertFalse(mgfCreated);
		}
	}

	@Test
	public void parameterCleanupTest() {
		Assert.assertEquals(RawToMgfWorker.cleanupFromToParams("-E100 -T200 -Fabc -F1 -L999 -Lhello -f3 -l30"), "-E100 -T200 -Fabc -Lhello", "All -f# and -l# must be removed");
		Assert.assertEquals(RawToMgfWorker.cleanupFromToParams(""), "", "All -f# and -l# must be removed");
		Assert.assertEquals(RawToMgfWorker.cleanupFromToParams("  -L10  -F20"), "", "All -f# and -l# must be removed");
	}

	@Test
	public void getParameterTest() {
		Assert.assertEquals(RawToMgfWorker.getParamValue("-F", "-K10 -B20 -faq33 -F -F13 -l20 -f17").longValue(), 13L, "Ignore non-numeric entries, return first instance of a parameter");
		Assert.assertEquals(RawToMgfWorker.getParamValue("-F", "-K10 -B20 -faq33 -F -Ff13 -l20 --f17"), null, "None of the parameters are in correct format");
		Assert.assertEquals(RawToMgfWorker.getParamValue("-F", "-K10 -B20 -faq33 -F -F1.3 -l20 -f17").longValue(), 17L, "Incorrect param, uses the value specified later");
		Assert.assertEquals(RawToMgfWorker.getParamValue("-F", "-K10 -B20 -faq33 -F -F1.3 -l20 -f1.7"), null, "None of the parameters are in correct format");
	}

	@AfterClass()
	public void cleanUp() {
		FileUtilities.cleanupTempFile(tempRootDir);
	}
}
