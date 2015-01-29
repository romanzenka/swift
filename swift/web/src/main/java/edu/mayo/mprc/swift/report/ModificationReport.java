package edu.mayo.mprc.swift.report;

import com.google.common.base.Charsets;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import edu.mayo.mprc.unimod.Unimod;
import edu.mayo.mprc.unimod.UnimodDao;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Dumps all the modifications Swift is currently operating with.
 *
 * @author Roman Zenka
 */
@Controller
public final class ModificationReport {
	private static final String TITLE = "Modifications defined in Swift";
	private transient UnimodDao unimodDao;
	private transient WebUiHolder webUiHolder;

	@RequestMapping(value = "/mods", method = RequestMethod.GET)
	public void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		OutputStreamWriter writer = null;
		try {
			unimodDao.begin();
			final Unimod unimod = unimodDao.load();
			final String report = unimod.report();
			unimodDao.commit();

			writer = new OutputStreamWriter(resp.getOutputStream(), Charsets.US_ASCII);
			writer.write("<html><head><title>" + TITLE + " | " + webUiHolder.getWebUi().getTitle() + "</title>" +
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

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	public void setWebUiHolder(WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}
}
