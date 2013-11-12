package edu.mayo.mprc.mascot;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.enginedeployment.DeploymentResult;
import edu.mayo.mprc.integration.Installer;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;


@Test(groups = {"linux"}, sequential = true)
public final class MascotDeploymentServiceTest {
	private static final Logger LOGGER = Logger.getLogger(MascotDeploymentServiceTest.class);

	private File tempFolder;
	private File testFastaFile;

	private synchronized void init() {
		tempFolder = FileUtilities.createTempFolder();
		try {
			testFastaFile = TestingUtilities.getTempFileFromResource(
					"/edu/mayo/mprc/mascot/testfasta/Test_001.fasta", /*autodelete*/true, tempFolder, ".fasta");
		} catch (IOException e) {
			throw new MprcException("Cannot create temporary fasta file for testing", e);
		}
	}

	@Test
	public void testMascotDeploymentSuccess() {
		init();

		final MockMonitor monitor = new MockMonitor(/*toSucceed*/ true);
		final File fastaFolder = Installer.testFastaFiles(null, Installer.Action.INSTALL);

		try {

			final File mockMascotDatFile = monitor.getMockMascotDatFile();
			final File mockMonitorLogFile = monitor.getMockMonitorLogFile();

			final MascotDeploymentService service = MascotDeploymentService.createForTesting(mockMascotDatFile, mockMonitorLogFile);

			new Thread(monitor).start();

			final File fakeFASTAFileToDeploy = new File(fastaFolder, "test_in.fasta");

			final Curation toDeploy = new Curation();
			toDeploy.setShortName("test_in");
			toDeploy.setCurationFile(fakeFASTAFileToDeploy);
			final DeploymentRequest request = new DeploymentRequest(getClass().getSimpleName(), toDeploy.getFastaFile());
			WorkPacketBase.simulateTransfer(request);

			final DeploymentResult result = service.performDeployment(request);

			Assert.assertNotNull(result);

		} catch (Exception e) {
			Assert.fail("Failed testMascotDeploymentSuccess: ", e);
		} finally {
			monitor.stop();
			Installer.testFastaFiles(fastaFolder, Installer.Action.UNINSTALL);
		}
	}

	@Test
	public void testAppendToMascotDat() throws Throwable {
		init();
		try {
			final MockMonitor monitor = new MockMonitor(/*toSucceed*/ true);
			final MascotDeploymentService service = MascotDeploymentService.createForTesting(monitor.getMockMascotDatFile(), monitor.getMockMonitorLogFile());

			final String expected = testFastaFile.getAbsolutePath();
			service.updateMascotDatFile("test_003", testFastaFile.getAbsoluteFile());
			final String actual = service.getPreviousDeploymentPath("test_003");
			Assert.assertEquals(actual, expected);

		} catch (Exception t) {
			LOGGER.error(t);
			throw t;
		}
	}

	@Test(expectedExceptions = MprcException.class)
	public void testDuplicateEntries() throws Throwable {
		init();
		final MockMonitor monitor = new MockMonitor(/*toSucceed*/ true);
		final MascotDeploymentService service = MascotDeploymentService.createForTesting(monitor.getMockMascotDatFile(), monitor.getMockMonitorLogFile());
		service.getPreviousDeploymentPath("test_002");
	}

	/**
	 * this class will need to pretend to be a
	 */
	private class MockMonitor implements Runnable {

		private final boolean toSucceed;

		private boolean keepRunning = true;

		private File mockLog = null;
		private File mockDat = null;

		MockMonitor() {
			toSucceed = true;
		}

		MockMonitor(final boolean toSucceed) {
			this.toSucceed = toSucceed;
		}


		/**
		 * gets a file that will appear to be a mascot.dat file that the deployer can modify and once it is modified
		 * by placing the line in the proper sections this MockMonitor will.
		 */
		public File getMockMascotDatFile() throws IOException {
			if (mockDat == null) {
				mockDat = TestingUtilities.getTempFileFromResource(getClass(), "/edu/mayo/mprc/mascot/mascot.dat", true, tempFolder);
			}
			return mockDat;
		}

		/**
		 * Gets a file that looks like the monitor.log file and will be appended by the MockMonitor in a fashion similar
		 * to ohow a real monitor.log file is appended to.
		 */
		public File getMockMonitorLogFile() throws IOException {
			if (mockLog == null) {
				mockLog = TestingUtilities.getTempFileFromResource(getClass(), "/edu/mayo/mprc/mascot/monitor.log", /*autoDelete*/true, tempFolder);
			}
			return mockLog;
		}

		/**
		 * this will run  the mock monitor and it will start listening to changes on the file returned by {@link #getMockMascotDatFile()}
		 * and when an entry is made it will start writing things out to the file given by {@link #getMockMonitorLogFile()}
		 * the commands that the MascotDeploymentService should be expecting.  If this object was created with the
		 * toSucceed=true then it will give a success message else a failure message.  When the success or failure line is
		 * put out to the file then the thread will terminate.
		 */
		@Override
		public void run() {

			keepRunning = true;
			RandomAccessFile writer = null;
			try {
				final long initDatSize = getMockMascotDatFile().length();

				while (keepRunning) {
					if (getMockMascotDatFile().length() > initDatSize) {
						break;
					}
					Thread.sleep(10);
				}


				writer = new RandomAccessFile(getMockMonitorLogFile(), "rw");

				String toWrite;
				if (toSucceed) {
					toWrite = "Thu Aug 23 13:00:41 2007 - ${shortname} Compressed files -&gt; cluster to Finished compressing files \n" +
							"Thu Aug 23 13:00:42 2007 - ${shortname} Finished compressing files  to Running 1st test           \n" +
							"Thu Aug 23 13:00:48 2007 - ${shortname} Running 1st test            to First test just run OK     \n" +
							"Thu Aug 23 13:00:49 2007 - ${shortname} First test just run OK      to Waiting for other DB to end\n" +
							"Thu Aug 23 13:00:50 2007 - ${shortname} Waiting for other DB to end to Trying to memory map files \n" +
							"Thu Aug 23 13:00:51 2007 - ${shortname} Trying to memory map files  to Just enabled memory mapping\n" +
							"Thu Aug 23 13:00:52 2007 - ${shortname} Just enabled memory mapping to In use  \n";
				} else {
					toWrite = "";
				}

				writer.seek(writer.length());

				toWrite = toWrite.replaceAll("\\$\\{shortname\\}", "test_in");

				writer.writeBytes(toWrite);

			} catch (Exception e) {
				LOGGER.error(e);
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						LOGGER.debug(e); //SWALLOWED: in purpose
					}
				}
			}
		}


		/**
		 * call this to issue a stop command to this thread
		 */
		public void stop() {
			keepRunning = false;
		}

	}

}
