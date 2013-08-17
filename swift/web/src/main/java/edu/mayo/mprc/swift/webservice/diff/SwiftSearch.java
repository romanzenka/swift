package edu.mayo.mprc.swift.webservice.diff;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.mayo.mprc.swift.dbmapping.SearchRun;

/**
 * A short representation of {@link SearchRun}.
 *
 * @author Roman Zenka
 */
@XStreamAlias("swiftSearch")
public final class SwiftSearch {
	private final int id;
	private final String title;

	public SwiftSearch(final String title, final int id) {
		this.title = title;
		this.id = id;
	}

	public SwiftSearch(final SearchRun run) {
		this(run.getTitle(), run.getId());
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}
}
