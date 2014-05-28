package edu.mayo.mprc.daemon;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.StringUtilities;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Abstraction of the cache folder storage.
 * <p/>
 * <ul>
 * <li>{@link #lookup} can lookup whether a packet is already within the cache </li>
 * <li>{@link #makeWorkFolder} will create a work folder for new packets</li>
 * <li>{@link #insert} will take the work folder and make it a proper member of the cache</li>
 * </ul>
 * Warning - always use {@link #makeWorkFolder} and {@link #insert} in tandem. The work folder is created in such
 * way for {@code insert} to work properly. They do not work independently.
 *
 * @author Roman Zenka
 */
public final class CacheFolder {
	private static final Logger LOGGER = Logger.getLogger(CacheFolder.class);

	private File cacheFolder;

	// The file that will store the task input parameters
	private static final String TASK_DESCRIPTION_FILE_NAME = "_task_description";
	private static final String TASK_DESCRIPTION_FILE_NAME_TEMP = "_task_description~";

	private static final Pattern CACHE_DATA_FOLDER = Pattern.compile("[0-9a-f][0-9a-f]");

	public static final int MAX_CACHE_FOLDERS = 1000 * 10;

	public CacheFolder() {
	}

	public File getCacheFolder() {
		return cacheFolder;
	}

	public void setCacheFolder(final File cacheFolder) {
		this.cacheFolder = cacheFolder;
	}

	/**
	 * @return Error message if something is wrong with the folder, null otherwise.
	 */
	public String check() {
		if (!cacheFolder.isDirectory() || !cacheFolder.canWrite()) {
			return "The cache folder is not writeable: " + cacheFolder.getAbsolutePath();
		}
		return null;
	}

	/**
	 * Get the cache ready to work.
	 */
	public void install() {
		FileUtilities.ensureFolderExists(cacheFolder);
	}

	private File getTargetFolder(final String taskDescription) {
		final String hash = hashTaskDescription(taskDescription);
		return new File(cacheFolder, hash);
	}

	/**
	 * By default returns folder based on the hash code of the task description.
	 * Your cache can override this implementation.
	 *
	 * @param taskDescription Description of the task.
	 * @return Relative path to the cache folder to store the task results in.
	 */
	protected String hashTaskDescription(final String taskDescription) {
		final int code = taskDescription.hashCode();
		return "" +
				StringUtilities.toHex(code >> 28) +
				StringUtilities.toHex(code >> 24) +
				"/" +
				StringUtilities.toHex(code >> 20) +
				StringUtilities.toHex(code >> 16) +
				"/" +
				StringUtilities.toHex(code >> 12) +
				StringUtilities.toHex(code >> 8) +
				"/" +
				StringUtilities.toHex(code >> 4) +
				StringUtilities.toHex(code);
	}

	/**
	 * For a given work packet, find if there is a cache folder that has data for this packet.
	 * <p/>
	 * While doing so, delete all matching, yet stale cache entries encountered. If packet is from scratch,
	 * delete all entries, including the non-stale ones.
	 *
	 * @param cachableWorkPacket Work packet to find folder with data for.
	 * @return The folder containing all the cached work data stored for the work package or {@code null}
	 * if no such folder exists.
	 */
	public File lookup(final CachableWorkPacket cachableWorkPacket) {
		// Now we check the cache.
		// There can be multiple files of the same name in the same cache bucket.
		// We pick the one which has a corresponding file that matches our params
		// We go through all subfolders of the output folder
		final String taskDescription = cachableWorkPacket.getStringDescriptionOfTask();
		final File targetCacheFolder = getTargetFolder(taskDescription);
		if (!targetCacheFolder.exists()) {
			return null;
		}
		if (!targetCacheFolder.isDirectory()) {
			throw new MprcException(MessageFormat.format("Malformed cache. The file {0} is not a directory.", targetCacheFolder.getAbsolutePath()));
		}
		final File[] files = targetCacheFolder.listFiles();
		final List<String> outputFiles = cachableWorkPacket.getOutputFiles();
		for (final File subFolder : files) {
			final File taskDescriptionFile = new File(subFolder, TASK_DESCRIPTION_FILE_NAME);
			if (allFilesExist(subFolder, outputFiles) && taskDescriptionFile.exists()) {
				// We found an output file with matching file name and a params file!
				// Check the params file
				String cachedTaskDescription = null;
				try {
					cachedTaskDescription = Files.toString(taskDescriptionFile, Charsets.UTF_8);
				} catch (final Exception t) {
					LOGGER.error("Cache cannot read request file " + taskDescriptionFile.getAbsolutePath(), t);
					continue;
				}
				if (taskDescription.equals(cachedTaskDescription)) {
					// We found a match. Shall we use it? We must not want to process from scratch, and we must not
					// have stale cache entry.
					if (!cachableWorkPacket.isFromScratch() && !cachableWorkPacket.cacheIsStale(subFolder, outputFiles)) {
						return subFolder;
					} else {
						// The output is older than the source.
						// Wipe the cache for the file, continue searching.
						LOGGER.info("Cache deleting stale entry " +
								(cachableWorkPacket.isFromScratch() ? "(user requested rerun from scratch)" : "(input is of newer date than the output)") + ": " + subFolder.getAbsolutePath());
						FileUtilities.deleteNow(subFolder);
					}
				}
			}
		}
		// No match
		return null;
	}

	/**
	 * Prepare a work folder for packet that should have data stored in the cache.
	 *
	 * @param cachableWorkPacket Packet that is to be processed.
	 * @return Folder where the packet results should go.
	 */
	public File makeWorkFolder(final CachableWorkPacket cachableWorkPacket) {
		final String taskDescription = cachableWorkPacket.getStringDescriptionOfTask();
		final File targetCacheFolder = getTargetFolder(taskDescription);

		// We make sure the target folder can be created - fail early
		FileUtilities.ensureFolderExists(targetCacheFolder);

		// Now we need to make a new folder within the target one
		// More than one task can theoretically get the same hash
		File workFolder = createNewFolder(targetCacheFolder);

		// We store the task description, so it is ready for publishing by moving the file name
		FileUtilities.writeStringToFile(new File(workFolder, TASK_DESCRIPTION_FILE_NAME_TEMP), taskDescription, true);

		return workFolder;
	}

	/**
	 * Create new folder within parent. The folder name is simply a number, starting at 1.
	 *
	 * @param parent Parent folder to create a new folder in.
	 * @return The new folder created.
	 */
	private File createNewFolder(final File parent) {
		final int numFiles = parent.listFiles().length;
		if (numFiles > MAX_CACHE_FOLDERS) {
			throw new MprcException("Too many cached folders in " + parent.getAbsolutePath() + ": " + numFiles);
		}
		if (!parent.canWrite()) {
			throw new MprcException("The cache directory [" + parent.getAbsolutePath() + "] is not writeable.");
		}
		int i = numFiles + 1;
		while (true) {
			final File newFolder = new File(parent, String.valueOf(i));
			if (!newFolder.exists()) {
				FileUtilities.ensureFolderExists(newFolder);
				break;
			}
			i += 1;
			if (i > MAX_CACHE_FOLDERS) {
				throw new MprcException("Could not create a new folder in " + parent.getAbsolutePath() + ": " + i + " attempts failed");
			}
		}
		return new File(parent, String.valueOf(i));
	}


	/**
	 * Insert a new entry into the cache.
	 *
	 * @param workPacket The work packet that successfully finished processing.
	 * @param wipFolder  The work in progress folder where the work packet's results are.
	 * @return The final folder location where the packet results are.
	 */
	public File insert(final CachableWorkPacket workPacket, final File wipFolder) {
		final List<String> outputFiles = workPacket.getOutputFiles();

		final File tempTaskDescriptionFile = new File(wipFolder, TASK_DESCRIPTION_FILE_NAME_TEMP);
		if (!tempTaskDescriptionFile.exists()) {
			throw new MprcException("The temp task description file does not exist. Likely programmer error - the insert function must operate on work folder returned by makeWorkFolder");
		}

		// Since our work folder is at the final location (the workers are expected to publish their
		// files safely), our work consists only of writing out the task description file, which finalizes
		// the entire process and marks the cache to be ready to use.

		// Sanity-check all files are in place
		for (final String outputFile : outputFiles) {
			final File resultingOutputFile = new File(wipFolder, outputFile);

			LOGGER.info("Caching output file: " + resultingOutputFile.getAbsolutePath());

			if (!resultingOutputFile.exists()) {
				throw new MprcException(MessageFormat.format("The cached output file {0} did not exist. " +
						"Programmer error in worker for packet {1} which signalized successful operation, yet failed to deliver expected files.", resultingOutputFile.getName(), workPacket.getClass().getName()));
			}
		}

		// Publishing the task description file finalizes the caching process and enables the data to be used
		// subsequently.
		FileUtilities.rename(tempTaskDescriptionFile,
				new File(wipFolder, TASK_DESCRIPTION_FILE_NAME));

		return wipFolder;
	}

	/**
	 * Wipe the cache completely.
	 */
	public void cleanup() {
		final File[] files = getCacheFolder().listFiles(new CacheFolderFilter());
		for (final File file : files) {
			FileUtilities.deleteNow(file);
		}
	}

	private static class CacheFolderFilter implements FilenameFilter {
		@Override
		public boolean accept(final File dir, final String name) {
			return CACHE_DATA_FOLDER.matcher(name).matches();
		}
	}

	/**
	 * @param folder      Base folder
	 * @param outputFiles List of file names to check
	 * @return True if all files exist.
	 */
	private boolean allFilesExist(final File folder, final List<String> outputFiles) {
		for (final String file : outputFiles) {
			if (!new File(folder, file).exists()) {
				return false;
			}
		}
		return true;
	}
}
