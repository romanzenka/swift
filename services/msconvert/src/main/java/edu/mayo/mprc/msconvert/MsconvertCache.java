package edu.mayo.mprc.msconvert;

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
public final class MsconvertCache extends WorkCache<MsconvertWorkPacket> {
	public static final String TYPE = "msconvertCache";
	public static final String NAME = "Msconvert Cache";
	public static final String DESC = "<p>Caches .mgf files previously converted from .RAW using msconvert. This can speed up operation if one file is being processed multiple times.</p>";

	public MsconvertCache() {
	}

	public static final class Config extends WorkCache.CacheConfig {
		public Config() {
		}
	}

	@Component("msconvertCacheFactory")
	public static final class Factory extends WorkCache.Factory<Config> {
		private static MsconvertCache cache;

		@Override
		public WorkCache getCache() {
			return cache;
		}

		@Override
		public WorkCache createCache(final Config config, final DependencyResolver dependencies) {
			return cache = new MsconvertCache();
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String DEFAULT_CACHE = "var/cache/mgf2";

		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(WorkCache.CacheConfig.CACHE_FOLDER, ".mgf cache folder", "When a .RAW file gets converted to .mgf, the result is stored in this folder. Subsequent conversions of the same file use the cached value."
							+ "<p>Ideally, this folder would be on a fast, potentially less reliable storage.</p>")
					.required()
					.defaultValue(DEFAULT_CACHE)

					.property(WorkCache.CacheConfig.SERVICE, "Msconvert instance", "The module that will do the conversion. The cache just caches the results.")
					.reference("msconvert", UiBuilder.NONE_TYPE);
		}
	}
}