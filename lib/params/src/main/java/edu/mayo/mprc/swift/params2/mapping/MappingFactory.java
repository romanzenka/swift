package edu.mayo.mprc.swift.params2.mapping;

/**
 * Search engine mapping factory interface. Produces {@link Mappings}
 */
public interface MappingFactory {
	/**
	 * @return String code of the search engine.
	 */
	String getSearchEngineCode();

	/**
	 * @param distinguishingString A string that distinguishes between multiple parameter files in the same directory.
	 * @return Typical name for the param file storing parameters for this mapping.
	 */
	String getCanonicalParamFileName(String distinguishingString);

	Mappings createMapping();
}
