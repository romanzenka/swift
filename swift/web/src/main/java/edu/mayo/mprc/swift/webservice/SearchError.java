package edu.mayo.mprc.swift.webservice;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Roman Zenka
 */
@XStreamAlias("error")
public final class SearchError {
	private final String message;

	public SearchError(final String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
