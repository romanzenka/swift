package edu.mayo.mprc.swift.report;

import com.google.common.base.Charsets;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Find all searches where a particular protein occured.
 *
 * @author Roman Zenka
 */
public class FindProtein implements HttpRequestHandler {
	private SearchDbDao searchDbDao;
	private WebUiHolder webUiHolder;

	@Override
	public void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(resp.getOutputStream(), Charsets.US_ASCII);

			final String accessionNumber = req.getParameter("id");

			searchDbDao.begin();
			try {
				final List<ReportData> reportDataList = searchDbDao.getSearchesForAccessionNumber(accessionNumber);

				writer.write("<html><head><title>Searches containing " + accessionNumber + " | " + webUiHolder.getWebUi().getTitle() + "</title>\n" +
						"<link rel=\"stylesheet\" href=\"/report/analysis.css\" type=\"text/css\">\n" +
						"<link href='http://fonts.googleapis.com/css?family=PT+Sans' rel='stylesheet' type='text/css'>\n" +
						"</head><body>\n");
				writer.write("<form action=\"/find-protein\" method=\"get\">Search: <input type=\"text\" name=\"id\" value=\"" + (null == accessionNumber ? "" : accessionNumber) + "\"></form>");
				writer.write("<h1>Searches containing " + accessionNumber + "</h1>\n");
				writer.write("<table>");
				writer.write("<tr><th>Title</th><th>Completed</th><th>Loaded Swift Data</th><th>Scaffold Files</th></tr>");

				matchingSearchesTable(writer, reportDataList, accessionNumber);

				writer.write("</table>");
				writer.write("</body></html>");

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

	private void matchingSearchesTable(final OutputStreamWriter writer, final List<ReportData> reportDataList, final String accessionNumber) throws IOException {
		Integer previousSearchRun = 0;
		final WebUi webUi = getWebUiHolder().getWebUi();
		for (final ReportData reportData : reportDataList) {
			final SearchRun searchRun = reportData.getSearchRun();
			if (!searchRun.getId().equals(previousSearchRun)) {
				closePreviousRun(writer, previousSearchRun);

				writer.write("<tr>");
				previousSearchRun = searchRun.getId();
				writer.write("<td><a href=\"/start/?load=" + searchRun.getId() + "\">" + searchRun.getTitle() + "</a></td>");
				writer.write("<td>" + searchRun.getEndTimestamp() + "</td>");
				writer.write("<td>");
			}
			final String reportName = reportData.getReportFile().getName();
			writer.write("<a href=\"/analysis?id=" + reportData.getId() + "&highlight=" + accessionNumber + "\">" + FileUtilities.stripExtension(reportName) + "</a> ");
			writer.write("</td><td>");
			final String link = webUi.fileToUserLink(reportData.getReportFile());
			writer.write("<a href=\"" + link + "\">" + reportName + "</a>");
		}
		closePreviousRun(writer, previousSearchRun);
	}

	private void closePreviousRun(final OutputStreamWriter writer, final Integer previousSearchRun) throws IOException {
		if (0 != previousSearchRun) {
			writer.write("</td>");
			writer.write("</tr>");
		}
	}

	public SearchDbDao getSearchDbDao() {
		return searchDbDao;
	}

	public void setSearchDbDao(SearchDbDao searchDbDao) {
		this.searchDbDao = searchDbDao;
	}

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	public void setWebUiHolder(WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}
}
