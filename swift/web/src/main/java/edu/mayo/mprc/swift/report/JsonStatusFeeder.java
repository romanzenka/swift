package edu.mayo.mprc.swift.report;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.db.SearchRunFilter;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;

/**
 * Provides status of several last searches in JSON format, including the task information.
 */
public class JsonStatusFeeder implements HttpRequestHandler {
	private SwiftDao swiftDao;
	private static final int TYPICAL_RESPONSE_SIZE = 1024;

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	public void setSwiftDao(SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}

	@Override
	public void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
		PrintWriter out = null;
		try {

			swiftDao.begin(); // Transaction-per-request

			final SearchRunFilter searchRunFilter = new SearchRunFilter();
			searchRunFilter.setStart("0");
			searchRunFilter.setCount("50");

			out = resp.getWriter();

			final StringBuilder response = new StringBuilder(TYPICAL_RESPONSE_SIZE);
			response.append("[");

			final List<SearchRun> searchRuns = swiftDao.getSearchRunList(searchRunFilter, false);
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

			swiftDao.commit();

		} catch (Exception e) {
			swiftDao.rollback();
			throw new MprcException("Could not obtain list of search runs", e);
		} finally {
			FileUtilities.closeQuietly(out);
		}
	}
}
