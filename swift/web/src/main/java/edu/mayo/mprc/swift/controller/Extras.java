package edu.mayo.mprc.swift.controller;

import edu.mayo.mprc.ReleaseInfoCore;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;

/**
 * Data for extras
 *
 * @author Roman Zenka
 */
@Controller
@RequestMapping("/extras")
public final class Extras {
	private WebUiHolder webUiHolder;

	@RequestMapping(method = RequestMethod.GET)
	public String extras(final ModelMap model) {
		model.addAttribute("title", getWebUi().getTitle());

		// Dates for report
		DateTime end = new DateTime();
		DateTime start = end.minusMonths(1);

		model.addAttribute("startDate", DateTimeFormat.forPattern("yyyy-MM-dd").print(start));
		model.addAttribute("endDate", DateTimeFormat.forPattern("yyyy-MM-dd").print(end));

		model.addAttribute("buildVersion", ReleaseInfoCore.buildVersion());
		model.addAttribute("buildRevision", ReleaseInfoCore.buildRevision());
		model.addAttribute("buildTimestamp", ReleaseInfoCore.buildTimestamp());
		model.addAttribute("buildLink", ReleaseInfoCore.buildLink());

		return "extras";
	}

	private WebUi getWebUi() {
		return getWebUiHolder().getWebUi();
	}

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	@Resource(name = "webUiHolder")
	public void setWebUiHolder(WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}
}

