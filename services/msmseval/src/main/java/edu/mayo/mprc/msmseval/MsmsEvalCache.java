package edu.mayo.mprc.msmseval;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkCache;
import org.springframework.stereotype.Component;

public final class MsmsEvalCache extends WorkCache<MSMSEvalWorkPacket> {

	public static final String TYPE = "msmsEvalCache";
	public static final String NAME = "MsmsEval Cache";
	public static final String DESC = "Caches previous evaluation of MSMS spectra quality.";

	public MsmsEvalCache() {
	}

	public static final class Config extends CacheConfig {
		public Config() {
		}
	}

	@Component("msmsEvalCacheFactory")
	public static final class Factory extends WorkCache.Factory<Config> {
		@Override
		public WorkCache createCache(final Config config, final DependencyResolver dependencies) {
			return new MsmsEvalCache();
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String DEFAULT_CACHE = "var/cache/msmseval";

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(CacheConfig.CACHE_FOLDER, "msmsEval cache folder", "When an .mgf file gets evaluated by msmsEval, the result is stored in this folder. Subsequent attempts to evaluate same file will use the cached results."
					+ "<p>Ideally, this folder would be on a fast, potentially less reliable storage.</p>")
					.defaultValue(DEFAULT_CACHE).required()

					.property(CacheConfig.SERVICE, "MsmsEval spectrum QA", "The msmsEval engine that will do the work. The cache just caches the results.")
					.reference("msmsEval", UiBuilder.NONE_TYPE);
		}
	}
}
