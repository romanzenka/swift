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
	}

	@Override
	public ParamsInfo getAbstractParamsInfo() {
		return paramsInfo;
	}

	@Override
	public void reportError(final String message, final Throwable t) {
		final Validation v = new Validation(message, ValidationSeverity.ERROR, currentParam, null, t);
		validations.addValidation(currentParam, v);
		noErrors = false;
	}

	@Override
	public void reportWarning(final String message) {
		final Validation v = new Validation(message, ValidationSeverity.WARNING, currentParam, null, null);
		validations.addValidation(currentParam, v);
	}

	@Override
	public void reportInfo(final String message) {
		final Validation v = new Validation(message, ValidationSeverity.INFO, currentParam, null, null);
		validations.addValidation(currentParam, v);
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
		noErrors = true;
	}
}
