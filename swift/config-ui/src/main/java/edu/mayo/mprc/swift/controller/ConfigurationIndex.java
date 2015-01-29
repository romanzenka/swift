package edu.mayo.mprc.swift.controller;

import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;

/**
 * @author Roman Zenka
 */
@Controller
public final class ConfigurationIndex {
	private WebUiHolder webUiHolder;

	@RequestMapping("/configuration")
	public String configuration(final ModelMap model) {
		final WebUi webUi = getWebUiHolder().getWebUi();
		model.addAttribute("newConfigPath", webUi.getNewConfigFile().getAbsolutePath());

		return "configuration/index";
	}

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	@Resource(name = "webUiHolder")
	public void setWebUiHolder(WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}
}
