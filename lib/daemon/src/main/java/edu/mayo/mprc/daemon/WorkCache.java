package edu.mayo.mprc.daemon;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.worker.NoLoggingWorker;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.utilities.FileListener;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.MonitorUtilities;
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
public abstract class WorkCache<T extends WorkPacket> implements NoLoggingWorker, Installable {
	private static final Logger LOGGER = Logger.getLogger(WorkCache.class);
	private DaemonConnection daemon;
	private final Map<String, CacheProgressReporter> workInProgress = new HashMap<String, CacheProgressReporter>(10);
	private final CacheFolder cacheFolder = new CacheFolder();

	public WorkCache() {
	}

	public final File getCacheFolder() {
		return cacheFolder.getCacheFolder();
	}

	public final void setCacheFolder(final File cacheFolder) {
		this.cacheFolder.setCacheFolder(cacheFolder);
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

	/**
	 * Strip extension from input file, replace with extension from output file.
	 * <p/>
	 * The result is the file name of the output file that is canonical in a sense.
	 */
	public static String getCanonicalOutput(final File inputFile, final File outputFile) {
		return getCanonicalOutput(inputFile, outputFile, FileUtilities.NO_EXTENSIONS);
	}

	/**
	 * Same as {@link #getCanonicalOutput(java.io.File, java.io.File)} only it also considers
	 * a provided list of multi-part extensions.
	 *
	 * @param inputFile  File to use for the name itself.
	 * @param outputFile File to get the extension from.
	 * @param extensions List of multi-part extensions to strip from both input and output.
	 * @return Output file created by using the input file name and output file extension.
	 */
	public static String getCanonicalOutput(final File inputFile, final File outputFile, final String[] extensions) {
		return
				FileUtilities.stripGzippedExtension(inputFile.getName(), extensions)
						+ "."
						+ FileUtilities.getGzippedExtension(outputFile.getName(), extensions);

	}

	@Override
	public final void processRequest(final WorkPacket workPacket, final ProgressReporter progressReporter) {
		try {
			process(workPacket, progressReporter);
		} catch (final Exception t) {
			progressReporter.reportFailure(t);
		}
	}

	@Override
	public String check() {
		LOGGER.info("Checking cache for " + getDaemon().getConnectionName());
		return cacheFolder.check();
	}

	@Override
	public void install(final Map<String, String> params) {
		LOGGER.info("Installing cache for " + getDaemon().getConnectionName());
		cacheFolder.install();
	}

	private void process(final WorkPacket workPacket, final ProgressReporter progressReporter) {
		if (workPacket instanceof CleanupCacheWorkPacket) {
			cleanupCache();
			return;
		}

		final CachableWorkPacket originalPacket;
		final CachableWorkPacket lookupPacket;
		if (workPacket instanceof CachableWorkPacket) {
			// We translate the output files in our work packet to files going against a dummy folder
			// This will allow us to look up a matching cache entry, instead of looking for outputs that our users want
			originalPacket = (CachableWorkPacket) workPacket;
			lookupPacket = (CachableWorkPacket) originalPacket.translateToCachePacket(new File("."));
		} else {
			ExceptionUtilities.throwCastException(workPacket, CachableWorkPacket.class);
			return;
		}

		// Find existing cache subfolder with data matching the work packet.
		final File existingEntry = cacheFolder.lookup(lookupPacket);

		if (existingEntry != null) {
			reportCachedValues(progressReporter, originalPacket, lookupPacket, existingEntry);
			return;
		}

		// A string describing the request. If two descriptions are the same,
		// the tasks are the same
		final String taskDescription = originalPacket.getStringDescriptionOfTask();

		// We have not found a suitable cache entry
		// But maybe someone is working on this exact task right now
		final CacheProgressReporter cacheProgressReporter;
		final CacheProgressReporter newReporter = new CacheProgressReporter();
		synchronized (workInProgress) {
			cacheProgressReporter = workInProgress.get(taskDescription);
			if (cacheProgressReporter == null) {
				// Nobody is working yet, register the new reporter
				workInProgress.put(taskDescription, newReporter);
				newReporter.addProgressReporter(progressReporter);
			} else {
				// We already are doing work. Register the new caller and quit
				cacheProgressReporter.addProgressReporter(progressReporter);
				return;
			}
		}

		// Nobody is doing this work, we need to do it ourselves
		// First establish a work-in-progress folder for this operation
		// The deal is to put all the output files there, and finally create the task description file to mark the cache as completed

		// Make a work-in-progress folder
		final File wipFolder = cacheFolder.makeWorkFolder(originalPacket);

		final WorkPacket modifiedWorkPacket = originalPacket.translateToCachePacket(wipFolder);
		//  Our new packet will bear ID of our current log. That would ensure that its child logs
		// will be properly hooked as children of our task.
		modifiedWorkPacket.setTaskId(progressReporter.getLog().getLogId());

		final ProgressListener listener = new MyProgressListener(lookupPacket, originalPacket, wipFolder, newReporter);
		daemon.sendWork(modifiedWorkPacket, listener);
	}

	/**
	 * We found a non-stale cache folder with all the files in it.
	 * Publish this finding directly to the caller.
	 */
	private void reportCachedValues(final ProgressReporter progressReporter,
	                                final CachableWorkPacket originalPacket,
	                                final CachableWorkPacket cachePacket,
	                                final File cacheFolder) {
		final List<String> cachedFiles = cachePacket.getOutputFiles();

		// The output was created after our input file, thus it is useable
		LOGGER.info("Using cached values from: " + cacheFolder.getAbsolutePath());
		progressReporter.reportStart(MonitorUtilities.getHostInformation());
		originalPacket.reportCachedResult(progressReporter, cacheFolder, cachedFiles);
		publishResultFiles(cacheFolder, cachedFiles, originalPacket);
		progressReporter.reportSuccess();
	}

	/**
	 * Delete everything in the cache.
	 */
	private void cleanupCache() {
		cacheFolder.cleanup();
	}

	/**
	 * Checks whether the work packet requested publishing the intermediate files.
	 * If so, copy the intermediate files to the originally requested target.
	 */
	private void publishResultFiles(final File cacheFolder, final List<String> cacheOutputFiles, final CachableWorkPacket originalWorkPacket) {
		final List<String> originalOutputs = originalWorkPacket.getOutputFiles();

		if (originalWorkPacket.isPublishResultFiles()) {
			final File targetFolder = originalWorkPacket.getOutputFile().getParentFile();
			FileUtilities.ensureFolderExists(targetFolder);
			for (int i = 0; i < originalOutputs.size(); i++) {
				final File fromFile = new File(cacheFolder, cacheOutputFiles.get(i));
				final File toFile = new File(targetFolder, originalOutputs.get(i));
				FileUtilities.copyFile(fromFile, toFile, true);
			}
		}
	}

	/**
	 * For test writing purposes. Access to class internals.
	 */
	boolean isWorkInProgress() {
		return !workInProgress.isEmpty();
	}

	private void finishedWorking(final String taskDescription) {
		synchronized (workInProgress) {
			workInProgress.remove(taskDescription);
		}
	}

	private final class MyProgressListener implements ProgressListener {
		private final CachableWorkPacket workPacket;
		private final CachableWorkPacket originalPacket;
		private final File wipFolder;
		private final ProgressReporter reporter;
		private final String taskDescription;

		private MyProgressListener(final CachableWorkPacket workPacket, final CachableWorkPacket originalPacket, final File wipFolder, final ProgressReporter reporter) {
			this.workPacket = workPacket;
			this.originalPacket = originalPacket;
			this.wipFolder = wipFolder;
			this.reporter = reporter;
			taskDescription = workPacket.getStringDescriptionOfTask();
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

			final List<String> outputFiles = workPacket.getOutputFiles();
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

						File target = cacheFolder.insert(workPacket, wipFolder);

						// Now we only need to notify the requestor that the output file was produced elsewhere
						workPacket.reportCachedResult(reporter, target, outputFiles);
						publishResultFiles(target, outputFiles, originalPacket);
					} catch (final Exception t) {
						reporter.reportFailure(t);
						return;
					} finally {
						finishedWorking(taskDescription);
					}
					reporter.reportSuccess();
				}
			});
		}

		@Override
		public void requestTerminated(final Exception e) {
			try {
				// The work in progress folder can be scratched
				FileUtilities.deleteNow(wipFolder);
				reporter.reportFailure(e);
			} finally {
				finishedWorking(taskDescription);
			}
		}

		@Override
		public void userProgressInformation(final ProgressInfo progressInfo) {
			// When we get progress info about
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
