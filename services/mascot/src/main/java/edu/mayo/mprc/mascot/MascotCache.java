package edu.mayo.mprc.mascot;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkCache;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import org.springframework.stereotype.Component;

import java.io.File;

public final class MascotCache extends WorkCache<MascotWorkPacket> {
	public static final String TYPE = "mascotCache";
	public static final String NAME = "Mascot Cache";
	public static final String DESC = "Caches previous Mascot search results. <p>Speeds up consecutive Mascot searches if the same file with same parameters is processed multiple times.</p>";


	@Override
	public void userProgressInformation(final File wipFolder, final ProgressInfo progressInfo) {
		// We store the extra Mascot URL as a special file so we can report it to the user later
		if (progressInfo instanceof MascotResultUrl) {
			final MascotResultUrl mascotResultUrl = (MascotResultUrl) progressInfo;
			FileUtilities.writeStringToFile(new File(wipFolder, MascotWorkPacket.MASCOT_URL_FILENAME), mascotResultUrl.getMascotUrl(), true);
		}
	}

	public static final class Config extends CacheConfig {
		public Config() {
		}
	}

	@Component("mascotCacheFactory")
	public static final class Factory extends WorkCache.Factory<Config> {
		@Override
		public WorkCache createCache(final Config config, final DependencyResolver dependencies) {
			return new MascotCache();
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String DEFAULT_CACHE = "var/cache/mascot";

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(CacheConfig.CACHE_FOLDER, "Mascot cache folder", "When a file gets searched by Mascot, the result is stored in this folder. Subsequent searches of the same file with same parameters use the cached value."
					+ "<p>Ideally, this folder would be on a fast, potentially less reliable storage.</p>")
					.required()
					.defaultValue(DEFAULT_CACHE)

					.property(CacheConfig.SERVICE, "Mascot Search Engine", "The Mascot engine that will do the search. The cache just caches the results.")
					.reference(MascotWorker.TYPE, UiBuilder.NONE_TYPE);
		}
	}
}
