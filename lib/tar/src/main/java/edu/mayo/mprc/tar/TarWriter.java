package edu.mayo.mprc.tar;


import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * used to write out files to a tar archive
 * When the tar is intially created a TarOuputStream is opened on it for writing to the tar.
 * A tar cannot be reopened and appended to once it has been closed (the tar library outputs EOF record when closing the tar,
 * so appending would mean seeking before the EOF header and rewriting the end of the file, which has not been implemented yet).
 * David Lentz decided to rollover the tar to another tar with extension {@link #ROLL_OVER_EXTENSION} on open, and then
 * concatenate the two tars on close by calling <tt>tar</tt> directly. That means this class <b>will work only on Unix</b>.
 * <p/>
 * When the tar is again closed the two tar files are concatenated, rollover + tar via a tar concatenate system call (ughh)
 * <p/>
 * The major operations are
 * <ul>
 * <li>
 * create the object using the constructor. If a tar file already exists an output stream on it would corrupt it
 * so instead a rollover tar is created with name provided by {@link #getRolloverFile()} .
 * On {@link #close} the two tar files will be concatenated together.
 * </li>
 * <li>
 * addFile, addFiles - to add file(s) to the tar (these do not close the output stream)
 * </li>
 * <li>
 * close - close the output stream on the tar (concatenating tars if required)
 * </li>
 * </ul>
 * </p>
 * The most effective way to use this is to create the instance,
 * add all the files to be included in the tar and then close the {@link TarWriter} instance.
 */
public final class TarWriter {
	private static final Logger LOGGER = Logger.getLogger(TarWriter.class);
	private TarOutputStream outputStream;
	private File tarFile;
	private static final int MAX_BUFF_SIZE = 1000;
	/**
	 * indicates tar written and closed
	 */
	private boolean written;
	private boolean rolled;
	private UserProgressReporter progressReporter;

	private static final String ROLL_OVER_EXTENSION = ".1.tar";

	/**
	 * if the tar file exists it rolls it otherwise creates it
	 */
	public TarWriter(final File tarFile, UserProgressReporter progressReporter) {
		initialize(tarFile, progressReporter);
	}

	/**
	 * create the tar file  if it does not exist. If it already exists create the rollover tar as backup and open the new file.
	 *
	 * @param file
	 */
	private void initialize(final File file, final UserProgressReporter progressReporter) {
		tarFile = file;
		this.progressReporter = progressReporter;
		if (tarFile.exists()) {
			// backup the existing one to the file with name tarFileName + ROLL_OVER_EXTENSION
			FileUtilities.rename(tarFile, getRolloverFile());
		}
		FileUtilities.ensureFileExists(tarFile);
	}

	/**
	 * determine if the tar file has been closed
	 *
	 * @return
	 */
	private boolean isTarClosed() {
		return outputStream == null;
	}

	/**
	 * place the file in the tar archive, providing it a header and
	 * content
	 */
	public void addFile(final File file) {
		if (isTarClosed()) {
			rolloverContents();
		}
		final String name = file.getName();
		final TarEntry t = new TarEntry(file);
		t.setName(name);
		try {
			outputStream.putNextEntry(t);
		} catch (IOException e) {
			throw new MprcException("failed adding tar entry for file=" + name + "to tar file=" + tarFile.getAbsolutePath(), e);
		}
		BufferedInputStream bis = null;

		try {
			bis = new BufferedInputStream(FileUtilities.getInputStream(file));
			final byte[] buf = new byte[MAX_BUFF_SIZE];
			int n = 0;
			while (true) {
				try {
					n = bis.read(buf, 0, MAX_BUFF_SIZE - 1);
				} catch (IOException e) {
					throw new MprcException("error reading stream", e);
				}
				if (n != -1) {
					try {
						outputStream.write(buf, 0, n);
					} catch (IOException e) {
						throw new MprcException("error writing to stream", e);
					}
				} else {
					try {
						outputStream.flush();
					} catch (IOException e) {
						throw new MprcException("could not flush stream to tar file=" + tarFile.getAbsolutePath(), e);
					}
					try {
						outputStream.closeEntry();
					} catch (IOException e) {
						throw new MprcException("could not close the stream to tar file=" + tarFile.getAbsolutePath(), e);
					}
					return;
				}
			}

		} finally {
			FileUtilities.closeQuietly(bis);
		}

	}

	/**
	 * append these files to the tar
	 *
	 * @param files- files to append
	 */
	public void addFiles(final List<File> files) {
		rolloverContents();
		for (final File file : files) {
			addFile(file);
		}
		close();
	}

	/**
	 * concatenate the files
	 */
	/**
	 * concatenate the tar files
	 *
	 * @param to       append to this file
	 * @param from     contents of this appended to 'to'
	 * @param reporter Object to help with reporting progress (specifically log files)
	 */
	public static void concatenateTars(final File to, final File from, final UserProgressReporter reporter) {
		if (to.getName().equals(from.getName())) {
			return;
		}
		LOGGER.debug("concatenating tars, " + to.getAbsolutePath() + " << " + from.getAbsolutePath());
		final List<String> command = Arrays.asList("tar", "-A", "-f", to.getAbsolutePath(), from.getAbsolutePath());
		final ProcessBuilder builder = new ProcessBuilder(command);
		final ProcessCaller caller = new ProcessCaller(builder, reporter.getLog());
		caller.runAndCheck("tar file concatenation");
		FileUtilities.quietDelete(from);
	}


	/**
	 * close the tar archive
	 */
	public void close() {
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				// this is a fatal exception as the tar file will be corrupted
				throw new MprcException("could not close the tar file=" + tarFile.getAbsolutePath(), e);
			}
			outputStream = null;
			written = true;
			rolled = false;

			// now concatenate this
			// tar concatenate example : <tar --concatenate --file foo.tar fooadd.tar>
			final File newTar = getRolloverFile();
			if (newTar.exists()) {
				concatenateTars(tarFile, newTar, progressReporter);
			}
		}
	}

	/**
	 * @return The file name used for doing the rollover.
	 */
	private File getRolloverFile() {
		return new File(tarFile.getAbsolutePath() + ROLL_OVER_EXTENSION);
	}

	/**
	 * create a new tar file, append the records to it
	 * then concatenate these to the intitial tar file
	 * then delete the new temporary tar
	 * leave the output stream of the tar file open so can add the additional files to it
	 * tar concatenate example : <tar --concatenate --file foo.tar fooadd.tar>
	 *
	 * @return the tar file object
	 */
	public File rolloverContents() {
		if (tarFile.exists() && written && !rolled) {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					throw new MprcException(e);
				}
				outputStream = null;
			}
			rolled = true;
			// create the new tar file then append the records to it
			final File newTar = getRolloverFile();
			// now append to this file
			final FileOutputStream fo = FileUtilities.getOutputStream(newTar);
			outputStream = new TarOutputStream(fo);
			return newTar;
		}

		if (isTarClosed()) {
			return openTar();
		}
		return null;
	}

	/**
	 * open the tar for writing
	 *
	 * @return file object for tar
	 */
	private File openTar() {
		outputStream = new TarOutputStream(FileUtilities.getOutputStream(tarFile));
		return tarFile;
	}

	/**
	 * @return tar file name
	 */
	public File getTarFile() {
		return tarFile;
	}
}
