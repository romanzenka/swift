package edu.mayo.mprc.chem;

/**
 * Represents a single isotope of an {@link Element}.  The isotopes of an element differ
 * in their neutron content.
 */
public final class Isotope {
	private final double mass;
	private final int intMass;
	private final double abundance;

	public Isotope(final double mass, final double abundance) {
		this.mass = mass;
		intMass = (int) Math.round(mass);
		this.abundance = abundance;
	}

	public double getMass() {
		return mass;
	}

	/**
	 * Mass as an integer. Used to speed up the Mercury6 algorithm.
	 */
	public int getIntMass() {
		return intMass;
	}

	/**
	 * The fractional abundance of this isotope.
	 */
	public double getAbundance() {
		return abundance;
	}

	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		final Isotope isotope = (Isotope) obj;

		if (Double.compare(isotope.abundance, abundance) != 0) {
			return false;
		}
		return Double.compare(isotope.mass, mass) == 0;

	}

	public int hashCode() {
		int result;
		long temp;
		temp = mass == +0.0d ? 0L : Double.doubleToLongBits(mass);
		result = (int) (temp ^ (temp >>> 32));
		temp = abundance == +0.0d ? 0L : Double.doubleToLongBits(abundance);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
}
