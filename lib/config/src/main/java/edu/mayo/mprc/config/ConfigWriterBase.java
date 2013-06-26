package edu.mayo.mprc.config;

import java.util.Collection;

/**
 * Simplifies implementation of {@link ConfigWriter}, provides some simple methods.
 *
 * @author Roman Zenka
 */
public abstract class ConfigWriterBase implements ConfigWriter {
	@Override
	public void put(final String key, final String value) {
		put(key, value, "");
	}

	@Override
	public void put(final String key, final boolean value) {
		put(key, value, "");
	}

	@Override
	public void put(final String key, final boolean value, final String comment) {
		put(key, Boolean.toString(value), comment);
	}

	@Override
	public void put(final String key, final int value, final String comment) {
		put(key, Integer.toString(value), comment);
	}

	@Override
	public void put(final String key, final int value, final int defaultValue, final String comment) {
		put(key, value, comment);
	}

	@Override
	public void put(final String key, final ResourceConfig config) {
		put(key, save(config));
	}

	@Override
	public void put(final String key, final Collection<? extends ResourceConfig> configs) {
		put(key, save(configs));
	}

	@Override
	public String save(final Collection<? extends ResourceConfig> configs) {
		final StringBuilder builder = new StringBuilder(configs.size() * 10);
		for (final ResourceConfig config : configs) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(save(config));
		}
		return builder.toString();
	}
}
