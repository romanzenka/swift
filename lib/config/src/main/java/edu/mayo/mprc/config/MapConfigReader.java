package edu.mayo.mprc.config;

import java.util.List;
import java.util.Map;

/**
 * A reader that can load a given config using a map of key->value pairs.
 *
 * @author Roman Zenka
 */
public final class MapConfigReader implements ConfigReader {
	private final DependencyResolver resolver;
	private final Map<String, String> values;

	public MapConfigReader(final DependencyResolver resolver, final Map<String, String> values) {
		this.values = values;
		this.resolver = resolver;
	}

	@Override
	public String get(final String key) {
		return values.get(key);
	}

	@Override
	public boolean getBoolean(final String key) {
		return Boolean.parseBoolean(get(key));
	}

	@Override
	public int getInteger(final String key) {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public ResourceConfig getObject(final String key) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public List<? extends ResourceConfig> getResourceList(final String key) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Iterable<String> getKeys() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public static void load(ResourceConfig resourceConfig, Map<String, String> values, DependencyResolver resolver) {
		resourceConfig.load(new MapConfigReader(resolver, values));
	}
}
