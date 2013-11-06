package edu.mayo.mprc.daemon;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.monitor.MonitorUtilities;
import edu.mayo.mprc.utilities.FileListener;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.StringUtilities;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Base class for implementing caches. A cache remembers previous work and can provide results fast.
 * To do so, it needs to store a file describing the previous task.
 * <p/>
 * The cache understands {@link CachableWorkPacket#isPublishResultFiles()} - if this
 * feature is enabled, the cache will copy the cached result to the originally requested target directory.
 * <p/>
 * When the cache detects a particular work is currently being performed, the new request is queued and response duplicated
 * from the currently running task. E.g. two users running the same search will both
 * have an illusion that a separate search runs for them, although only one search is running.
 *
 * @param <T>
 */
public abstract class WorkCache<T extends WorkPacket> implements NoLoggingWorker {
	private static final Logger LOGGER = Logger.getLogger(WorkCache.class);
	public static final int MAX_CACHE_FOLDERS = 1000 * 10;
	private File cacheFolder;
	private DaemonConnection daemon;
	private final Map<String, CacheProgressReporter> workInProgress = new HashMap<String, CacheProgressReporter>(10);

	public WorkCache() {
	}

	public final File getCacheFolder() {
		return cacheFolder;
	}

	public final void setCacheFolder(final File cacheFolder) {
		this.cacheFolder = cacheFolder;
	}

	public final DaemonConnection getDaemon() {
		return daemon;
	}

	public final void setDaemon(final DaemonConnection daemon) {
		this.daemon = daemon;
	}

	public void userProgressInformation(final File wipFolder, final ProgressInfo progressInfo) {
		// Do nothing
	}

	@Override
	public final void processRequest(final WorkPacket workPacket, final ProgressReporter progressReporter) {
		try {
			process(workPacket, progressReporter);
			workPacket.synchronizeFileTokensOnReceiver();
		} catch (Exception t) {
			progressReporter.reportFailure(t);
		}
	}

	@Override
	public void check() {
		if (!cacheFolder.isDirectory() || !cacheFolder.canWrite()) {
			throw new MprcException("The cache folder is not writeable: " + cacheFolder.getAbsolutePath());
		}
	}

	/**
	 * By default returns folder based on the hash code of the task description.
	 * Your cache can override this implementation.
	 *
	 * @param taskDescription Description of the task.
	 * @return Relative path to the cache folder to store the task results in.
	 */
	protected String getFolderForTaskDescription(final String taskDescription) {
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

	private void process(final WorkPacket workPacket, final ProgressReporter progressReporter) {
		final T typedWorkPacket = (T) workPacket;
		final CachableWorkPacket cachableWorkPacket;
		if (workPacket instanceof CachableWorkPacket) {
			cachableWorkPacket = (CachableWorkPacket) workPacket;
		} else {
			ExceptionUtilities.throwCastException(workPacket, CachableWorkPacket.class);
			return;
		}

		// A string describing the request. If two descriptions are the same,
		// the tasks are the same
		final String taskDescription = cachableWorkPacket.getStringDescriptionOfTask();

		// The file that will store the task input parameters
		final String taskDescriptionFileName = "_task_description";

		// Obtain a list of files we expect as a result of this task
		final List<String> outputFiles = cachableWorkPacket.getOutputFiles();

		// Request hashcode
		// Folder is derived from the hash code
		final File targetCacheFolder = new File(cacheFolder, getFolderForTaskDescription(taskDescription));
		// We make sure the target folder can be created - fail early
		FileUtilities.ensureFolderExists(targetCacheFolder);

		// Now we check the cache.
		// There can be multiple files of the same name in the same cache bucket.
		// We pick the one which has a corresponding file that matches our params
		// We go through all subfolders of the output folder
		final File[] files = targetCacheFolder.listFiles();
		for (final File subFolder : files) {
			final File taskDescriptionFile = new File(subFolder, taskDescriptionFileName);
			if (allFilesExist(subFolder, outputFiles) && taskDescriptionFile.exists()) {
				// We found an output file with matching file name and a params file!
				// Check the params file
				String cachedTaskDescription = null;
				try {
					cachedTaskDescription = Files.toString(taskDescriptionFile, Charsets.UTF_8);
				} catch (Exception t) {
					LOGGER.error("Cache cannot read request file " + taskDescriptionFile.getAbsolutePath(), t);
					continue;
				}
				if (taskDescription.equals(cachedTaskDescription)) {
					// We found a match. Shall we use it? We must not want to process from scratch, and we must not
					// have stale cache entry.
					if (!typedWorkPacket.isFromScratch() && !cachableWorkPacket.cacheIsStale(subFolder, outputFiles)) {
						// The output was created after our input file, thus it is useable
						LOGGER.info("Using cached values from: " + subFolder.getAbsolutePath());
						progressReporter.reportStart(MonitorUtilities.getHostInformation());
						cachableWorkPacket.reportCachedResult(progressReporter, subFolder, outputFiles);
						publishResultFiles(cachableWorkPacket, subFolder, outputFiles);
						progressReporter.reportSuccess();
						return;
					} else {
						// The output is older than the source.
						// Wipe the cache for the file, continue searching.
						LOGGER.info("Cache deleting stale entry " +
								(typedWorkPacket.isFromScratch() ? "(user requested rerun from scratch)" : "(input is of newer date than the output)") + ": " + subFolder.getAbsolutePath());
						FileUtilities.deleteNow(subFolder);
					}
				}
			}
		}

		// We have not found a suitable cache entry
		// But maybe someone is working on this right now
		final CacheProgressReporter cacheProgressReporter;
		final CacheProgressReporter newReporter = new CacheProgressReporter();
		synchronized (workInProgress) {
			cacheProgressReporter = workInProgress.get(taskDescription);
			if (cacheProgressReporter == null) {
				workInProgress.put(taskDescription, newReporter);
				newReporter.addProgressReporter(progressReporter);
			} else {
				cacheProgressReporter.addProgressReporter(progressReporter);
			}
		}

		if (cacheProgressReporter == null) {
			// Make a work-in-progress folder
			final File wipBase = new File(cacheFolder, "wip");
			final File wipFolder = FileUtilities.createTempFolder(wipBase, "wip", true);

			final WorkPacket modifiedWorkPacket = cachableWorkPacket.translateToWorkInProgressPacket(wipFolder);

			final MyProgressListener listener = new MyProgressListener(cachableWorkPacket, wipFolder, targetCacheFolder, outputFiles, taskDescriptionFileName, taskDescription, newReporter);
			daemon.sendWork(modifiedWorkPacket, listener);
		}
	}

	/**
	 * Checks whether the work packet requested publishing the intermediate files.
	 * If so, copy the intermediate files to the originally requested target.
	 */
	private void publishResultFiles(final CachableWorkPacket workPacket, final File outputFolder, final List<String> outputFiles) {
		if (workPacket.isPublishResultFiles()) {
			final File targetFolder = workPacket.getOutputFile().getParentFile();
			FileUtilities.ensureFolderExists(targetFolder);
			for (final String outputFile : outputFiles) {
				FileUtilities.copyFile(new File(outputFolder, outputFile), new File(targetFolder, outputFile), true);
			}
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

	/**
	 * For test writing purposes. Access to class internals.
	 */
	boolean isWorkInProgress() {
		return !workInProgress.isEmpty();
	}

	private final class MyProgressListener implements ProgressListener {
		private final CachableWorkPacket workPacket;
		private final File wipFolder;
		private final List<String> outputFiles;
		private final String taskDescriptionFile;
		private final String taskDescription;
		private final File targetFolder;
		private final ProgressReporter reporter;

		private MyProgressListener(final CachableWorkPacket workPacket, final File wipFolder, final File targetCacheFolder, final List<String> outputFiles, final String taskDescriptionFile, final String taskDescription, final ProgressReporter reporter) {
			this.workPacket = workPacket;
			this.wipFolder = wipFolder;
			targetFolder = targetCacheFolder;
			this.outputFiles = outputFiles;
			this.taskDescriptionFile = taskDescriptionFile;
			this.taskDescription = taskDescription;
			this.reporter = reporter;
		}

		@Override
		public void requestEnqueued(final String hostString) {
		}

		@Override
		public void requestProcessingStarted(final String hostString) {
			reporter.reportStart(hostString);
		}

		@Override
		public void requestProcessingFinished() {
			final File target = createNewFolder(targetFolder);
			final ArrayList<File> toWaitFor = new ArrayList<File>(outputFiles.size());
			for (final String outputFile : outputFiles) {
				final File wipFile = new File(wipFolder, outputFile);
				toWaitFor.add(wipFile);
			}

			FileUtilities.waitForFiles(toWaitFor, new FileListener() {
				@Override
				public void fileChanged(final Collection<File> files, final boolean timeout) {
					// This is called from a different thread
					try {
						if (timeout) {
							reporter.reportFailure(new MprcException("Timeout when waiting for file collection: [" + Joiner.on(", ").join(files) + "]"));
							return;
						}

						for (final String outputFile : outputFiles) {
							// Move the work in progress folder to its final location
							final File wipFile = new File(wipFolder, outputFile);
							final File resultingOutputFile = new File(target, outputFile);

							LOGGER.info("Caching output file: " + resultingOutputFile.getAbsolutePath());

							// We move the output file
							FileUtilities.rename(wipFile, resultingOutputFile);
						}

						// We write out the parameters used for creating the output file
						FileUtilities.writeStringToFile(new File(target, taskDescriptionFile), taskDescription, true);
						// And the wip folder is no longer needed
						FileUtilities.deleteNow(wipFolder);

						// Now we only need to notify the requestor that the output file was produced elsewhere
						workPacket.reportCachedResult(reporter, target, outputFiles);
						publishResultFiles(workPacket, target, outputFiles);
					} catch (Exception t) {
						reporter.reportFailure(t);
						return;
					} finally {
						synchronized (workInProgress) {
							workInProgress.remove(taskDescription);
						}
					}
					reporter.reportSuccess();
				}
			});
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

		@Override
		public void requestTerminated(final Exception e) {
			try {
				// The work in progress folder can be scratched
				FileUtilities.deleteNow(wipFolder);
				reporter.reportFailure(e);
			} finally {
				synchronized (workInProgress) {
					workInProgress.remove(taskDescription);
				}
			}
		}

		@Override
		public void userProgressInformation(final ProgressInfo progressInfo) {
			// Let the cache know what happened
			WorkCache.this.userProgressInformation(wipFolder, progressInfo);
			reporter.reportProgress(progressInfo);
		}
	}

	/**
	 * The cache factory creates singletons - there is only one cache for all requests, unlike the workers
	 * that are created for each task separately.
	 *
	 * @param <S> Configuration the cache takes to set itself up.
	 */
	public abstract static class Factory<S extends CacheConfig> extends WorkerFactoryBase<S> {
		private WorkCache cache;

		@Override
		public synchronized Worker create(final S config, final DependencyResolver dependencies) {
			if (cache == null) {
				cache = createCache(config, dependencies);
				cache.setCacheFolder(new File(config.getCacheFolder()).getAbsoluteFile());
				cache.setDaemon((DaemonConnection) dependencies.createSingleton(config.getService()));
			}
			return cache;
		}

		public abstract WorkCache createCache(S config, DependencyResolver dependencies);
	}

	/**
	 * Generic work cache config. A work cache knows its folder and the service whose output it is caching.
	 */
	public static class CacheConfig implements ResourceConfig {
		public static final String CACHE_FOLDER = "cacheFolder";
		public static final String SERVICE = "service";
		private String cacheFolder;
		private ServiceConfig service;

		public void setService(final ServiceConfig service) {
			this.service = service;
		}

		public void setCacheFolder(final String cacheFolder) {
			this.cacheFolder = cacheFolder;
		}

		public String getCacheFolder() {
			return cacheFolder;
		}

		public ServiceConfig getService() {
			return service;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(CACHE_FOLDER, getCacheFolder(), "Where to cache files");
			writer.put(SERVICE, writer.save(getService()), "Service being cached");
		}

		@Override
		public void load(final ConfigReader reader) {
			cacheFolder = reader.get(CACHE_FOLDER);
			service = (ServiceConfig) reader.getObject(SERVICE);
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}
}
