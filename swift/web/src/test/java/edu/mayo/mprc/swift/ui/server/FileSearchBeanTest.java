package edu.mayo.mprc.swift.ui.server;

import com.google.common.collect.Lists;
import edu.mayo.mprc.utilities.FileUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class FileSearchBeanTest {
	private File testFolder;
	private File agilentFolder;
	private File normalFolder;
	private File normalFile;

	@BeforeClass
	public void startup() throws IOException {
		testFolder = FileUtilities.createTempFolder();

		agilentFolder =new File(testFolder, "agilent.d");
		agilentFolder.mkdir();

		normalFolder = new File(testFolder, "normal");
		normalFolder.mkdir();

		normalFile = new File(testFolder, "test.RAW");
		normalFile.createNewFile();
	}

	@AfterClass
	public void teardown() {
		FileUtilities.cleanupTempFile(testFolder);
	}

	@Test
	public void shouldTreatAgilentFoldersAsFiles() {
		List<File> dirs = Lists.newArrayList(agilentFolder, normalFolder);
		List<File> files = Lists.newArrayList(normalFile);
	 	FileSearchBean.moveDirsToFiles(dirs, files);
		Assert.assertEquals(dirs.size(), 1);
		Assert.assertEquals(dirs.get(0), normalFolder);
		Assert.assertEquals(files.size(), 2);
		Assert.assertEquals(files.get(0), normalFile);
		Assert.assertEquals(files.get(1), agilentFolder);
	}

}
