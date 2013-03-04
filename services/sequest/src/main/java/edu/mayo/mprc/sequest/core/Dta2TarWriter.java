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
	public void writeDtaFilesToTar(final List<String> dtaFileNames, final File outputDir, final TarWriter tarWriter) {
		final List<File> allFiles = new ArrayList<File>();
		for (final String fileName1 : dtaFileNames) {
			final File dtaFile = new File(fileName1);
			final String fileName = dtaFile.getAbsolutePath();
			if (!new File(fileName).isFile()) {
				throw new MprcException("not a file : " + fileName);
			}
			final String prefix = FileUtilities.stripExtension(fileName);
			final String outFileName = prefix + OUT_EXT;
			if (!new File(outFileName).isFile()) {
				// move all the dtas and out files to a backup folder so can use for troubleshooting
				// find the temporary folder
				final String path = dtaFile.getParent();

				final String lastFolder = FileUtilities.getLastFolderName(path);
				String topPath = path;
				final File someFile = new File(path);
				if (someFile.getParent() != null) {
					topPath = someFile.getParent();
				}
				final File p = new File(path);
				File outs = new File(new File(topPath), "outs");
				FileUtilities.ensureFolderExists(outs);
				File destination = new File(outs, lastFolder);
				LOGGER.debug("moving .out files to " + destination.getAbsolutePath());
				FileUtilities.rename(p, destination);
				throw new MprcException("tar failed, as sequest out file does not exist [" + outFileName + "].\n" +
						"Moving the Sequest working folder to ["+destination.getAbsolutePath()+"] - check what went wrong and delete this folder.");
			}
			final File out = new File(outFileName);
			allFiles.add(dtaFile);
			allFiles.add(out);

		}
		// now tar these files
		if (allFiles.size() > 0) {
			tarWriter.addFiles(allFiles);
		}
		// And since we added them all, we can delete them now
		for(final File file : allFiles) {
			FileUtilities.quietDelete(file);
		}

	}


}
