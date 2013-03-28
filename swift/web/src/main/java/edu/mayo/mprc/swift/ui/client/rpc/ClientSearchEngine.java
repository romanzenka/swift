package edu.mayo.mprc.swift.ui.client.rpc;

import java.io.Serializable;

/**
 * Abbreviated information about a search engine - the only part that the UI cares about
 */
public final class ClientSearchEngine implements Serializable {
	private static final long serialVersionUID = 20111119L;
	private ClientSearchEngineConfig engineConfig;
	private String friendlyName;
	private boolean isOnByDefault;
	private int order;

	public ClientSearchEngine() {
	}

	/**
	 * @param engineConfig  Reference to a particular engine config.
	 * @param friendlyName  The name we display for the user.
	 * @param isOnByDefault If this is true, the search engine's checkboxes will be checked by default.
	 */
	public ClientSearchEngine(final ClientSearchEngineConfig engineConfig, final String friendlyName, final boolean isOnByDefault, final int order) {
		this.engineConfig = engineConfig;
		this.friendlyName = friendlyName;
		this.isOnByDefault = isOnByDefault;
		this.order = order;
	}

	public ClientSearchEngineConfig getEngineConfig() {
		return engineConfig;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public boolean isOnByDefault() {
		return isOnByDefault;
	}

	public int getOrder() {
		return order;
	}
}
