package edu.mayo.mprc.config;

import java.util.Map;

/**
 * @author Roman Zenka
 */
public abstract class WorkerConfig implements ResourceConfig {
	/**
	 * We are writing the configuration inline within a service.
	 *
	 * @param writer Writer to write the configuration into.
	 */
	public void writeInline(final ConfigWriter writer) {
		final Map<String, String> save = save(writer.getDependencyResolver());
		for (final Map.Entry<String, String> entry : save.entrySet()) {
			writer.addConfig(entry.getKey(), entry.getValue(), null);
		}
	}

	@Override
	public void write(final ConfigWriter writer) {
		// We just make sure that the writer gets to resolve all our dependencies (and save them)
		save(writer.getDependencyResolver());
		writer.register(this);
	}
}
