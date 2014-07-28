package edu.mayo.mprc.config.ui;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
public abstract class HierarchicalUiBuilder<T> implements UiBuilder, PropertyValues<T> {
	private static final Pattern SUB_PROPERTY = Pattern.compile("([^.]+)\\.\\d+\\.(.*)");
	private HierarchicalUiBuilder parent;
	private Map<String/*prefix*/, HierarchicalUiBuilder<T>> children = new HashMap<String, HierarchicalUiBuilder<T>>();

	public HierarchicalUiBuilder(final HierarchicalUiBuilder parent) {
		this.parent = parent;
	}

	/**
	 * @return A builder for making a given property array.
	 */
	protected abstract HierarchicalUiBuilder createSubBuilder();

	/**
	 * @param propertyName Name of a property
	 * @return Whatever value we collected for that property.
	 */
	protected abstract T getValueForSimpleProperty(String propertyName);

	public abstract Iterable<String> keySet();

	@Override
	public UiBuilder nativeInterface(final String className) {
		return this;
	}

	@Override
	public UiBuilder property(final String name, final String displayName, final String description) {
		return this;
	}

	@Override
	public UiBuilder propertyArray(String prefix, String displayName, String description) {
		HierarchicalUiBuilder child = createSubBuilder();
		children.put(prefix, child);
		return child;
	}

	@Override
	public UiBuilder propertyArrayEnd() {
		if (parent == null) {
			throw new MprcException("Programmer error: mismatched calls to propertyArray and propertyArrayEnd");
		}
		return parent;
	}

	@Override
	public UiBuilder required() {
		return this;
	}

	@Override
	public UiBuilder defaultValue(final String value) {
		return this;
	}

	@Override
	public UiBuilder defaultValue(final ResourceConfig value) {
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

	@Override
	public T getValue(String propertyName) {
		Matcher matcher = SUB_PROPERTY.matcher(propertyName);
		if (!matcher.matches()) {
			return getValueForSimpleProperty(propertyName);
		} else {
			final String prefix = matcher.group(1);
			final String subPropertyName = matcher.group(2);
			final HierarchicalUiBuilder<T> value = children.get(prefix);
			if (value != null) {
				return value.getValue(subPropertyName);
			}
			ExceptionUtilities.throwCastException(value, HierarchicalUiBuilder.class);
			return null;
		}
	}
}
