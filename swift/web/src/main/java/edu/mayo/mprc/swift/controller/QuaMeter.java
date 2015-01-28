package edu.mayo.mprc.swift.controller;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.quameterdb.QuameterUi;
import edu.mayo.mprc.swift.commands.SwiftEnvironment;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.io.StringWriter;

/**
 * @author Roman Zenka
 */
@Controller
public final class QuaMeter {
	private SwiftEnvironment environment;

	private WebUiHolder webUiHolder;

	@RequestMapping(value = "/quameter", method = RequestMethod.GET)
	public final String quameter(final ModelMap model) {

		final QuameterUi quameterUi = getQuameterUi();
		model.addAttribute("quameterUi", quameterUi);
		model.addAttribute("title", getWebUi().getTitle());
		model.addAttribute("daemonName", environment.getDaemonConfig().getName());

		if (quameterUi != null) {
			final StringWriter writer = new StringWriter(10000);
			quameterUi.writeMetricsJson(writer);
			model.addAttribute("metricsJson", writer.toString());
		} else {
			model.addAttribute("metricsJson", "null");
		}

		if (quameterUi != null) {
			quameterUi.begin();
			try {
				final StringWriter writer = new StringWriter(10000);
				quameterUi.dataTableJson(writer);
				quameterUi.commit();
				model.addAttribute("dataJson", writer.toString());
			} catch (Exception e) {
				quameterUi.rollback();
				throw new MprcException(e);
			}
		} else {
			model.addAttribute("dataJson", "null");
		}

		return "quameter/index";
	}

	private QuameterUi getQuameterUi() {
		final ResourceConfig quameterUiConfig = environment.getSingletonConfig(QuameterUi.Config.class);
		final QuameterUi quameterUi;
		if (quameterUiConfig != null) {
			quameterUi = (QuameterUi) environment.createResource(quameterUiConfig);
		} else {
			quameterUi = null;
		}
		return quameterUi;
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
