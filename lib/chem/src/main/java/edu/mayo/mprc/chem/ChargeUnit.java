package edu.mayo.mprc.chem;

import java.util.ArrayList;
import java.util.List;

/**
 * The charge carrying unit of a charged molecule.  Most often, this is
 * simply a proton.  This currently supports only a single element; the idea
 * was to allow multiple different charge units in a given analysis but
 * this is more complicated than was first envisioned and is not supported.
 * <p/>
 * Typical use is simply {@link #getProtonChargeUnit}.
 */
public final class ChargeUnit {

	/**
	 * Don't use this constructor, use getChargeUnit() or getProtonChargeUnit().
	 */
	private ChargeUnit(final Element chargeCarrier, final PeriodicTable pt) {
		this.chargeCarrier = chargeCarrier;
		this.pt = pt;
		mass = this.chargeCarrier.getMonoisotopicMass() - this.pt.getElectronMass();
	}

	/**
	 * Determines neutral mass from given m/z and charge state. It is like if the
	 * molecule lost charge state times the mass of the current charge unit (typically a proton).
	 *
	 * @param mz          M over z of the charged molecule
	 * @param chargeState How many charges (z)
	 * @return Neutral mass of the molecule.
	 */
	public double chargedToNeutral(final double mz, final int chargeState) {
		return mz * Math.abs(chargeState) - (chargeState * mass);
	}

	/**
	 * Adds chargeState charges to a molecule of given mass, returns resulting mass.
	 *
	 * @param mass        Mass of the molecule to charge.
	 * @param chargeState How many charges are added.
	 * @return Final mass of the molecule.
	 */
	public double neutralToCharged(final double mass, final int chargeState) {
		return (mass + (chargeState * this.mass)) / Math.abs(chargeState);
	}

	public double getMass() {
		return mass;
	}

	public Element getChargeCarrier() {
		return chargeCarrier;
	}

	public PeriodicTable getPeriodicTable() {
		return pt;
	}

	/**
	 * Returns the "charge state string" like [M+5H+]5+.
	 */
	public String toString(final int chargeState) {
		return "[M" + (chargeState >= 0 ? "+" : "-") + Math.abs(chargeState)
				+ chargeCarrier.getSymbol() + "+]" + Math.abs(chargeState) + (chargeState >= 0 ? "+" : "-");
	}

	/**
	 * This is a non-standard, abbreviated version of the "charge state string".
	 * You're probably better off using the normal version returned by {@link #toString(int)}.
	 */
	public String toShortString(final int chargeState) {
		if ("H".equals(chargeCarrier.getSymbol())) {
			return "[M " + Math.abs(chargeState)
					+ (chargeState >= 0 ? "+" : "-") + "]";
		} else {
			return "[M " + Math.abs(chargeState) + chargeCarrier.getSymbol()
					+ (chargeState >= 0 ? "+" : "-") + "]";
		}
	}

	/**
	 * Returns the singleton ChargeUnit for protons.
	 */
	public static ChargeUnit getProtonChargeUnit(final PeriodicTable pt) {
		return findChargeUnit(pt.getElementBySymbol("H"), pt);
	}

	/**
	 * Returns the singleton ChargeUnit for the given element.
	 */
	public static ChargeUnit getChargeUnit(final Element chargeCarrier, final PeriodicTable pt) {
		return findChargeUnit(chargeCarrier, pt);
	}

	private final Element chargeCarrier;
	private final PeriodicTable pt;
	private final double mass;

	private static final List<ChargeUnit> chargeUnits = new ArrayList<ChargeUnit>();

	private static synchronized ChargeUnit findChargeUnit(final Element chargeCarrier, final PeriodicTable pt) {
		for (final ChargeUnit chargeUnit : chargeUnits) {
			if (chargeUnit.getChargeCarrier().equals(chargeCarrier) && chargeUnit.getPeriodicTable().equals(pt)) {
				return chargeUnit;
			}
		}
		final ChargeUnit chargeUnit = new ChargeUnit(chargeCarrier, pt);
		chargeUnits.add(chargeUnit);
		return chargeUnit;
	}
}
