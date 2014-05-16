package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HorizontalPanel;
import edu.mayo.mprc.swift.ui.client.dialogs.Validatable;
import edu.mayo.mprc.swift.ui.client.rpc.ClientEnabledEngines;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngine;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngineConfig;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;
import edu.mayo.mprc.swift.ui.client.widgets.EngineVersionSelector;

import java.util.*;

/**
 * The enabled engines panel is pretty generic, with an exception of
 * the QuaMeter panel. That one also contains a combo box for selecting which
 * category does the search belong to.
 *
 * @author Roman Zenka
 */
public final class EnabledEnginesEditor extends HorizontalPanel implements Validatable {
	private final Map<String/*code*/, EngineVersionSelector> engines;

	/**
	 * The enabled engines panel needs access not only to a list of available engines,
	 * but also to metadata so it can offer special combo-box for QuaMeter search category.
	 *
	 * @param availableEngines List of available engines.
	 */
	public EnabledEnginesEditor(final Collection<ClientSearchEngine> availableEngines) {
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
		}
	}

	/**
	 * @return All the engines the user enabled, together with versions they picked.
	 */
	public ClientEnabledEngines getEnabledEngines() {
		if (engines == null) {
			return new ClientEnabledEngines();
		}
		final ArrayList<ClientSearchEngineConfig> result = new ArrayList<ClientSearchEngineConfig>(engines.size());
		for (final EngineVersionSelector selector : engines.values()) {
			if (selector.isEnabled()) {
				result.add(new ClientSearchEngineConfig(selector.getCode(), selector.getVersion()));
			}
		}
		return new ClientEnabledEngines(result);
	}

	public void setCurrentEngines(final ClientEnabledEngines enabledEngines) {
		if (enabledEngines == null || engines == null) {
			return;
		}
		// Disable all
		for (final EngineVersionSelector selector : engines.values()) {
			selector.setEnabled(false);
		}
		// Enable selected
		for (final ClientSearchEngineConfig config : enabledEngines.getEnabledEngines()) {
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

	@Override
	public ClientValue getValue() {
		return getEnabledEngines();
	}

	@Override
	public void setValue(final ClientValue value) {
		if (value instanceof ClientEnabledEngines) {
			setCurrentEngines((ClientEnabledEngines) value);
		}
	}

	@Override
	public void setValue(ClientValue value, boolean fireEvents) {
		ClientValueUtils.setValue(this, value, fireEvents);
	}

	@Override
	public void focus() {
		if (!engines.isEmpty()) {
			engines.values().iterator().next().focus();
		}
	}

	@Override
	public void setValidationSeverity(int validationSeverity) {
		ValidationController.setValidationSeverity(validationSeverity, asWidget());
	}

	@Override
	public boolean needsAllowedValues() {
		return false;
	}

	@Override
	public void setAllowedValues(List<? extends ClientValue> values) {
	}

	@Override
	public void setEnabled(final boolean enabled) {
		for (EngineVersionSelector selector : engines.values()) {
			selector.setEnabled(enabled);
		}
	}

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<ClientValue> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}
}
