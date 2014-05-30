package edu.mayo.mprc.msmseval;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.daemon.DaemonWorkerTester;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.io.mgf.MgfPeakListReaderFactory;
import edu.mayo.mprc.io.mgf.MzXmlConverter;
import edu.mayo.mprc.peaklist.PeakListReaderFactory;
import edu.mayo.mprc.peaklist.PeakListReaders;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MSMSEvalWorkerTest {

	private static final Logger LOGGER = Logger.getLogger(MSMSEvalWorkerTest.class);

	private static final String INPUT_MGF = "/edu/mayo/mprc/msmseval/input.mgf";
	private static final String INPUT_PARAMS = "/edu/mayo/mprc/msmseval/msmsEval_orbi.params";
	private File mgfFile;
	private File tempDirectory;
	private File outputFile;
	private File emFile;
	private File paramFile;

	@Test(enabled = true)
	public void createTestFiles() {

		LOGGER.info("Creating source and parameter files.");
		tempDirectory = FileUtilities.createTempFolder();

		try {
			mgfFile = TestingUtilities.getTempFileFromResource(INPUT_MGF, false, tempDirectory, ".mgf");
			paramFile = TestingUtilities.getTempFileFromResource(INPUT_PARAMS, false, tempDirectory);
		} catch (IOException e) {
			throw new MprcException("Failed creating files in: [" + tempDirectory.getAbsolutePath() + "]", e);
		}

		outputFile = MSMSEvalWorkPacket.getExpectedResultFileName(mgfFile, tempDirectory);
		emFile = MSMSEvalWorkPacket.getExpectedEmOutputFileName(mgfFile, tempDirectory);

		LOGGER.info("Files created:\n" +
				mgfFile.getAbsolutePath() +
				"\n" +
				paramFile.getAbsolutePath());
	}

	@Test(dependsOnMethods = {"createTestFiles"}, enabled = true)
	public void msmsEvalWorkerTest() {
		final File msmsEvalExecutable = MSMSEvalTest.getMsmsEvalExecutable();
		if (!msmsEvalExecutable.canExecute()) {
			LOGGER.debug("Cannot test msmsEval, the executable is not present");
			return;
		}

		final MSMSEvalWorker.Config config = new MSMSEvalWorker.Config(msmsEvalExecutable.getAbsolutePath(), "orbi," + paramFile.getAbsolutePath());
		final MSMSEvalWorker.Factory factory = new MSMSEvalWorker.Factory();
		final List<PeakListReaderFactory> peakListReadersList = new ArrayList<PeakListReaderFactory>();
		peakListReadersList.add(new MgfPeakListReaderFactory());
		final PeakListReaders readers = new PeakListReaders();
		readers.setReaderFactories(peakListReadersList);
		factory.setConverter(new MzXmlConverter(readers));

		final Worker worker = factory.create(config, new DependencyResolver(null));
		if (!(worker instanceof MSMSEvalWorker)) {
			ExceptionUtilities.throwCastException(worker, MSMSEvalWorker.class);
			return;
		}
		final MSMSEvalWorker msmsEvalWorker = (MSMSEvalWorker) worker;

		msmsEvalWorker.setMsmsEvalExecutable(msmsEvalExecutable);

		final DaemonWorkerTester daemonWorkerTester = new DaemonWorkerTester(msmsEvalWorker);
		try {
			daemonWorkerTester.start();
			final Object workerToken = daemonWorkerTester.sendWork(new MSMSEvalWorkPacket(mgfFile, paramFile, outputFile, emFile, "0", false), null);

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

			final Object workerToken = daemonWorkerTester.sendWork(new MSMSEvalWorkPacket(mgfFile, paramFile, outputFile, emFile, "0", false), null);

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
