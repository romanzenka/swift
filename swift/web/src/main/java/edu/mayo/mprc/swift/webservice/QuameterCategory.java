package edu.mayo.mprc.swift.webservice;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Roman Zenka
 */
@XStreamAlias("quameter-category")
public final class QuameterCategory {
	private final String name;
	private final String code;

	public QuameterCategory(final String code, final String name) {
		this.code = code;
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}
}
