package edu.mayo.mprc.swift.webservice;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.mayo.mprc.swift.params2.SavedSearchEngineParameters;

/**
 * @author Roman Zenka
 */
@XStreamAlias("parameter-set")
public final class ParameterSet {
	private final int id;
	private final String name;
	private final String initials;

	public ParameterSet(final SavedSearchEngineParameters parameterSet) {
		id = parameterSet.getId();
		name = parameterSet.getName();
		initials = parameterSet.getUser().getInitials();
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getInitials() {
		return initials;
	}
}
