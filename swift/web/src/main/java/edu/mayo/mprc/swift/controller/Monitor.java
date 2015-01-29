package edu.mayo.mprc.swift.controller;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.monitor.DaemonStatus;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author Roman Zenka
 */
@Controller
public final class Monitor {
	private WebUiHolder webUiHolder;

	@RequestMapping("/monitor")
	public String monitor(ModelMap model) {
		final Map<DaemonConnection, DaemonStatus> monitoredConnections = getWebUi().getSwiftMonitor().getMonitoredConnections();
		model.addAttribute("monitoredConnections", monitoredConnections);

		return "monitor";
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
