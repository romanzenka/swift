package edu.mayo.mprc.config;

import edu.mayo.mprc.MprcException;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds currently instantiated triples (id, config, pair). When given one of these, returns the other two.
 */
public final class DependencyResolver {
	private ResourceFactory<ResourceConfig, Object> factory;
	private final Map<ResourceConfig, IdObjectPair> dependencies = new HashMap<ResourceConfig, IdObjectPair>();
	private int id = 1;

	public DependencyResolver(final ResourceFactory<ResourceConfig, Object> factory) {
		this.factory = factory;
	}

	/**
	 * Creates a new pair from given config. Keeps track of created objects, so next time
	 * same config is provided, identical pair is returned.
	 */
	public Object createSingleton(final ResourceConfig config) {
		final Object o = resolveDependencyQuietly(config);
		if (o != null) {
			return o;
		}
		return factory.createSingleton(config, this);
	}

	/**
	 * @param config Dependency configuration.
	 * @return Null if the dependency does not exist.
	 */
	public Object resolveDependencyQuietly(final ResourceConfig config) {
		final IdObjectPair objectPair = dependencies.get(config);
		return objectPair == null ? null : objectPair.getObject();
	}

	/**
	 * No pair, we are just mapping ids to configurations.
	 */
	public void addConfig(final String id, final ResourceConfig config) {
		testUniqueness(config);
		dependencies.put(config, new IdObjectPair(id, null));
	}

	public void addConfig(final ResourceConfig config) {
		setDependency(config, null);
	}

	public void setDependency(final ResourceConfig config, final Object dependency) {
		if (dependencies.containsKey(config)) {
			dependencies.put(config, new IdObjectPair(dependencies.get(config).getId(), dependency));

		} else {
			if (config instanceof NamedResource) {
				dependencies.put(config, new IdObjectPair(((NamedResource) config).getName(), dependency));
			} else {
				dependencies.put(config, new IdObjectPair(String.valueOf(id++), dependency));
			}
		}
	}

	private void testUniqueness(final ResourceConfig config) {
		if (dependencies.containsKey(config)) {
			throw new MprcException("Dependency for " + config + " already defined.");
		}
	}

	public String getIdFromDependency(final Object dependency) {
		for (final IdObjectPair idObject : dependencies.values()) {
			if (idObject.getObject().equals(dependency)) {
				return idObject.getId();
			}
		}
		return null;
	}

	public Object getDependencyFromId(final String id) {
		for (final IdObjectPair idObject : dependencies.values()) {
			if (idObject.getId().equals(id)) {
				return idObject.getObject();
			}
		}
		return null;
	}

	public String getIdFromConfig(final ResourceConfig config) {
		final IdObjectPair objectPair = dependencies.get(config);
		return objectPair == null ? null : objectPair.getId();
	}

	public ResourceConfig getConfigFromId(final String id) {
		for (final Map.Entry<ResourceConfig, IdObjectPair> entry : dependencies.entrySet()) {
			if (entry.getValue().getId().equals(id)) {
				return entry.getKey();
			}
		}
		return null;
	}

	/**
	 * This is actually quite nasty. We are saying that the same model's corresponding config has changed from
	 * one pair to another.
	 *
	 * @param oldConfig Old resource that used to be mapped.
	 * @param newConfig New resource to map in its place.
	 */
	public void changeConfigType(final ResourceConfig oldConfig, final ResourceConfig newConfig) {
		final IdObjectPair idObjectPair = dependencies.get(oldConfig);
		dependencies.remove(oldConfig);
		dependencies.put(newConfig, idObjectPair);
	}

	/**
	 * For a given class, return the only instance of previously created object that is assignable to this class.
	 *
	 * @param clazz Class to look for.
	 * @return Previously created object of a matching class.
	 */
	public Object getSingletonForType(final Class clazz) {
		Object result = null;
		for (final IdObjectPair pair : dependencies.values()) {
			Object object = pair.getObject();
			if (clazz.isInstance(object)) {
				if (result != null) {
					throw new MprcException("More than one configured object matches the requested class " + clazz.getName() + ":\n1)\t" + result + "\n2)\t" + object);
				}
				result = object;
			}
		}
		return result;
	}

	private static final class IdObjectPair {
		private final String id;
		private final Object object;

		private IdObjectPair(final String id, final Object object) {
			this.id = id;
			this.object = object;
		}

		public String getId() {
			return id;
		}

		public Object getObject() {
			return object;
		}
	}
}
                                       	