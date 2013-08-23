package edu.mayo.mprc.sequest.core;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Test(sequential = true)
public final class TestPvmUtilities {

	private static final Logger LOGGER = Logger.getLogger(TestPvmUtilities.class);
	private static final String PVM_DAEMON = "pvmd3";

	private File hostsFile;
	private String userName;

	@BeforeClass
	public void init() {
		hostsFile = new File("/etc/pvmhosts");
		userName = System.getProperty("user.name");
		LOGGER.debug("userName=" + userName);
	}

	/**
	 * see if can parse the slave nodes
	 */
	@Test(enabled = true, groups = {"linux", "pvm"})
	public void testgetSlaveNodesUofMN() {
		final String[] nodes = {"sequest1.umn"};
		String tempFolder = null;
		try {
			// create a temp file
			final File folder = FileUtilities.createTempFolder();
			tempFolder = folder.getAbsolutePath();
			final File f = File.createTempFile("mine", "txt", folder);
			// add a comment
			final List<String> lines = new ArrayList<String>();
			lines.add("# my uofMN test");


			for (final String node : nodes) {
				lines.add("" + node + " ep=/project/sequest/swift/sequest/");

			}
			FileUtilities.writeStringsToFileNoBackup(f, lines, "\n");
			final PvmUtilities pvm = new PvmUtilities();
			final List<String> foundNodes = pvm.getSlaveNodes(f.getAbsolutePath());
			Assert.assertEquals(foundNodes, Arrays.asList(nodes), "did not find the nodes");
		} catch (Exception t) {
			Assert.fail("exception occurred", t);

		} finally {
			TestingUtilities.quietDelete(tempFolder);
		}
	}

	/**
	 * see if can parse the slave nodes
	 */
	@Test(enabled = true, groups = {"linux", "pvm"})
	public void testgetSlaveNodes() {
		final String[] nodes = {"node001", "node002", "node003", "node004", "node005"};
		String tempFolder = null;
		try {
			// create a temp file
			final File folder = FileUtilities.createTempFolder();
			tempFolder = folder.getAbsolutePath();
			final File f = File.createTempFile("mine", "txt", folder);
			final List<String> lines = new ArrayList<String>();

			for (final String node : nodes) {
				lines.add("" + node + " sp=2");

			}
			FileUtilities.writeStringsToFileNoBackup(f, lines, "\n");
			final PvmUtilities pvm = new PvmUtilities();
			final List<String> foundNodes = pvm.getSlaveNodes(f.getAbsolutePath());
			Assert.assertEquals(foundNodes, Arrays.asList(nodes), "did not find the nodes");
		} catch (Exception t) {
			Assert.fail("exception occurred", t);

		} finally {
			TestingUtilities.quietDelete(tempFolder);
		}
	}

	/**
	 * see if can get the process ID from output of process listing
	 */
	@Test(enabled = true, groups = {"linux", "pvm"})
	public void testgetProcessID() {
		final String pvmd = "pvmd3";
		final String expectedpid = "1001";
		final String[] lines = {
				"mprcdev2 " + expectedpid + " 0.0 0.5 1384 724 ? s a5:57 0:00 /usr/local/pvm3/lib/LINUX/" + pvmd + " -s",
				"mprcdev2 512 0.0 0.5 1384 724 ? s a5:57 0:00 grep -s",
				"mprcdev2 816 0.0 0.5 1384 724 ? s a5:57 0:00 which -s",
				"mprcdev2 20001 0.0 0.5 1384 724 ? s a5:57 0:00 ps -s",
		};
		final PvmUtilities pvm = new PvmUtilities();
		final long pid = pvm.getProcessID(Arrays.asList(lines), pvmd);
		Assert.assertEquals(expectedpid, "" + pid, "process id mismatch");

	}

	/**
	 * get the standard ouptput result from a call transmitted via ssh
	 */
	@Test(enabled = true, groups = {"linux", "pvm", "overnight"})
	public void testgetSshResult() {
		if (!new File("/etc/pvmhosts").exists()) {
			LOGGER.debug("We cannot test sequest on this machine");
			return;
		}
		if (FileUtilities.isWindowsPlatform()) {
			return;
		}
		LOGGER.debug("in testgetSshResult");
		final PvmUtilities pvm = new PvmUtilities();
		// look up the pvmhosts
		final List<String> slaves = pvm.getSlaveNodes(hostsFile);
		if (slaves != null && slaves.isEmpty()) {
			LOGGER.warn("No slaves detected in this pvm setup. The ssh test cannot be performed.");
			return;
		}
		Assert.assertTrue(slaves != null && !slaves.isEmpty(), "slaves not found");
		String hostName = null;
		if (slaves != null && !slaves.isEmpty()) {
			hostName = slaves.get(0);
		}

		LOGGER.debug("first slave=" + hostName);
		final List<String> lines = pvm.getSshResult(hostName, "ls -AlL /");
		if (lines == null) {
			Assert.fail("no result from ssh " + hostName + " ls /");
		}
	}

	public static String getHostName() {
		// find the hostName
		String hostName = "unknown";
		final InetAddress host;
		try {
			host = InetAddress.getLocalHost();
			hostName = host.getHostName();
		} catch (Exception t) {
			// ignore here since not the main exception
			throw new MprcException("cannot determine host name", t);
		}
		return hostName;
	}

	@Test(enabled = true, groups = {"linux", "pvm"})
	public void testgetCmdResult() {
		if (FileUtilities.isWindowsPlatform()) {
			return;
		}

		final PvmUtilities pvm = new PvmUtilities();
		final List<String> args = Arrays.asList("ls", "-AlL", "/");

		final List<String> lines = pvm.getCmdResult(args);
		if (lines == null || lines.isEmpty()) {
			Assert.fail("no result from ls /");

		}
	}


	@Test(enabled = false, groups = {"linux", "pvm"})
	public void testKillPVMonNode() {
		try {
			LOGGER.debug("in testKillPVMonNode");
			final PvmUtilities pvm = new PvmUtilities();
			final List<String> nodes = pvm.getSlaveNodes(hostsFile);
			if (nodes == null || nodes.isEmpty()) {
				LOGGER.debug("no slave nodes");
				return;
			}
			LOGGER.debug("found " + nodes.size() + " slave nodes");
			final String hostName = nodes.get(0);
			LOGGER.debug("hostName=" + hostName);
			final long pid = pvm.findProcessIDforExe(hostName, userName, "pvmd");
			if (pid != 0) {
				LOGGER.debug("killing pvm on node=" + hostName);
				pvm.killPVMonNode(hostName, userName, "pvmd", "/tmp", false);
				LOGGER.debug("finding process id for pvmd");
				final long adjusted_pid = pvm.findProcessIDforExe(hostName, userName, "pvmd");
				Assert.assertTrue(adjusted_pid == 0, "pvmd pid = " + adjusted_pid + " not killed");
			}
		} catch (Exception t) {
			Assert.fail("failure in testKillPVMonNode", t);
		}
	}

	@Test(enabled = false, groups = {"linux", "pvm"})
	public void testKillPVMonSlaveNodes() {
		try {
			LOGGER.debug("in testKillPVMonNodes");
			final PvmUtilities pvm = new PvmUtilities();
			final List<String> nodes = pvm.getSlaveNodes(hostsFile);
			if (nodes == null || nodes.isEmpty()) {
				LOGGER.debug("no slave nodes");
				return;
			}
			LOGGER.debug("found " + nodes.size() + " slave nodes");
			for (final String hostName : nodes) {
				LOGGER.debug("hostName=" + hostName);
				final long pid = pvm.findProcessIDforExe(hostName, userName, "pvmd");
				if (pid != 0) {
					LOGGER.debug("killing pvm on node=" + hostName);
					pvm.killPVMonNode(hostName, userName, "pvmd", "/tmp", false);
					LOGGER.debug("finding process id for pvmd");
					final long adjusted_pid = pvm.findProcessIDforExe(hostName, userName, "pvmd");
					Assert.assertTrue(adjusted_pid == 0, "pvmd pid = " + adjusted_pid + " not killed");
				}
			}
		} catch (Exception t) {
			Assert.fail("failure in testKillPVMonNode", t);
		}
	}

	@Test(enabled = false, groups = {"linux", "pvm"})
	public void testKillPVMonMaster
			() {
		try {
			LOGGER.debug("in testKillPVMonMaster");
			final PvmUtilities pvm = new PvmUtilities();

			final long pid = pvm.findProcessIDforExe(userName, "pvmd");
			if (pid != 0) {
				LOGGER.debug("killing pvm locally");
				pvm.killPVMonMasterNode(userName, "pvmd", "/tmp");
				LOGGER.debug("finding process id for pvmd");
				final long adjusted_pid = pvm.findProcessIDforExe(userName, "pvmd");
				Assert.assertTrue(adjusted_pid == 0, "pvmd pid = " + adjusted_pid + " not killed");
			}

		} catch (Exception t) {
			Assert.fail("failure in testKillPVMonMaster", t);
		}
	}

	@Test(enabled = false, groups = {"linux", "pvm"})
	public void testIsPvmRunningCorrectly() {
		LOGGER.debug("in testIsPVMRunning");
		try {
			final PvmUtilities pvm = new PvmUtilities();

			final File pFile = hostsFile;
			final boolean running = pvm.isPVMRunningCorrectly(userName, pFile.getAbsolutePath());
			if (running) {
				LOGGER.debug("pvm is running");
			} else {
				LOGGER.debug("pvm is not running");
			}

		} catch (Exception t) {
			Assert.fail("failure in testIsPVMRunning", t);
		}
	}

	@Test(enabled = false, groups = {"linux", "pvm"})
	public void testPVMRestartForcedbySlaveFailure() {
		LOGGER.debug("in testPVMRestartForcedbySlaveFailure");
		PvmUtilities pvm = null;
		boolean running = false;
		try {
			pvm = new PvmUtilities();


			running = pvm.isPVMRunningCorrectly(userName, hostsFile.getAbsolutePath());
			if (running) {
				LOGGER.debug("pvm is running");
			} else {
				LOGGER.debug("pvm is not running");
			}

		} catch (Exception t) {
			Assert.fail("failure in testPVMRestartForcedbySlaveFailure", t);
		}
		if (running) {
			// kill pvm on a slave
			try {
				LOGGER.debug("killng  pvm on slave");
				final List<String> nodes = pvm.getSlaveNodes(hostsFile);
				if (nodes == null || nodes.isEmpty()) {
					LOGGER.debug("no slave nodes");
					return;
				}
				LOGGER.debug("found " + nodes.size() + " slave nodes");
				final String hostName = nodes.get(0);
				LOGGER.debug("hostName=" + hostName);
				final long pid = pvm.findProcessIDforExe(hostName, userName, "pvmd");
				if (pid != 0) {
					LOGGER.debug("killing pvm on node=" + hostName);
					pvm.killPVMonNode(hostName, userName, "pvmd", "/tmp", false);
					LOGGER.debug("finding process id for pvmd");
					final long adjusted_pid = pvm.findProcessIDforExe(hostName, userName, "pvmd");
					Assert.assertTrue(adjusted_pid == 0, "pvmd pid = " + adjusted_pid + " not killed");
				}
			} catch (Exception t) {
				Assert.fail("failure in testPVMRestartForcedbySlaveFailure", t);
			}
			LOGGER.debug("pvm should not be running correctly");
			try {
				pvm = new PvmUtilities();

				final File pFile = hostsFile;
				running = pvm.isPVMRunningCorrectly(userName, pFile.getAbsolutePath());
				if (running) {
					LOGGER.debug("pvm is running");
					Assert.fail("pvm shown as running when should not be");
				} else {
					LOGGER.debug("pvm is not running");
				}

			} catch (Exception t) {
				Assert.fail("failure in testPVMRestartForcedbySlaveFailure", t);
			}
			LOGGER.debug("restarting pvm");
			pvm.makeSurePVMOk(userName, hostsFile.getAbsolutePath(), PVM_DAEMON, "/tmp");
		}
	}

	@Test(enabled = false, groups = {"linux", "pvm"})
	public void testIsPvmRunning() {
		final PvmUtilities pvm = new PvmUtilities();
		final long pid = pvm.isPVMRunning(userName);
		LOGGER.debug("pvm process id = " + pid);
	}

	@Test(enabled = false, groups = {"linux", "pvm"})
	public void testIsPvmRunningOk() {
		final PvmUtilities pvm = new PvmUtilities();
		final boolean running = pvm.isPVMRunningCorrectly(userName, "/Users/m039201/pvm/pvmhosts");
		String rest = "";
		if (!running) {
			rest = "not";
		}
		LOGGER.debug("pvm is running " + rest);
	}


	@Test(enabled = false, groups = {"linux", "pvm"}, dependsOnMethods = {"testPVMRestartForcedbySlaveFailure"})
	public void testPVMRestartForcedbyMasterFailure() {
		LOGGER.debug("in testPVMRestartForcedbyMasterFailure");
		PvmUtilities pvm = null;
		boolean running = false;
		try {
			pvm = new PvmUtilities();


			running = pvm.isPVMRunningCorrectly(userName, hostsFile.getAbsolutePath());
			if (running) {
				LOGGER.debug("pvm is running");
			} else {
				LOGGER.debug("pvm is not running");
			}

		} catch (Exception t) {
			Assert.fail("failure in testPVMRestartForcedbyMasterFailure", t);
		}
		if (running) {
			// kill pvm on the master
			try {
				LOGGER.debug("killng  pvm on master");
				final long pid = pvm.findProcessIDforExe(userName, "pvmd");
				if (pid != 0) {
					LOGGER.debug("killing pvm on master node");
					pvm.killPVMonMasterNode(userName, "pvmd", "/tmp");
					LOGGER.debug("finding process id for pvmd");
					final long adjusted_pid = pvm.findProcessIDforExe(userName, "pvmd");
					Assert.assertTrue(adjusted_pid == 0, "pvmd pid = " + adjusted_pid + " not killed");
				}
			} catch (Exception t) {
				Assert.fail("failure in testPVMRestartForcedbyMasterFailure", t);
			}
			LOGGER.debug("pvm should not be running correctly");
			try {
				pvm = new PvmUtilities();

				final File pFile = hostsFile;
				running = pvm.isPVMRunningCorrectly(userName, pFile.getAbsolutePath());
				if (running) {
					LOGGER.debug("pvm is running");
					Assert.fail("pvm shown as running when should not be");
				} else {
					LOGGER.debug("pvm is not running");
				}

			} catch (Exception t) {
				Assert.fail("failure in testPVMRestartForcedbyMasterFailure", t);
			}
			LOGGER.debug("restarting pvm");
			pvm.makeSurePVMOk(userName, hostsFile.getAbsolutePath(), PVM_DAEMON, "/tmp");
		}
	}


	@Test(enabled = true, groups = {"linux", "pvm"})
	public void testGetFilenames
			() {
		final PvmUtilities pvm = new PvmUtilities();
		pvm.getPvmTempFileNames("/tmp", userName);
	}

	@Test(enabled = false, groups = {"linux", "pvm"})
	public void testMakeSurePVMOk() {
		final PvmUtilities pvm = new PvmUtilities();
		pvm.makeSurePVMOk(userName, hostsFile.getAbsolutePath(), PVM_DAEMON, "/tmp");
	}

	/**
	 * create a temporary file with more than PvmUtilities.TEMP_FILE_TAIL_TO_LOG
	 * then remove it
	 * make sure last lines in file logged to the log4J log
	 */
	@Test(enabled = true, groups = {"linux", "pvm"})
	public void testDeleteFile() {
		final String[] lines = {"node001", "node002", "node003", "node004", "node005"};
		final List<String> fileLines = new ArrayList<String>();
		final String tempFolder = null;
		try {
			// create a temp file
			final File folder = FileUtilities.createTempFolder();
			final File f = File.createTempFile("mine", "txt", folder);
			int total = 0;
			int counter = 0;
			while (total < PvmUtilities.TEMP_FILE_TAIL_TO_LOG) {
				for (String line : lines) {
					line = counter++ + " " + line;
					total += line.length() + 1;
					fileLines.add(line);
				}
			}
			FileUtilities.writeStringsToFileNoBackup(f, fileLines, "\n");
			final PvmUtilities pvm = new PvmUtilities();
			pvm.deleteFile(folder.getAbsolutePath(), f.getName());
			// now look in the log to see if it contains fileLines
			// unfortunately this must be done manually
		} catch (Exception t) {
			Assert.fail("exception occurred", t);

		} finally {
			TestingUtilities.quietDelete(tempFolder);
		}
	}


}

