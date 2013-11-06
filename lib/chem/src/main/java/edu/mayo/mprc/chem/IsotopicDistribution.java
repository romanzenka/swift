package edu.mayo.mprc.chem;

import edu.mayo.mprc.MprcException;

import java.io.PrintWriter;

/**
 * A theoretical isotopic distribution.  Each isotope has a mass and an intensity. Obtain from {@link Chemical#getIsotopicDistribution}.
 * <p/>
 * Note that, perhaps oddly, an IsotopicDistribution does not contain Isotopes.
 * <p/>
 * One might assume that theoretical isotopic distributions would have well defined beginnings and ends.  Indeed, it might
 * be reasonable to suspect that the first isotope is the monoisotopic (all 12C, all 14N, etc).  And, at the higher mass end
 * that there was some place where there were simply no further stable isotopes to be had.
 * <p/>
 * We violate these assumptions for two reasons: <ol>
 * <li>Mercury doesn't work that way, it is an approximation
 * based on convolution, and returns isotopes before the monoisotope, or, for highly enriched
 * distributions, doesn't return the monoisotope at all (and the function hasMonoisotope() is provided
 * for this latter case).</li>
 * <li>Sometimes it can be useful to have extra isotopes before the monoisotope, and after the end.  We
 * provide the ability to specify a threshold and a number of extra isotopes to include.  If these
 * are provided, we return extra isotopes before the monoisotope, and extra isotopes after
 * the last isotope with relative intensity > thresh.  This is a bit of a hack, but what to do instead?</li>
 * </ol>
 */
public final class IsotopicDistribution implements Cloneable {
	public IsotopicDistribution(final double therMonoMZ, final Chemical chem, final MassIntensityArray dist) {
		this(therMonoMZ, chem, "", -1, 0, dist, true);
	}

	/**
	 * Typically you use {@link Chemical#getIsotopicDistribution} to get an IsotopicDistribution instead of this constructor.
	 */
	public IsotopicDistribution(final double therMonoMZ, final Chemical chem, final String name, final double thresh, final int extra, final MassIntensityArray dist) {
		this(therMonoMZ, chem, name, thresh, extra, dist, true);
	}

	private IsotopicDistribution(final double therMonoMZ, final Chemical chem, final String name, final double thresh, final int extra, final MassIntensityArray dist, final boolean init) {
		this.therMonoMZ = therMonoMZ;
		this.chem = chem;
		this.name = name;
		this.thresh = thresh;
		this.extra = extra;
		this.dist = new MassIntensityArray(dist);
		mostAbundant = -1;
		if (init) {
			doInit();
		}

	}

	public IsotopicDistribution(final IsotopicDistribution rhs) {
		this(rhs, true);
	}

	private IsotopicDistribution(final IsotopicDistribution rhs, final boolean init) {
		set(rhs);
		if (init) {
			doInit();
		}
	}

	private void set(final IsotopicDistribution rhs) {
		therMonoMZ = rhs.therMonoMZ;
		chem = rhs.chem;
		name = rhs.name;
		dist = new MassIntensityArray(rhs.dist);
		mostAbundant = rhs.mostAbundant;
		monoisotope = rhs.monoisotope;
		thresh = rhs.thresh;
		extra = rhs.extra;
	}

	@Override
	public IsotopicDistribution clone() {
		IsotopicDistribution distribution = null;
		try {
			distribution = (IsotopicDistribution) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new MprcException("Cannot clone " + IsotopicDistribution.class.getSimpleName(), e);
		}
		distribution.set(this);
		return distribution;
	}

	/**
	 * Returns a new copy of this IsotopicDistribution, with monoisotopic m/z set to {@code therMonoMZ}, with
	 * {@code mzShift} added to all mzs, and all intensities scaled by {@code intenScale}.
	 */
	public IsotopicDistribution copy(final double therMonoMZ, final double mzShift, final double intenScale) {
		final IsotopicDistribution ret = new IsotopicDistribution(this, false);
		ret.therMonoMZ = therMonoMZ;
		for (int i = 0; i < ret.dist.size(); ++i) {
			ret.dist.setMass(i, ret.dist.getMass(i) + mzShift);
			ret.dist.setIntensity(i, ret.dist.getIntensity(i) * intenScale);
		}
		ret.doInit();
		return ret;
	}

	public int getNumIsotopes() {
		return dist.size();
	}

	/**
	 * Returns the m/z of the num'th isotope.
	 */
	public double getMassOfIsotope(final int num) {
		return _getMassOfIsotope(num);
	}

	public double _getMassOfIsotope(final int num) {
		return dist.getMass(num);
	}

	/**
	 * Returns the intensity of the num'th isotope.  Currently this is a number between 0 and 100;
	 * scale appropriately (it really should be a fractional value).
	 */
	public double getIntensityOfIsotope(final int num) {
		return _getIntensityOfIsotope(num);
	}

	public double _getIntensityOfIsotope(final int num) {
		return dist.getIntensity(num);
	}

	/**
	 * Returns the isotope number of the most abundant isotope.
	 */
	public int getMostAbundantIsotope() {
		return mostAbundant;
	}

	/**
	 * Return which isotope is the monoisotopic.  Note that, for reasons explained above, theoretical isotopes may exist \b before the monoisotopic.
	 * Returns -1 if the monoisotope does not exist in the distribution; this can occur for highly enriched distributions.
	 */
	public int getMonoisotope() {
		return monoisotope;
	}

	public String toString() {
		final StringBuilder out = new StringBuilder();
		for (int i = 0; i < getNumIsotopes(); ++i) {
			out.append(getMassOfIsotope(i));
			out.append(" ");
			out.append(getIntensityOfIsotope(i));
			out.append(i == monoisotope ? " mono" : "");
			out.append(i == mostAbundant ? " max" : "");
			out.append("\n");
		}
		return out.toString();
	}

	public PrintWriter write(final PrintWriter out) {
		out.println(toString());
		return out;
	}

	/**
	 * Combines isotopic distributions.  Adds the given isotopic distribution, scaled by
	 * fracOther, to this IsotopicDistribution, scaled by 1 - fracOther.  Matches peaks
	 * to within the given error tolerance (peaks which don't match are simply scaled
	 * and passed through).
	 *
	 * @return New isotopic distribution representing {@code (1-fracOther)*this + fracOther*other}
	 */
	public IsotopicDistribution add(final IsotopicDistribution other, final double fracOther, double errorTolPPM) {
		// TODO: which Chemical?
		errorTolPPM /= 1000000.0;
		double maxinten = 0.0;
		int thisi = 0;
		int otheri = 0;
		final MassIntensityArray localDist = new MassIntensityArray();
		while (otheri < other.getNumIsotopes() && thisi < getNumIsotopes()) {
			final double othermass = other.getMassOfIsotope(otheri);
			final double thismass = getMassOfIsotope(thisi);
			final double inten;
			if (Math.abs(thismass - othermass) <= thismass * errorTolPPM) {
				inten = fracOther * other.getIntensityOfIsotope(otheri) + (1.0 - fracOther) * getIntensityOfIsotope(thisi);
				final double mass = (thismass + othermass) / 2.0;
				localDist.add(mass, inten);
				++thisi;
				++otheri;
			} else if (thismass < othermass) {
				inten = (1.0 - fracOther) * getIntensityOfIsotope(thisi);
				localDist.add(thismass, inten);
				++thisi;
			} else {
				inten = fracOther * other.getIntensityOfIsotope(otheri);
				localDist.add(othermass, inten);
				++otheri;
			}
			if (inten > maxinten) {
				maxinten = inten;
			}
		}
		// only one of the two loops below will run
		while (otheri < other.getNumIsotopes()) {
			final double inten = fracOther * other.getIntensityOfIsotope(otheri);
			localDist.add(other.getMassOfIsotope(otheri), inten);
			++otheri;
			if (inten > maxinten) {
				maxinten = inten;
			}
		}
		while (thisi < getNumIsotopes()) {
			final double inten = (1.0 - fracOther) * getIntensityOfIsotope(thisi);
			localDist.add(getMassOfIsotope(thisi), inten);
			++thisi;
			if (inten > maxinten) {
				maxinten = inten;
			}
		}

		return new IsotopicDistribution((therMonoMZ + other.therMonoMZ) / 2.0, chem, name, thresh, extra, localDist, true);
	}

	/**
	 * this nasty hack is necessary in part because the mercury code returns isotopes
	 * /before/ the monoisotope.  here we remove those, and find the most abundant.
	 */
	private void doInit() {
		int maxIsotope = 0;
		int monoIsotope = 0;
		if (dist.size() <= 1) {
			return;
		}
		for (int i = 0; i < dist.size(); ++i) {
			if (_getIntensityOfIsotope(i) < 0.0) {
				dist.setIntensity(i, 0.0);
			}
			if (_getIntensityOfIsotope(maxIsotope) < _getIntensityOfIsotope(i)) {
				maxIsotope = i;
			}
			if (Math.abs(_getMassOfIsotope(i) - therMonoMZ) < Math.abs(_getMassOfIsotope(monoIsotope) - therMonoMZ)) {
				monoIsotope = i;
			}
		}
		final double maxInten = _getIntensityOfIsotope(maxIsotope);
		int end = dist.size() - 1;
		int start = monoIsotope;

		// this is a cheezy upper bound on inter-isotope error.
		final double isotopeDiff = (_getMassOfIsotope(1) - _getMassOfIsotope(0));

		if (thresh >= 0.0) {
			while ((_getIntensityOfIsotope(end) / maxInten) < thresh && end >= 0) {
				--end;
			}
			end += extra;
			if (end >= dist.size()) {
				final int sz = end - dist.size() + 1;
				assert (dist.size() >= 2);
				final double dist = _getMassOfIsotope(this.dist.size() - 1) - _getMassOfIsotope(this.dist.size() - 2);
				double mass = _getMassOfIsotope(this.dist.size() - 1) + dist;
				for (int i = 0; i < sz; ++i) {
					this.dist.add(mass, 0.0);
					mass += dist;
				}
			}

			int add = extra - monoIsotope;

			if (add >= 0) {
				// if we don't have an entry for the monoisotope
				if (monoIsotope == 0 && (_getMassOfIsotope(0) - therMonoMZ) > isotopeDiff / 2.0) {
					final int monodist = (int) (Math.round((_getMassOfIsotope(0) - therMonoMZ) / isotopeDiff));
					monoIsotope = extra;
					add = monodist + extra;
					maxIsotope += add;
				} else if (add > 0) {
					monoIsotope += add;
					maxIsotope += add;
				}
				if (add > 0) {
					double fromMass = _getMassOfIsotope(0) - isotopeDiff;
					dist.insertNCopiesBeforeStart(add, 0, 0);
					end += add;
					for (int i = add - 1; i >= 0; --i) {
						dist.setMass(i, fromMass);
						fromMass -= isotopeDiff;
					}
					start = 0;
				} else {
					start = monoIsotope - extra;
				}
			} else {
				start = monoIsotope - extra;
			}
		}

		if (end != dist.size() - 1) {
			dist.erase(end + 1, dist.size());
		}

		if (start != 0) {
			dist.erase(0, start);
			maxIsotope -= start;
			monoIsotope -= start;
		}

		mostAbundant = maxIsotope;

		if (Math.abs(_getMassOfIsotope(monoIsotope) - therMonoMZ) > isotopeDiff) {
			// couldn't find the monoisotopic
			monoisotope = -1;
		} else {
			monoisotope = monoIsotope;
		}
	}

	/**
	 * Return the Chemical which was used to generated this isotopic Distribution.
	 * TODO: this should probably be a list of Chemicals
	 */
	Chemical getChemical() {
		return chem;
	}

	double getTheoreticalMonoisotopicMZ() {
		return therMonoMZ;
	}

	String getName() {
		return name;
	}

	void setName(final String name) {
		this.name = name;
	}

	double getThresh() {
		return thresh;
	}

	int getExtra() {
		return extra;
	}

	boolean isExtraIsotope(final int ison) {
		return ison < extra || ison >= dist.size() - extra;
	}

	private double therMonoMZ;
	private double thresh;
	private int extra;
	private Chemical chem;
	private String name;
	private MassIntensityArray dist;
	private int mostAbundant;
	private int monoisotope;
}
