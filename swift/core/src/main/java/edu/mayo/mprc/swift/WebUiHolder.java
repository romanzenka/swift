package edu.mayo.mprc.swift;

/**
 * @author Roman Zenka
 */
public final class WebUiHolder {
	WebUi webUi;

	public WebUi getWebUi() {
		return webUi;
	}

	public void setWebUi(final WebUi webUi) {
		this.webUi = webUi;
	}

	public void stopSwiftMonitor() {
		if (getWebUi() != null) {
			getWebUi().stopSwiftMonitor();
		}
	}
}
