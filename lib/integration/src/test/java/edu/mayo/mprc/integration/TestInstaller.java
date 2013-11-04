package edu.mayo.mprc.integration;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

public class TestInstaller {
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
