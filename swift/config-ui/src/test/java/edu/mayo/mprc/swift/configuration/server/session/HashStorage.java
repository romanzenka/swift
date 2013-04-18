package edu.mayo.mprc.swift.configuration.server.session;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class HashStorage implements SessionStorage {
	private Map<String, Object> map = new HashMap<String, Object>();

	@Override
	public Object get(String key) {
		return map.get(key);
	}

	@Override
	public void put(String key, Object object) {
		map.put(key, object);
	}
}
