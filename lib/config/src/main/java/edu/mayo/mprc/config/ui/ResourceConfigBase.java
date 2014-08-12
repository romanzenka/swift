package edu.mayo.mprc.config.ui;

import com.google.common.base.Strings;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ConfigReader;
import edu.mayo.mprc.config.ConfigWriter;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.ResourceConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A base for trivial resource configs that only store values as strings.
 * You can get away from implementing your own setup.
 *
 * @author Roman Zenka
 */
public abstract class ResourceConfigBase implements ResourceConfig {
	private Map<String, String> data = new TreeMap<String, String>();

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
		DescriptionCollectingUiBuilder descriptions = getFieldDescriptions();

		for (final Map.Entry<String, String> entry : data.entrySet()) {
			final String key = entry.getKey();
			final String value = entry.getValue();
			if (!Strings.isNullOrEmpty(value)) {
				writer.put(key, value, Strings.nullToEmpty(descriptions.getValue(key)));
			}
		}
	}

	private DescriptionCollectingUiBuilder getFieldDescriptions() {
		final DescriptionCollectingUiBuilder builder = new DescriptionCollectingUiBuilder();
		getUiFactory().createUI(new DaemonConfig(), this, builder);
		return builder;
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

}
