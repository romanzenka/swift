package edu.mayo.mprc.swift.webservice;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;

/**
 * @author Roman Zenka
 */
@XStreamAlias("search")
public final class Search {
	private final int id;
	private final String title;

	public Search(final SwiftSearchDefinition searchDefinition) {
		id = searchDefinition.getId();
		title = searchDefinition.getTitle();
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}
}
