package edu.mayo.mprc.comet;

import edu.mayo.mprc.swift.params2.mapping.MappingFactory;
import edu.mayo.mprc.swift.params2.mapping.Mappings;

public final class CometMappingFactory implements MappingFactory {
	public static final String COMET = "COMET";

	@Override
	public Mappings createMapping() {
		return new CometMappings();
	}

	@Override
	public String getSearchEngineCode() {
		return COMET;
	}

	/**
	 * @return Typical name for the param file storing parameters for this mapping.
	 */
	@Override
	public String getCanonicalParamFileName(final String distinguishingString) {
		return "comet" + distinguishingString + ".params";
	}
}

