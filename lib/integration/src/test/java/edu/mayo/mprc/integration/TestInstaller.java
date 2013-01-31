package edu.mayo.mprc.integration;

import edu.mayo.mprc.utilities.FileUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

public class TestInstaller {
	@Test
	public static void shouldInstallExtractMsn() {
		final File folder = Installer.extractMsn(null, Installer.Action.INSTALL);
		Assert.assertTrue(folder.exists(), "extract_msn folder does not exist");
		Assert.assertTrue(new File(folder, "extract_msn.exe").exists(), "extract_msn.exe does not exist");
		Installer.extractMsn(folder, Installer.Action.UNINSTALL);
	}

	@Test
	public static void shouldInstallOmssa() {
		final File folder = Installer.omssa(null, Installer.Action.INSTALL);
		Assert.assertTrue(folder.exists(), "Omssa folder does not exist");
		final File ommsaExecutable;
		if (FileUtilities.isWindowsPlatform()) {
			ommsaExecutable = new File(folder, "omssacl.exe");
		} else {
			ommsaExecutable = new File(folder, "omssacl");
		}
		Assert.assertTrue(ommsaExecutable.exists(), "OMSSA executable does not exist");
		Installer.omssa(folder, Installer.Action.UNINSTALL);
	}

	@Test
	public static void shouldInstallFormatDb() {
		final File folder = Installer.formatDb(null, Installer.Action.INSTALL);
		Assert.assertTrue(folder.exists(), "formatdb folder does not exist");
		final File executable;
		if (FileUtilities.isWindowsPlatform()) {
			executable = new File(folder, "formatdb.exe");
		} else {
			executable = new File(folder, "formatdb");
		}
		Assert.assertTrue(executable.exists(), "formatdb executable does not exist");
		Installer.formatDb(folder, Installer.Action.UNINSTALL);
	}

	@Test
	public static void shouldInstallTandem() {
		if (FileUtilities.isMacPlatform()) {
			// Not supported on mac
			return;
		}
		final File folder = Installer.tandem(null, Installer.Action.INSTALL);
		Assert.assertTrue(folder.exists(), "Tandem folder does not exist");
		final File tandemExecutable;
		if (FileUtilities.isMacPlatform()) {
			tandemExecutable = new File(folder, "tandem");
		} else {
			tandemExecutable = new File(folder, "tandem.exe");
		}
		Assert.assertTrue(tandemExecutable.exists(), "Tandem executable does not exist");
		Installer.tandem(folder, Installer.Action.UNINSTALL);
	}

	@Test
	public static void shouldInstallXvfbWrapper() {
		final File wrapper = Installer.xvfbWrapper(null, Installer.Action.INSTALL);
		Assert.assertTrue(wrapper.exists() && wrapper.isFile(), "Wrapper must be a file");
		Installer.xvfbWrapper(wrapper, Installer.Action.UNINSTALL);
		Assert.assertTrue(!wrapper.exists(), "File must be deleted");
	}

	@Test
	public static void shouldInstallTestFasta() {
		final File folder = Installer.testFastaFiles(null, Installer.Action.INSTALL);
		Assert.assertTrue(folder.exists(), "Fasta file folder must exist");
		Installer.testFastaFiles(folder, Installer.Action.UNINSTALL);
		Assert.assertTrue(!folder.exists(), "Folder must be deleted");
	}

	@Test
	public static void shouldInstallYeastFasta() {
		final File folder = Installer.yeastFastaFiles(null, Installer.Action.INSTALL);
		Assert.assertTrue(folder.exists(), "Fasta file folder must exist");
		Installer.yeastFastaFiles(folder, Installer.Action.UNINSTALL);
		Assert.assertTrue(!folder.exists(), "Folder must be deleted");
	}

	@Test
	public static void shouldInstallTestMgf() {
		final File folder = Installer.mgfFiles(null, Installer.Action.INSTALL);
		Assert.assertTrue(folder.exists(), "MGF folder must exist");
		Installer.mgfFiles(folder, Installer.Action.UNINSTALL);
		Assert.assertTrue(!folder.exists(), "Folder must be deleted");
	}
}
