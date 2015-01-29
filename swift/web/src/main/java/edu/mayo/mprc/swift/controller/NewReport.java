package edu.mayo.mprc.swift.controller;

import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.report.JsonWriter;
import edu.mayo.mprc.utilities.StringUtilities;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDao;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Roman Zenka
 */
@Controller
public final class NewReport {
	private SwiftDao swiftDao;
	private WorkspaceDao workspaceDao;

	@RequestMapping(value = "/newreport", method = RequestMethod.GET)
	public String report(final ModelMap model) {
		getSwiftDao().begin();
		final StringBuilder userList = new StringBuilder();
		final StringBuilder idList = new StringBuilder();
		try {
			final List<User> userInfos = getWorkspaceDao().getUsers(false);
			if (userInfos != null) {
				for (final User userInfo : userInfos) {
					if (userList.length() > 0) {
						userList.append(",");
						idList.append(",");
					}
					userList.append("'").append(StringUtilities.toUnicodeEscapeString(JsonWriter.escapeSingleQuoteJavascript(userInfo.getFirstName()))).append(' ').append(StringUtilities.toUnicodeEscapeString(JsonWriter.escapeSingleQuoteJavascript(userInfo.getLastName()))).append("'");
					idList.append("'").append(JsonWriter.escapeSingleQuoteJavascript(String.valueOf(userInfo.getId()))).append("'");
				}
			}
			getSwiftDao().commit();
		} catch (final Exception ignore) {
			getSwiftDao().rollback();
		}
		model.addAttribute("userListJson", userList.toString());
		model.addAttribute("idListJson", idList.toString());

		return "report/report";
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	@Resource(name = "swiftDao")
	public void setSwiftDao(final SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}

	public WorkspaceDao getWorkspaceDao() {
		return workspaceDao;
	}

	@Resource(name = "workspaceDao")
	public void setWorkspaceDao(final WorkspaceDao workspaceDao) {
		this.workspaceDao = workspaceDao;
	}
}
