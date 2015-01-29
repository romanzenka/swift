package edu.mayo.mprc.swift.controller;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.ReportUtils;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.TaskData;
import edu.mayo.mprc.swift.report.JsonWriter;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import edu.mayo.mprc.utilities.StringUtilities;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDao;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Roman Zenka
 */
@Controller
public final class Report {
	private WorkspaceDao workspaceDao;
	private WebUiHolder webUiHolder;
	private SwiftDao swiftDao;

	@RequestMapping(value = "/report", method = RequestMethod.GET)
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

	@RequestMapping(value = "/taskerror")
	public String taskError(final ModelMap model,
	                        @RequestParam(value = "id", required = false) final Integer taskId,
	                        @RequestParam(value = "tid", required = false) final Integer searchRunId) {

		if (taskId != null) {
			getSwiftDao().begin();
			try {
				final TaskData data = getSwiftDao().getTaskData(taskId);
				final String taskName = ReportUtils.replaceTokensWithHyperlinks(data.getTaskName(),
						getWebUi().getBrowseRoot(),
						getWebUi().getBrowseWebRoot(), getWebUi().getFileTokenFactory());
				model.addAttribute("taskName", taskName);

				final String taskDescription = ReportUtils.replaceTokensWithHyperlinks(data.getDescriptionLong(),
						getWebUi().getBrowseRoot(),
						getWebUi().getBrowseWebRoot(), getWebUi().getFileTokenFactory());
				model.addAttribute("taskDescription", taskDescription);

				final String exception = ReportUtils.newlineToBr(
						ReportUtils.replaceTokensWithHyperlinks(data.getErrorMessage(),
								getWebUi().getBrowseRoot(), getWebUi().getBrowseWebRoot(), getWebUi().getFileTokenFactory()));
				model.addAttribute("errorMessage", exception);

				model.addAttribute("exception", data.getExceptionString());
				getSwiftDao().commit();

				return "report/taskerror";
			} catch (Exception e) {
				getSwiftDao().rollback();
				throw new MprcException(e);
			}
		}

		if (searchRunId != null) {
			getSwiftDao().begin();
			try {
				final SearchRun data = getSwiftDao().getSearchRunForId(searchRunId);
				model.addAttribute("data", data);
				final String exception = ReportUtils.replaceTokensWithHyperlinks(
						StringUtilities.escapeHtml(data.getErrorMessage()),
						getWebUi().getBrowseRoot(),
						getWebUi().getBrowseWebRoot(),
						getWebUi().getFileTokenFactory());

				model.addAttribute("exception", exception);
				getSwiftDao().commit();

				return "report/searchrunerror";
			} catch (Exception e) {
				getSwiftDao().rollback();
				throw new MprcException(e);
			}
		}

		throw new MprcException("Neither task nor transaction ids were specified");

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

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	@Resource(name = "webUiHolder")
	public void setWebUiHolder(final WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}

	private WebUi getWebUi() {
		return getWebUiHolder().getWebUi();
	}


}
