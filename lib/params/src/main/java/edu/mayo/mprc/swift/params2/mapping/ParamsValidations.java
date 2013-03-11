package edu.mayo.mprc.swift.params2.mapping;

import edu.mayo.mprc.swift.params2.ParamName;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A map of validation results tied to separate {@link edu.mayo.mprc.swift.params2.ParamName}.
 */
public final class ParamsValidations implements Cloneable {
	private final Map<ParamName, ValidationList> validationMap = new HashMap<ParamName, ValidationList>();

	public void addValidation(final ParamName name, final Validation v) {
		ValidationList list = validationMap.get(name);
		if (list == null) {
			list = new ValidationList();
			validationMap.put(name, list);
		}
		list.add(v);
	}

	public ValidationList getValidationFor(final ParamName name) {
		if (validationMap.containsKey(name)) {
			return validationMap.get(name);
		} else {
			return new ValidationList();
		}
	}

	public void clearValidationsFor(final ParamName param) {
		if (validationMap.containsKey(param)) {
			validationMap.remove(param);
		}
	}

	/**
	 * @return Unmodifiable map of validations.
	 */
	public Map<ParamName, ValidationList> getValidationMap() {
		return Collections.unmodifiableMap(validationMap);
	}

	public String toString(final ValidationSeverity minSeverity) {
		final StringBuilder result = new StringBuilder();
		for (final Map.Entry<ParamName, ValidationList> entry : validationMap.entrySet()) {
			for (final Validation validation : entry.getValue()) {
				if (validation.getSeverity().compareTo(minSeverity) >= 0) {
					result
							.append(entry.getKey().getDesc())
							.append(": ")
							.append(validation.getMessage())
							.append("\n");
				}
			}
		}
		return result.toString();
	}
}