package edu.mayo.mprc.myrimatch;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkCache;
import org.springframework.stereotype.Component;

public final class MyriMatchCache extends WorkCache<MyriMatchWorkPacket> {
	public static final String TYPE = "myrimatchCache";
	public static final String NAME = "MyriMatch Cache";
	public static final String DESC = "Caches previous MyriMatch search results. <p>Speeds up consecutive MyriMatch searches if the same file with same parameters is processed multiple times.</p>";

	public MyriMatchCache() {
	}

	public static final class Config extends WorkCache.CacheConfig {
		public Config() {
		}
	}

	@Component("myrimatchCacheFactory")
	public static final class Factory extends WorkCache.Factory<Config> {
		private static MyriMatchCache cache;

		@Override
		public WorkCache getCache() {
			return cache;
		}

		@Override
		public WorkCache createCache(final Config config, final DependencyResolver dependencies) {
			return cache = new MyriMatchCache();
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String DEFAULT_CACHE = "var/cache/myrimatch";

		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(WorkCache.CacheConfig.CACHE_FOLDER, "MyriMatch cache folder", "When a file gets searched by MyriMatch, the result is stored in this folder. Subsequent searches of the same file with same parameters use the cached value."
							+ "<p>Ideally, this folder would be on a fast, potentially less reliable storage.</p>")
					.required()
					.defaultValue(DEFAULT_CACHE)

					.property(WorkCache.CacheConfig.SERVICE, "MyriMatch Search Engine", "The MyriMatch engine that will do the search. The cache just caches the results.")
					.reference("myrimatch", UiBuilder.NONE_TYPE);
		}
	}
}
