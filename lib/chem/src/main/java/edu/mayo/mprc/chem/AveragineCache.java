package edu.mayo.mprc.chem;

import edu.mayo.mprc.MprcException;

import java.util.HashMap;
import java.util.Map;

/**
 * Caches the averagine calculations so they do not have to be constantly repeated.
 */
public final class AveragineCache {
	private final Chemical averagine;
	private final double precision;
	private final Map<Request, IsotopicDistribution> cache = new HashMap<Request, IsotopicDistribution>(100);
	private final ChargeUnit chargeUnit;
	private final PeriodicTable periodicTable;
	private final Element hydrogen;
	private final double hydrogenMass;
	/**
	 * Peaks with smaller abundance will not be reported. Peak abundance is in [0, 100] range.
	 */
	private static final double MIN_ABUNDANCE_THRESHOLD = 0.001;

	/**
	 * @param precision     How precisely are averagine molecules calculated. The required mass is rounded using the precision
	 *                      and if there is any averagine for the resulting number, it is returned.
	 * @param periodicTable Periodic table to do the calculations with.
	 */
	public AveragineCache(final double precision, final PeriodicTable periodicTable) {
		this.periodicTable = periodicTable;
		this.precision = precision;
		averagine = new Averagine(periodicTable);
		chargeUnit = ChargeUnit.getProtonChargeUnit(periodicTable);
		hydrogen = periodicTable.getElementBySymbol("H");
		hydrogenMass = hydrogen.getMonoisotopicMass();
	}

	/**
	 * Return averagine distribution.
	 *
	 * @param averagineMass Requested molecular mass.
	 * @param charge        Charge - we add charge amount of protons to the formula.
	 * @return Isotopic distribution for given combination of charge and mass.
	 */
	public IsotopicDistribution getDistribution(final double averagineMass, final int charge) {
		final Request key = new Request(averagineMass, charge, precision);
		final IsotopicDistribution cached = cache.get(key);
		if (cached != null) {
			return cached;
		}

		Chemical avg = null;
		try {
			avg = averagine.clone();
		} catch (CloneNotSupportedException e) {
			throw new MprcException(e);
		}
		avg.addElement(chargeUnit.getChargeCarrier(), charge);
		final double mass = avg.getMonoisotopicMass();
		avg.multiply(averagineMass / mass);
		final Chemical avgInt = new Chemical(periodicTable);
		// First fill in integral counts for all the elements
		for (int element = 0; element < avg.getNumElements(); element++) {
			avgInt.addElement(avg.getElement(element), Math.round(avg.getElementCount(element)));
		}
		// Since we will be off, tweak the hydrogens to match
		avgInt.addElement(hydrogen, Math.floor((averagineMass - avgInt.getMonoisotopicMass() / hydrogenMass)));

		avgInt.setName("Averagine " + averagineMass);
		final IsotopicDistribution distribution = avgInt.getIsotopicDistribution(charge, chargeUnit, MIN_ABUNDANCE_THRESHOLD, 0);
		cache.put(key, distribution);
		return distribution;
	}

	private static final class Request {
		private final int mass;
		private final int charge;

		private Request(final double mass, final int charge, final double precision) {
			this.mass = (int) (mass / precision);
			this.charge = charge;
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Request)) {
				return false;
			}

			final Request request = (Request) o;

			if (mass != request.mass) {
				return false;
			}
			if (charge != request.charge) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = mass;
			result = 31 * result + charge;
			return result;
		}
	}
}
