package edu.mayo.mprc.swift.report;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.SyndFeedOutput;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import org.apache.log4j.Logger;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;
import java.util.*;

public final class RssStatusFeeder implements HttpRequestHandler {
	private static final Logger LOGGER = Logger.getLogger(RssStatusFeeder.class);
	/**
	 * will not contain entries older than 96 hours
	 */
	private static final long MAX_STATUS_AGE = 96 * 60 * 60 * 1000;

	/**
	 * at most the feed will be updated every 5 minutes no matter how many requests are retreived
	 */
	private static final long FEED_UPDATE_INTERVAL = 5L * 60L * 1000L;

	private static volatile Date lastFeedEntryTime = null;
	private transient SwiftDao swiftDao;

	/**
	 * that last generated cache
	 */
	private static transient volatile Map<String, SyndFeed> cachedFeeds = null;

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	public void setSwiftDao(SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}

	private static SyndFeed getCachedFeed(final String id) {
		if (cachedFeeds == null || lastFeedEntryTime == null) {
			cachedFeeds = new HashMap<String, SyndFeed>();
		}

		SyndFeed cachedFeed = cachedFeeds.get(id);
		if (cachedFeed == null) {
			cachedFeed = new SyndFeedImpl();
			cachedFeed.setTitle("Swift status feed");
			cachedFeed.setDescription("This is a feed that keeps those interested tuned in to the status of an installation of " +
					"the Swift Proteomics search tool");
			cachedFeeds.put(id, cachedFeed);
		}

		return cachedFeed;
	}

	@Override
	public void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
		swiftDao.begin(); // Transaction-per-request
		try {
			Boolean showSuccess = true;
			final String showSuccessParameter = req.getParameter("showSuccess");
			if (showSuccessParameter != null) {
				try {
					showSuccess = Boolean.valueOf(showSuccessParameter);
				} catch (Exception t) {
					LOGGER.warn("Could not convert the paramter showSuccess to a boolean");
				}
			}
			final SyndFeed feed = getFeed(showSuccess, true, true);
			feed.setLink(HttpUtils.getRequestURL(req).toString());
			feed.setFeedType("rss_2.0");
			resp.setContentType("text/plain");
			new SyndFeedOutput().output(feed, resp.getWriter());
			swiftDao.commit();
		} catch (Exception t) {
			swiftDao.rollback();
			throw new ServletException("There was a problem writing the RSS feed.", t);
		}
	}

	protected synchronized SyndFeed getFeed(final boolean showSuccess, final boolean showFailures, final boolean showWarnings) {
		final Date now = new Date();

		final String cacheKey = showSuccess + "_" + showFailures + "_" + showWarnings;

		final SyndFeed cachedFeed = getCachedFeed(cacheKey);

		if (lastFeedEntryTime != null && (now.getTime() - lastFeedEntryTime.getTime() < FEED_UPDATE_INTERVAL)) {
			return cachedFeed;
		}

		final Set<SearchRun> newTxData;
		if (lastFeedEntryTime == null) {
			//go back 48 hours (or whatever MAX_STATUS_AGE is) and get all entries that far back
			newTxData = swiftDao.getSearchRuns(showSuccess, showFailures, showWarnings, new Date(now.getTime() - MAX_STATUS_AGE));
		} else {
			newTxData = swiftDao.getSearchRuns(showSuccess, showFailures, showWarnings, lastFeedEntryTime);
		}

		lastFeedEntryTime = now;

		final List<SyndEntry> entries = new ArrayList<SyndEntry>();
		entries.addAll(cachedFeed.getEntries());

		for (final SearchRun txDatum : newTxData) {

			if (!showSuccess && txDatum.getTasksFailed() == 0) {
				continue; //if no failures just skip this one.
			}

			if (txDatum.getStartTimestamp() != null && txDatum.getStartTimestamp().getTime() > lastFeedEntryTime.getTime()) {
				lastFeedEntryTime = txDatum.getStartTimestamp();
			}

			if (txDatum.getEndTimestamp() != null && txDatum.getEndTimestamp().getTime() > lastFeedEntryTime.getTime()) {
				lastFeedEntryTime = txDatum.getEndTimestamp();
			}

			final SyndEntry newEntry = new SyndEntryImpl();
			newEntry.setTitle(txDatum.getTitle() + " " + getSummaryInfo(txDatum));
			newEntry.setLink(""); //todo: probably just give the link to the status page here...
			newEntry.setPublishedDate(getMostRecentDate(txDatum));
			newEntry.setDescription(getContentFor(txDatum));

			entries.add(newEntry);
		}

		cachedFeed.setEntries(entries);

		return cachedFeed;
	}

	/**
	 * will find the most recent data in the the trasaction data
	 * <p/>
	 * Currently picks between end_timestamp and start_timestamp.
	 *
	 * @param data
	 * @return
	 */
	private static Date getMostRecentDate(final SearchRun data) {
		final List<Date> candidates = new ArrayList<Date>();
		if (data.getEndTimestamp() != null) {
			candidates.add(data.getEndTimestamp());
		}
		if (data.getStartTimestamp() != null) {
			candidates.add(data.getStartTimestamp());
		}
		return Collections.max(candidates);
	}

	/**
	 * Gets the information that we are intested in seeing in the RSS feed from the search run
	 *
	 * @param data
	 * @return
	 */
	private static SyndContent getContentFor(final SearchRun data) {
		final StringBuilder content = new StringBuilder(200);

		//todo: implement this, currently just does something stupid

		content.append("  Out of ").append(data.getNumTasks()).append(" tasks, ");
		if (data.getTasksCompleted() > 0) {
			content.append(data.getTasksCompleted()).append(" tasks completed, ");
		}
		if (data.getTasksFailed() > 0) {
			content.append(data.getTasksFailed()).append(" tasks failed, ");
		}
		if (data.getTasksWithWarning() > 0) {
			content.append(data.getTasksWithWarning()).append(" tasks have warnings, ");
		}

		final SyndContent retContent = new SyndContentImpl();
		retContent.setValue(content.toString());
		return retContent;
	}

	private static String getSummaryInfo(final SearchRun data) {
		if (data.getTasksCompleted() == data.getNumTasks()) {
			return "Successful";
		} else if (data.getTasksFailed() > 0) {
			return "Failed";
		}
		return "Still running";
	}
}
