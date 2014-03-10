package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.HorizontalPanel;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngine;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngineConfig;

import java.util.*;

/**
 * The enabled engines panel is pretty generic, with an exception of
 * the QuaMeter panel. That one also contains a combo box for selecting which
 * category does the search belong to.
 *
 * @author Roman Zenka
 */
public final class EnabledEnginesPanel extends HorizontalPanel {
	private final Map<String/*code*/, EngineVersionSelector> engines;
	private QuameterCategorySelector quameterCategorySelector;

	/**
	 * The enabled engines panel needs access not only to a list of available engines,
	 * but also to metadata so it can offer special combo-box for QuaMeter search category.
	 *
	 * @param availableEngines List of available engines.
	 * @param searchMetadata   Generic metadata associated with the search.
	 * @param uiConfiguration  Configuration of the user interface itself.
	 */
	public EnabledEnginesPanel(final Collection<ClientSearchEngine> availableEngines,
	                           final SearchMetadata searchMetadata,
	                           final UiConfiguration uiConfiguration) {
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
			add(selector);
			if ("QUAMETER".equals(selector.getCode())) {
				quameterCategorySelector = new QuameterCategorySelector(uiConfiguration, searchMetadata);
				add(quameterCategorySelector);
			}
		}
	}

	/**
	 * @return All the engines the user enabled, together with versions they picked.
	 */
	public ArrayList<ClientSearchEngineConfig> getEnabledEngines() {
		if (engines == null) {
			return new ArrayList<ClientSearchEngineConfig>(0);
		}
		final ArrayList<ClientSearchEngineConfig> result = new ArrayList<ClientSearchEngineConfig>(engines.size());
		for (final EngineVersionSelector selector : engines.values()) {
			if (selector.isEnabled()) {
				result.add(new ClientSearchEngineConfig(selector.getCode(), selector.getVersion()));
			}
		}
		return result;
	}

	public void setCurrentEngines(final Iterable<ClientSearchEngineConfig> enabledEngines) {
		if (enabledEngines == null || engines == null) {
			return;
		}
		// Disable all
		for (final EngineVersionSelector selector : engines.values()) {
			selector.setEnabled(false);
		}
		// Enable selected
		for (final ClientSearchEngineConfig config : enabledEngines) {
			final String code = config.getCode();
			final EngineVersionSelector selector = engines.get(code);
			if (selector == null) {
				reportWarning("The engine " + code + " is not available.");
			} else {
				if (!selector.selectVersion(config.getVersion())) {
					reportWarning("Requested " + code + " version " + config.getVersion() + " not available. Using " + selector.getVersion() + " instead.");
				}
				selector.setEnabled(true);
			}
		}
	}

	private void reportWarning(final String warning) {
		// Do nothing now
		int i = 0;
	}
}
