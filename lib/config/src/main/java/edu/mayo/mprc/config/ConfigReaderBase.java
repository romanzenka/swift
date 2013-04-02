package edu.mayo.mprc.config;

import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Zenka
 */
public abstract class ConfigReaderBase implements ConfigReader {
	/**
	 * @param key Key whose value to retrieve.
	 * @return Boolean value parsed from the string using {@link Boolean#parseBoolean(String)}.
	 */
	@Override
	public boolean getBoolean(final String key) {
		return Boolean.parseBoolean(get(key));
	}

	@Override
	public int getInteger(final String key) {
		return Integer.parseInt(get(key));
	}

	@Override
	public List<? extends ResourceConfig> getResourceList(final String key) {
		final String resources = get(key);
		final Iterable<String> split = Splitter.on(",").trimResults().omitEmptyStrings().split(resources);
		final ArrayList<ResourceConfig> configs = new ArrayList<ResourceConfig>();
		for (final String item : split) {
			configs.add(getObject(item));
		}
		return configs;
	}
}
