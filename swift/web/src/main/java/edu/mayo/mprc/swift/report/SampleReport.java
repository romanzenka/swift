package edu.mayo.mprc.swift.report;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Report about the .RAW files. Parses contents of {@link TandemMassSpectrometrySample}
 *
 * @author Roman Zenka
 */
public final class SampleReport implements HttpRequestHandler {
	private SearchDbDao searchDbDao;

	@Override
	public void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) {
		resp.setContentType("text/plain");
		resp.setHeader("Content-Disposition", "attachment; filename=sample-report.csv");
		Writer writer = null;
		try {
			writer = new OutputStreamWriter(resp.getOutputStream());
			SampleReportData.writeCsv(writer, searchDbDao);
		} catch (Exception e) {
			throw new MprcException(e);
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
