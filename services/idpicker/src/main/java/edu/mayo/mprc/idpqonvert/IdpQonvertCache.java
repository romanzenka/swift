package edu.mayo.mprc.idpqonvert;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkCache;
import org.springframework.stereotype.Component;

/**
 * @author Roman Zenka
 */
public final class IdpQonvertCache extends WorkCache<IdpQonvertWorkPacket> {
	public static final String TYPE = "idpqonvertCache";
	public static final String NAME = "IdpQonvert Cache";
	public static final String DESC = "<p>Caches IdpQonvert result files.</p>";

	public IdpQonvertCache() {
	}

	public static final class Config extends CacheConfig {
		public Config() {
		}
	}

	@Component("idpQonvertCacheFactory")
	public static final class Factory extends WorkCache.Factory<Config> {
		private static IdpQonvertCache cache;

		@Override
		public WorkCache getCache() {
			return cache;
		}

		@Override
		public WorkCache createCache(final Config config, final DependencyResolver dependencies) {
			return cache = new IdpQonvertCache();
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String DEFAULT_CACHE = "var/cache/idpqonvert";

		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(CacheConfig.CACHE_FOLDER, "IdpQonvert cache folder", "IdpQonvert will store .idpdb files here.</p>")
					.required()
					.defaultValue(DEFAULT_CACHE)

					.property(CacheConfig.SERVICE, "IdpQonvert instance", "The module that will run IdpQonvert. The cache just caches the results.")
					.reference("idpqonvert", UiBuilder.NONE_TYPE);
		}
	}
}