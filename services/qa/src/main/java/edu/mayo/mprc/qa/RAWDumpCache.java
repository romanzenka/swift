package edu.mayo.mprc.qa;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkCache;
import org.springframework.stereotype.Component;

public final class RAWDumpCache extends WorkCache<RAWDumpWorkPacket> {

	public static final String TYPE = "rawdumpCache";
	public static final String NAME = "RAW Dump Cache";
	public static final String DESC = "Caches previously extracted .RAW file information.";

	public RAWDumpCache() {
	}

	public static final class Config extends CacheConfig {
		public Config() {
		}
	}

	@Component("rawDumpCacheFactory")
	public static final class Factory extends WorkCache.Factory<Config> {
		@Override
		public WorkCache createCache(final Config config, final DependencyResolver dependencies) {
			return new RAWDumpCache();
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String DEFAULT_CACHE = "var/cache/rawdump";

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(CacheConfig.CACHE_FOLDER, "RAW dump cache folder", "When a .RAW file gets processed by RAW Dump, the result is stored in this folder. Subsequent attempts to dump information from the same file will use the cached results."
					+ "<p>Ideally, this folder would be on a fast, potentially less reliable storage.</p>")
					.required()
					.defaultValue(DEFAULT_CACHE)

					.property(CacheConfig.SERVICE, "RAW Dump", "The RAW dump engine that will do the work. The cache just caches the results.")
					.reference("rawdump", UiBuilder.NONE_TYPE);
		}
	}
}
