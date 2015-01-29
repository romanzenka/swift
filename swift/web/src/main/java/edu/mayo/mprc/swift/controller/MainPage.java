package edu.mayo.mprc.swift.controller;

import edu.mayo.mprc.ReleaseInfoCore;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;

/**
 * First page of Swift
 *
 * @author Roman Zenka
 */
@Controller
@RequestMapping("/")
public final class MainPage {
	private WebUiHolder webUiHolder;

	@RequestMapping(method = RequestMethod.GET)
	public final String firstPage(final ModelMap model) {
		model.addAttribute("buildVersion", ReleaseInfoCore.buildVersion());
		model.addAttribute("scaffoldViewerUrl", getWebUi().getScaffoldViewerUrl());

		return "mainpage";
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
