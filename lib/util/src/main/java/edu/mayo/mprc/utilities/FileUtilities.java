package edu.mayo.mprc.utilities;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of utility methods for dealing with files.
 *
 * @author Roman Zenka
 */
public final class FileUtilities {
	/**
	 * Chunks for input reading.
	 */
	private static final int BUFF_SIZE = 2048;
	/**
	 * Typical small file size.
	 */
	private static final int SMALL_FILE_BUFFER = BUFF_SIZE * 2;

	private static final Logger LOGGER = Logger.getLogger(FileUtilities.class);

	private static final String FILE_URL_PREFIX = "file://";
	private static final String GZIP_EXTENSION = "gz";
	private static final int FILE_LOCK_WAIT_GRANULARITY = 500;
	private static final double MS_PER_SECOND = 1000.0;
	private static final int WAIT_FOR_FILE_TIMEOUT = 5 * 1000;
	private static final int FILE_WAIT_GRANULARITY = 50;
	private static final int UMASK_UNKNOWN = -2;
	/**
	 * 40 seconds should be enough to list a folder
	 */
	private static final int FOLDER_LIST_TIMEOUT = 40000;
	/**
	 * If a folder has more than so many entries, it makes sense to call the 'ls' process on Linux instead
	 * of checking each entry for being a file/folder
	 */
	private static final int MIN_FOLDER_SIZE_FOR_LS = 40;
	/**
	 * How many milliseconds to wait for NFS to have a file appear.
	 */
	public static final int NFS_FILE_TIMEOUT_MILLIS = 2 * 60 * 1000;

	private static final FileMonitor FILE_MONITOR = new FileMonitor(1000);

	private FileUtilities() {

	}

	/**
	 * This will take a lock file and will block while that file exists.  You can specify a timeout at which point
	 * the InterruptException will be thrown.
	 *
	 * @param lockFile  File to lock on.
	 * @param msTimeout How long to wait to acquire the lock.
	 * @throws InterruptedException if the lock cannot be acquired in given time.
	 */
	public static void waitOnLockFile(final File lockFile, final long msTimeout) throws InterruptedException {
		final long startTime = System.currentTimeMillis();
		while (lockFile.exists()) {
			if ((System.currentTimeMillis() - startTime) > msTimeout) {
				throw new InterruptedException("Your timeout interval has been exceeded.  Interval: " + msTimeout);
			}
			Thread.sleep(FILE_LOCK_WAIT_GRANULARITY);
		}
	}

	/**
	 * @param folder Folder to inspect for files.
	 * @return A list of all the files contained in the given parent folder. The folder is examined recursively.
	 */
	public static List<File> getFilesFromFolder(final File folder) {
		if (folder.exists() && folder.isDirectory()) {
			final LinkedList<File> files = new LinkedList<File>();
			getFilesRecursively(folder, files);
			return files;
		} else {
			throw new MprcException("File object: " + folder.getAbsolutePath() + " must exist and must be a directory.");
		}
	}

	private static void getFilesRecursively(final File file, final List<File> files) {
		if (!file.isDirectory()) {
			files.add(file.getAbsoluteFile());
		} else {
			for (final File childFile : file.listFiles()) {
				getFilesRecursively(childFile, files);
			}
		}
	}

	/**
	 * find lines that contain the expression 'regExp'
	 *
	 * @param file   - name of the file
	 * @param regExp - the regular expression
	 * @return list of lines that match
	 * @throws IOException Failing to read the file
	 */
	public static List<String> findLinesContaining(final File file, final String regExp) throws IOException {
		BufferedReader reader = null;
		final Pattern p = Pattern.compile(regExp);
		try {
			reader = new BufferedReader(new FileReader(file));

			String line = reader.readLine();
			final List<String> retList = new ArrayList<String>();
			while (line != null) {
				final Matcher matcher = p.matcher(line);
				if (matcher.find()) {
					retList.add(line);
				}
				line = reader.readLine();
			}
			return retList;
		} finally {
			closeQuietly(reader);
		}
	}

	/**
	 * Load everything from given reader into one string.
	 *
	 * @param reader    Reader to read data from.
	 * @param maxLength Maximum amount of data to read.
	 * @return String containing all the data.
	 * @throws IOException Reading failed.
	 */
	public static String readIntoString(final Reader reader, final long maxLength) throws IOException {
		final StringBuilder builder = new StringBuilder(SMALL_FILE_BUFFER);
		final char[] buff = new char[BUFF_SIZE];
		int readBytes = reader.read(buff);
		while (readBytes >= 0) {
			builder.append(buff, 0, readBytes);
			if (builder.length() > maxLength) {
				throw new MprcException("Resource exceeds maximum length " + maxLength);
			}
			readBytes = reader.read(buff);
		}
		return builder.toString();
	}

	/**
	 * @param file File to read lines from.
	 * @return A list of all lines in the file. Wraps a Guava method.
	 */
	public static List<String> readLines(final File file) {
		try {
			return Files.readLines(file, Charsets.UTF_8);
		} catch (IOException e) {
			throw new MprcException("Could not read lines from " + file.getAbsolutePath(), e);
		}
	}

	/**
	 * @param file File to count lines in.
	 * @return How many lines were there in the input file.
	 * @throws IOException If reading failed.
	 */
	public static int countLines(final File file) throws IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			int count = 0;
			while (reader.readLine() != null) {
				count++;
			}
			return count;
		} finally {
			closeQuietly(reader);
		}
	}

	/**
	 * Looks in a directory for a file identical to the one given except only the first file we find that matches will
	 * be returned.  This will no return the toFind itself...
	 *
	 * @param toFind       the file we want to find a similar file of
	 * @param withinFolder the folder we want to restrict our search to
	 * @return the similar File
	 */
	public static File findSingleSimilarFile(final File toFind, final File withinFolder) {

		final File[] matchingFileArray = withinFolder.listFiles(new FileFilter() {

			@Override
			public boolean accept(final File pathname) {
				if (pathname.isDirectory() || pathname.equals(toFind) || !pathname.canRead()) {
					return false;
				} else {
					return toFind.length() == pathname.length();
				}
			}
		});

		if (matchingFileArray == null || matchingFileArray.length == 0) {
			return null;
		}

		for (final File file : matchingFileArray) {
			try {
				if (equalFiles(toFind, file)) {
					return file;
				}
			} catch (IOException ignore) {
				// SWALLOWED - we do not care if comparison accidentally fails?
				return null;
			}
		}

		return null;
	}

	/**
	 * TODO: WARNING: This method is currently potentially dangerous in some corner cases (e.g. already existing link going in wrong direction,
	 * or two files of the same size where the link was to be created.
	 * <p/>
	 * this method will create a link to a given file. It is like if we 'copy' linkSource to linkTarget, only we create a link.
	 * <p/>
	 * It will be a hard link if isSymLink is false or a symlink if true.  This method currently
	 * only supports environments with a ln command such as flavors of Unix.
	 *
	 * @param linkSource the source, physical file
	 * @param linkTarget the target link file. This should not exist - if it does, it gets deleted!!!
	 * @param isSymLink  true if you want to create a symbolic link else false
	 * @throws IOException                   if there was an error when creating the link
	 * @throws UnsupportedOperationException if we can't create sym links in the current environment
	 */
	public static void createLink(final File linkSource, final File linkTarget, final boolean isSymLink) throws IOException {
		if (linkSource == null || linkTarget == null) {
			throw new IllegalArgumentException("The createLink parameters must not be null.");
		}

		final File canonicalLinkSource = linkSource.getCanonicalFile();
		final File canonicalLinkTarget = linkTarget.getCanonicalFile();

		if (canonicalLinkTarget.equals(canonicalLinkSource)) {
			// TODO: The link could be going the other way than we wanted!
			LOGGER.debug("Link already exists: " + linkSource.getAbsolutePath() + " and " + linkTarget.getAbsolutePath() + " are the same file.");
			return;
		}

		if (canonicalLinkSource.isDirectory() && !isSymLink) {
			throw new IOException("Only symlinks can link to a directory " + canonicalLinkSource.getAbsolutePath());
		}

		if (isWindowsPlatform()) {
			throw new UnsupportedOperationException("Windows is not currently supported for creating links");
		} else {//assuming a posix flavor
			makeUnixLink(isSymLink, canonicalLinkSource, canonicalLinkTarget);
		}
	}

	private static void makeUnixLink(final boolean isSymLink, final File canonicalLinkSource, final File canonicalLinkTarget) throws IOException {
		if (!isSymLink) {
			final File commonPath = getCommonPath(canonicalLinkSource, canonicalLinkTarget);
			if (commonPath == null) {
				throw new IOException("Hard link failed, " + canonicalLinkSource.getAbsolutePath() + " and " + canonicalLinkTarget.getAbsolutePath() + " do not share any common subpath, therefore must be on different subsystems.");  //this shouldn't happen on linux
			}
		}

		// The link contains information how to get from the linkSource file to the linkTarget file.
		// Therefore if the source and target both get moved, the link will keep working.
		final String actualLink = getRelativePath(canonicalLinkTarget.getParentFile().getAbsolutePath(), canonicalLinkSource.getAbsolutePath());

		if (canonicalLinkTarget.exists()) {
			quietDelete(canonicalLinkTarget);
		}

		final ProcessCaller caller = getUnixLinkCaller(isSymLink, canonicalLinkTarget, actualLink);

		caller.run();

		final boolean unexpectedResult = caller.getExitValue() != 0 || !caller.getOutputLog().isEmpty() || !caller.getErrorLog().isEmpty();

		if (unexpectedResult && !canonicalLinkSource.exists()) {
			throw new IOException(caller.getFailedCallDescription());
		}

		if (!canonicalLinkTarget.exists()) {
			throw new MprcException("Could not create link - the resulting file does not exist: " + canonicalLinkTarget.getAbsolutePath() + "\nLink established using: " + caller.getFailedCallDescription());
		}
	}

	private static ProcessCaller getUnixLinkCaller(final boolean isSymLink, final File target, final String source) {
		final List<String> command = new ArrayList<String>(4);

		command.add("ln");
		if (isSymLink) {
			command.add("-s");
		}
		command.add(source);
		command.add(target.getAbsolutePath());

		final ProcessBuilder pb = new ProcessBuilder()
				.directory(target.getParentFile())
				.command(command);

		final ProcessCaller caller = new ProcessCaller(pb);
		caller.setLogToConsole(false);
		return caller;
	}

	private static String getOsNameLowerCase() {
		return System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
	}

	/**
	 * @return True if running on windows, false otherwise.
	 */
	public static boolean isWindowsPlatform() {
		return getOsNameLowerCase().contains("windows");
	}

	/**
	 * @return True if running on 64-bit windows, determined by checking that the OS name contains "windows" and "64" strings
	 */
	public static boolean isWindows64Platform() {
		final String osName = getOsNameLowerCase();
		return osName.contains("windows") && osName.contains("64");
	}

	/**
	 * @return True if running on linux platform (platform name contains "linux")
	 */
	public static boolean isLinuxPlatform() {
		return getOsNameLowerCase().contains("linux");
	}

	/**
	 * @return True if running on a mac platform (platform name contains "mac")
	 */
	public static boolean isMacPlatform() {
		return getOsNameLowerCase().contains("mac");
	}

	/**
	 * creates a copy of one File to another File. Throws an exception when the copy operation fails.
	 * Use {@link #tryCopyFile} if you do not want to be bothered with exceptions.
	 *
	 * @param copyFrom  the file we want to copy from.
	 * @param copyTo    the file we want to create.
	 * @param overwrite set to true if you want to overwrite any existing file else false to fail if the file already exists
	 * @see #tryCopyFile
	 */
	public static void copyFile(final File copyFrom, final File copyTo, final boolean overwrite) {
		LOGGER.debug("Copying file from " + copyFrom.getAbsolutePath() + " to " + copyTo.getAbsolutePath());
		final String copyErrorPrefix = "copy '" + copyFrom.getAbsolutePath() + "' '" + copyTo.getAbsolutePath() + "' failed: ";

		if (!copyFrom.isFile()) {
			throw new MprcException(copyErrorPrefix + "the source file does not exist.");
		}

		if (copyFrom.equals(copyTo)) {
			throw new MprcException(copyErrorPrefix + "cannot copy file over itself.");
		}

		try {
			if (copyFrom.getCanonicalPath().equals(copyTo.getCanonicalPath())) {
				throw new MprcException(copyErrorPrefix + "cannot copy file over itself - the target already links to the source.");
			}
		} catch (IOException ignore) {
			// SWALLOWED: Ignore - if both files linked to the same place, we would be able to determine that without throwing an exception
			LOGGER.warn("Cannot determine if the files map to the same place: " + copyFrom.getAbsolutePath() + " " + copyTo.getAbsolutePath());
		}

		if (copyTo.exists() && !overwrite) {
			throw new MprcException(copyErrorPrefix + "the target file already exists");
		}

		try {
			Files.copy(copyFrom, copyTo);
			if (!copyTo.exists()) {
				throw new MprcException(copyErrorPrefix + "the target file was not created.");
			}
		} catch (IOException e) {
			throw new MprcException(copyErrorPrefix + "I/O exception", e);
		}
	}

	/**
	 * creates a copy of one File to another File. Returns false when the operation fails. Do this for
	 * non-critical operations, otherwise use {@link #copyFile}
	 *
	 * @param copyFrom  the file we want to copy from.  If it doesn't exist then return false
	 * @param copyTo    the file we want to create.  If it already exists then return false
	 * @param overwrite set to true if you want to overwrite any existing file else false to fail if the file already exists
	 * @return true if copyFrom now exists at copyTo as well else false.
	 * @see #copyFile
	 */
	public static boolean tryCopyFile(final File copyFrom, final File copyTo, final boolean overwrite) {
		try {
			copyFile(copyFrom, copyTo, overwrite);
		} catch (MprcException e) {
			LOGGER.warn("Copy failed", e);
		}
		return copyTo.exists();
	}

	/**
	 * Get extension from filename. ie
	 * <pre>
	 * foo.txt    --> "txt"
	 * a\b\c.jpg --> "jpg"
	 * a\b\c     --> ""
	 * </pre>
	 *
	 * @param filename the filename
	 * @return the extension of filename or "" if none
	 * @see #getGzippedExtension(String) for extensions supporting the .gz suffix
	 */
	public static String getExtension(final String filename) {
		final int index = filename.lastIndexOf('.');

		if (-1 == index) {
			return "";
		} else {
			return filename.substring(index + 1);
		}
	}

	/**
	 * Same as {@link #getExtension(String)}, only if the extension is .gz, previous segment of extension is removed as well.
	 * <pre>
	 * foo.txt --> "txt"
	 * foo.gz --> "gz"
	 * foo.tar.gz -> "tar.gz"
	 * </pre>
	 *
	 * @param filename File to extract extension from.
	 * @return The extension of the file, "" if none, special treatment of ".gz" able to correctly detect "tar.gz" case.
	 */
	public static String getGzippedExtension(final String filename) {
		final String extension = getExtension(filename);
		if (extension.equalsIgnoreCase(GZIP_EXTENSION)) {
			final String base = filename.substring(0, filename.length() - 1 - GZIP_EXTENSION.length());
			final String prevExtension = getExtension(base);
			if (prevExtension.isEmpty()) {
				return extension;
			} else {
				return prevExtension + "." + extension;
			}
		}
		return extension;
	}

	/**
	 * Remove extension from filename. ie
	 * <pre>
	 * foo.txt    --> foo
	 * a\b\c.jpg --> a\b\c
	 * a\b\c     --> a\b\c
	 * </pre>
	 *
	 * @param filename the filename
	 * @return the filename minus extension
	 */
	public static String stripExtension(final String filename) {
		final int index = filename.lastIndexOf('.');
		if (-1 == index) {
			return filename;
		} else {
			return filename.substring(0, index);
		}
	}

	/**
	 * Returns file name without the extension. Extension being everything beyond the last dot.
	 * Example:
	 * <pre>
	 * a/b/c/foo.txt --> foo
	 * </pre>
	 *
	 * @param file Input file
	 * @return File name without extension
	 */
	public static String getFileNameWithoutExtension(final File file) {
		if (file == null) {
			return null;
		}
		return stripExtension(file.getName());
	}

	/**
	 * @param file      File to write the string into.
	 * @param s         String to write.
	 * @param overwrite If true, the file must not exist. If the file exists, it must be of zero length. Otherwise an exception is thrown.
	 */
	public static void writeStringToFile(final File file, final String s, final boolean overwrite) {
		if (!overwrite && file.exists() && file.length() > 0) {
			throw new MprcException("File already exists: " + file.getAbsolutePath());
		}
		final File parentFolder = file.getParentFile();
		if (parentFolder != null) {
			ensureFolderExists(parentFolder);
		}
		try {
			Files.write(s, file, Charsets.UTF_8);
		} catch (Exception e) {
			throw new MprcException("Cannot write string to " + file.getAbsolutePath(), e);
		}
	}

	/**
	 * this is a method that will write an input stream to a file.
	 *
	 * @param stream the stream we want to write to a file this stream must not be null
	 * @param file   the file to write to
	 * @throws IOException if there was a problem writing the stream to the file
	 */
	public static void writeStreamToFile(final InputStream stream, final File file) throws IOException {
		if (stream == null) {
			throw new IllegalArgumentException("Stream given cannot be null.");
		}

		final FileOutputStream fileOutputStream = new FileOutputStream(file);
		try {
			ByteStreams.copy(stream, fileOutputStream);
		} finally {
			closeQuietly(fileOutputStream);
		}
	}

	/**
	 * Method travels from the root folder of this folder
	 * and lists all the files within the folder.
	 * <p/>
	 * TODO: This is a horrible hack, trying to force NFS to cooperate.
	 * <p/>
	 * Why does this work, or even whether it works remains unclear.
	 *
	 * @param folder Folder to "refresh".
	 */
	public static void refreshFolder(final File folder) {
		if (folder.isDirectory()) {
			final File parentFolder = folder.getParentFile();
			if (parentFolder != null) {
				refreshFolder(parentFolder);
			}

			folder.list();
		}
	}

	/**
	 * remove from filename any 'file://' from it
	 *
	 * @param filename the name of the file to scrub
	 * @return the scrubbed filename
	 */
	public static String removeFileUrlPrefix(final String filename) {
		if (filename.indexOf(FILE_URL_PREFIX) != -1) {
			return filename.replace(FILE_URL_PREFIX, "");
		}
		return filename;
	}

	/**
	 * finds the name of the folder without the path to the folder
	 *
	 * @param foldername the folder you want to get just the name of
	 * @return the name of the folder without the path
	 */
	public static String getLastFolderName(final String foldername) {
		return new File(foldername).getName();
	}

	/**
	 * Creates temporary folder in a given one.
	 *
	 * @param folder       Folder to create temp folder in.
	 * @param name         Name of the temporary folder.
	 * @param deleteOnExit optionally remove the directory (TODO: How is on exit useful for a Daemon?)
	 * @return Temporary folder.
	 */
	public static File createTempFolder(final File folder, final String name, final boolean deleteOnExit) {
		File tempDirFileLock = null;
		try {
			ensureFolderExists(folder);
			tempDirFileLock = File.createTempFile(name, "dir", folder);
		} catch (IOException e) {
			throw new MprcException("Failure creating temporary file (to determine temporary folder name) named " + name + " in folder " + folder.getAbsolutePath(), e);
		}
		final String dirName = tempDirFileLock.getPath().substring(0, tempDirFileLock.getPath().length() - 3);
		try {
			final File tempDir = new File(dirName);
			if (deleteOnExit) {
				tempDir.deleteOnExit();
			}
			final boolean mkdirOk = tempDir.mkdir();
			if (!tempDir.exists()) {
				throw new MprcException(mkdirOk ? "The folder does not exist after successful mkdir (something is very wrong)." : "mkdir command failed");
			}
			quietDelete(tempDirFileLock);
			return tempDir;
		} catch (Exception t) {
			throw new MprcException("Failed to create temporary folder " + dirName, t);
		}
	}

	/**
	 * Recursively deletes a file or folder. If the deletion fails, the function does not throw any exceptions.
	 * <p/>
	 * In test cases, where it is important to ensure all files were properly closed, use {@link FileUtilities#cleanupTempFile(File)}.
	 *
	 * @param toDelete is the file or folder you want to delete
	 * @return True if delete succeeded, false if there was any error. No exception is thrown.
	 */
	public static boolean deleteNow(final File toDelete) {
		Preconditions.checkNotNull(toDelete);
		LOGGER.log(Level.DEBUG, "Deleting file: " + toDelete.getAbsolutePath());
		try {
			Files.deleteRecursively(toDelete.getCanonicalFile());
			return true;
		} catch (Exception t) {
			// SWALLOWED - if the recursive delete fails, we do not even mention it
			LOGGER.warn("Could not delete: " + toDelete.getAbsolutePath(), t);
			return false;
		}
	}

	/**
	 * used to quietly delete a file, no exceptions are thrown.
	 *
	 * @param file - file of folder
	 */
	public static void quietDelete(final File file) {
		if (file != null && file.exists()) {
			// Ignore the output of file.delete
			final boolean ignore = file.delete();
			if (!ignore) {
				LOGGER.debug("Could not delete " + file.getAbsolutePath());
			}
		}
	}

	/**
	 * determines if the two files are the same.
	 * Process:
	 * 1.  check file size
	 * 2.  do a byte for byte comparison
	 *
	 * @param f1 file to compare
	 * @param f2 file to compare
	 * @return true if they have the same contents else false
	 * @throws IOException If any of the two files could not be read.
	 */
	public static boolean equalFiles(final File f1, final File f2) throws IOException {
		return Files.equal(f1, f2);
	}

	/**
	 * Finds the greatest common folder in the path between two files. It is wise to use canonical form of the files
	 * to make sure you get a proper answer.
	 *
	 * @param f1 Folder 1.
	 * @param f2 Folder 2.
	 * @return Which is the longest path that these two folders have in common.
	 */
	public static File getCommonPath(final File f1, final File f2) {
		if (f1 == null || f2 == null) {
			return null;
		}

		if (f1.equals(f2)) {
			if (f1.isDirectory()) {
				return f1;
			} else {
				return f1.getParentFile();
			}
		}

		if (f1.getPath().startsWith(f2.getPath())) {
			return f2;
		} else if (f2.getPath().startsWith(f1.getPath())) {
			return f1;
		} else {
			final File followingF1 = getCommonPath(f1.getParentFile(), f2);
			final File followingF2 = getCommonPath(f1, f2.getParentFile());

			if (followingF1 == null && followingF2 == null) {
				return null;
			} else if (followingF1 == null) {
				return followingF2;
			} else if (followingF2 == null) {
				return followingF1;
			} else {
				if (followingF1.getPath().length() > followingF2.getPath().length()) {
					return followingF1;
				} else {
					return followingF2;
				}
			}
		}
	}

	/**
	 * Attempts to create a symlink or hardlink to a file.  If a link cannot be created then a copy will be attempted.
	 *
	 * @param copyFrom  the file we want to link to or create a copy of
	 * @param copyTo    the target copy/link
	 * @param overwrite true if you want to allow overwrite else false;
	 * @param symLink   true if this is a symbolic (not hard) link
	 * @return if the link was successful
	 */
	public static boolean linkOrCopy(final File copyFrom, final File copyTo, final boolean overwrite, final boolean symLink) {
		if (!copyFrom.isFile()) {
			throw new MprcException("Cannot copy " + copyFrom.getAbsolutePath() + " - it is not a file.");
		}
		if (!copyFrom.exists()) {
			throw new MprcException("Cannot copy " + copyFrom.getAbsolutePath() + " - it does not exist.");
		}
		ensureFolderExists(copyTo.getParentFile());

		try {
			createLink(copyFrom, copyTo, symLink);
		} catch (UnsupportedOperationException e) {
			LOGGER.debug("Linking is not supported: " + MprcException.getDetailedMessage(e));
		} catch (Exception e) {
			LOGGER.warn("Could not create a link.", e);
		}

		// TODO: This would be successful for many different reasons - cannot be used directly!
		final boolean linkSuccessful = copyTo.exists() && copyFrom.length() == copyTo.length();

		if (linkSuccessful) {
			LOGGER.debug("Have created a link at " + copyTo.getAbsolutePath() + " pointing to " + copyFrom.getAbsolutePath());
			return true;
		} else {
			LOGGER.warn("Could not create a link so will now try to copy. " + copyFrom.getAbsolutePath() + " --> " + copyTo.getAbsolutePath());
			return tryCopyFile(copyFrom, copyTo, overwrite);
		}
	}

	/**
	 * Ensures that a folder either exists or is created raising an exception if not.
	 * If folder is <c>null</c>, nothing happens.
	 *
	 * @param folder is the folder that we want to make sure is exists.
	 * @throws MprcException if the folder was not created or it already exists but is a file not a directory.
	 */
	public static void ensureFolderExists(final File folder) {
		if (folder == null) {
			return;
		}
		if (folder.exists()) {
			if (folder.isFile()) {
				throw new MprcException("The directory " + folder.getAbsolutePath() + " cannot be created - a file of the same name already exists.");
			}
			return;
		}
		final boolean mkdirOk = folder.mkdirs();
		if (!folder.isDirectory()) {
			throw new MprcException("Cannot create directory  " + folder.getAbsolutePath() + (mkdirOk ? " (did not appear after being created)" : ""));
		}
	}

	/**
	 * If the file does not exist, ensure that all its parent directories exist. Then create a new file.
	 *
	 * @param file File whose existence to ensure, including parent directories.
	 */
	public static void ensureFileExists(final File file) {
		if (!file.exists()) {
			ensureFolderExists(file.getParentFile());
			try {
				if (!file.createNewFile()) {
					LOGGER.warn("Parallel creation of " + file.getAbsolutePath());
				}
			} catch (IOException e) {
				throw new MprcException("Failed to create file " + file.getAbsolutePath(), e);
			}
		}
	}

	/**
	 * Renames one file to the other.
	 * If the to file exists, the rename overwrites it.
	 *
	 * @param from Rename from.
	 * @param to   Rename to. If the file exists, it is overwritten.
	 */
	public static void rename(final File from, final File to) {
		if (from.equals(to)) {
			return;
		}
		try {
			Files.move(from, to);
		} catch (Exception t) {
			throw new MprcException("Unable to move " + from.getAbsolutePath() + " to " + to.getAbsolutePath(), t);
		}
	}

	/**
	 * blocks until a file comes into existence.
	 *
	 * @param toWaitFor the file we want to wait for
	 * @param msTimeout how long should we wait before throwing an exception
	 * @throws MprcException if we have waited too long for the file to come around
	 */
	public static void waitForFile(final File toWaitFor, final int msTimeout, final FileListener listener) {
		FILE_MONITOR.filesToExist(Arrays.asList(toWaitFor), listener, msTimeout);
	}

	/**
	 * Blocks until all the files come into existence.
	 *
	 * @param toWaitFor Set of files to wait for.
	 * @param msTimeout How long to wait till the files appear.
	 */
	public static void waitForFiles(final Collection<File> toWaitFor, final int msTimeout, final FileListener listener) {
		FILE_MONITOR.filesToExist(toWaitFor, listener, msTimeout);
	}

	/**
	 * Default wait for a file, to accomodate for NFS timeouts. These can be up to 1 minute by default, wait 2 mins to be safe.
	 *
	 * @param toWaitFor File to wait for.
	 */
	public static void waitForFile(final File toWaitFor, final FileListener listener) {
		waitForFile(toWaitFor, NFS_FILE_TIMEOUT_MILLIS, listener);
	}

	/**
	 * Just like {@link #waitForFile(java.io.File, int, FileListener)} with default timeout of {@link #NFS_FILE_TIMEOUT_MILLIS}.
	 *
	 * @param toWaitFor Set of files to wait for.
	 */
	public static void waitForFiles(final Collection<File> toWaitFor, final FileListener listener) {
		waitForFiles(toWaitFor, NFS_FILE_TIMEOUT_MILLIS, listener);
	}

	/**
	 * Blocking wait for files.
	 *
	 * @param toWaitFor Files to wait for.
	 */
	public static void waitForFilesBlocking(final Collection<File> toWaitFor) {
		if (toWaitFor.isEmpty()) {
			return;
		}
		final Semaphore lock = new Semaphore(0);
		waitForFiles(toWaitFor, NFS_FILE_TIMEOUT_MILLIS, new FileListener() {
			@Override
			public void fileChanged(Collection<File> files, boolean timeout) {
				synchronized (lock) {
					lock.release();
				}
			}
		});
		boolean run = true;
		while (run) {
			synchronized (lock) {
				try {
					lock.acquire();
				} catch (InterruptedException e) {
					throw new MprcException("No longer waiting for files, we got interrupted", e);
				}
			}
			run = false;
			for (final File file : toWaitFor) {
				if (!file.exists()) {
					run = true;
				}
			}
		}
	}

	public static void waitForFileBlocking(final File toWaitFor) {
		waitForFilesBlocking(Arrays.asList(toWaitFor));
	}


	public static final File DEFAULT_TEMP_DIRECTORY = getDefaultTempDirectory();

	/**
	 * gets the default temporary directory for this system.
	 *
	 * @return System temporary directory.
	 */
	public static File getDefaultTempDirectory() {
		final String tmpDir = System.getProperty("java.io.tmpdir");
		if (tmpDir == null) {
			throw new MprcException("Could not obtain the default temporary directory (java.io.tmpdir)");
		}
		return new File(tmpDir);
	}

	/**
	 * Overwrites given file with provided lines.
	 * Lines are separated by given endOfLineMarker.
	 *
	 * @param toWriteTo       the file we want to write the lines to
	 * @param lines           the Strings to write out as individual lines
	 * @param endOfLineMarker the character to be used as an end of line marker
	 */
	public static void writeStringsToFileNoBackup(final File toWriteTo, final List<String> lines, final String endOfLineMarker) {
		Writer writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(toWriteTo));
			for (final String line : lines) {
				writer.write(line);
				writer.write(endOfLineMarker);
			}
		} catch (IOException e) {
			throw new MprcException(e);
		} finally {
			closeQuietly(writer);
		}
	}

	/**
	 * Returns true if two file names are identical. On windows that means ignore case, on linux no ignoring is done.
	 *
	 * @param name1         First name to check
	 * @param name2         Second name to check
	 * @param caseSensitive True on platforms where file names are case sensitive. Use {@link #isCaseSensitiveFilePlatform()}
	 * @return True if the file names are equivalent.
	 */
	private static boolean fileNameEquals(final String name1, final String name2, final boolean caseSensitive) {
		if (name1 == null) {
			return name2 == null;
		}
		if (caseSensitive) {
			return name1.equals(name2);
		} else {
			return name1.equalsIgnoreCase(name2);
		}
	}

	/**
	 * @return True on platforms where file names are case sensitive.
	 */
	public static boolean isCaseSensitiveFilePlatform() {
		return !isWindowsPlatform();
	}

	/**
	 * Finds the relative path between two files. The base should be a directory.
	 * The idea is that base path + relative path = target path. If the target is not within the base,
	 * null is returned.
	 *
	 * @param basePath      the path we will start from. Has to correspond to a directory.
	 * @param targetPath    the path we want to find a relative path to
	 * @param separator     Separator of path elements - use {@link File#separator}
	 * @param caseSensitive True on platforms where file names are case sensitive. Use {@link #isCaseSensitiveFilePlatform()}
	 * @return the relative path between the two files
	 */
	public static String getRelativePathToParent(final String basePath, final String targetPath, final String separator, final boolean caseSensitive) {
		return getRelativePath(basePath, targetPath, separator, caseSensitive, true);
	}


	/**
	 * Finds the relative path between two files. The base should be a directory.
	 * The idea is that base path + relative path = target path. This might not be possible to achieve
	 * (e.g. on windows with different roots). In such case, an exception is thrown.
	 *
	 * @param basePath      the path we will start from. Has to correspond to a directory.
	 * @param targetPath    the path we want to find a relative path to
	 * @param separator     Separator of path elements - use {@link File#separator}
	 * @param caseSensitive True on platforms where file names are case sensitive. Use {@link #isCaseSensitiveFilePlatform()}
	 * @param noDoubleDots  When true, no .. paths are produced. If the target is not within base, the method returns null.
	 * @return the relative path between the two files
	 */
	public static String getRelativePath(final String basePath, final String targetPath, final String separator, final boolean caseSensitive, final boolean noDoubleDots) {
		final String[] base = basePath.split(Pattern.quote(separator), 0);
		final String[] target = targetPath.split(Pattern.quote(separator), 0);

		//  First get all the common elements. Store their total length
		//  and also count how many of them there are.
		int commonIndex = 0;
		int commonLength = 0;
		for (int i = 0; i < target.length && i < base.length; i++) {
			if (fileNameEquals(target[i], base[i], caseSensitive)) {
				commonLength += target[i].length() + separator.length();
				commonIndex++;
			} else {
				break;
			}
		}

		if (commonIndex == 0) {
			//  Whoops -- not even a single common path element. This most
			//  likely indicates differing drive letters, like C: and D:.
			//  These paths cannot be relativized. Throw an exception.
			throw new MprcException("Could not relativize target path " + targetPath + " to given base path " + basePath);
			//  This should never happen when all absolute paths
			//  begin with / as in *nix.
		}

		final StringBuilder relative = new StringBuilder();
		if (base.length != commonIndex) {
			final int numDirsUp = base.length - commonIndex;
			if (noDoubleDots && numDirsUp > 0) {
				return null;
			}
			//  The number of directories we have to backtrack is the length of
			//  the base path MINUS the number of common path elements.
			for (int i = 1; i <= (numDirsUp); i++) {
				relative.append("..").append(separator);
			}
		}
		relative.append(targetPath.substring(commonLength));

		return relative.toString();
	}

	/**
	 * Convenience method for {@link #getRelativePath} for current platform, allowing <c>..</c> paths.
	 * See {@link #getRelativePath(String, String, String, boolean, boolean)} for the full version of this method.
	 *
	 * @param basePath   the path we will start from. Has to correspond to a directory.
	 * @param targetPath the path we want to find a relative path to
	 * @return Relative path that when appended to {@code basePath} gets the user to {@code targetPath}.
	 */
	public static String getRelativePath(final String basePath, final String targetPath) {
		return getRelativePath(basePath, targetPath, File.separator, isCaseSensitiveFilePlatform(), false);
	}

	/**
	 * Create  an empty temporary folder name where can write files
	 *
	 * @return Folder - a temporary folder
	 */
	public static File createTempFolder() {
		try {
			return Files.createTempDir();
		} catch (Exception e) {
			throw new MprcException("Could not create temp folder.", e);
		}
	}

	/**
	 * Recursively deletes a file or folder. If the deletion fails, the function throws an exception, informing
	 * the test writer of unclosed files.
	 * <p/>
	 * We canonicalize the link we are requested to delete, because the recursive file deletion funciton will stop at
	 * symbolic links for security.
	 *
	 * @param toDelete is the file or folder you want to delete
	 */
	public static void cleanupTempFile(final File toDelete) {
		Preconditions.checkNotNull(toDelete);
		LOGGER.log(Level.DEBUG, "Deleting file: " + toDelete.getAbsolutePath());
		try {
			Files.deleteRecursively(toDelete.getCanonicalFile());
		} catch (Exception t) {
			throw new MprcException("Could not delete: " + toDelete.getAbsolutePath(), t);
		}
	}

	/**
	 * Get an input stream for a given file, handling exceptions gracefully.
	 *
	 * @param file File to get input stream from.
	 * @return {@link FileInputStream} for the given file.
	 */
	public static FileInputStream getInputStream(final File file) {
		FileInputStream fi = null;
		try {
			fi = new FileInputStream(file);
		} catch (IOException ioe) {
			throw new MprcException("could not open input stream on file=" + file.getAbsolutePath(), ioe);
		}
		return fi;
	}

	/**
	 * Get an output stream for a given file, handling exceptions gracefully.
	 *
	 * @param file File to get input stream from.
	 * @return {@link FileOutputStream} for the given file.
	 */
	public static FileOutputStream getOutputStream(final File file) {
		FileOutputStream fo = null;
		try {
			fo = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			throw new MprcException("could not open output stream on file=" + file.getAbsolutePath(), e);
		}
		return fo;
	}

	public static BufferedReader getReader(final File file) {
		if (file == null) {
			throw new IllegalArgumentException("The file to read from was null.");
		}
		BufferedReader r = null;
		try {
			r = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new MprcException("Cannot read from file " + file.getAbsolutePath(), e);
		}
		return r;
	}

	public static FileWriter getWriter(final File file) {
		if (file == null) {
			throw new IllegalArgumentException("The file to write into was null.");
		}
		FileWriter w = null;
		try {
			w = new FileWriter(file);
		} catch (IOException e) {
			throw new MprcException("Cannot write to file " + file.getAbsolutePath(), e);
		}
		return w;
	}

	/**
	 * Use when you do not care whether a close operation failed. The failures are reported to the log, the method
	 * does not throw an exception.
	 *
	 * @param closeable Object to run the {@link Closeable#close} method on.
	 */
	public static void closeQuietly(final Closeable closeable) {
		if (closeable == null) {
			return;
		}

		try {
			if (closeable instanceof Flushable) {
				((Flushable) closeable).flush();
			}
			closeable.close();
		} catch (IOException e) {
			// SWALLOWED: Warn the user about closing that could not be performed.
			if (closeable instanceof File) {
				LOGGER.warn("Could not close file " + ((File) closeable).getAbsolutePath(), e);
			} else {
				LOGGER.warn("Could not close a " + closeable.getClass().getSimpleName() + " resource.", e);
			}
		}
	}

	/**
	 * Call close() method on object. Method expects Object class to implement close() method or be of the Closeable type.
	 *
	 * @param object Object to call {@code close()} method on.
	 */
	public static void closeObjectQuietly(final Object object) {
		if (object != null) {
			if (object instanceof Closeable) {
				closeQuietly((Closeable) object);
			}

			try {
				final Method closeMethod = object.getClass().getMethod("close");
				closeMethod.invoke(object);
			} catch (Exception e) {
				LOGGER.warn("Could not execute close() method on object of type " + object.getClass().getName(), e);
			}
		}
	}

	/**
	 * Downloads given url into a temporary file and then serves it as an InputStream.
	 * Is is possible to use the URL directly to obtain an input stream, but the URL source does not
	 * have to specify total size of the input. In case the size needs to be known in advance, this is a preferred
	 * method.
	 *
	 * @param url URL to download.
	 * @return Input stream backed by a temporary file containing the URL contents.
	 */
	public static InputStream getStream(final URL url) {
		try {
			final File tmpFile = File.createTempFile("swift", "tmp");
			quietDelete(tmpFile);

			final AsyncFileWriter writer = AsyncFileWriter.writeURLToFile(url, tmpFile, "Download progress: ");
			return new TempFileBackedInputStream(writer.get());
		} catch (Exception t) {
			throw new MprcException("Couldn't retrieve the stream from the given URL: " + url, t);
		}
	}

	/**
	 * This function will obtain current umask setting and set the access rights on the given file
	 * to be at least as permissive as the umask. In other words, if the umask specifies new files should be
	 * group writeable and the supplied file is not, it will be set group + write.
	 * <p/>
	 * The implementation calls unix commands directly, so on other systems than linux-based this function just
	 * does nothing. Same happens when the umask cannot be retrieved or chmod fails - the command just quietly does nothing.
	 *
	 * @param file      File to restore umask rights on.
	 * @param recursive When true, r
	 *                  estore rights recursively for this file and all subfolders.
	 */
	public static void restoreUmaskRights(final File file, final boolean recursive) {
		if (umask == UMASK_UNKNOWN) {
			umask = getCurrentUmask();
		}
		if (umask < 0) {
			// Umask value could not be determined, we cannot work.
			return;
		}
		if (!file.exists()) {
			// Nothing to do for nonexistent files
			return;
		}

		int flags = 0666; // rw-rw-rw- octal is the base from which we subtract the umask

		// Mask our requested flags by the umask
		flags &= ~umask;

		// Apply rights to the file. We only extend the current file rights, never lower them.
		chmod(file, flags, '+', recursive);
	}

	/**
	 * Call chmod on given file.
	 *
	 * @param file      File to change rights for.
	 * @param flags     New set of rights to set.
	 * @param operation - a chmod operation to perform on the flags. E.g. for flags=0500, the operation will have following effect:
	 *                  <dl>
	 *                  <dt>+</dt><dd>chmod u+rx</dd>
	 *                  <dt>-</dt><dd>chmod u-rx</dd>
	 *                  <dt>=</dt><dd>chmod u=rx</dd>
	 *                  </dl>
	 * @param recursive When true, restore the rights recursively for this folder and all its subfolders.
	 */
	public static void chmod(final File file, final int flags, final char operation, final boolean recursive) {
		if (isWindowsPlatform()) {
			LOGGER.warn("Cannot execute chmod on windows for file: " + file.getAbsolutePath());
			return;
		}
		final String chmodString = flagsToChmodString(flags, operation);

		final ProcessBuilder builder;
		if (recursive) {
			builder = new ProcessBuilder("chmod", "-R", chmodString, file.getAbsolutePath());
		} else {
			builder = new ProcessBuilder("chmod", chmodString, file.getAbsolutePath());
		}
		final ProcessCaller caller = new ProcessCaller(builder);
		caller.setLogToConsole(false);
		caller.run();
		if (caller.getExitValue() != 0) {
			LOGGER.info("Could not change permissions:" + caller.getFailedCallDescription());
		}
	}

	/**
	 * Turns given file flags into a chmod command string. Utility for {@link #chmod}.
	 *
	 * @param flags     Flags, e.g. 0775
	 * @param operation Operation to perform - +, - or =
	 * @return chmod string
	 */
	static String flagsToChmodString(final int flags, final char operation) {
		String chmodRights = "";
		if ((flags & 0700) != 0) {
			chmodRights += "u" + operation +
					((flags & 0400) == 0 ? "" : "r") +
					((flags & 0200) == 0 ? "" : "w") +
					((flags & 0100) == 0 ? "" : "x");
		}
		if ((flags & 0070) != 0) {
			if (!chmodRights.isEmpty()) {
				chmodRights += ",";
			}
			chmodRights += "g" + operation +
					((flags & 0040) == 0 ? "" : "r") +
					((flags & 0020) == 0 ? "" : "w") +
					((flags & 0010) == 0 ? "" : "x");
		}
		if ((flags & 0007) != 0) {
			if (!chmodRights.isEmpty()) {
				chmodRights += ",";
			}
			chmodRights += "o" + operation +
					((flags & 0004) == 0 ? "" : "r") +
					((flags & 0002) == 0 ? "" : "w") +
					((flags & 0001) == 0 ? "" : "x");
		}
		return chmodRights;
	}

	private static int umask = UMASK_UNKNOWN;

	/**
	 * Obtains current umask settings.
	 * <p/>
	 * If the settings cannot be obtained, -1 is returned.
	 */
	public static int getCurrentUmask() {
		if (isWindowsPlatform()) {
			// Windows not supported. Everything else is assumed to support 'bash -c umask' and 'chmod'.
			return -1;
		}
		ProcessCaller caller = null;
		try {
			final ProcessBuilder builder = new ProcessBuilder("bash", "-c", "umask");
			caller = new ProcessCaller(builder);
			caller.setLogToConsole(false);
			caller.run();
			if (caller.getExitValue() != 0) {
				throw new MprcException("Umask call failed:" + caller.getFailedCallDescription());
			}
		} catch (Exception t) {
			throw new MprcException("Could not obtain umask value", t);
		}

		String output = caller.getOutputLog();

		try {
			output = output.trim();
			return Integer.parseInt(output, 8);
		} catch (Exception t) {
			LOGGER.warn("Could not parse umask value: " + output, t);
			return -1;
		}
	}


	/**
	 * The user wants to run an executable file. There are three options:
	 * <ol><li>they specify absolute path</li>
	 * <li>they specify relative path</li>
	 * <li>they specify just the executable name, no path</li>
	 * </ol>
	 * The third options causes trouble when calling {@link File#getAbsoluteFile}, because
	 * the name itself is relative not to the current working directory, but to the system path.
	 * <p/>
	 * We detect this case and do not run getAbsoluteFile.
	 *
	 * @param file Executable file to be resolved to absolute path.
	 * @return Absolute path to the executable file (unless only the file name is specified).
	 */
	public static File getAbsoluteFileForExecutables(final File file) {
		if (file.isAbsolute() || file.getParentFile() == null) {
			return file;
		}
		return file.getAbsoluteFile();
	}

	/**
	 * On linux platforms creates a temporary file that links to the original. The purpose is path shortening.
	 * If making the link fails or the platform is not linux, this method MUST throw an exception. Returning the same
	 * file would be dangerous, because caller will try to delete the temporary link!
	 * <p/>
	 * The shortened file path is in temporary directory, in a temporary folder with short name, the file name is retained in full.
	 *
	 * @param file File to create a short link to.
	 * @return The link to the file.
	 * @see #cleanupShortenedPath
	 */
	public static File shortenFilePath(final File file) {
		if (!isLinuxPlatform()) {
			throw new MprcException("File path shortening not supported anywhere but linux.");
		}
		try {
			final File tempFolder = createTempFolder();
			final File tempFile = new File(tempFolder, file.getName());
			createLink(file, tempFile, true);
			return tempFile;
		} catch (Exception t) {
			throw new MprcException("Failed to shorten file path " + file.getAbsolutePath(), t);
		}
	}

	/**
	 * Cleans up the shortened path created by {@link #shortenFilePath}.
	 *
	 * @param file The file whose path was shortened.
	 */
	public static void cleanupShortenedPath(final File file) {
		quietDelete(file);
		quietDelete(file.getParentFile());
	}

	public static String removeFileUrlPrefix(final URI filename) {
		if (filename.getScheme().equals("file")) {
			return filename.getPath();
		}
		return filename.toString();
	}

	/**
	 * Returns canonical path to a given directory. The canonicality means:
	 * <ul>
	 * <li>uses only forward slashes</li>
	 * <li>lowercase/uppercase, ., .. are resolved via {@link FileUtilities#getCanonicalFileNoLinks}.</li>
	 * <li>always ends with a forward slash</li>
	 * </ul>
	 * <p/>
	 *
	 * @param file Directory to get path of.
	 * @return Canonical directory path.
	 */
	public static String canonicalDirectoryPath(final File file) {
		String path;
		try {
			File fileWithTrailingSlash = file;
			if (!file.getPath().endsWith("/")) {
				fileWithTrailingSlash = new File(file.getPath() + "/");
			}
			path = removeFileUrlPrefix(getCanonicalFileNoLinks(fileWithTrailingSlash).toURI());
		} catch (Exception ignore) {
			// SWALLOWED: We failed, try something else
			path = removeFileUrlPrefix(file.getAbsoluteFile().toURI());
		}
		if (path.endsWith("/")) {
			return path;
		}
		return path + "/";
	}

	/**
	 * Since calling {@code System.out.println} is not considered a good practice, we wrap all these calls with this
	 * method to prevent static code analysis from complaning. Also allows us to change this practice in one spot.
	 *
	 * @param s String to print to {@code System.out.println}
	 */
	public static void out(final String s) {
		System.out.println(s);
	}

	/**
	 * Since calling {@code System.err.println} is not considered a good practice, we wrap all these calls with this
	 * method to prevent static code analysis from complaning. Also allows us to change this practice in one spot.
	 *
	 * @param s String to print to {@code System.err.println}
	 */
	public static void err(final String s) {
		System.err.println(s);
	}

	/**
	 * Since calling {@link Exception#printStackTrace()} is considered a bad practice, we wrap all these calls with  this
	 * method to prevent static code analysis from complaning. Also allows us to change this practice in one spot.
	 *
	 * @param t Exception to print to {@code System.err}
	 */
	public static void stackTrace(final Exception t) {
		t.printStackTrace(System.err);
	}

	/**
	 * Lists folder contents, separating the results into directories and files.
	 * On linux, this call is accelerated by doing a native call for "ls" command. This is typically
	 * much faster than querying each file separately about being a folder or a file itself, especially
	 * when using NFS.
	 * <p/>
	 * TODO: Get rid of this once migrated to Java 7.
	 *
	 * @param folder Folder whose contents to list
	 * @param filter Filter to apply to files (not directories).
	 * @param dirs   Resulting directories.
	 * @param files  Resulting files.
	 */
	public static void listFolderContents(final File folder, final FilenameFilter filter, final List<File> dirs, final List<File> files) {
		dirs.clear();
		files.clear();
		final File[] fileEntries = folder.listFiles(new NotHiddenFilter());

		if (fileEntries != null && fileEntries.length > MIN_FOLDER_SIZE_FOR_LS && isLinuxPlatform()) {
			// Speed up on Linux
			// List all files in a folder
			// -p directories have trailing slash
			// -1 single column
			// -L dereference symlinks
			final ProcessBuilder builder =
					new ProcessBuilder("/bin/ls", "-p1L", "--", folder.getAbsolutePath())
							.directory(folder);
			final ProcessCaller caller = new ProcessCaller(builder);
			caller.setLogToConsole(false);
			caller.setKillTimeout(FOLDER_LIST_TIMEOUT);
			caller.setOutputMonitor(new LogMonitor() {
				@Override
				public void line(final String line) {
					if (line.endsWith("/")) {
						dirs.add(new File(folder, line));
					} else if (filter.accept(folder, line)) {
						files.add(new File(folder, line));
					}
				}
			});
			caller.run();
		} else {
			if (fileEntries != null) {
				for (final File file : fileEntries) {
					if (file.isDirectory()) {
						dirs.add(file);
					} else if (filter.accept(folder, file.getName())) {
						files.add(file);
					}
				}
			}
		}
	}

	/**
	 * True if given character can be a delimiter on some system. Currently / and \\
	 *
	 * @param delimiter Delimiter to test
	 * @return True if it is a potential path delimiter
	 */
	public static boolean isKnownPathDelimiter(final char delimiter) {
		return delimiter == '/' || delimiter == '\\';
	}

	/**
	 * Turn absolute Windows paths into unix-friendly equivalent. Keep as-is if not Windows absolute path.
	 * Warning: A canonical version of <c>C:/hello</c> is actually <c>/C:/hello</c>. This needs to be worked out.
	 * <p/>
	 * Example:
	 * c:something -> c/something
	 * D:/test/test -> d/test/test
	 * <p/>
	 * Retaining the drive letter in the path prevents collisions in case multiple disk drives are used on
	 * the Windows host. Removing the colon ensures friendly paths (colon is a special character).
	 *
	 * @param pathToCheck Path to be checked for being a Windows path with drive letter. We assume it is canonical already, so the backslashes are turned to slashes.
	 * @return Cleaned up Windows path.
	 */
	public static String unixizeWindowsAbsolutePath(final String pathToCheck) {
		final String path;
		if (pathToCheck.length() >= 1 && isKnownPathDelimiter(pathToCheck.charAt(0))) {
			// Initial slash. Remove temporarily;
			path = pathToCheck.substring(1);
		} else {
			path = pathToCheck;
		}
		if (path.length() >= 2 && path.charAt(1) == ':') {
			if (path.length() >= 3 && isKnownPathDelimiter(path.charAt(2))) {
				return Character.toLowerCase(path.charAt(0)) + path.substring(2);
			} else {
				return Character.toLowerCase(path.charAt(0)) + "/" + path.substring(2);
			}
		}
		return pathToCheck;
	}

	/**
	 * Get file name by trimming the extension including the .gz (e.g. .fasta or .fasta.gz)
	 *
	 * @param fileName Name of the file.
	 * @return The name without the .ext or .ext.gz
	 */
	public static String stripGzippedExtension(final String fileName) {
		final String extension = getGzippedExtension(fileName);
		return fileName.substring(0, fileName.length() - extension.length() - (/*dot*/!extension.isEmpty() ? 1 : 0));
	}

	/**
	 * Throw an exception if the given file does not exist, is not a file, or is not readable.
	 *
	 * @param fileDescription Description of the file to use in the exception.
	 * @param file            The file to check.
	 */
	public static void ensureReadableFile(String fileDescription, File file) {
		if (file.exists()) {
			if (!file.isFile()) {
				throw new MprcException("The " + fileDescription + " " + file.getAbsolutePath() + " is not an actual file.");
			}
			if (!file.canRead()) {
				throw new MprcException("The " + fileDescription + " " + file.getAbsolutePath() + " is not readable.");
			}
		} else {
			throw new MprcException("The " + fileDescription + " " + file.getAbsolutePath() + " does not exist.");
		}
	}

	/**
	 * Read up to specified amount of characters from a given file into a string.
	 * If the file is larger, read gets truncated.
	 * <p/>
	 * TODO: Optimize this!
	 *
	 * @param file      File to read.
	 * @param charset   Character set to read.
	 * @param maxLength Maximum lenght of a string to read.
	 * @return String that was read from the file.
	 */
	public static String toString(final File file, final Charset charset, final int maxLength) {
		try {
			final String content = Files.toString(file, charset);
			if (content.length() > maxLength) {
				return content.substring(0, maxLength);
			}
			return content;
		} catch (IOException e) {
			throw new MprcException("Could not read file: [" + file.getAbsolutePath() + "]", e);
		}
	}

	public static Long sizeOfDirectory(final File directory) {
		long length = 0;
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				length += file.length();
			} else {
				length += sizeOfDirectory(file);
			}
		}
		return length;
	}

	private static final class TempFileBackedInputStream extends FileInputStream {
		private final File backingFile;

		public TempFileBackedInputStream(final File backingFile) throws FileNotFoundException {
			super(backingFile);
			this.backingFile = backingFile;
		}

		@Override
		public void close() throws IOException {
			quietDelete(backingFile);
			super.close();
		}
	}

	/**
	 * Same as {@link File#getCanonicalFile()} only does not resolve links. The current implementation checks
	 * we are on Windows platform and then calls the original canonicalize, on Linux we do nothing.
	 *
	 * @param file File to make canonical.
	 * @return Canonical file on Windows, do nothing on Linux. Used to resolve upper/lower case problem issues that do not
	 *         manifest themselves on case-sensitive filesystem like Linux.
	 */
	public static File getCanonicalFileNoLinks(final File file) {
		if (file == null) {
			return null;
		}
		if (isWindowsPlatform()) {
			try {
				return file.getAbsoluteFile().getCanonicalFile();
			} catch (Exception t) {
				throw new MprcException("Cannot canonicalize given file " + file.getPath(), t);
			}
		} else {
			return file.getAbsoluteFile();
		}
	}

	/**
	 * Creates a file from given uri. First attempts normal conversion using File constructor. If that fails,
	 * it checks whether the URI is opaque, with schema set to file:, and attempts to create the file
	 * manually from the scheme-specific part.
	 *
	 * @param uri File URI to open
	 * @return File for the given URI
	 */
	public static File fileFromUri(final URI uri) {
		try {
			return new File(uri);
		} catch (Exception t) {
			// It did not work, try to save it...
			if (uri != null && uri.isOpaque()) {
				return new File(uri.getSchemeSpecificPart());
			}
			throw new MprcException("Could not create file from URI [" + (uri == null ? "null" : uri.toString()) + "]", t);
		}
	}

	/**
	 * Returns log file directory with the following directory hierarchy.
	 * <p/>
	 * {@code rootLogFolder/year/month/day}
	 *
	 * @param parentDirectory Parent directory to create the date-based directory in
	 * @param date            Directory hierarchy is created out of this Date object. If null, current time is used.
	 * @return The newly created year/month/day directory.
	 */
	public static File getDateBasedDirectory(final File parentDirectory, final Date date) {
		final Date myDate;
		if (date == null) {
			myDate = new Date();
		} else {
			myDate = date;
		}

		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(myDate);

		if (parentDirectory != null && parentDirectory.exists() && parentDirectory.isDirectory()) {
			final File folder = new File(new File(new File(parentDirectory, Integer.toString(calendar.get(Calendar.YEAR))), Integer.toString(calendar.get(Calendar.MONTH) + 1)), Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));

			ensureFolderExists(folder);

			return folder;
		}

		throw new MprcException("Parent folder must exist. Folder [" + parentDirectory + "] does not exist or may not be a folder.");
	}

	/**
	 * Sets last modification time of a file, checking for exceptions and the return value.
	 *
	 * @param file File to set the modified time to
	 * @param time Time (obtain using e.g. {@link Date#getTime()}
	 */
	public static void setLastModified(final File file, final long time) {
		if (file == null) {
			throw new MprcException("File to set last modified time was null");
		}
		boolean success = false;
		try {
			success = file.setLastModified(time);
		} catch (Exception t) {
			throw new MprcException("Failed to change last modification time for file [" + file.getAbsolutePath() + "]", t);
		}
		if (!success) {
			throw new MprcException("Failed to change last modification time for file [" + file.getAbsolutePath() + "]");
		}
	}

	private static final class FileExists implements Predicate<File> {
		private FileExists() {
		}

		@Override
		public boolean apply(@Nullable final File input) {
			return input == null ? null : input.exists();
		}
	}

	public static class AbsolutePath implements Function<File, String> {

		@Override
		public String apply(File from) {
			return from == null ? null : from.getAbsolutePath();
		}
	}
}
