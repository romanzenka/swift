package edu.mayo.mprc.swift.configuration.server;

import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.PropertyChangeListener;
import edu.mayo.mprc.config.ui.UiBuilder;

import java.util.List;
import java.util.Map;

/**
 * A special {@link edu.mayo.mprc.config.ui.UiBuilder} that collects property default settings.
 * Using this builder we can collect.
 */
class DefaultSettingUiBuilder implements UiBuilder {
	private String lastPropertyName;
	private Map<String, String> values;
	private DependencyResolver resolver;

	public DefaultSettingUiBuilder(final Map<String, String> initialValues, final DependencyResolver resolver) {
		values = initialValues;
		this.resolver = resolver;
	}

	public Map<String, String> getValues() {
		return values;
	}

	@Override
	public UiBuilder nativeInterface(final String className) {
		return this;
	}

	@Override
	public UiBuilder property(final String name, final String displayName, final String description) {
		// Remember the property name
		lastPropertyName = name;
		return this;
	}

	@Override
	public UiBuilder required() {
		return this;
	}

	@Override
	public UiBuilder defaultValue(final String value) {
		// Store the default value to the last property
		values.put(lastPropertyName, value);
		return this;
	}

	@Override
	public UiBuilder defaultValue(final ResourceConfig value) {
		values.put(lastPropertyName, resolver.getIdFromConfig(value));
		return this;
	}

	@Override
	public UiBuilder addChangeListener(final PropertyChangeListener listener) {
		return this;
	}

	@Override
	public UiBuilder addDaemonChangeListener(final PropertyChangeListener listener) {
		return this;
	}

	@Override
	public UiBuilder validateOnDemand(final PropertyChangeListener validator) {
		return this;
	}

	@Override
	public UiBuilder boolValue() {
		return this;
	}

	@Override
	public UiBuilder existingDirectory() {
		return this;
	}

	@Override
	public UiBuilder existingFile() {
		return this;
	}

	@Override
	public UiBuilder integerValue(final Integer minimum, final Integer maximum) {
		return this;
	}

	@Override
	public UiBuilder executable(final List<String> commandLineParams) {
		return this;
	}

	@Override
	public UiBuilder reference(final String... type) {
		return this;
	}

	@Override
	public UiBuilder enable(final String propertyName, final boolean synchronous) {
		return this;
	}
}
