package edu.mayo.mprc.config;

import edu.mayo.mprc.MprcException;

import java.util.LinkedHashMap;

/**
 * @author Roman Zenka
 */
public final class GenericRunner extends RunnerConfig {
	private LinkedHashMap<String, String> values;

	public GenericRunner() {
		values = new LinkedHashMap<String, String>();
	}

	@Override
	public void save(ConfigWriter writer) {
		throw new MprcException("Generic runner cannot be saved. To be used for loading only.");
	}

	@Override
	public void load(ConfigReader reader) {
		super.load(reader);
		for (final String key : reader.getKeys()) {
			values.put(key, reader.get(key));
		}
	}
}
