package edu.mayo.mprc.swift.params2.mapping;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.swift.params2.ParamName;

public class TestMappingContextBase implements MappingContext {
	private ParamsInfo paramsInfo;

	/**
	 * Null Constructor
	 */
	public TestMappingContextBase(final ParamsInfo paramsInfo) {
		this.paramsInfo = paramsInfo;
	}

	@Override
	public ParamsInfo getAbstractParamsInfo() {
		return paramsInfo;
	}

	@Override
	public void startMapping(final ParamName paramName) {
		// Do nothing
	}

	@Override
	public void reportError(final String message, final Throwable t, ParamName paramName) {
		throw new MprcException(message, t);
	}

	@Override
	public void reportWarning(final String message, ParamName paramName) {
		throw new MprcException(message);
	}

	@Override
	public void reportInfo(final String message, ParamName paramName) {
		throw new MprcException(message);
	}

	@Override
	public boolean noErrors() {
		return true;
	}

	@Override
	public Curation addLegacyCuration(final String legacyName) {
		return null;
	}
}
