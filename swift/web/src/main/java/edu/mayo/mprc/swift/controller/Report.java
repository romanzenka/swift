package edu.mayo.mprc.swift.controller;

import edu.mayo.mprc.swift.SwiftWebContext;
import edu.mayo.mprc.swift.report.JsonWriter;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import edu.mayo.mprc.utilities.StringUtilities;
import edu.mayo.mprc.workspace.User;
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
public final class Report {
	private WebUiHolder webUiHolder;

	@RequestMapping(value = "/report", method = RequestMethod.GET)
	public String report(ModelMap model) {
		model.addAttribute("title", getWebUi().getTitle());
		model.addAttribute("pathPrefix", SwiftWebContext.getPathPrefix());
		model.addAttribute("pathWebPrefix", getWebUi().getBrowseWebRoot());
		model.addAttribute("messageDefined", getWebUi().getUserMessage().messageDefined());
		model.addAttribute("userMessage", getWebUi().getUserMessage().getMessage());

		SwiftWebContext.getWebUi().getSwiftDao().begin();
		final StringBuilder userList = new StringBuilder();
		final StringBuilder idList = new StringBuilder();
		try {
			final List<User> userInfos = SwiftWebContext.getWebUi().getWorkspaceDao().getUsers(false);
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
			SwiftWebContext.getWebUi().getSwiftDao().commit();
		} catch (Exception ignore) {
			SwiftWebContext.getWebUi().getSwiftDao().rollback();
		}
		model.addAttribute("userListJson", userList.toString());
		model.addAttribute("idListJson", idList.toString());

		return "report/report";
	}


	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	@Resource(name = "webUiHolder")
	public void setWebUiHolder(WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}

	private WebUi getWebUi() {
		return getWebUiHolder().getWebUi();
	}
}
