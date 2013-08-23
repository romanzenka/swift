package edu.mayo.mprc.swift.ui.client.widgets;

public final class ValidatedIntegerTextBox extends ValidatedTextBox {
	private static class IntegerValidator extends TextBoxValidator {
		private int low, high;

		public IntegerValidator(final int low, final int high) {
			this.low = low;
			this.high = high;
		}

		@Override
		public boolean isValueValid(final String data) {
			final int val;

			try {
				val = Integer.parseInt(data);
			} catch (NumberFormatException e) {
				setLastError("Value must be an integer.");
				return false;
			}

			if (val < low || val > high) {
				setLastError("Value must be from " + low + " to " + high + ".");
				return false;
			} else {
				setLastError(null);
				return true;
			}
		}
	}

	private int low;
	private int high;
	private int defaultValue;

	/**
	 * Constructor
	 *
	 * @param low  The lowest possible integer this input will accept; use Integer.MIN_VALUE to ignore the lower limit (except for that imposed by the JRE)
	 * @param high The highest possible integer this input will accept; use Integer.MAX_VALUE to ignore the upper limit (except for that imposed by the JRE)
	 */
	public ValidatedIntegerTextBox(final int low, final int high, final int defaultValue) {
		super(new IntegerValidator(low, high));
		this.low = low;
		this.high = high;
		this.defaultValue = defaultValue;
	}

	/**
	 * Get the lowest value this input will accept
	 *
	 * @return The lowest value this input will accept
	 */
	public int getLow() {
		return low;
	}

	/**
	 * Get the highest value this input will accept
	 *
	 * @return The highest value this input will accept
	 */
	public int getHigh() {
		return high;
	}

	public int getIntegerValue() {
		try {
			return Integer.valueOf(getText());
		} catch (NumberFormatException ignore) {
			// SWALLOWED
			return defaultValue;
		}
	}
}