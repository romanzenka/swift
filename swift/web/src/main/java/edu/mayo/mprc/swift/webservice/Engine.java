package edu.mayo.mprc.swift.webservice;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.mayo.mprc.swift.dbmapping.SearchEngineConfig;

/**
 * @author Roman Zenka
 */
@XStreamAlias("engine")
public final class Engine {
	private final String code;

	public Engine(final SearchEngineConfig config) {
		code = config.getCode();
	}

	public String getCode() {
		return code;
	}
}
