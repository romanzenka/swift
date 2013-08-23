package edu.mayo.mprc.config.generic;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;

import java.util.LinkedHashMap;

/**
 * A generic resource config. Holds all the key-values as a hashmap.
 * <p/>
 * Useful when you do not have the implementation of the particular
 * config class but you still need to parse the configuration file.
 *
 * @author Roman Zenka
 */
public class GenericResource implements ResourceConfig, NamedResource, TypedResource {
	private LinkedHashMap<String, String> values;
	private String name;
	private String type;

	public GenericResource() {
		values = new LinkedHashMap<String, String>();
	}

	@Override
	public void save(final ConfigWriter writer) {
		throw new MprcException("The generic resources cannot be saved");
	}

	@Override
	public void load(final ConfigReader reader) {
		for (final String key : reader.getKeys()) {
			values.put(key, reader.get(key));
		}
	}

	public String get(final String key) {
		return values.get(key);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public int getPriority() {
		return 0;
	}
}
