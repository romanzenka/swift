package edu.mayo.mprc.swift.webservice;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.mayo.mprc.swift.db.SearchEngine;

/**
 * @author Roman Zenka
 */
@XStreamAlias("engine")
public final class Engine {
	private final String code;
	private final String version;

	public Engine(final SearchEngine searchEngine) {
		code = searchEngine.getEngineMetadata().getCode();
		version = searchEngine.getVersion();
	}

	public String getCode() {
		return code;
	}

	public String getVersion() {
		return version;
	}
}
