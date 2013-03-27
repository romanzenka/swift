package edu.mayo.mprc.swift.ui.client.widgets;

import edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngineConfig;

import java.util.ArrayList;

/**
 * @author Roman Zenka
 */
public final class EnabledEnginesPanel {
	private ArrayList<ClientSearchEngineConfig> enabledEngines;

	public ArrayList<ClientSearchEngineConfig> getEnabledEngines() {
		return enabledEngines;
	}
}
