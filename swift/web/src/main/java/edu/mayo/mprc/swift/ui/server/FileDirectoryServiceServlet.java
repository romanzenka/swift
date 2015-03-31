package edu.mayo.mprc.swift.ui.server;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@Controller
public final class FileDirectoryServiceServlet {
	private static final Logger LOGGER = Logger.getLogger(FileDirectoryServiceServlet.class);

	private WebUiHolder webUiHolder;
	public static final String DIRECTORY_PATH_ATTRIBUTE_NAME = "d";
	public static final String EXPANDED_PATHS_ATTRIBUTE_NAME = "e";
	public static final String SORT_ORDER_ATTRIBUTE_NAME = "order";

	@RequestMapping(value = "/start/DirectoryService", method = RequestMethod.POST)
	public void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws
			ServletException, IOException {

		resp.setContentType("text/xml");
		resp.setHeader("Cache-Control", "no-cache");
		final PrintWriter out = resp.getWriter();
		String directory_path;
		String expanded_paths;
		String order;
		FileSearchBean.SortBy sortBy;
		try {
			order = req.getParameter(SORT_ORDER_ATTRIBUTE_NAME);
			if ("date".equals(order)) {
				sortBy = FileSearchBean.SortBy.DATE;
			} else {
				sortBy = FileSearchBean.SortBy.NAME;
			}
			directory_path = req.getParameter(DIRECTORY_PATH_ATTRIBUTE_NAME);
			if (directory_path == null) {
				directory_path = "";
			}
			directory_path = removePathParentUsage(directory_path);
			expanded_paths = req.getParameter(EXPANDED_PATHS_ATTRIBUTE_NAME);
			if (expanded_paths == null) {
				expanded_paths = "";
			}

			final FileSearchBean fileBean = new FileSearchBean(getBaseFolder().getAbsolutePath(), sortBy);
			fileBean.setPath(fixFileSeparators(directory_path));
			fileBean.setExpandedPaths(expanded_paths);
			fileBean.writeFolderContent(out);
		} catch (MprcException e) {
			throw new ServletException("Problem in FileDirectoryServiceServlet, " + e.getMessage(), e);
		} catch (Exception e) {
			throw new ServletException("Problem in login session:" + e.getMessage(), e);
		} finally {
			out.close();
		}
	}  // end doGet

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	public void setWebUiHolder(WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}

	public File getBaseFolder() {
		return webUiHolder.getWebUi().getBrowseRoot();
	}

	/**
	 * replace the file separators with the localhost file separators
	 *
	 * @param filename - the filename
	 * @return - modified file name
	 */
	public static String fixFileSeparators(String filename) {
		if (filename == null) {
			return null;
		}
		switch (File.separatorChar) {
			case '/':
				return filename.replace('\\', File.separatorChar);
			case '\\':
				return filename.replace('/', File.separatorChar);
			default:
				LOGGER.warn("warning, unrecognized file separator=" + File.separatorChar);
				filename = filename.replace('/', File.separatorChar);
				filename = filename.replace('\\', File.separatorChar);
				return filename;
		}
	}

	public static String removePathParentUsage(final String path) {
		return path != null ? path.replace("\\.\\.", "") : null;
	}
}
