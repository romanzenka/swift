package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.HorizontalPanel;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngine;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngineConfig;

import java.util.*;

/**
 * @author Roman Zenka
 */
public final class EnabledEnginesPanel extends HorizontalPanel {
	private final Map<String/*code*/, EngineVersionSelector> engines;

	public EnabledEnginesPanel(final List<ClientSearchEngine> availableEngines) {
		engines = new HashMap<String, EngineVersionSelector>(availableEngines.size());

		for (final ClientSearchEngine engine : availableEngines) {
			final String code = engine.getEngineConfig().getCode();
			if (engines.containsKey(code)) {
				engines.get(code).addEngine(engine);
			} else {
				EngineVersionSelector selector = new EngineVersionSelector(engine);
				engines.put(code, selector);
			}
		}
		for (final EngineVersionSelector selector : engines.values()) {
			selector.done();
		}

		List<EngineVersionSelector> list = new ArrayList<EngineVersionSelector>(engines.values());
		Collections.sort(list);
		for (final EngineVersionSelector selector : list) {
			this.add(selector);
		}
	}

	/**
	 * @return All the engines the user enabled, together with versions they picked.
	 */
	public ArrayList<ClientSearchEngineConfig> getEnabledEngines() {
		final ArrayList<ClientSearchEngineConfig> result = new ArrayList<ClientSearchEngineConfig>(engines.size());
		for (final EngineVersionSelector selector : engines.values()) {
			if (selector.isEnabled()) {
				result.add(new ClientSearchEngineConfig(selector.getCode(), selector.getVersion()));
			}
		}
		return result;
	}
}
