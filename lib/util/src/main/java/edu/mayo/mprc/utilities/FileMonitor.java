package edu.mayo.mprc.utilities;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Monitors disk files for changes.
 * <ul>
 * <li>A file is considered changed if its last modification date changes (even if contents stay identical)</li>
 * <li>Deleting the file does not fire the change event.</li>
 * <li>Creating a previously nonexistent file fires the change event, unless the file is recreated with identical modification time it had before.</li>
 * </ul>
 * <p/>
 * Usage:
 * <ul>
 * <li>Implement FileListener interface</li>
 * <li>Create new instance of file monitor.</li>
 * <li>Add files/directories to watch</li>
 * </ul>
 */
public final class FileMonitor {
	private static final Logger LOGGER = Logger.getLogger(FileMonitor.class);

	private Timer timer;
	private final List<FileInfo> files;
	private final Object lock = new Object();
	private final long pollingInterval;

	/**
	 * Create a file monitor instance with specified polling interval.
	 *
	 * @param pollingInterval Polling interval in milliseconds.
	 */
	public FileMonitor(final long pollingInterval) {
		this.pollingInterval = pollingInterval;
		synchronized (lock) {
			files = new ArrayList<FileInfo>();
		}
	}

	private void startTimer() {
		if (timer == null) {
			timer = new Timer(true);
			timer.schedule(new FileMonitorNotifier(), 0, pollingInterval);
		}
	}


	/**
	 * Stop the file monitor polling.
	 */
	public void stop() {
		timer.cancel();
	}


	/**
	 * Add a list of files - once all of these exist, the listener gets notified.
	 *
	 * @param filesToMonitor A collection of files. All of these have to change, for the listener to be notified.
	 * @param listener       Listener to call when all the files change
	 * @param expireInMillis After so many milliseconds, the listener gets notified no matter what and the monitoring stops.
	 */
	public void filesToExist(final Collection<File> filesToMonitor, final FileListener listener, final int expireInMillis) {
		final FileInfo e = new FileInfo(filesToMonitor, listener, expireInMillis, false);
		if (shortcutTrigger(e)) return;

		synchronized (lock) {
			files.add(e);
			startTimer();
		}
	}

	/**
	 * Trigger the listener whenever the given file changes.
	 *
	 * @param fileToMonitor The file to check for changes.
	 * @param listener      Listener to be notified on change.
	 */
	public void fileToBeChanged(final File fileToMonitor, final FileListener listener) {
		final FileInfo e = new FileInfo(Arrays.asList(fileToMonitor), listener, 0, true);
		if (shortcutTrigger(e)) return;

		synchronized (lock) {
			files.add(e);
			startTimer();
		}
	}

	private boolean shortcutTrigger(final FileInfo info) {
		if (info.shouldTriggerListener()) {
			info.fireListener(false);
			return true;
		}
		return false;
	}

	/**
	 * Information about a file being monitored
	 */
	private static class FileInfo {
		private List<File> files;
		private List<Long> modifiedTimes;
		private FileListener listener;
		private long expirationDate;
		private boolean checkForChange;

		/**
		 * @param filesToAdd     File to check for changes.
		 * @param listener       Listener to notify of changes.
		 * @param expireInMillis How many milliseconds to monitor for. If 0, monitor indefinitely.
		 * @param checkForChange Whether to check for the file to change modification time. If false, only file creation triggers notification.
		 */
		FileInfo(final Collection<File> filesToAdd, final FileListener listener, final int expireInMillis, final boolean checkForChange) {
			files = new ArrayList<File>(filesToAdd.size());
			modifiedTimes = new ArrayList<Long>(filesToAdd.size());
			this.checkForChange = checkForChange;

			for (final File file : filesToAdd) {
				files.add(file);
				if (file.exists()) {
					modifiedTimes.add(file.lastModified());
				} else {
					modifiedTimes.add(-1L);
				}
			}
			if (expireInMillis == 0) {
				expirationDate = 0;
			} else {
				expirationDate = new Date().getTime() + (long) expireInMillis;
			}
			this.listener = listener;
		}

		private FileListener getListener() {
			return listener;
		}

		private boolean shouldTriggerListener() {
			for (int i = 0; i < files.size(); i++) {
				final File file = files.get(i);
				if (checkForChange) {
					final long modified = modifiedTimes.get(i);
					if (file.exists()) {
						final long modificationTime = file.lastModified();
						if (modificationTime == modified) {
							return false;
						} else {
							modifiedTimes.set(i, modificationTime);
						}
					} else {
						if (modified == -1L) {
							return false;
						}
					}
				} else {
					if (!file.exists()) {
						return false;
					}
				}
			}
			return true;
		}

		private boolean isExpired() {
			if (expirationDate == 0) {
				return false;
			}
			return new Date().getTime() >= expirationDate;
		}

		public void fireListener(final boolean expired) {
			listener.fileChanged(files, expired);
		}

		private boolean isCheckForChange() {
			return checkForChange;
		}
	}


	/**
	 * This is the timer thread which is executed every n milliseconds
	 * according to the setting of the file monitor. It investigates the
	 * file in question and notify listeners if changed.
	 */
	private class FileMonitorNotifier extends TimerTask {
		public void run() {
			synchronized (lock) {
				for (Iterator<FileInfo> iterator = files.iterator(); iterator.hasNext(); ) {
					FileInfo fileInfo = iterator.next();
					if (fileInfo.isExpired()) {
						fileInfo.fireListener(true);
						iterator.remove();
					} else if (fileInfo.shouldTriggerListener()) {
						fileInfo.fireListener(false);
						if (!fileInfo.isCheckForChange()) {
							// We are monitoring for files to appear, and they did
							iterator.remove();
						}
					}
				}
			}
		}
	}
}
