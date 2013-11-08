package edu.mayo.mprc.daemon;

import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.daemon.files.FileToken;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.daemon.files.ReceiverTokenTranslator;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.FileUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

public final class FileTokenFactoryTest {

	@Test
	public void shouldSupportToString() {
		final FileToken token = FileTokenFactory.createAnonymousFileToken(new File("/file.txt"));
		final String tokenString = token.toString();
		Assert.assertTrue(tokenString.contains("/file.txt"), "The toString should mention the file path");
		// (no null pointer exception)
	}

	@Test(groups = "windows")
	public void shouldRoundTripWindows() {
		if (!FileUtilities.isWindowsPlatform()) {
			return;
		}
		roundtrip("C:\\", "C:\\test.txt", "shared:/test.txt");
		roundtrip("C:\\hello", "C:\\hello\\test2.txt", "shared:/test2.txt");

		roundtrip("C:/", "C:\\test.txt", "shared:/test.txt");
		roundtrip("C:/hello", "C:\\hello\\test2.txt", "shared:/test2.txt");
		roundtrip("C:/mnt/raid1/", "C:\\mnt\\raid1\\test2.txt", "shared:/test2.txt");

		roundtrip("C:/test/", "C:\\mnt\\raid1\\test2.txt", "local:/C:/mnt/raid1/test2.txt");

		roundtrip("C:/", "C:\\te st.txt", "shared:/te st.txt");
		roundtrip("C:/hell o", "C:\\hell o\\test 2.txt", "shared:/test 2.txt");
		roundtrip("C:/mnt/raid 1/", "C:\\mnt\\raid 1\\test 2.txt", "shared:/test 2.txt");
	}

	@Test(groups = "linux")
	public void shouldRoundTripLinux() {
		if (!FileUtilities.isLinuxPlatform()) {
			return;
		}
		roundtrip("/mnt/raid1", "/mnt/raid1/test.txt", "shared:/test.txt");
		roundtrip("/mnt/raid1/hello", "/mnt/raid1/hello/test2.txt", "shared:/test2.txt");

		roundtrip("/mnt/raid1/", "/mnt/raid1/test.txt", "shared:/test.txt");
		roundtrip("/", "/test.txt", "shared:/test.txt");

		roundtrip("/var/", "/mnt/raid1/test.txt", "local:/mnt/raid1/test.txt");

		roundtrip("/mnt/raid 1", "/mnt/raid 1/te st.txt", "shared:/te st.txt");
		roundtrip("/mnt/raid 1/hell o", "/mnt/raid 1/hell o/te st2.txt", "shared:/te st2.txt");
	}

	@Test(groups = "windows")
	public void shouldRoundTripEmptyDaemonFolderWindows() {
		if (!FileUtilities.isWindowsPlatform()) {
			return;
		}
		roundtrip("", "C:\\test.txt", "local:/C:/test.txt");
		roundtrip("", "C:\\hello\\test2.txt", "local:/C:/hello/test2.txt");

		roundtrip("", "C:\\test.txt", "local:/C:/test.txt");
		roundtrip("", "C:\\hello\\test2.txt", "local:/C:/hello/test2.txt");
		roundtrip("", "C:\\mnt\\raid1\\test2.txt", "local:/" +
				"C:/mnt/raid1/test2.txt");
	}

	@Test(groups = "linux")
	public void shouldRoundTripEmptyDaemonFolderLinux() {
		if (!FileUtilities.isLinuxPlatform()) {
			return;
		}
		roundtrip("", "/test.txt", "local:/test.txt");
		roundtrip("", "/hello/test2.txt", "local:/hello/test2.txt");

		roundtrip("", "/test.txt", "local:/test.txt");
		roundtrip("", "/hello/test2.txt", "local:/hello/test2.txt");
		roundtrip("", "/mnt/raid 1/test2.txt", "local:/mnt/raid 1/test2.txt");
	}

	@Test
	public void shouldTransferNullAsNull() {
		roundtrip("", null, null);
		transfer("fromdir", null, "todir", null);
	}

	@Test(groups = "windows")
	public void shouldTransferBetweenDaemonsWindows() {
		if (!FileUtilities.isWindowsPlatform()) {
			return;
		}

		transfer("C:\\", "C:\\test.txt", "D:\\", "D:\\test.txt");
		transfer("C:\\hello", "C:\\hello\\test.txt", "D:\\", "D:\\test.txt");
		transfer("C:", "C:\\hello\\test.txt", "D:\\", "D:\\hello\\test.txt");
	}

	private DatabaseFileTokenFactory makeDaemonTokenFactory(final String daemonSharedFolder, final String daemonName) {
		final DaemonConfigInfo mainDaemon = new DaemonConfigInfo(daemonName, daemonSharedFolder);
		final DatabaseFileTokenFactory factory = new DatabaseFileTokenFactory(mainDaemon);
		factory.setDatabaseDaemonConfigInfo(mainDaemon);
		return factory;
	}

	private void roundtrip(final String daemonSharedFolder, final String daemonSharedFile, final String expectedToken) {
		final String daemomName = "main";
		final DatabaseFileTokenFactory factory = makeDaemonTokenFactory(daemonSharedFolder, daemomName);
		final File input = daemonSharedFile == null ? null : new File(daemonSharedFile);
		final String token = factory.fileToDatabaseToken(input);
		Assert.assertEquals(token, expectedToken, "The tokens stored and retrieved from the database do not match");
		final File output = factory.databaseTokenToFile(token);
		Assert.assertEquals(output == null ? null : output.getAbsoluteFile(), input == null ? null : input.getAbsoluteFile(), "The file translations should round trip through database without changing the file");
	}

	private void transfer(final String homeDaemon1, final String sourceFile, final String homeDaemon2, final String destFile) {
		final FileTokenFactory factory1 = makeDaemonTokenFactory(homeDaemon1, "daemon1");
		final File input = sourceFile == null ? null : new File(sourceFile);
		final FileToken token = FileTokenFactory.createAnonymousFileToken(input);
		final FileToken beforeSendToken = factory1.translateBeforeTransfer(token);

		final ReceiverTokenTranslator receiveTranslator = makeDaemonTokenFactory(homeDaemon2, "daemon2");
		final File output = receiveTranslator.getFile(beforeSendToken);
		final File expectedOutput = destFile == null ? null : new File(destFile);

		Assert.assertEquals(output == null ? null : output.getAbsoluteFile(), expectedOutput == null ? null : expectedOutput.getAbsoluteFile(), "The file translations failed in the simulated transfer between daemons");
	}

}
