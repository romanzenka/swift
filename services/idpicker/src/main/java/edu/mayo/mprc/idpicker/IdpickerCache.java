package edu.mayo.mprc.idpicker;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkCache;

/**
 * @author Roman Zenka
 */
public final class IdpickerCache extends WorkCache<IdpickerWorkPacket> {
	public static final String TYPE = "idpickerCache";
	public static final String NAME = "IDPicker Cache";
	public static final String DESC = "<p>Caches IDPicker result files.</p>";

	public IdpickerCache() {
	}

	public static final class Config extends CacheConfig {
		public Config() {
		}
	}

	public static final class Factory extends WorkCache.Factory<Config> {
		private static IdpickerCache cache;

		@Override
		public WorkCache getCache() {
			return cache;
		}

		@Override
		public WorkCache createCache(final Config config, final DependencyResolver dependencies) {
			return cache = new IdpickerCache();
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String DEFAULT_CACHE = "var/cache/idpicker";

		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(CacheConfig.CACHE_FOLDER, "IDPicker cache folder", "IDPicker .</p>")
					.required()
					.defaultValue(DEFAULT_CACHE)

					.property(CacheConfig.SERVICE, "IDPicker instance", "The module that will run IDPicker. The cache just caches the results.")
					.reference("idpicker", UiBuilder.NONE_TYPE);
		}
	}
}