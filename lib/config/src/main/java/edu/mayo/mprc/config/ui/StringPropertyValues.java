package edu.mayo.mprc.config.ui;

import java.util.Map;

/**
* @author Roman Zenka
*/
public class StringPropertyValues implements PropertyValues<String> {
	private Map<String, String> values;

	public StringPropertyValues(Map<String, String> values) {
		this.values = values;
	}

	@Override
	public String getValue(String propertyName) {
		return values.get(propertyName);
	}

	@Override
	public Iterable<String> keySet() {
		return values.keySet();
	}
}
