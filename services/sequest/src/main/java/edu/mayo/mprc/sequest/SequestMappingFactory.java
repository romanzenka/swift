package edu.mayo.mprc.sequest;

import edu.mayo.mprc.swift.params2.mapping.MappingFactory;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import org.springframework.stereotype.Component;

@Component("sequestMappingFactory")
public final class SequestMappingFactory implements MappingFactory {
	public static final String SEQUEST = "SEQUEST";

	@Override
	public Mappings createMapping() {
		return new SequestMappings();
	}

	@Override
	public String getSearchEngineCode() {
		return SEQUEST;
	}

	@Override
	public String getCanonicalParamFileName(final String distinguishingString) {
		return "sequest" + distinguishingString + ".params";
	}
}

