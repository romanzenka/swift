package edu.mayo.mprc.config;

import edu.mayo.mprc.config.ui.HierarchicalUiBuilder;
import edu.mayo.mprc.config.ui.UiBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * A special {@link edu.mayo.mprc.config.ui.UiBuilder} that collects property default settings.
 * Using this builder we can collect.
 */
public class DefaultSettingUiBuilder extends HierarchicalUiBuilder<String> {
	private String lastPropertyName;
	private Map<String, String> values;
	private DependencyResolver resolver;

	public DefaultSettingUiBuilder(final Map<String, String> initialValues, final DependencyResolver resolver) {
		this(null, initialValues, resolver);
	}

	public DefaultSettingUiBuilder(final HierarchicalUiBuilder parent, final Map<String, String> values, final DependencyResolver resolver) {
		super(parent);
		this.values = new HashMap<String, String>(values);
		this.resolver = resolver;
	}

	@Override
	protected HierarchicalUiBuilder createSubBuilder() {
		return new DefaultSettingUiBuilder(this, new HashMap<String, String>(), resolver);
	}

	@Override
	protected String getValueForSimpleProperty(final String propertyName) {
		return values.get(propertyName);
	}

	@Override
	public Iterable<String> keySet() {
		return values.keySet();
	}

	@Override
	public UiBuilder property(final String name, final String displayName, final String description) {
		super.property(name, displayName, description);
		// Remember the property name
		lastPropertyName = name;
		return this;
	}

	@Override
	public UiBuilder defaultValue(final String value) {
		super.defaultValue(value);
		// Store the default value to the last property
		values.put(lastPropertyName, value);
		return this;
	}

	@Override
	public UiBuilder defaultValue(final ResourceConfig value) {
		super.defaultValue(value);
		values.put(lastPropertyName, resolver.getIdFromConfig(value));
		return this;
	}
}
