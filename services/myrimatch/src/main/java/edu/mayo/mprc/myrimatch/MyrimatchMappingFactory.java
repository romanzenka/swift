package edu.mayo.mprc.myrimatch;

import edu.mayo.mprc.swift.params2.mapping.MappingFactory;
import edu.mayo.mprc.swift.params2.mapping.Mappings;

public final class MyriMatchMappingFactory implements MappingFactory {

	private static final long serialVersionUID = 20110711L;
	public static final String MYRIMATCH = "MYRIMATCH";

	@Override
	public Mappings createMapping() {
		return new MyriMatchMappings();
	}

	@Override
	public String getSearchEngineCode() {
		return MYRIMATCH;
	}

	/**
	 * @return Typical name for the param file storing parameters for this mapping.
	 */
	@Override
	public String getCanonicalParamFileName() {
		return "myrimatch.template.cfg";
	}
}
