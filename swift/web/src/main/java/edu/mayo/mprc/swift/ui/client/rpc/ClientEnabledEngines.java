package edu.mayo.mprc.swift.ui.client.rpc;

import java.util.ArrayList;

/**
 * @author Roman Zenka
 */
public final class ClientEnabledEngines implements ClientValue {
	private static final long serialVersionUID = 5954423554370719968L;

	ArrayList<ClientSearchEngineConfig> enabledEngines;

	public ClientEnabledEngines() {
		this(new ArrayList<ClientSearchEngineConfig>(0));
	}

	public ClientEnabledEngines(ArrayList<ClientSearchEngineConfig> enabledEngines) {
		this.enabledEngines = enabledEngines;
	}

	public ArrayList<ClientSearchEngineConfig> getEnabledEngines() {
		return enabledEngines;
	}

	public void setEnabledEngines(ArrayList<ClientSearchEngineConfig> enabledEngines) {
		this.enabledEngines = enabledEngines;
	}
}
