package edu.mayo.mprc.swift.report;

import com.google.common.base.Charsets;
import edu.mayo.mprc.unimod.Unimod;
import edu.mayo.mprc.unimod.UnimodDao;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Dumps all the modifications Swift is currently operating with.
 *
 * @author Roman Zenka
 */
public final class ModificationReport implements HttpRequestHandler {
	private static final String TITLE = "Modifications defined in Swift";
	private transient UnimodDao unimodDao;

	@Override
	public void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		OutputStreamWriter writer = null;
		try {
			unimodDao.begin();
			final Unimod unimod = unimodDao.load();
			final String report = unimod.report();
			unimodDao.commit();

			writer = new OutputStreamWriter(resp.getOutputStream(), Charsets.US_ASCII);
			writer.write("<html><head><title>" + TITLE + "</title>" +
					"<style>" +
					"table { border-collapse: collapse }" +
					"table td, table th { border: 1px solid black }" +
					"</style>" +
					"</head><body>");
			writer.write("<h1>" + TITLE + "</h1>");
			writer.write(report);
			writer.write("</body></html>");

		} finally {
			FileUtilities.closeQuietly(writer);
		}
	}

	public UnimodDao getUnimodDao() {
		return unimodDao;
	}

	public void setUnimodDao(UnimodDao unimodDao) {
		this.unimodDao = unimodDao;
	}
}
