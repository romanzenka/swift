package edu.mayo.mprc.sequest.core;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.tar.TarReader;
import edu.mayo.mprc.tar.TarWriter;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.GZipUtilities;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This is reponsible for packaging dtas to send to sequest
 */
final class SequestSubmit implements SequestSubmitterInterface {

	private static final Logger LOGGER = Logger.getLogger(SequestSubmit.class);

	/**
	 * list of dta files to pass to sequest
	 */
	private List<File> sequestDtaFiles;

	/**
	 * the accumulated length of filenames in segment
	 */
	private int accumulatedLength;


	/**
	 * max command line length
	 */
	private int maxLineLength;


	/**
	 * the ions section count always goes up
	 */
	private int n;

	/**
	 * the search params for sequest
	 */
	private File paramsFile;

	/**
	 * result output directory
	 */
	private File outputDir;

	/**
	 * The tar file.
	 */
	private File tarFile;
	/**
	 * Exception
	 */
	private Throwable exceptionThrown;

	private SequestCallerInterface sequestCaller;

	private int submitCount;

	private File hostsFile;

	private static final String SEQUEST_LOG = "sequest.log";

	private long creationTime;

	private UserProgressReporter progressReporter;

	SequestSubmit(final long maxLineLength, final File paramsFile, final File workingDir, final File tarFile,
	              final File hostsFile, final UserProgressReporter progressReporter) {
		// this needs to be grabbed from the system
		this.maxLineLength = (int) maxLineLength;
		this.paramsFile = paramsFile;
		sequestDtaFiles = new ArrayList<File>();
		outputDir = workingDir;
		this.tarFile = tarFile;
		this.hostsFile = hostsFile;
		creationTime = new Date().getTime();
		this.progressReporter = progressReporter;
	}


	/**
	 * add a dta file for submission
	 */
	@Override
	public void addDtaFile(final File file, final boolean forced) {
		if (n == 0) {
			creationTime = new Date().getTime();
		}

		final int addedLength = file.getName().length() + 1;

		// We would get over max line length - submit
		if (!forced && accumulatedLength + addedLength >= maxLineLength) {
			submitFilesToSequest();
		}

		n++;
		sequestDtaFiles.add(file);
		accumulatedLength += addedLength;

		if (forced) {
			submitFilesToSequest();
		}
	}

	private boolean haveSequestDtaFiles() {
		return (sequestDtaFiles != null && !sequestDtaFiles.isEmpty());
	}

	/**
	 * force cleanup by forcing a submit to sequest of all file in the queue
	 */
	@Override
	public void forceSubmit() {
		if (haveSequestDtaFiles()) {
			submitFilesToSequest();
		}

		// do the cleanup
		// now create the zip file
		final File zipFile = new File(tarFile + ".gz");
		final Date startZip = new Date();
		FileUtilities.quietDelete(zipFile);
		try {
			GZipUtilities.compressFile(tarFile, zipFile);
		} catch (IOException e) {
			throw new MprcException("gzip failed for " + tarFile, e);
		}
		final Date endZip = new Date();
		final long zipTime = endZip.getTime() - startZip.getTime();
		LOGGER.info("ziptime = " + zipTime);

		// remove the tar file as no longer needed
		FileUtilities.quietDelete(tarFile);
	}

	private List<File> getSequestDtaFiles() {
		return sequestDtaFiles;
	}

	/**
	 * see if can submit to sequest
	 */
	private void submitFilesToSequest() {

		LOGGER.info("start submitting batch of files to sequest, after " + (new Date().getTime() - creationTime) + " ms of preprocessing");

		submitCount++;
		// see if a sequest.log exists
		final File sequestLog = new File(outputDir, SEQUEST_LOG);
		if (sequestLog.exists()) {
			// move the file to a name incremented one
			FileUtilities.copyFile(sequestLog, new File(outputDir, sequestLog.getName() + "." + submitCount), true);
		}

		// Get a copy of the list, just in case
		final List<File> dtaFiles = new ArrayList<File>(getSequestDtaFiles());

		runSequestForDtaFiles(dtaFiles);

		final List<File> dtaFilesWithNoOut = getDtaFilesWithNoOut(dtaFiles);
		if (!dtaFilesWithNoOut.isEmpty()) {
			LOGGER.warn("Sequest failed to produce all out files. Give it one more chance.");
			runSequestForDtaFiles(dtaFilesWithNoOut);
		}

		// now the tar
		LOGGER.info("tar file name=" + tarFile);
		TarWriter tt = null;
		try {
			tt = new TarWriter(tarFile, progressReporter);
			// .out and .dta files are in the working  dir for sequest
			final List<String> dtasToTar = new ArrayList<String>();
			final List<File> sequestDtaSnapshot = new ArrayList<File>(getSequestDtaFiles());
			final File workingDir = sequestCaller.getWorkingDir();
			for (final File sequestDtaSnapshotFile : sequestDtaSnapshot) {
				dtasToTar.add(new File(workingDir, sequestDtaSnapshotFile.getName()).getAbsolutePath());
			}
			// need to tar these files and the corresponding .out files
			final Date startTar = new Date();

			final Dta2TarWriter dtaWriter = new Dta2TarWriter();
			dtaWriter.writeDtaFilesToTar(dtasToTar, tt);
			final Date endTar = new Date();
			final long tarTime = endTar.getTime() - startTar.getTime();

			LOGGER.info("Tarring finished, tar time: " + tarTime);
		} finally {
			if (tt != null) {
				try {
					tt.close();
				} catch (Exception e) {
					cleanTarOnFailure(tt.getTarFile(), e);
				}
			}

			// validate the tar file, if it is corrupted then delete it and throw an exception
			validateTarFile(tarFile);
		}

		LOGGER.info("tar file = " + tt.getTarFile() + " has " + TarReader.readNumberHeaders(tt.getTarFile()) + " headers");

		// then remove the files
		sequestDtaFiles = new ArrayList<File>();
		accumulatedLength = 0;
		creationTime = new Date().getTime();
	}

	/**
	 * A list of dta files that did not get a matching .out file.
	 */
	private static List<File> getDtaFilesWithNoOut(final Iterable<File> dtaFiles) {
		final List<File> noOutDtas = new ArrayList<File>(10);
		for (final File file : dtaFiles) {
			if (!Dta2TarWriter.getMatchingOutFile(file).isFile()) {
				noOutDtas.add(file);
			}
		}
		return noOutDtas;
	}

	private void runSequestForDtaFiles(final List<File> dtaFiles) {
		SequestRunner sequestRunner = null;
		// make the call to sequest
		if (sequestCaller == null) {
			sequestCaller = new SequestRunner(outputDir, paramsFile, dtaFiles, hostsFile, progressReporter);
			sequestRunner = (SequestRunner) sequestCaller;
		} else {
			sequestRunner = (SequestRunner) sequestCaller.createInstance(sequestCaller.getWorkingDir(), paramsFile, dtaFiles, hostsFile, progressReporter);
			sequestRunner.setStartTimeOut(sequestCaller.getStartTimeOut());
			sequestRunner.setWatchDogTimeOut(sequestCaller.getWatchDogTimeOut());
			sequestRunner.setSearchResultsFolder(sequestCaller.getSearchResultsFolder());
		}

		// blocking call for now

		final Thread t = new Thread(sequestRunner);
		t.setUncaughtExceptionHandler(new ProcessExceptionCatcher(this));
		final Date startSearch = new Date();
		t.start();

		try {
			t.join();
		} catch (InterruptedException ignore) {
			// SWALLOWED Does not affect the main flow
		}
		final Date endSearch = new Date();
		final long searchTime = endSearch.getTime() - startSearch.getTime();
		LOGGER.info("Sequest search done. Time: " + searchTime);


		if (exceptionThrown != null) {
			throw new MprcException(exceptionThrown);
		}
	}


	class ProcessExceptionCatcher implements Thread.UncaughtExceptionHandler {
		private SequestSubmitterInterface submitter;

		ProcessExceptionCatcher(final SequestSubmitterInterface sequestSubmitter) {
			submitter = sequestSubmitter;
		}

		@Override
		public void uncaughtException(final Thread t, final Throwable e) {
			submitter.setExceptionThrown(e);
			throw new MprcException(e);
		}
	}

	/**
	 * This will throw an exception if the tar file is not readable
	 *
	 * @param tarFile - the tar file
	 */
	void validateTarFile(final File tarFile) {

		try {
			TarReader.readNumberHeaders(tarFile);
		} catch (Exception t) {
			cleanTarOnFailure(tarFile, t);
		}
	}

	/**
	 * if the tar file was corrupted an exception would have been triggered reading it
	 * rename it so it will not block the creation of a new tar file
	 *
	 * @param tarFile - the tar file name
	 * @param t       - the exception
	 */
	private void cleanTarOnFailure(final File tarFile, final Throwable t) {
		final String newName = FileUtilities.stripExtension(tarFile.getAbsolutePath()) + "_tar_backup";
		if (tarFile.exists()) {
			FileUtilities.rename(tarFile, new File(newName));
		}
		throw new MprcException("tar file=" + tarFile.getAbsolutePath() + " is corrupted, renamed to " + newName, t);
	}

	@Override
	public int getHowManyFiles() {
		return sequestDtaFiles.size();
	}

	@Override
	public void setExceptionThrown(final Throwable exceptionThrown) {
		this.exceptionThrown = exceptionThrown;
	}

	@Override
	public SequestCallerInterface getSequestCaller() {
		return sequestCaller;
	}

	@Override
	public void setSequestCaller(final SequestCallerInterface sequestCaller) {
		this.sequestCaller = sequestCaller;
	}

	public int getMaxLineLength() {
		return maxLineLength;
	}

	public void setMaxLineLength(final int maxLineLength) {
		this.maxLineLength = maxLineLength;
	}

	public int getAccumulatedLength() {
		return accumulatedLength;
	}

}
