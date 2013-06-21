package edu.mayo.mprc.xtandem;

import edu.mayo.mprc.swift.params2.mapping.MappingFactory;
import edu.mayo.mprc.swift.params2.mapping.Mappings;

import java.io.File;

public final class XTandemMappingFactory implements MappingFactory {
	private static final long serialVersionUID = 20101221L;
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

	/**
	 * Return the file name for the resulting parameter file (after the template got patched)
	 *
	 * @param paramsFile Input parameter file template
	 * @return Resulting file name (same folder, different extension).
	 */
	public static File resultingParamsFile(File paramsFile) {
		return new File(paramsFile.getParentFile(), paramsFile.getName().replaceAll("\\.xml\\.template$", ".xml"));

	}
}

