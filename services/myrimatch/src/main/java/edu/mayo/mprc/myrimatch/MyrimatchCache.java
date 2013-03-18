package edu.mayo.mprc.myrimatch;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkCache;
import org.springframework.stereotype.Component;

public final class MyrimatchCache extends WorkCache<MyrimatchWorkPacket> {
	public static final String TYPE = "myrimatchCache";
	public static final String NAME = "Myrimatch Cache";
	public static final String DESC = "Caches previous Myrimatch search results. <p>Speeds up consecutive Myrimatch searches if the same file with same parameters is processed multiple times.</p>";

	public MyrimatchCache() {
	}

	public static final class Config extends WorkCache.CacheConfig {
		public Config() {
		}
	}

	@Component("myrimatchCacheFactory")
	public static final class Factory extends WorkCache.Factory<Config> {
		private static MyrimatchCache cache;

		@Override
		public WorkCache getCache() {
			return cache;
		}

		@Override
		public WorkCache createCache(final Config config, final DependencyResolver dependencies) {
			return cache = new MyrimatchCache();
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String DEFAULT_CACHE = "var/cache/myrimatch";

		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(WorkCache.CacheConfig.CACHE_FOLDER, "Myrimatch cache folder", "When a file gets searched by Myrimatch, the result is stored in this folder. Subsequent searches of the same file with same parameters use the cached value."
							+ "<p>Ideally, this folder would be on a fast, potentially less reliable storage.</p>")
					.required()
					.defaultValue(DEFAULT_CACHE)

					.property(WorkCache.CacheConfig.SERVICE, "Myrimatch Search Engine", "The Myrimatch engine that will do the search. The cache just caches the results.")
					.reference("myrimatch", UiBuilder.NONE_TYPE);
		}
	}
}
