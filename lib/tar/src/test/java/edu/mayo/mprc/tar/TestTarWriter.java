package edu.mayo.mprc.tar;


import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Test(sequential = true)
public final class TestTarWriter {
	private static final Logger LOGGER = Logger.getLogger(TestTarWriter.class);
	private String[] lines = {
			"BEGIN IONS",
			"TITLE=test_1199-1302_46GB0402spot46 scan 10 10 (test_1199-1302_46GB0402spot46.10.10.3.dta)",
			"CHARGE=3+",
			"PEPMASS=771.99743764",
			"222.99 25.1",
			"224.37 6.9",
			"226.08 25.9",
			"227.07 162.2",
			"255.09 10.4",
			"255.70 5.5",
			"257.09 1.5",
			"259.14 665.8",
			"END IONS",
	};

	/**
	 * test writing out one file to the tar file;
	 */
	@Test(enabled = true)
	public void testWritetoTarFile() {
		if (FileUtilities.isWindowsPlatform() || FileUtilities.isMacPlatform()) {
			return;
		}

		String name = null;
		String tarName = null;

		try {

			name = createFilledTempFile("myfile", ".dta", lines);

			final File tar = File.createTempFile("mytarfile", ".tar");
			tarName = tar.getAbsolutePath();

			final TarWriter tt = new TarWriter(tar);

			LOGGER.debug("wrote tar at " + tar.getAbsolutePath());

			tt.addFile(new File(name));

			tt.close();

			// read the tar to see if stored properly;
			Assert.assertEquals(TarReader.readNumberHeaders(tt.getTarFile()), 1, "no headers in the file");
		} catch (Exception t) {
			Assert.fail("test failed", t);
		} finally {
			TestingUtilities.quietDelete(name);
			TestingUtilities.quietDelete(tarName);
		}
	}


	private String createFilledTempFile(final String prefix, final String extension, final String[] content) {

		try {
			final File f1 = File.createTempFile(prefix, extension);

			FileUtilities.writeStringsToFileNoBackup(f1, Arrays.asList(content), "\n");

			return f1.getAbsolutePath();
		} catch (IOException ioe) {
			throw new MprcException(ioe);
		}
	}

	private File createFilledTempFileinTempFolder(final File folder, final String prefix, final String extension, final String[] content) {
		try {

			final File f1 = new File(folder + File.separator + prefix + "." + extension);
			f1.createNewFile();
			FileUtilities.writeStringsToFileNoBackup(f1, Arrays.asList(content), "\n");

			return f1;
		} catch (IOException ioe) {
			throw new MprcException(ioe);
		}
	}

	/**
	 * test writing out two files to the tar file;
	 */
	@Test(enabled = true)
	public void testWritetoTarFileTwice() throws IOException {
		if (FileUtilities.isWindowsPlatform() || FileUtilities.isMacPlatform()) {
			return;
		}

		String name = null;
		String name1 = null;
		String tarName = null;

		try {

			name = createFilledTempFile("myfile", ".dta", lines);
			name1 = createFilledTempFile("myfile2", ".dta", lines);

			final File tar = File.createTempFile("mytarfile2", ".tar");
			tarName = tar.getAbsolutePath();

			final TarWriter tt = new TarWriter(tar);

			LOGGER.debug("wrote tar at " + tar.getAbsolutePath());

			tt.addFile(new File(name));
			tt.addFile(new File(name1));

			tt.close();

			// read the tar to see if stored properly;
			Assert.assertEquals(TarReader.readNumberHeaders(tar), 2, "wrong number of headers in the file");
		} finally {
			TestingUtilities.quietDelete(name);
			TestingUtilities.quietDelete(name1);
			TestingUtilities.quietDelete(tarName);
		}
	}

	/**
	 * test writing dta files to tar file
	 */
	@Test(enabled = true)
	public void testWritetoTarAll() throws IOException {
		if (FileUtilities.isWindowsPlatform() || FileUtilities.isMacPlatform()) {
			return;
		}
		String tempFolder = null;
		try {
			final File folder = FileUtilities.createTempFolder();
			tempFolder = folder.getAbsolutePath();

			LOGGER.debug("tempfolder=" + tempFolder);

			final File dta1 = createFilledTempFileinTempFolder(folder, "myfile", "dta", lines);
			LOGGER.debug("dta=" + dta1);
			final File dta2 = createFilledTempFileinTempFolder(folder, "myfile2", "dta", lines);
			LOGGER.debug("dta=" + dta2);

			final File tar = File.createTempFile("mytarfileall", ".tar");

			final TarWriter tt = new TarWriter(tar);

			LOGGER.debug("wrote tar at " + tar.getAbsolutePath());

			final List<File> dtas = new ArrayList<File>();
			dtas.add(dta1);
			dtas.add(dta2);
			tt.addFiles(dtas);

			tt.close();

			// read the tar to see if stored properly;
			Assert.assertEquals(TarReader.readNumberHeaders(tar), 2, "wrong number of headers in the file");
		} finally {
			TestingUtilities.quietDelete(tempFolder);
		}
	}

	@Test(enabled = true)
	public void testWriteToTarFileWithRollover() throws IOException {
		if (FileUtilities.isWindowsPlatform() || FileUtilities.isMacPlatform()) {
			return;
		}
		String tempFolder = null;
		try {

			final File folder = FileUtilities.createTempFolder();
			tempFolder = folder.getAbsolutePath();

			LOGGER.debug("tempfolder=" + tempFolder);

			final File dta1 = createFilledTempFileinTempFolder(folder, "myfile", "dta", lines);
			LOGGER.debug("dta=" + dta1);
			final File dta2 = createFilledTempFileinTempFolder(folder, "myfile2", "dta", lines);
			LOGGER.debug("dta=" + dta2);


			final File tar = File.createTempFile("mytarfile2", ".tar");

			final TarWriter tt = new TarWriter(tar);

			LOGGER.debug("wrote tar at " + tar.getAbsolutePath());

			tt.addFile(dta1);
			tt.addFile(dta2);

			tt.close();

			// read the tar to see if stored properly;
			Assert.assertEquals(TarReader.readNumberHeaders(tar), 2, "wrong number of headers in the file");

			final File dta3 = createFilledTempFileinTempFolder(folder, "myfile", "dta.1", lines);
			LOGGER.debug("dta=" + dta1);
			final File dta4 = createFilledTempFileinTempFolder(folder, "myfile2", "dta.1", lines);
			LOGGER.debug("dta=" + dta2);

			final List<File> files = new ArrayList<File>();
			files.add(dta3);
			files.add(dta4);
			tt.addFiles(files);


			tt.close();
			Assert.assertEquals(4, TarReader.readNumberHeaders(tar), "number headers incorrect");
		} finally {
			TestingUtilities.quietDelete(tempFolder);
		}
	}


}
