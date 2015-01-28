package edu.mayo.mprc.swift.controller;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.heme.HemeEntry;
import edu.mayo.mprc.heme.HemeTestStatus;
import edu.mayo.mprc.heme.HemeUi;
import edu.mayo.mprc.swift.commands.SwiftEnvironment;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
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
public final class Heme {
	private SwiftEnvironment environment;
	private WebUiHolder webUiHolder;

	@RequestMapping(value = "/heme", method = RequestMethod.GET)
	public String heme(final ModelMap model) {
		final ResourceConfig hemeUiConfig = environment.getSingletonConfig(HemeUi.Config.class);
		model.addAttribute("hemeUiConfig", hemeUiConfig);
		model.addAttribute("daemonName", environment.getDaemonConfig().getName());
		model.addAttribute("notStarted", HemeTestStatus.NOT_STARTED);
		model.addAttribute("failed", HemeTestStatus.FAILED);
		model.addAttribute("running", HemeTestStatus.RUNNING);
		model.addAttribute("success", HemeTestStatus.SUCCESS);

		if (hemeUiConfig != null) {
			HemeUi hemeUi = (HemeUi) environment.createResource(hemeUiConfig);
			hemeUi.begin();
			try {
				hemeUi.scanFileSystem();
				final List<HemeEntry> currentEntries = hemeUi.getCurrentEntries();
				hemeUi.commit();
				model.addAttribute("currentEntries", currentEntries);
			} catch (Exception e) {
				hemeUi.rollback();
				throw new MprcException(e);
			}
		}

		return "heme/heme";
	}

	public SwiftEnvironment getEnvironment() {
		return environment;
	}

	@Resource(name = "swiftEnvironment")
	public void setEnvironment(SwiftEnvironment environment) {
		this.environment = environment;
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
