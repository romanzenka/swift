package edu.mayo.mprc.swift.configuration.client.validation.local;

public final class IntegerValidator implements Validator {
	private Integer minimumValue;
	private Integer maximumValue;

	public IntegerValidator() {
	}

	public IntegerValidator(final Integer minimumValue, final Integer maximumValue) {
		this.maximumValue = maximumValue;
		this.minimumValue = minimumValue;
	}

	@Override
	public String validate(final String value) {
		try {
			final Integer integer = Integer.valueOf(value);

			if (minimumValue != null && integer.compareTo(minimumValue) < 0) {
				return "Value must be greater than or equal to " + minimumValue;
			}

			if (maximumValue != null && integer.compareTo(maximumValue) > 0) {
				return "Value must be smaller than or equal to " + maximumValue;
			}
		} catch (NumberFormatException ignore) {
			return "Invalid integer value.";
		}
		return null;
	}
}
