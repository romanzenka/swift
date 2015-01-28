package edu.mayo.mprc.swift.report;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.qstat.QstatOutput;
import edu.mayo.mprc.qstat.QstatWorkPacket;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.db.SearchRunFilter;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import edu.mayo.mprc.swift.search.AssignedSearchRunId;
import edu.mayo.mprc.swift.search.SwiftSearcherCaller;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

// TODO: Ideally refactor into RESTful interface - would provide clean access to Swift

@Controller
public final class ReportUpdate {
	private static final String CONTENT_TYPE = "application/javascript; charset=utf-8";
	private static final Logger LOGGER = Logger.getLogger(ReportUpdate.class);
	private transient SwiftDao swiftDao;
	private transient SearchDbDao searchDbDao;
	private transient DatabaseFileTokenFactory fileTokenFactory;
	private transient WebUiHolder webUiHolder;
	private transient SwiftSearcherCaller swiftSearcherCaller;
	/**
	 * How many milliseconds to wait till qstat considered down.
	 */
	private static final int QSTAT_TIMEOUT = 30 * 1000;

	public ReportUpdate() {
		int i = 0;
	}

	private void printError(final PrintWriter output, final String message, final Throwable t) {
		LOGGER.error(message, t);
		output.println(message);
		if (t != null) {
			t.printStackTrace(output);
		}
	}

	@RequestMapping(value = "/report/reportupdate", method = RequestMethod.GET)
	public void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException {
		resp.setHeader("Cache-Control", "no-cache");

		// If there is a rerun parameter, we want to rerun the given search run, otherwise we produce data
		final String rerun = req.getParameter("rerun");
		if (rerun != null) {
			swiftDao.begin(); // Transaction-per-request
			final String skipSuccessfulParam = req.getParameter("skipSuccessful");
			final boolean skipSuccessful = "true".equals(skipSuccessfulParam);
			rerunSearch(req, resp, rerun, skipSuccessful);
			swiftDao.commit();
			return;
		}

		// If there is a hide parameter, we want to hide the given search run, otherwise we produce data
		final String hide = req.getParameter("hide");
		if (hide != null) {
			swiftDao.begin(); // Transaction-per-request
			hideSearch(req, resp, hide);
			swiftDao.commit();
			return;
		}

		// If there is a qa parameter, we want to redirect to QA for the given transaction
		final String qa = req.getParameter("qa");
		if (qa != null) {
			swiftDao.begin(); // Transaction-per-request
			final SearchRun searchRun = swiftDao.getSearchRunForId(Integer.valueOf(qa));
			final int swiftSearchId = searchRun.getSwiftSearch();
			final SwiftSearchDefinition swiftSearchDefinition = swiftDao.getSwiftSearchDefinition(swiftSearchId);
			final File indexHtmlLocation = new File(new File(swiftSearchDefinition.getOutputFolder(), "qa"), "index.html");
			final String link = getWebUi().fileToUserLink(indexHtmlLocation);
			swiftDao.commit();
			try {
				resp.sendRedirect(link);
			} catch (IOException e) {
				throw new ServletException("Could not redirect the user to QA directory", e);
			}
			return;
		}

		// Qstat causes qstat daemon info to be printed out
		final String qstatJobId = req.getParameter("qstat");
		if (qstatJobId != null) {
			final DaemonConnection connection = getWebUi().getQstatDaemonConnection();
			resp.setContentType("text/plain");
			final SgeStatusProgressListener listener = new SgeStatusProgressListener();
			connection.sendWork(new QstatWorkPacket(Integer.parseInt(qstatJobId)), listener);
			try {
				listener.waitForEvent(QSTAT_TIMEOUT);
			} catch (InterruptedException ignore) {
				// SWALLOWED: We just exit
			}
			try {
				final ServletOutputStream outputStream = resp.getOutputStream();
				outputStream.print(listener.getResult());
			} catch (IOException e) {
				throw new ServletException(e);
			}

			return;
		}

		// Action field defines what to do next
		final String action = req.getParameter("action");

		// This action does not make much sense, not sure if it is actually used
		if ("open".equals(action)) {
			String file = req.getParameter("file");
			file = getWebUi().fileToUserLink(new File(file));
			try {
				resp.sendRedirect(file);
			} catch (IOException e) {
				throw new ServletException(e);
			}
			return;
		}

		JsonWriter out = null;
		try {
			// All following actions require a search run
			swiftDao.begin(); // Transaction-per-request

			final String timestamp = req.getParameter("timestamp");

			final SearchRunFilter searchRunFilter = new SearchRunFilter();
			searchRunFilter.setStart(req.getParameter("start"));
			searchRunFilter.setCount(req.getParameter("count"));
			searchRunFilter.setShowHidden(req.getParameter("showHidden") != null && "true".equals(req.getParameter("showHidden")));
			searchRunFilter.setUserFilter(req.getParameter("userfilter"));
			searchRunFilter.setTitleFilter(req.getParameter("titlefilter"));

			final PrintWriter printOut = resp.getWriter();
			out = new JsonWriter(printOut);
			resp.setContentType(CONTENT_TYPE);

			// No action - clear everything, get fresh copy of data
			if (action == null || action.isEmpty() || "load".equals(action)) {
				out.clearAll();
				printSearchRuns(out, searchRunFilter, "insert");
			} else if ("rewrite".equals(action)) {
				// Rewrite given range, do not erase/modify anything else
				printSearchRuns(out, searchRunFilter, "rewrite");
			} else if ("expand".equals(action)) {
				// Expand - provide detailed task info for one task
				final int id = Integer.parseInt(req.getParameter("id"));
				out.rewriteTaskDataList(id, swiftDao.getTaskDataList(id));
			} else if ("update".equals(action)) {
				// We print out all new search runs + updates to the ones that changed
				final Date time = new Date();
				if (null != timestamp) {
					time.setTime(Long.parseLong(timestamp));
				} else {
					time.setTime(0);
				}
				updateSearchRuns(out, searchRunFilter, time);

				// TODO: Do not output all expanded task lists, only the changed ones
				final String expanded = req.getParameter("expanded");
				if (null != expanded) {
					final String[] expandedIds = expanded.split(",");
					for (final String idString : expandedIds) {
						if (null != idString && !idString.isEmpty()) {
							final int id = Integer.parseInt(idString);
							out.rewriteTaskDataList(id, swiftDao.getTaskDataList(id));
						}
					}
				}
			}
			swiftDao.commit();
		} catch (Exception t) {
			swiftDao.rollback();
			throw new ServletException(t);
		} finally {
			if (null != out) {
				out.close();
			}
		}
	}

	private WebUi getWebUi() {
		return getWebUiHolder().getWebUi();
	}

	private void rerunSearch(final HttpServletRequest req, final HttpServletResponse resp, final String rerun, final boolean skipSuccessful) throws ServletException {
		PrintWriter output;
		try {
			output = resp.getWriter();
		} catch (IOException ignore) {
			// SWALLOWED: We can live even if this fails.
			output = new PrintWriter(System.out);
		}
		final SearchRun td = getSearchRunForId(rerun);
		// If are successful and skipSuccessful is true, we do not restart
		if (!skipSuccessful || !td.isCompleted() || td.getTasksFailed() != 0) {
			final ResubmitProgressListener listener = new ResubmitProgressListener();

			try {
				swiftSearcherCaller.resubmitSearchRun(td, listener);
			} catch (Exception t) {
				throw new ServletException(t);
			}

			try {
				listener.waitForResubmit(60 * 1000);
			} catch (InterruptedException e) {
				throw new ServletException("Resubmit was interrupted", e);
			}

			if (listener.getAssignedId() == -1) {
				if (listener.getLastException() != null) {
					printError(output, "Rerun failed", listener.getLastException());
				} else {
					printError(output, "Timeout passed and rerun did not report success", null);
				}
				return;
			}
		}
		forwardToReportPage(req, resp);
	}

	private void forwardToReportPage(final HttpServletRequest req, final HttpServletResponse resp) {
		try {
			// Forward the viewer back to the report page
			resp.sendRedirect("/report/report");
		} catch (Exception ignore) {
			// SWALLOWED: This is not essential, if it fails, no big deal
		}
	}

	private void hideSearch(final HttpServletRequest req, final HttpServletResponse resp, final String hide) throws ServletException {
		final SearchRun td = getSearchRunForId(hide);
		td.setHidden(1);
		forwardToReportPage(req, resp);
	}

	private SearchRun getSearchRunForId(final String id) {
		final int searchRunId = Integer.parseInt(id);

		final SearchRun td;
		try {
			td = swiftDao.getSearchRunForId(searchRunId);
		} catch (Exception t) {
			throw new MprcException("Failure looking up search run with id=" + searchRunId, t);
		}
		return td;
	}

	/**
	 * Prints search runs in given range to given writer. The search run data is passed as a parameter to a specified
	 * function, such as "insert", "rewrite" or "update".
	 *
	 * @param out    Where to write the search runs to.
	 * @param method Method to use on the search runs. "update" will send a command to update search run data, while "insert" will insert new search runs.
	 * @param filter Filter defining what search runs and how sorted to output.
	 */
	private void printSearchRuns(final JsonWriter out, final SearchRunFilter filter, final String method) {
		final List<SearchRun> searchRuns = swiftDao.getSearchRunList(filter, true);
		searchDbDao.fillInInstrumentSerialNumbers(searchRuns);
		getWebUi().mapInstrumentSerialNumbers(searchRuns);
		swiftDao.fillNumberRunningTasksForSearchRun(searchRuns);

		final int firstSearchRun = 0;
		final int lastSearchRun = Math.min(
				filter.getCount() != null ? Integer.parseInt(filter.getCount()) : 0,
				searchRuns.size());

		Date newTimestamp = new Date();
		newTimestamp.setTime(0);
		for (int i = firstSearchRun; i < lastSearchRun; i++) {
			final SearchRun searchRun = searchRuns.get(i);
			final ArrayList<ReportInfo> reports = getReportsForSearchRun(searchRun);
			if (null != searchRun.getStartTimestamp() && searchRun.getStartTimestamp().compareTo(newTimestamp) > 0) {
				newTimestamp = searchRun.getStartTimestamp();
			}
			if (null != searchRun.getEndTimestamp() && searchRun.getEndTimestamp().compareTo(newTimestamp) > 0) {
				newTimestamp = searchRun.getEndTimestamp();
			}

			out.processSearchRun(i, searchRun, reports, method);
		}
		out.setTimestamp(newTimestamp);
	}

	private ArrayList<ReportInfo> getReportsForSearchRun(final SearchRun searchRun) {
		final ArrayList<ReportInfo> reports = new ArrayList<ReportInfo>();
		for (final ReportData report : searchRun.getReports()) {
			reports.add(
					new ReportInfo(report.getId(),
							fileTokenFactory.fileToTaggedDatabaseToken(report.getReportFile()),
							report.getAnalysisId() != null
					)
			);
		}
		Collections.sort(reports);
		return reports;
	}

	/**
	 * Produces code to update given range of search runs. The update affects only data changed since last timestamp.
	 * The produced code contains instruction for setting a new timestamp.
	 *
	 * @param out       Where to write the search runs to.
	 * @param timestamp Time when the last update was performed.
	 * @param filter    Filter defining what search runs and how sorted to output.
	 */
	private void updateSearchRuns(final JsonWriter out, final SearchRunFilter filter, final Date timestamp) {
		final List<SearchRun> searchRuns = swiftDao.getSearchRunList(filter, true);
		swiftDao.fillNumberRunningTasksForSearchRun(searchRuns);
		searchDbDao.fillInInstrumentSerialNumbers(searchRuns);
		getWebUi().mapInstrumentSerialNumbers(searchRuns);
		final int firstSearchRun = filter.getStart() != null ? Integer.parseInt(filter.getStart()) : 0;
		final int lastSearchRun = Math.min(firstSearchRun + (filter.getCount() != null ? Integer.parseInt(filter.getCount()) : searchRuns.size()), searchRuns.size());

		Date newTimestamp = timestamp;
		for (int i = firstSearchRun; i < lastSearchRun; i++) {
			final SearchRun searchRun = searchRuns.get(i);
			final ArrayList<ReportInfo> reports = getReportsForSearchRun(searchRun);
			if (null != searchRun.getStartTimestamp() && searchRun.getStartTimestamp().compareTo(newTimestamp) > 0) {
				newTimestamp = searchRun.getStartTimestamp();
			}
			if (null != searchRun.getEndTimestamp() && searchRun.getEndTimestamp().compareTo(newTimestamp) > 0) {
				newTimestamp = searchRun.getEndTimestamp();
			}

			final boolean doInsert = null != searchRun.getStartTimestamp() && searchRun.getStartTimestamp().compareTo(timestamp) > 0;
			out.processSearchRun(i, searchRun, reports, doInsert ? "insert" : "update");
		}
		out.setTimestamp(newTimestamp);
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	public void setSwiftDao(final SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}

	public SearchDbDao getSearchDbDao() {
		return searchDbDao;
	}

	public void setSearchDbDao(final SearchDbDao searchDbDao) {
		this.searchDbDao = searchDbDao;
	}

	public DatabaseFileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	public void setFileTokenFactory(final DatabaseFileTokenFactory fileTokenFactory) {
		this.fileTokenFactory = fileTokenFactory;
	}

	private static final class SgeStatusProgressListener implements ProgressListener {
		// TODO: This directly prints out messages - unclean
		private boolean finished;
		private final Object lock = new Object();
		private String result;

		SgeStatusProgressListener() {
		}

		@Override
		public void requestEnqueued(final String hostString) {
		}

		@Override
		public void requestProcessingStarted(final String hostString) {
		}

		public void waitForEvent(final long timeout) throws InterruptedException {
			// TODO: This suffers from spurious wakeups, however this function can safely fail once in a while
			synchronized (lock) {
				if (!finished) {
					lock.wait(timeout);
				}
			}
		}

		@Override
		public void requestProcessingFinished() {
			signalFinished();
		}

		private String getResult() {
			synchronized (lock) {
				return result;
			}
		}

		private void signalFinished() {
			synchronized (lock) {
				finished = true;
				lock.notifyAll();
			}
		}

		@Override
		public void requestTerminated(final Exception e) {
			final String info = e.getMessage();
			result = info;
			signalFinished();
		}

		@Override
		public void userProgressInformation(final ProgressInfo progressInfo) {
			if (progressInfo instanceof QstatOutput) {
				final QstatOutput info = (QstatOutput) progressInfo;
				result = info.getQstatOutput();
			}
		}
	}

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	public void setWebUiHolder(final WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}

	public SwiftSearcherCaller getSwiftSearcherCaller() {
		return swiftSearcherCaller;
	}

	public void setSwiftSearcherCaller(final SwiftSearcherCaller swiftSearcherCaller) {
		this.swiftSearcherCaller = swiftSearcherCaller;
	}

	private class ResubmitProgressListener implements ProgressListener {
		private Throwable lastException = null;
		private long assignedId = -1;
		private final Object lock = new Object();

		ResubmitProgressListener() {
		}

		private boolean isComplete() {
			return assignedId != -1 || lastException != null;
		}

		public void waitForResubmit(final long timeout) throws InterruptedException {
			long currentTime = System.currentTimeMillis();
			final long finalTime = currentTime + timeout;
			synchronized (lock) {
				while (!isComplete()) {
					lock.wait(Math.max(finalTime - currentTime, timeout));
					currentTime = System.currentTimeMillis();
					if (currentTime >= finalTime) {
						// Timed out
						break;
					}
				}
			}
		}

		public Throwable getLastException() {
			synchronized (lock) {
				return lastException;
			}
		}

		public long getAssignedId() {
			synchronized (lock) {
				return assignedId;
			}
		}

		@Override
		public void requestEnqueued(final String hostString) {
		}

		@Override
		public void requestProcessingStarted(final String hostString) {
		}

		@Override
		public void requestProcessingFinished() {
		}

		@Override
		public void requestTerminated(final Exception e) {
			synchronized (lock) {
				lastException = e;
				lock.notifyAll();
			}
		}

		@Override
		public void userProgressInformation(final ProgressInfo progressInfo) {
			if (progressInfo instanceof AssignedSearchRunId) {
				synchronized (lock) {
					assignedId = ((AssignedSearchRunId) progressInfo).getSearchRunId();
					lock.notifyAll();
				}
			}
		}
	}
}
