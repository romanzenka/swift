package edu.mayo.mprc.config;

/**
 * A reader that can load a given config using a map of key->value pairs.
 *
 * @author Roman Zenka
 */
public final class MapConfigReader extends ConfigReaderBase {
	private final DependencyResolver resolver;
	private final PropertyValues<String> values;

	public MapConfigReader(final DependencyResolver resolver, final PropertyValues values) {
		this.values = values;
		this.resolver = resolver;
	}

	@Override
	public String get(final String key) {
		return values.getValue(key);
	}

	@Override
	public ResourceConfig getObjectFromId(final String id) {
		return resolver.getConfigFromId(id);
	}

	@Override
	public Iterable<String> getKeys() {
		return values.keySet();
	}

	public static void load(final ResourceConfig resourceConfig, PropertyValues<String> values, final DependencyResolver resolver) {
		resourceConfig.load(new MapConfigReader(resolver, values));
	}
}
