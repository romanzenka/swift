package edu.mayo.mprc.config;

import com.google.common.base.Strings;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ui.PropertyChangeListener;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A base for trivial resource configs that only store values as strings.
 * You can get away from implementing your own setup.
 *
 * @author Roman Zenka
 */
public abstract class ResourceConfigBase implements ResourceConfig {
	private Map<String, String> data = new HashMap<String, String>();

	/**
	 * Get value for given key.
	 *
	 * @param key Key to get value for.
	 * @return The value for the key.
	 */
	public String get(final String key) {
		return data.get(key);
	}

	public String put(final String key, final String value) {
		return data.put(key, value);
	}

	/**
	 * Get Ui factory for this config. This is done by looking at a class implementing
	 * {@link ServiceUiFactory} that is next to this class within the same parent class.
	 *
	 * @return The matching UI factory.
	 */
	public ServiceUiFactory getUiFactory() {
		final Class<?>[] classes = getClass().getEnclosingClass().getClasses();
		for (final Class<?> clazz : classes) {
			if (ServiceUiFactory.class.isAssignableFrom(clazz)) {
				try {
					return (ServiceUiFactory) clazz.newInstance();
				} catch (InstantiationException e) {
					throw new MprcException(e);
				} catch (IllegalAccessException e) {
					throw new MprcException(e);
				}
			}
		}
		throw new MprcException("No UI defined for class " + this.getClass().getName());
	}

	@Override
	public void save(final ConfigWriter writer) {
		final Map<String, String> descriptions = getFieldDescriptions();

		for (final Map.Entry<String, String> entry : data.entrySet()) {
			final String key = entry.getKey();
			writer.put(key, entry.getValue(), Strings.nullToEmpty(descriptions.get(key)));
		}
	}

	private Map<String, String> getFieldDescriptions() {
		final DescriptionCollectingUiBuilder builder = new DescriptionCollectingUiBuilder();
		getUiFactory().createUI(new DaemonConfig(), this, builder);
		return builder.getDescriptions();
	}

	@Override
	public void load(final ConfigReader reader) {
		data = new HashMap<String, String>();
		final Iterable<String> keys = reader.getKeys();
		for (final String key : keys) {
			data.put(key, reader.get(key));
		}
	}

	@Override
	public int getPriority() {
		return 0;
	}

	private static class DescriptionCollectingUiBuilder implements UiBuilder {
		private Map<String, String> descriptions = new HashMap<String, String>();

		private Map<String, String> getDescriptions() {
			return descriptions;
		}

		@Override
		public UiBuilder nativeInterface(final String className) {
			return this;
		}

		@Override
		public UiBuilder property(final String name, final String displayName, final String description) {
			descriptions.put(name, displayName);
			return this;
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
	}
}
