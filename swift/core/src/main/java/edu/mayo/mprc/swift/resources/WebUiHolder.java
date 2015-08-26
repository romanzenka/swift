package edu.mayo.mprc.swift.resources;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.RunningApplicationContext;
import edu.mayo.mprc.swift.Swift;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.io.File;

/**
 * @author Roman Zenka
 */
public class WebUiHolder {
	private static final Logger LOGGER = Logger.getLogger(WebUiHolder.class);
	private RunningApplicationContext context;
	private File swiftHome = new File(".").getAbsoluteFile();
	private DateTime startTime;

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
			webUiConfig = new WebUi.Config(null, "8080", "Swift", "/", "/", new File(getSwiftHome(), Swift.DEFAULT_NEW_CONFIG_FILE).getAbsolutePath(), null, null, null);
		}
		final Object resource = context.createResource(webUiConfig);
		if (!(resource instanceof WebUi)) {
			ExceptionUtilities.throwCastException(resource, WebUi.class);
			return null;
		}
		return (WebUi) resource;
	}

	public InstrumentSerialNumberMapper getInstrumentSerialNumberMapper() {
		return getWebUi();
	}

	public RunningApplicationContext getContext() {
		return context;
	}

	public void setContext(final RunningApplicationContext context) {
		this.context = context;
	}

	public File getSwiftHome() {
		return swiftHome;
	}

	/**
	 * This can be reset by the servlet initialization process, otherwise cwd is used.
	 */
	public void setSwiftHome(File swiftHome) {
		this.swiftHome = swiftHome;
	}

	public DateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(DateTime startTime) {
		this.startTime = startTime;
	}
}
