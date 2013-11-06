package edu.mayo.mprc.omssa;

import edu.mayo.mprc.swift.params2.mapping.MappingFactory;
import edu.mayo.mprc.swift.params2.mapping.Mappings;

public final class OmssaMappingFactory implements MappingFactory {
	public static final String OMSSA = "OMSSA";

	@Override
	public Mappings createMapping() {
		return new OmssaMappings();
	}

	@Override
	public String getSearchEngineCode() {
		return OMSSA;
	}

	/**
	 * @return Typical name for the param file storing parameters for this mapping.
	 */
	@Override
	public String getCanonicalParamFileName(final String distinguishingString) {
		return "omssa" + distinguishingString + ".params.xml";
	}
}

