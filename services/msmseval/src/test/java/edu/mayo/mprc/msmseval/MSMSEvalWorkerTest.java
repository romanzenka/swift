package edu.mayo.mprc.msmseval;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonWorkerTester;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public final class MSMSEvalWorkerTest {

	private static final Logger LOGGER = Logger.getLogger(MSMSEvalWorkerTest.class);

	private static final String INPUT_MGF = "/edu/mayo/mprc/msmseval/input.mgf";
	private static final String INPUT_PARAMS = "/edu/mayo/mprc/msmseval/msmsEval_orbi.params";
	private File mgfFile;
	private File tempDirectory;
	private File paramFile;

	@Test(enabled = true)
	public void createTestFiles() {

		LOGGER.info("Creating source and parameter files.");
		tempDirectory = FileUtilities.createTempFolder();

		try {
			mgfFile = TestingUtilities.getTempFileFromResource(INPUT_MGF, false, tempDirectory);
			paramFile = TestingUtilities.getTempFileFromResource(INPUT_PARAMS, false, tempDirectory);
		} catch (IOException e) {
			throw new MprcException("Failed creating files in: [" + tempDirectory.getAbsolutePath() + "]", e);
		}

		LOGGER.info("Files created:\n" +
				mgfFile.getAbsolutePath() +
				"\n" +
				paramFile.getAbsolutePath());
	}

	@Test(dependsOnMethods = {"createTestFiles"}, enabled = true)
	public void msmsEvalWorkerTest() {

		final MSMSEvalWorker msmsEvalWorker = new MSMSEvalWorker();
		final File msmsEvalExecutable = MSMSEvalTest.getMsmsEvalExecutable();
		if (!msmsEvalExecutable.canExecute()) {
			LOGGER.debug("Cannot test msmsEval, the executable is not present");
			return;
		}
		msmsEvalWorker.setMsmsEvalExecutable(msmsEvalExecutable);

		final DaemonWorkerTester daemonWorkerTester = new DaemonWorkerTester(msmsEvalWorker);
		try {
			daemonWorkerTester.start();
			final Object workerToken = daemonWorkerTester.sendWork(new MSMSEvalWorkPacket(mgfFile, paramFile, tempDirectory, "0"), null);

			while (!daemonWorkerTester.isDone(workerToken)) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					//SWALLOWED
					LOGGER.warn(e);
				}
			}

			Assert.assertFalse(msmsEvalWorker.isSkippedExecution(), "The " + MSMSEvalWorker.class.getSimpleName() + " skipped execution.");

			Assert.assertTrue(daemonWorkerTester.isSuccess(workerToken), "Method processRequest(..) from " + MSMSEvalWorker.class.getSimpleName() + " class failed.");
		} finally {
			daemonWorkerTester.stop();
		}
	}

	@Test(dependsOnMethods = {"msmsEvalWorkerTest"}, enabled = true)
	public void msmsEvalWorkerSkippedExecutionTest() {

		final MSMSEvalWorker msmsEvalWorker = new MSMSEvalWorker();
		final File msmsEvalExecutable = MSMSEvalTest.getMsmsEvalExecutable();
		if (!msmsEvalExecutable.canExecute()) {
			LOGGER.debug("Cannot test msmsEval, the executable is not present");
			return;
		}

		msmsEvalWorker.setMsmsEvalExecutable(msmsEvalExecutable);

		final DaemonWorkerTester daemonWorkerTester = new DaemonWorkerTester(msmsEvalWorker);
		try {
			daemonWorkerTester.start();

			final Object workerToken = daemonWorkerTester.sendWork(new MSMSEvalWorkPacket(mgfFile, paramFile, tempDirectory, "0", false), null);

			while (!daemonWorkerTester.isDone(workerToken)) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					//SWALLOWED
					LOGGER.warn(e);
				}
			}

			Assert.assertTrue(msmsEvalWorker.isSkippedExecution(), "The " + MSMSEvalWorker.class.getSimpleName() + " did not skip execution.");
		} finally {
			daemonWorkerTester.stop();
		}
	}

	@Test(dependsOnMethods = {"msmsEvalWorkerTest", "msmsEvalWorkerSkippedExecutionTest"}, enabled = false)
	public void cleanUpGeneratedFiles() {
		LOGGER.info("Deleting test generated files and temp directory.");
		FileUtilities.cleanupTempFile(tempDirectory);
		LOGGER.info("Test generated files and temp directory deleted.");
	}
}
