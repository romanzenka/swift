package edu.mayo.mprc.swift.webservice;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Roman Zenka
 */
@XStreamAlias("search")
public final class Search {
	private final long id;
	private final String title;

	public Search(final long searchRunId, final String searchTitle) {
		id = searchRunId;
		title = searchTitle;
	}

	public long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}
}
