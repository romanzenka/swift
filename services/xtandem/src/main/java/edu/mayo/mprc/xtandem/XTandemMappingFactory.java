package edu.mayo.mprc.xtandem;

import edu.mayo.mprc.swift.params2.mapping.MappingFactory;
import edu.mayo.mprc.swift.params2.mapping.Mappings;

public final class XTandemMappingFactory implements MappingFactory {
	public static final String TANDEM = "TANDEM";

	@Override
	public Mappings createMapping() {
		return new XTandemMappings();
	}

	@Override
	public String getSearchEngineCode() {
		return TANDEM;
	}

	/**
	 * @return Typical name for the param file storing parameters for this mapping.
	 */
	@Override
	public String getCanonicalParamFileName(final String distinguishingString) {
		return "tandem" + distinguishingString + ".xml.template";
	}
}

