package edu.mayo.mprc.filesharing.jms;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.filesharing.FileTransfer;
import edu.mayo.mprc.filesharing.FileTransferHandler;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.apache.activemq.broker.BrokerService;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public final class JmsFileSharingTest {

	private URI uri;
	private BrokerService broker;
	private static String brokerUri = "tcp://localhost:0";
	private final String sourceFileResourcePath1 = "/edu/mayo/mprc/filesharing/jms/1234.test";
	private final String sourceFileResourcePath2 = "/edu/mayo/mprc/filesharing/jms/5678.test";
	private final String sourceFileResourcePath3 = "/edu/mayo/mprc/filesharing/jms/91011.test";
	private final String sourceFileResourcePath4 = "/edu/mayo/mprc/filesharing/jms/1213.test";
	private File tempFolder;
	private FileTransferHandler fileSharing;
	private FileTransferHandler fileSharingClient;
	private FileTransferHandler fileSharingServer;

	private static final Logger LOGGER = Logger.getLogger(JmsFileSharingTest.class);

	@BeforeClass
	public void setUp() {
		LOGGER.debug("Starting set up");

		try {
			uri = new URI(brokerUri);
			broker = new BrokerService();
			broker.setPersistent(false);
			broker.setUseJmx(false);
			broker.addConnector(uri);
			broker.start();
			uri = new URI("failover:(" + this.broker.getTransportConnectors().get(0).getUri().toString() + ")");
		} catch (Exception e) {
			throw new MprcException("Could not start broker for uri " + uri.toString(), e);
		}

		tempFolder = FileUtilities.createTempFolder();

		final JmsFileTransferHandlerFactory factory = new JmsFileTransferHandlerFactory(uri, null, null);
		fileSharing = factory.createFileSharing("test");
		fileSharing.startProcessingRequests();

		fileSharingClient = factory.createFileSharing("client");
		fileSharingClient.startProcessingRequests();

		fileSharingServer = factory.createFileSharing("server");
		fileSharingServer.startProcessingRequests();

		LOGGER.debug("Ending set up");
	}

	private File sourceTempFile11;
	private File destinationFile11;

	@Test
	public void transferFileSuccessfullyTest() throws IOException {
		LOGGER.debug("Starting test");

		sourceTempFile11 = TestingUtilities.getTempFileFromResource(sourceFileResourcePath1, false, tempFolder);
		destinationFile11 = new File(tempFolder, "destination1.test");

		final FileTransfer fileTransfer = fileSharing.getFile("test", sourceTempFile11.getAbsolutePath(), destinationFile11);

		final File resultFile = fileTransfer.done().get(0);

		Assert.assertEquals(fileTransfer.getFiles().size(), fileTransfer.getTransferedFiles().size(), "Number of transfered files should be the same as requested.");
		Assert.assertTrue(resultFile.exists(), "Resulting transfered file " + resultFile.getAbsolutePath() + " should exist.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile11, resultFile) == null, "Source and destination file contents are not the same.");

		LOGGER.debug("Ending test");
	}

	@Test(dependsOnMethods = {"transferFileSuccessfullyTest"})
	public void transferFileRetrialSuccessfullyTest() {
		LOGGER.debug("Starting test");

		final FileTransfer fileTransfer = fileSharing.getFile("test", sourceTempFile11.getAbsolutePath(), destinationFile11);

		final File resultFile = fileTransfer.done().get(0);

		Assert.assertEquals(fileTransfer.getTransferedFiles().size(), 0, "No files should have been transfered.");
		Assert.assertTrue(resultFile.exists(), "Resulting transfered file " + resultFile.getAbsolutePath() + " should exist.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile11, resultFile) == null, "Source and destination file contents are not the same.");

		LOGGER.debug("Ending test");
	}


	@Test(dependsOnMethods = {"transferFileRetrialSuccessfullyTest"})
	public void transferFileDeletionSuccessfullyTest() {
		LOGGER.debug("Starting test");

		final String sourcePath = sourceTempFile11.getAbsolutePath();
		FileUtilities.cleanupTempFile(sourceTempFile11);
		final FileTransfer fileTransfer = fileSharing.getFile("test", sourcePath, destinationFile11);

		final File resultFile = fileTransfer.done().get(0);

		Assert.assertEquals(fileTransfer.getTransferedFiles().size(), 0, "No files should have been transfered.");
		Assert.assertFalse(destinationFile11.exists(), "Resulting transfered file " + resultFile.getAbsolutePath() + " should have been deleted.");

		LOGGER.debug("Ending test");
	}

	@Test
	public void transferFolderSuccessfullyTest() {
		LOGGER.debug("Starting test");

		final File destinationFolder = new File(tempFolder, "destination");
		FileUtilities.ensureFolderExists(destinationFolder);

		final FileTransfer fileTransfer = fileSharing.getFile("test", tempFolder.getAbsolutePath(), destinationFolder);

		final File resultFile = fileTransfer.done().get(0);

		Assert.assertTrue(resultFile.exists(), "Resulting transfered folder " + resultFile.getAbsolutePath() + " should exist.");

		LOGGER.debug("Ending test");
	}

	@Test
	public void transferFileFailedTest() {
		LOGGER.debug("Starting test");

		final File destinationFile = new File(tempFolder, "destination2.test");

		final FileTransfer fileTransfer = fileSharing.getFile("test", new File("./" + System.currentTimeMillis() + "/abc123.def").getAbsolutePath(), destinationFile);

		final File resultFile = fileTransfer.done().get(0);

		Assert.assertFalse(resultFile.exists(), "Resulting transfered file should not exist.");

		LOGGER.debug("Ending test");
	}

	@Test
	public void synchronizedRemoteFileTest() throws IOException {
		LOGGER.debug("Starting test");

		final File sourceTempFile = TestingUtilities.getTempFileFromResource(sourceFileResourcePath1, false, tempFolder);
		final File destinationFile = new File(tempFolder, "synchronized.test");

		final FileTransfer fileTransfer = fileSharingClient.uploadFile("server", sourceTempFile, destinationFile.getAbsolutePath());

		final File resultFile = fileTransfer.done().get(0);

		Assert.assertTrue(resultFile.exists(), "Resulting synchronized file " + resultFile.getAbsolutePath() + " should exist.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile, resultFile) == null, "Source and destination file contents are not the same.");

		LOGGER.debug("Ending test");
	}

	private File sourceTempFile1;
	private File sourceTempFile2;
	private File sourceTempFile3;
	private File sourceTempFile4;
	private File destinationFile1;
	private File destinationFile2;
	private File destinationFile3;
	private File destinationFile4;

	@Test
	public void synchronizedRemoteFolderTest() throws IOException {
		LOGGER.debug("Starting test");

		final File tempSourceSynchFolder = new File(tempFolder, "synchSource");
		FileUtilities.ensureFolderExists(tempSourceSynchFolder);

		final File tempDestSynchFolder = new File(tempFolder, "synchDest");

		sourceTempFile1 = TestingUtilities.getTempFileFromResource(sourceFileResourcePath1, false, tempSourceSynchFolder);
		sourceTempFile2 = TestingUtilities.getTempFileFromResource(sourceFileResourcePath2, false, tempSourceSynchFolder);
		final File dir1 = new File(tempSourceSynchFolder, "dir1");
		FileUtilities.ensureFolderExists(dir1);
		final File dir2 = new File(tempSourceSynchFolder, "dir2");
		FileUtilities.ensureFolderExists(dir2);
		sourceTempFile3 = TestingUtilities.getTempFileFromResource(sourceFileResourcePath3, false, dir1);
		sourceTempFile4 = TestingUtilities.getTempFileFromResource(sourceFileResourcePath4, false, dir2);

		destinationFile1 = new File(tempDestSynchFolder, sourceTempFile1.getName());
		destinationFile2 = new File(tempDestSynchFolder, sourceTempFile2.getName());
		destinationFile3 = new File(new File(tempDestSynchFolder, "dir1"), sourceTempFile3.getName());
		destinationFile4 = new File(new File(tempDestSynchFolder, "dir2"), sourceTempFile4.getName());

		final FileTransfer fileTransfer = fileSharingClient.uploadFolder("server", tempSourceSynchFolder, tempDestSynchFolder.getAbsolutePath());

		final List<File> resultFiles = fileTransfer.done();

		Assert.assertTrue(resultFiles.size() > 0, "Resulting synchronized folder " + tempSourceSynchFolder.getAbsolutePath() + " should exist.");

		Assert.assertEquals(fileTransfer.getFiles().size(), fileTransfer.getTransferedFiles().size(), "Number of transfered files should be the same as requested.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile1, destinationFile1) == null, "Source and destination file contents are not the same.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile2, destinationFile2) == null, "Source and destination file contents are not the same.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile3, destinationFile3) == null, "Source and destination file contents are not the same.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile4, destinationFile4) == null, "Source and destination file contents are not the same.");

		LOGGER.debug("Ending test");
	}

	@Test(dependsOnMethods = {"synchronizedRemoteFolderTest"})
	public void synchronizedRemoteFolderRetrialTest() {
		LOGGER.debug("Starting test");

		final File tempSourceSynchFolder = new File(tempFolder, "synchSource");
		FileUtilities.ensureFolderExists(tempSourceSynchFolder);

		final File tempDestSynchFolder = new File(tempFolder, "synchDest");

		final FileTransfer fileTransfer = fileSharingClient.uploadFolder("server", tempSourceSynchFolder, tempDestSynchFolder.getAbsolutePath());

		final List<File> resultFiles = fileTransfer.done();

		Assert.assertTrue(resultFiles.size() > 0, "Resulting synchronized folder " + tempSourceSynchFolder.getAbsolutePath() + " should exist.");

		Assert.assertEquals(fileTransfer.getTransferedFiles().size(), 0, "Number of transfered files should be the same as requested.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile1, destinationFile1) == null, "Source and destination file contents are not the same.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile2, destinationFile2) == null, "Source and destination file contents are not the same.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile3, destinationFile3) == null, "Source and destination file contents are not the same.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile4, destinationFile4) == null, "Source and destination file contents are not the same.");

		Assert.assertNull(fileTransfer.getErrorException(), "No exception should have been thrown if folder synchronization is successful.");

		LOGGER.debug("Ending test");
	}

	@Test(dependsOnMethods = {"synchronizedRemoteFolderRetrialTest"})
	public void synchronizedRemoteFolderRetrialChangeTest() {
		LOGGER.debug("Starting test");

		final File tempSourceSynchFolder = new File(tempFolder, "synchSource");
		FileUtilities.ensureFolderExists(tempSourceSynchFolder);

		final File tempDestSynchFolder = new File(tempFolder, "synchDest");

		FileUtilities.setLastModified(sourceTempFile1, sourceTempFile1.lastModified() + 60000);
		FileUtilities.setLastModified(sourceTempFile4, sourceTempFile4.lastModified() + 60000);

		final FileTransfer fileTransfer = fileSharingClient.uploadFolder("server", tempSourceSynchFolder, tempDestSynchFolder.getAbsolutePath());

		final List<File> resultFiles = fileTransfer.done();

		Assert.assertTrue(resultFiles.size() > 0, "Resulting synchronized folder " + tempSourceSynchFolder.getAbsolutePath() + " should exist.");

		Assert.assertEquals(fileTransfer.getTransferedFiles().size(), 2, "Number of transfered files should be the same as requested.");
		Assert.assertTrue(fileTransfer.getTransferedFiles().contains(sourceTempFile1), "File [" + sourceTempFile1.getAbsolutePath() + "] was not transfered.");
		Assert.assertTrue(fileTransfer.getTransferedFiles().contains(sourceTempFile4), "File [" + sourceTempFile4.getAbsolutePath() + "] was not transfered.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile1, destinationFile1) == null, "Source and destination file contents are not the same.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile2, destinationFile2) == null, "Source and destination file contents are not the same.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile3, destinationFile3) == null, "Source and destination file contents are not the same.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile4, destinationFile4) == null, "Source and destination file contents are not the same.");

		LOGGER.debug("Ending test");
	}

	private File sourceTempFile44;
	private File destinationFile44;

	@Test
	public void synchronizedLocalFileTest() throws IOException {
		LOGGER.debug("Starting test");

		sourceTempFile44 = TestingUtilities.getTempFileFromResource(sourceFileResourcePath1, false, tempFolder);
		destinationFile44 = new File(tempFolder, "synchronizedLocal.test");

		final FileTransfer fileTransfer = fileSharingClient.downloadFile("server", destinationFile44, sourceTempFile44.getAbsolutePath());

		final File resultFile = fileTransfer.done().get(0);


		Assert.assertEquals(fileTransfer.getFiles().size(), fileTransfer.getTransferedFiles().size(), "Number of transfered files should be the same as requested.");
		Assert.assertTrue(resultFile.exists(), "Resulting synchronized file " + resultFile.getAbsolutePath() + " should exist.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile44, resultFile) == null, "Source and destination file contents are not the same.");
		Assert.assertNull(fileTransfer.getErrorException(), "No exception should have been thrown if file synchronization is successful.");

		LOGGER.debug("Ending test");
	}

	@Test(dependsOnMethods = {"synchronizedLocalFileTest"})
	public void synchronizedLocalFileRetrialTest() {
		LOGGER.debug("Starting test");

		final FileTransfer fileTransfer = fileSharingClient.downloadFile("server", destinationFile44, sourceTempFile44.getAbsolutePath());

		final File resultFile = fileTransfer.done().get(0);

		Assert.assertEquals(fileTransfer.getTransferedFiles().size(), 0, "No file should be transfered.");
		Assert.assertTrue(resultFile.exists(), "Resulting synchronized file " + resultFile.getAbsolutePath() + " should exist.");
		Assert.assertTrue(TestingUtilities.compareFilesByLine(sourceTempFile44, resultFile) == null, "Source and destination file contents are not the same.");

		LOGGER.debug("Ending test");
	}

	@Test
	public void synchronizedLocalFileFailedTest() {
		LOGGER.debug("Starting test");

		final File sourceTempFile = new File(tempFolder, "synchronizedLocal145785ABCD.test");
		final File destinationFile = new File(tempFolder, "synchronizedLocalFailed.test");

		final FileTransfer fileTransfer = fileSharingClient.downloadFile("server", destinationFile, sourceTempFile.getAbsolutePath());

		final File resultFile = fileTransfer.done().get(0);

		Assert.assertFalse(resultFile.exists(), "Resulting transfered file should exist for a failed transfer.");

		LOGGER.debug("Ending test");
	}

	@AfterClass
	public void cleanUp() {
		LOGGER.debug("Starting clean up");

		FileUtilities.cleanupTempFile(tempFolder);
		fileSharing.stopProcessingRequest();
		fileSharingClient.stopProcessingRequest();
		fileSharingServer.stopProcessingRequest();

		try {
			broker.stop();
			broker.waitUntilStopped();
		} catch (Exception e) {
			throw new MprcException("Could not stop broker", e);
		}

		LOGGER.debug("Ending clean up");
	}
}
