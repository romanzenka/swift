package edu.mayo.mprc.swift.resources;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.RunningApplicationContext;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import org.apache.log4j.Logger;

/**
 * @author Roman Zenka
 */
public class WebUiHolder {
	private static final Logger LOGGER = Logger.getLogger(WebUiHolder.class);
	private RunningApplicationContext context;

	public WebUi getWebUi() {
		final RunningApplicationContext context = getContext();
		ResourceConfig webUiConfig = null;
		try {
			webUiConfig = context.getSingletonConfig(WebUi.Config.class);
		} catch (MprcException e) {
			LOGGER.warn("Cannot obtain web config", e);
			// If this fails, we recover and use a default config
		}
		if (webUiConfig == null) {
			// Config file does not define web ui. Return the default one
			webUiConfig = new WebUi.Config(null, "8080", "Swift", "/", "/", "var/conf/swift.conf", null, null);
		}
		final Object resource = context.createResource(webUiConfig);
		if (!(resource instanceof WebUi)) {
			ExceptionUtilities.throwCastException(resource, WebUi.class);
			return null;
		}
		return (WebUi) resource;

	}

	public void stopSwiftMonitor() {
		if (getWebUi() != null) {
			getWebUi().stopSwiftMonitor();
		}
	}

	public RunningApplicationContext getContext() {
		return context;
	}

	public void setContext(final RunningApplicationContext context) {
		this.context = context;
	}
}
