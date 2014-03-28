package edu.mayo.mprc.swift.params2.mapping;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.swift.params2.ParamName;

/**
 * This context collects errors as the mappings progress and stores them in supplied {@link ParamsValidations}.
 */
public class ParamValidationsMappingContext implements MappingContext {
	private ParamsValidations validations;
	private ParamsInfo paramsInfo;
	private ParamName currentParam;
	private boolean noErrors = false;

	public ParamValidationsMappingContext(final ParamsValidations validations, final ParamsInfo paramsInfo) {
		this.validations = validations;
		this.paramsInfo = paramsInfo;
		if (paramsInfo == null) {
			throw new MprcException("The mapping context cannot be initialized with paramsInfo==null");
		}
		noErrors = true;
	}

	@Override
	public ParamsInfo getAbstractParamsInfo() {
		return paramsInfo;
	}

	@Override
	public void reportError(final String message, final Throwable t, final ParamName paramName) {
		final ParamName param = paramName == null ? currentParam : paramName;
		final Validation v = new Validation(message, ValidationSeverity.ERROR, param, null, t);
		validations.addValidation(param, v);
		noErrors = false;
	}

	@Override
	public void reportWarning(final String message, final ParamName paramName) {
		final ParamName param = paramName == null ? currentParam : paramName;
		final Validation v = new Validation(message, ValidationSeverity.WARNING, param, null, null);
		validations.addValidation(param, v);
	}

	@Override
	public void reportInfo(final String message, final ParamName paramName) {
		final ParamName param = paramName == null ? currentParam : paramName;
		final Validation v = new Validation(message, ValidationSeverity.INFO, param, null, null);
		validations.addValidation(param, v);
	}

	/**
	 * @return True if no errors occured since last call to a mapping method.
	 *         Use this if you want to do an action only if all mappings validated ok.
	 */
	@Override
	public boolean noErrors() {
		return noErrors;
	}

	@Override
	public Curation addLegacyCuration(final String legacyName) {
		return null;
	}

	@Override
	public void startMapping(final ParamName paramName) {
		currentParam = paramName;
	}
}
