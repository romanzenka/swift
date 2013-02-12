package edu.mayo.mprc.swift.report;

import com.google.common.base.Charsets;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.searchdb.Report;
import edu.mayo.mprc.searchdb.dao.Analysis;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Dumps all that was loaded from a Scaffold search into HTML.
 *
 * @author Roman Zenka
 */
public class AnalysisReport implements HttpRequestHandler {
	private SearchDbDao searchDbDao;

	@Override
	public void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(resp.getOutputStream(), Charsets.US_ASCII);

			final String reportIdStr = req.getParameter("id");
			final long reportId;
			try {
				reportId = Long.parseLong(reportIdStr);
			} catch (NumberFormatException e) {
				throw new MprcException("Cannot process report id: " + reportIdStr, e);
			}

			final String highlight = req.getParameter("highlight");

			searchDbDao.begin();
			try {
				final Analysis analysis = searchDbDao.getAnalysis(reportId);

				writer.write("<html><head><title>Scaffold Report</title>\n" +
						"<link rel=\"stylesheet\" href=\"/report/analysis.css\" type=\"text/css\">\n" +
						"<link href='http://fonts.googleapis.com/css?family=PT+Sans' rel='stylesheet' type='text/css'>\n" +
						"</head><body>\n");
				writer.write("<h1>Scaffold Report</h1>\n");
				analysis.htmlReport(new Report(writer), searchDbDao, highlight);

				searchDbDao.commit();
			} catch (Exception e) {
				searchDbDao.rollback();
				throw new MprcException("Could not obtain analysis data", e);
			}

			writer.write("</body></html>");
		} finally {
			FileUtilities.closeQuietly(writer);
		}
	}

	public SearchDbDao getSearchDbDao() {
		return searchDbDao;
	}

	public void setSearchDbDao(SearchDbDao searchDbDao) {
		this.searchDbDao = searchDbDao;
	}
}

