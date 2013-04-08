package edu.mayo.mprc.config;

import java.util.HashMap;
import java.util.Map;

/**
 * A config writer producing a map of key->value pairs for the saved object.
 *
 * @author Roman Zenka
 */
public final class MapConfigWriter extends ConfigWriterBase {
	private final DependencyResolver resolver;
	private final Map<String, String> values = new HashMap<String, String>();

	public MapConfigWriter(final DependencyResolver resolver) {
		this.resolver = resolver;
	}

	public Map<String, String> getMap() {
		return values;
	}

	@Override
	public void put(final String key, final String value, final String comment) {
		values.put(key, value);
	}

	@Override
	public void comment(final String comment) {
		// Ignore comments
	}

	@Override
	public String save(final ResourceConfig resourceConfig) {
		if (resourceConfig == null) {
			return "";
		}
		final String code = resolver.getIdFromConfig(resourceConfig);
		if (code == null) {
			resolver.addConfig(resourceConfig);
		 	return resolver.getIdFromConfig(resourceConfig);
		} else {
			return code;
		}
	}

	public static Map<String, String> save(final ResourceConfig resourceConfig, final DependencyResolver resolver) {
		final MapConfigWriter writer = new MapConfigWriter(resolver);
		resourceConfig.save(writer);
		return writer.getMap();
	}
}
