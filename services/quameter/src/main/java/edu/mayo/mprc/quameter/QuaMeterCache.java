package edu.mayo.mprc.quameter;

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
public final class QuaMeterCache extends WorkCache<QuaMeterWorkPacket> {
	public static final String TYPE = "quameterCache";
	public static final String NAME = "QuaMeter Cache";
	public static final String DESC = "Caches previous QuaMeter search results. <p>Speeds up consecutive QuaMeter searches if the same file with same parameters is processed multiple times.</p>";

	public QuaMeterCache() {
	}

	public static final class Config extends WorkCache.CacheConfig {
		public Config() {
		}
	}

	@Component("quaMeterCacheFactory")
	public static final class Factory extends WorkCache.Factory<Config> {
		@Override
		public WorkCache createCache(final Config config, final DependencyResolver dependencies) {
			return new QuaMeterCache();
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String DEFAULT_CACHE = "var/cache/quameter";

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(WorkCache.CacheConfig.CACHE_FOLDER, "QuaMeter cache folder", "When a file gets searched by QuaMeter, the result is stored in this folder. Subsequent searches of the same file with same parameters use the cached value."
							+ "<p>Ideally, this folder would be on a fast, potentially less reliable storage.</p>")
					.required()
					.defaultValue(DEFAULT_CACHE)

					.property(WorkCache.CacheConfig.SERVICE, "QuaMeter Search Engine", "The QuaMeter engine that will do the search. The cache just caches the results.")
					.reference("quameter", UiBuilder.NONE_TYPE);
		}
	}
}
