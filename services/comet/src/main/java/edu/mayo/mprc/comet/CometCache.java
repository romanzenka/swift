package edu.mayo.mprc.comet;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkCache;
import org.springframework.stereotype.Component;

public final class CometCache extends WorkCache<CometWorkPacket> {
	public static final String TYPE = "cometCache";
	public static final String NAME = "Comet Cache";
	public static final String DESC = "Caches previous Comet search results. <p>Speeds up consecutive Comet searches if the same file with same parameters is processed multiple times.</p>";

	public CometCache() {
	}

	public static final class Config extends CacheConfig {
		public Config() {
		}
	}

	@Component("cometCacheFactory")
	public static final class Factory extends WorkCache.Factory<Config> {
		@Override
		public WorkCache createCache(final Config config, final DependencyResolver dependencies) {
			return new CometCache();
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String DEFAULT_CACHE = "var/cache/comet";

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(CacheConfig.CACHE_FOLDER, "Comet Cache Folder", "When a file gets searched by Comet, the result is stored in this folder. Subsequent searches of the same file with same parameters use the cached value."
							+ "<p>Ideally, this folder would be on a fast, potentially less reliable storage.</p>")
					.required()
					.defaultValue(DEFAULT_CACHE)

					.property(CacheConfig.SERVICE, "Comet Search Engine", "The Comet engine that will do the search. The cache just caches the results.")
					.reference("comet", UiBuilder.NONE_TYPE);
		}
	}
}
