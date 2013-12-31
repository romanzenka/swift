package edu.mayo.mprc.swift.params2;

import edu.mayo.mprc.MprcException;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Mass tolerance.
 * <p/>
 * Unit is a simple string to keep things simple.
 */
public class Tolerance implements Serializable {
	private static final long serialVersionUID = 20111119L;
	private double value;
	private MassUnit unit;
	private static final DecimalFormat df;

	static {
		df = new DecimalFormat("0.########");
		df.setMinimumFractionDigits(0);
		df.setDecimalSeparatorAlwaysShown(false);
	}

	public Tolerance() {
	}

	public Tolerance(final double value, final MassUnit unit) {
		this.value = value;
		this.unit = unit;
	}

	/**
	 * Create tolerance from its textual representation.
	 *
	 * @param text Textual representation, e.g. "0.8 Da" or "5 ppm"
	 */
	public Tolerance(final String text) {
		final String trimmed = text.trim();
		final String lower = trimmed.toLowerCase(Locale.ENGLISH);
		for (final MassUnit massUnit : MassUnit.values()) {
			final String code = massUnit.getCode();
			if (tryParseCode(lower, massUnit, code)) {
				return;
			}
			for (final String altName : massUnit.getAlternativeNames()) {
				if (tryParseCode(lower, massUnit, altName)) {
					return;
				}
			}
		}
		throw new MprcException("Unrecognized unit, please use one of " + MassUnit.getOptions());
	}

	private boolean tryParseCode(String input, MassUnit unit, String unitName) {
		if (input.length() > unitName.length() && input.endsWith(unitName.toLowerCase(Locale.ENGLISH))) {
			final String number = input.substring(0, input.length() - unitName.length());
			try {
				value = Double.parseDouble(number);
				this.unit = unit;
				return true;
			} catch (NumberFormatException e) {
				throw new MprcException("Bad format: '" + number.trim() + "' should be amount of " + unit.getDescription(), e);
			}
		}
		return false;
	}

	void setValue(final double value) {
		this.value = value;
	}

	public double getValue() {
		return value;
	}

	void setUnit(final MassUnit unit) {
		this.unit = unit;
	}

	public MassUnit getUnit() {
		return unit;
	}

	@Override
	public String toString() {
		return df.format(value) + " " + unit.getCode();
	}

	public Tolerance copy() {
		return new Tolerance(getValue(), getUnit());
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		final Tolerance tolerance = (Tolerance) obj;

		if (Double.compare(tolerance.value, value) != 0) {
			return false;
		}
		return unit == tolerance.unit;
	}

	@Override
	public int hashCode() {
		int result;
		final long temp;
		temp = value == +0.0d ? 0L : Double.doubleToLongBits(value);
		result = (int) (temp ^ (temp >>> 32));
		result = 31 * result + (unit != null ? unit.hashCode() : 0);
		return result;
	}
}

