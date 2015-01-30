package edu.mayo.mprc.swift.report;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.searchdb.SearchRunFilter;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;

/**
 * Provides status of several last searches in JSON format, including the task information.
 */
@Controller
public class JsonStatusFeeder {
	private SearchDbDao searchDbDao;
	private SwiftDao swiftDao;

	private static final int TYPICAL_RESPONSE_SIZE = 1024;

	@RequestMapping(value = "/status/searches", method = RequestMethod.GET)
	public void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException {
		PrintWriter out = null;
		try {

			searchDbDao.begin(); // Transaction-per-request

			final SearchRunFilter searchRunFilter = new SearchRunFilter();
			searchRunFilter.setStart("0");
			searchRunFilter.setCount("50");

			out = resp.getWriter();

			final StringBuilder response = new StringBuilder(TYPICAL_RESPONSE_SIZE);
			response.append("[");

			final List<SearchRun> searchRuns = searchDbDao.getSearchRunList(searchRunFilter, false);
			swiftDao.fillNumberRunningTasksForSearchRun(searchRuns);
			for (int i = 0; i < searchRuns.size(); i++) {
				final SearchRun searchRun = searchRuns.get(i);
				JsonWriter.appendSearchRunJson(response, i, searchRun, null, false);
				if (i + 1 < searchRuns.size()) {
					response.append(",\n");
				}
			}
			response.append("]");

			out.print(response.toString());

			searchDbDao.commit();

		} catch (final Exception e) {
			searchDbDao.rollback();
			throw new MprcException("Could not obtain list of search runs", e);
		} finally {
			FileUtilities.closeQuietly(out);
		}
	}

	public SearchDbDao getSearchDbDao() {
		return searchDbDao;
	}

	@Resource(name = "searchDbDao")
	public void setSearchDbDao(final SearchDbDao searchDbDao) {
		this.searchDbDao = searchDbDao;
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	@Resource(name = "swiftDao")
	public void setSwiftDao(final SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}
}
