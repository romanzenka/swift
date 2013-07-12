package edu.mayo.mprc.sequest.core;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.tar.TarWriter;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class Dta2TarWriter {
	private static final Logger LOGGER = Logger.getLogger(Dta2TarWriter.class);
	private static final String OUT_EXT = ".out";

	public Dta2TarWriter() {
	}


	/*
  * find the matching .out files based on same prefix
  * then insert the dta's and out's into the tar in order
  * dta, out, dta, out...
  * @param dtaFileNames - the dta file names
  * @param outputDir - tar file will be placed here
  *
  */
	public void writeDtaFilesToTar(final List<String> dtaFileNames, final TarWriter tarWriter) {
		final List<File> allFiles = new ArrayList<File>();
		for (final String fileName1 : dtaFileNames) {
			final File dtaFile = new File(fileName1);
			final String fileName = dtaFile.getAbsolutePath();
			if (!new File(fileName).isFile()) {
				throw new MprcException("not a file : " + fileName);
			}
			final File out = getMatchingOutFile(dtaFile);

			if (!out.isFile()) {
				// move all the dtas and out files to a backup folder so can use for troubleshooting
				// find the temporary folder
				final File destination = moveFailedSequestRun(dtaFile.getParentFile());
				throw new MprcException("tar failed, as sequest out file does not exist [" + out.getAbsolutePath() + "].\n" +
						"Moving the Sequest working folder to [" + destination.getAbsolutePath() + "] - check what went wrong and delete this folder.");
			}

			allFiles.add(dtaFile);
			allFiles.add(out);

		}
		// now tar these files
		if (!allFiles.isEmpty()) {
			tarWriter.addFiles(allFiles);
		}
		// And since we added them all, we can delete them now
		for (final File file : allFiles) {
			FileUtilities.quietDelete(file);
		}

	}

	/**
	 * Strip the .dta extension. add the .out extension.
	 */
	public static File getMatchingOutFile(final File dtaFile) {
		final String prefix = FileUtilities.getFileNameWithoutExtension(dtaFile);
		final String outFileName = prefix + OUT_EXT;
		return new File(outFileName);
	}

	/**
	 * Move the working folder of Sequest to another location so the user can check it later.
	 *
	 * @param workingFolder Folder to move the data to.
	 * @return Where the data was moved to.
	 */
	private File moveFailedSequestRun(final File workingFolder) {
		final String lastFolder = workingFolder.getName();
		String topPath = workingFolder.getPath();
		if (workingFolder.getParent() != null) {
			topPath = workingFolder.getParent();
		}
		File outs = new File(new File(topPath), "outs");
		FileUtilities.ensureFolderExists(outs);
		File destination = new File(outs, lastFolder);
		LOGGER.debug("moving .out files to " + destination.getAbsolutePath());
		FileUtilities.rename(workingFolder, destination);
		return destination;
	}


}
