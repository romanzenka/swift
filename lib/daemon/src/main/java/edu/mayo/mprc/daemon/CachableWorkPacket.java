package edu.mayo.mprc.daemon;

import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.io.File;
import java.util.List;

/**
 * A work packet whose results can be cached.
 * <ul>
 * <li>
 * Can describe itself using a unique string that can then be hashed
 * using the cache.
 * <li>
 * Can translate itself into another work packet that will produce results into the cache folder.
 * <li>
 * Can provide a timestamp determining at least how new must the cached result be to be useful.
 * <li>
 * Given a cache folder, it can simulate the execution of the worker and report the results as if they were just made.
 * <li>
 * It knows whether the user demands the resulting files to be placed at the original location, or whether they are okay
 * with running from the cache folders. It provides a main output file whose parent folder is where all the result files
 * will be copied to by the cache.
 * </ul>
 */
public interface CachableWorkPacket extends WorkPacket {
	/**
	 * @return True if the results of this work packet should be made publicly available to the user
	 *         (not just stored in the intermediate result cache).
	 */
	boolean isPublishResultFiles();

	/**
	 * @return The main resulting output file. Its parent folder is the destination to put all the cached results
	 *         to in case the user asks for {@link #isPublishResultFiles()}
	 */
	File getOutputFile();

	/**
	 * For given work packet, obtain a string that describes the work to be done.
	 * When two of these strings are identical, the two tasks are identical and their results
	 * are fully interchangeable.
	 */
	String getStringDescriptionOfTask();

	/**
	 * The original work packet requested files somewhere.
	 * Translate the work packet to one that requests the files to be put into the work in progress folder of the cache.
	 *
	 * @param wipFolder The folder where the work-in-progress copy should go
	 * @return New work packet that puts files into the WIP folder of the cache
	 */
	WorkPacket translateToWorkInProgressPacket(File wipFolder);

	/**
	 * @return A list of all output files that are expected to be a result of processing this work packet. Just names, no paths.
	 */
	List<String> getOutputFiles();

	/**
	 * @param subFolder   Cache subfolder with output files.
	 * @param outputFiles List of output files as previously generated.
	 * @return True if cached output in a particular folder is stale (older than the input files)
	 */
	boolean cacheIsStale(File subFolder, List<String> outputFiles);

	/**
	 * Report that the original work packet got processed and its resulting files are in a given folder.
	 *
	 * @param reporter     The object to report the result on.
	 * @param targetFolder Folder with the output files (cache)
	 * @param outputFiles  List of the output files (previously obtained by {@link #getOutputFiles()}/
	 */
	void reportCachedResult(ProgressReporter reporter, File targetFolder, List<String> outputFiles);
}
