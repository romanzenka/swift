package edu.mayo.mprc.myrimatch;

import edu.mayo.mprc.swift.params2.mapping.MappingFactory;
import edu.mayo.mprc.swift.params2.mapping.Mappings;

import java.io.File;

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
	public String getCanonicalParamFileName(final String distinguishingString) {
		return "myrimatch" + distinguishingString + ".template.cfg";
	}

	/**
	 * Return the file name for the resulting parameter file (after the template got patched)
	 *
	 * @param paramsFile Input parameter file template
	 * @return Resulting file name (same folder, different extension).
	 */
	public static File resultingParamsFile(File paramsFile) {
		return new File(paramsFile.getParentFile(), paramsFile.getName().replaceAll("\\.template\\.cfg$", ".cfg"));

	}
}
