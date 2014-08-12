package edu.mayo.mprc.config.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects descriptions for all the properties.
 *
 * @author Roman Zenka
 */
class DescriptionCollectingUiBuilder extends HierarchicalUiBuilder<String> {
	private Map<String, String> descriptions = new HashMap<String, String>();

	DescriptionCollectingUiBuilder() {
		this(null);
	}

	DescriptionCollectingUiBuilder(final DescriptionCollectingUiBuilder parent) {
		super(parent);
	}

	@Override
	protected HierarchicalUiBuilder createSubBuilder() {
		return new DescriptionCollectingUiBuilder(this);
	}

	@Override
	protected String getValueForSimpleProperty(String propertyName) {
		return descriptions.get(propertyName);
	}

	@Override
	public Iterable<String> keySet() {
		return descriptions.keySet();
	}

	@Override
	public UiBuilder property(final String name, final String displayName, final String description) {
		super.property(name, displayName, description);
		descriptions.put(name, displayName);
		return this;
	}
}
