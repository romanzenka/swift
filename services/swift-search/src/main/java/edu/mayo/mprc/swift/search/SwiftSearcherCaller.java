package edu.mayo.mprc.swift.search;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.AssignedTaskData;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import org.apache.log4j.Logger;

/**
 * Used to send a socket request for a search run to {@link SwiftSearcher}
 */
public final class SwiftSearcherCaller {
	private static final Logger LOGGER = Logger.getLogger(SwiftSearcherCaller.class);

	private SwiftSearcherCaller() {
	}

	/**
	 * used to resubmit a transaction that has been run before
	 *
	 * @param td                         - Transaction to resubmit
	 * @param dispatcherDaemonConnection Daemon connection to the dispatcher.
	 * @param listener                   Progress listener for this submission.
	 */
	public static void resubmitSearchRun(final SearchRun td, final DaemonConnection dispatcherDaemonConnection, final ProgressListener listener) {
		try {
			final String sBatchName = td.getTitle();
			sendCallToDispatcher(dispatcherDaemonConnection, td.getSwiftSearch(), sBatchName, false, 0, td.getId(), listener);
		} catch (Exception t) {
			throw new MprcException("resubmitSearchRun : failure sending call to dispatcher, " + t.getMessage(), t);
		}
	}

	public static SearchProgressListener startSearch(final int swiftSearchId, final String batchName, final boolean fromScratch, final int priority, final int previousSearchId, final DaemonConnection dispatcherDaemonConnection) {
		final SearchProgressListener listener = new SearchProgressListener();
		sendNewSearchToDispatcher(swiftSearchId, batchName, fromScratch, priority, previousSearchId, dispatcherDaemonConnection, listener);
		return listener;
	}

	public static void sendNewSearchToDispatcher(final int swiftSearchId, final String batchName, final boolean fromScratch, final int priority, final int previousSearchId, final DaemonConnection dispatcherDaemonConnection, final ProgressListener listener) {
		sendCallToDispatcher(dispatcherDaemonConnection, swiftSearchId, batchName, fromScratch, priority, previousSearchId, listener);
	}

	private static void sendCallToDispatcher(final DaemonConnection connection, final Integer swiftSearchId, final String sBatchName, final boolean fromScratch, final int priority, final int previousSearchId, final ProgressListener listener) {
		// Send work. We are not interested in progress at all, but we must specify progress listener
		final SwiftSearchWorkPacket workPacket = new SwiftSearchWorkPacket(swiftSearchId, sBatchName, fromScratch, previousSearchId);
		workPacket.setPriority(priority);
		connection.sendWork(workPacket, listener);
	}

	public static class SearchProgressListener implements ProgressListener {
		private Exception exception;
		private long searchId;
		private boolean running = true;
		private volatile boolean ready;
		private final Object monitor = new Object();

		SearchProgressListener() {
			exception = null;
		}

		/**
		 * Synchronization object. This object gets notifications every time the search progresses.
		 *
		 * @return Synchronization monitor for the search.
		 */
		public Object getMonitor() {
			return monitor;
		}

		public boolean isSuccessful() {
			return !isRunning() && getException() == null;
		}

		public boolean isRunning() {
			synchronized (monitor) {
				return running;
			}
		}

		public long getSearchRunId() {
			synchronized (monitor) {
				return searchId;
			}
		}

		public Exception getException() {
			synchronized (monitor) {
				return exception;
			}
		}

		@Override
		public void requestEnqueued(final String hostString) {
			LOGGER.debug("Request enqueued " + hostString);
		}

		@Override
		public void requestProcessingStarted(final String hostString) {
			LOGGER.debug("Request processing started " + hostString);
		}

		public void waitForSearchReady(final long timeout) throws InterruptedException {
			synchronized (monitor) {
				while (!ready) {
					monitor.wait(timeout);
				}
			}
		}

		@Override
		public void requestProcessingFinished() {
			LOGGER.debug("Request processing finished successfully");
			synchronized (monitor) {
				ready = true;
				running = false;
				monitor.notifyAll();
			}
		}

		@Override
		public void requestTerminated(final Exception e) {
			LOGGER.debug("Request terminated with error", e);
			synchronized (monitor) {
				ready = true;
				running = false;
				this.exception = e;
				monitor.notifyAll();
			}
		}

		@Override
		public void userProgressInformation(final ProgressInfo progressInfo) {
			synchronized (monitor) {
				if (progressInfo instanceof AssignedSearchRunId) {
					searchId = ((AssignedSearchRunId) progressInfo).getSearchRunId();
				}

				// First ProgressInfo object must be the AssignedTaskData
				if (progressInfo instanceof AssignedTaskData) {
					return;
				}

				ready = true;
				monitor.notifyAll();
			}
		}
	}
}
