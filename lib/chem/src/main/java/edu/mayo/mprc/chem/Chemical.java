package edu.mayo.mprc.chem;

import edu.mayo.mprc.MprcException;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A specific chemical compound that can be written as a single chemical formula of the
 * form <CODE>CH NH4 COOH (CH2)2SCH3</CODE>.  Spaces are allowed but not required in
 * chemical formulas.
 * <p/>
 * Note that chemicals can have negative numbers of elements; this is useful
 * for, eg, fragmentation patterns or modifications that result in the
 * loss of atoms.  A loss chemical formula would look like <CODE>C-1H-3</CODE>.
 */
public class Chemical implements Molecule, Cloneable {
	public Chemical(final PeriodicTable pt) {
		this(pt, "");
	}

	public Chemical(final PeriodicTable pt, final String name) {
		this.pt = pt;
		this.name = name;
		init();
	}

	public Chemical(final String formula, final PeriodicTable pt) {
		this(formula, pt, "");
	}

	public Chemical(final String formula, final PeriodicTable pt, final String name) {
		this(pt, name);
		setFormula(formula);
	}

	public Chemical(final Chemical rhs) {
		set(rhs);
	}

	private void set(final Chemical rhs) {
		this.pt = rhs.pt;
		this.name = rhs.name;
		this.sorted = rhs.sorted;
		this.elements = new ArrayList<ElementCountPair>(rhs.elements.size());
		for (final ElementCountPair e : rhs.elements) {
			this.elements.add(new ElementCountPair(e.getElement(), e.getCount()));
		}
		this.massesCalced = rhs.massesCalced;
		this.average = rhs.average;
		this.monoisotopic = rhs.monoisotopic;
		this.mostAbundant = rhs.mostAbundant;
	}

	public Chemical clone() throws CloneNotSupportedException {
		final Chemical copy = (Chemical) super.clone();
		copy.set(this);
		return copy;
	}

	public String getName() {
		if ("".equals(name)) {
			return getCanonicalFormula();
		} else {
			return name;
		}
	}

	void setName(final String name) {
		this.name = name;
	}

	/**
	 * Set the ChemicalFormula of this Chemical to formula.  Any existing elements are cleared.
	 */
	void setFormula(final String formula) {
		final Chemical c = parse(formula, "", 0);
		elements = c.elements;
		resetCached();
	}

	/**
	 * Adds the given number of atoms of element to this Chemical; if element already
	 * exists on this element, the number of atoms is incremented by count.
	 */
	void addElement(final Element element, final double atoms) {
		final ElementCountPair i = find(element);
		if (i == null) {
			//if (atoms < 0) throw InvalidArgumentException(String("Can't remove ") + element.getSymbol() + atoms
			//	+ " from " + getCanonicalFormula());
			elements.add(new ElementCountPair(element, atoms));
		} else {
			i.setCount(i.getCount() + atoms);
			if (i.getCount() == 0) {
				elements.remove(i);
			}
		}
		resetCached();
	}

	/**
	 * Removes the given number of atoms of element from this Chemical; if the
	 * Element does not already exist on this Chemical, an exception is thrown.
	 */
	void removeElement(final Element element, final double atoms) {
		addElement(element, -atoms);
		resetCached();
	}

	/**
	 * Returns the chemical formula of this Chemical in canonical format: with one
	 * instance of each element (in alphabetic order) followed by the number of atoms of
	 * that element that occur in this Chemical.
	 */
	String getCanonicalFormula() {
		if (!sorted) {
			Collections.sort(elements, new ElementLess());
			sorted = true;
		}
		final StringBuilder ret = new StringBuilder();
		for (final ElementCountPair pair : elements) {
			if (ret.length() > 0) {
				ret.append(" ");
			}

			ret.append(pair.getElement().getSymbol());
			if (pair.getCount() == (int) pair.getCount()) {
				ret.append((int) pair.getCount());
			} else {
				ret.append(pair.getCount());
			}
		}
		return ret.toString();
	}

	/**
	 * Returns the monoisotopic mass of this Chemical.  The monoisotopic mass is the mass
	 * that contains only the lowest mass isotopes.
	 */
	public double getMonoisotopicMass() {
		calcMasses();
		return monoisotopic;
	}

	/**
	 * Returns the average mass of this Chemical.  The average mass is an abundance weighted average
	 * of isotope masses.
	 */
	public double getAverageMass() {
		calcMasses();
		return average;
	}

	/**
	 * Returns the mass of the most abundant isotope species.
	 */
	double getMostAbundantMass() {
		calcMasses();
		return mostAbundant;
	}

	/**
	 * Returns a simulated isotopic distribution for this Chemical, given a
	 * charge state (for example, -11 for the 11- charge state). Uses the
	 * Mercury algorithm by Rockwood and Van Orden. This function is expensive; you should call it
	 * only once and store the result somewhere.
	 * <p/>
	 * See IsotopicDistribution for an explanation of <code>thresh</code> and <code>extra</code>.
	 * <p/>
	 *
	 * @param thresh (default -1)
	 * @param extra  (default 0)
	 */
	public IsotopicDistribution getIsotopicDistribution(final int chargeState, final ChargeUnit chargeUnit, final double thresh, final int extra) {
		final int abscharge = Math.abs(chargeState);
		final Chemical chem = new Chemical(this);
		if (chargeState > 0) {
			chem.addElement(chargeUnit.getChargeCarrier(), abscharge);
		} else if (chargeState < 0) {
			chem.removeElement(chargeUnit.getChargeCarrier(), abscharge);  // what about, for example, a Potassiated Protein?
		}
		final MassIntensityArray a = new MassIntensityArray();
		Mercury6.mercury6(chem, chargeState, a);

		return new IsotopicDistribution(
				chargeUnit.neutralToCharged(getMonoisotopicMass(), chargeState), this, "", thresh, extra, a);
	}

	/**
	 * Returns true if the given element appears in this Chemical.  Note that
	 * this will return true even if the number of atoms of that element
	 * is negative.
	 */
	boolean hasElement(final Element element) {
		return find(element) != null;
	}

	int getNumElements() {
		return elements.size();
	}

	final Element getElement(final int i) {
		return elements.get(i).getElement();
	}

	public double getElementCount(final int i) {
		return elements.get(i).getCount();
	}

	/**
	 * Returns the number of atoms of the given element that occur in a molecule of this Chemical.
	 * This number might be negative.
	 */
	double getAtomsOf(final Element element) {
		if (element == null) {
			throw new MprcException("Null element.");
		}
		final ElementCountPair i = find(element);
		if (i == null) {
			return 0;
		}
		return i.getCount();
	}

	/**
	 * Returns the number of atoms of Element i that occur in this Chemical.
	 */
	double getAtomCount(final int i) {
		return elements.get(i).getCount();
	}

	PeriodicTable getPeriodicTable() {
		return pt;
	}

	/**
	 * Switch to the given PeriodicTable, which must contain all
	 * the needed elements. Elements are identified by their symbols.
	 */
	void setPeriodicTable(final PeriodicTable pt) {
		for (final ElementCountPair i : elements) {
			final Element olde = i.getElement();
			final Element newe = pt.getElementBySymbol(olde.getSymbol());
			if (newe == null) {
				throw new MprcException("The given periodic table doesn't have " + olde.getSymbol());
			}
			i.setElement(newe);
		}
		this.pt = pt;
		resetCached();
	}

	public PrintWriter write(final PrintWriter out) {
		out.println(toString());
		return out;
	}

	public String toString() {
		final StringBuilder out = new StringBuilder();
		out.append(getName())
				.append(" (")
				.append(getCanonicalFormula())
				.append(") ")
				.append(getMonoisotopicMass());
		return out.toString();
	}

	/**
	 * Remove all elements from this Chemical.
	 */
	public void clear() {
		elements.clear();
		resetCached();
	}

	protected void resetCached() {
		massesCalced = false;
		sorted = false;
	}

	private Chemical parse(final String s, String full, final int where) {
		final Chemical ret = new Chemical(pt);
		if ("".equals(full)) {
			full = s;
		}

		Chemical c = null;
		final StringBuilder e = new StringBuilder();
		int n = 0;
		boolean neg = false;
		int elementStart = 0;
		int fraction = 0;

		for (int i = 0; i <= s.length(); ++i) {

			//if (!(islower(s[i]) || isdigit(s[i]) || s[i] == '-')) {
			if (i == s.length() || Character.isUpperCase(s.charAt(i))) {
				if (n == 0) {
					n = 1;
				}
				if (neg) {
					n *= -1;
				}
				if (e.length() > 0) {
					final Element ee = pt.getElementBySymbol(e.toString());
					if (ee == null) {
						final int markPosition = where + elementStart;
						final String marked = full.substring(0, markPosition) + ">>>" + full.substring(markPosition);
						throw new MprcException("Can't find element " + e.toString() + " in chemical formula:\n" + marked);
					}
					if (fraction == 0) {
						ret.addElement(ee, n);
					} else {
						ret.addElement(ee, (double) n / fraction);
					}
					e.setLength(0);
				} else if (c != null) {
					ret.add(c.multiply(n));
					c = null;
				}
				n = 0;
				neg = false;
				fraction = 0;
			}
			if (i == s.length()) {
				break;
			}
			if (s.charAt(i) == ' ') {
				continue;
			}
			if (s.charAt(i) == '(') {
				int parens = 1;
				int j;
				for (j = i + 1; j < s.length() && parens > 0; ++j) {
					if (s.charAt(j) == '(') {
						parens++;
					} else if (s.charAt(j) == ')') {
						parens--;
					}
				}
				final String sub = s.substring(i + 1, j - 1);
				c = parse(sub, full, i + 1);
				i = j - 1;
			} else if (Character.isUpperCase(s.charAt(i))) {
				elementStart = i;
				e.setLength(1);
				e.setCharAt(0, s.charAt(i));
			} else if (Character.isLowerCase(s.charAt(i))) {
				e.setLength(2);
				e.setCharAt(1, s.charAt(i));
			} else if (s.charAt(i) == '-') { // && isalpha(prev) && i != s.length() - 1) {
				neg = true;
			} else if (s.charAt(i) >= '0' && s.charAt(i) <= '9') {
				n *= 10;
				n += s.charAt(i) - '0';
				fraction *= 10;
			} else if (s.charAt(i) == '.') {
				fraction = 1;
			} else {
				final String marked = full.substring(0, where + i) + ">>>" + full.substring(where + i);
				throw new MprcException("Parse error, can't understand '" + s.charAt(i) + "':\n" + marked);
			}
		}
		return ret;
	}

	private void init() {
		massesCalced = false;
		sorted = false;
		monoisotopic = -1;
		average = -1;
		mostAbundant = -1;
	}

	private void calcMasses() {
		if (!massesCalced) {
			doCalcMasses();
		}
	}

	private ElementCountPair find(final Element element) {
		for (final ElementCountPair i : elements) {
			if (i.getElement().equals(element)) {
				return i;
			}
		}
		return null;
	}

	void doCalcMasses() {
		average = 0.;
		monoisotopic = 0.;
		mostAbundant = 0.;
		for (final ElementCountPair i : elements) {
			final Element elem = i.getElement();
			final double count = i.getCount();
			average += elem.getAverageMass() * count;
			monoisotopic += elem.getMonoisotopicMass() * count;
			mostAbundant += elem.getMostAbundantMass() * count;
		}
		massesCalced = true;

	}

	/**
	 * Adds another chemical to this one
	 */
	public Chemical add(final Chemical rhs) {
		if (rhs == this) {
			multiply(2);
		} else {
			for (final ElementCountPair pair : rhs.elements) {
				addElement(pair.getElement(), pair.getCount());
			}
		}
		resetCached();
		return this;
	}

	/**
	 * Sums two chemicals
	 */
	public static Chemical add(final Chemical a, final Chemical b) {
		final Chemical ret = cloneChemical(a);
		ret.add(b);
		return ret;
	}

	public Chemical subtract(final Chemical rhs) {
		if (rhs == this) {
			this.clear();
		} else {
			for (final ElementCountPair i : rhs.elements) {
				removeElement(i.getElement(), i.getCount());
			}
		}
		resetCached();
		return this;
	}

	public static Chemical subtract(final Chemical a, final Chemical b) {
		final Chemical ret = cloneChemical(a);
		ret.subtract(b);
		return ret;
	}

	/**
	 * Multiplies all the atom counts in given molecule by n.
	 */
	static Chemical multiply(final Chemical a, final double b) {
		final Chemical ret = cloneChemical(a);
		ret.multiply(b);
		return ret;
	}

	/**
	 * Clone chemical quietly, wrapping CloneNotSupportedException.
	 */
	private static Chemical cloneChemical(final Chemical chemical) {
		final Chemical ret;
		try {
			ret = chemical.clone();
		} catch (CloneNotSupportedException e) {
			throw new MprcException(e);
		}
		return ret;
	}

	/**
	 * Multiplies all the atom counts in this molecule by n.
	 */
	Chemical multiply(final double b) {
		if (b == 0) {
			this.clear();
		} else {
			for (final ElementCountPair pair : elements) {
				pair.setCount(pair.getCount() * b);
			}
		}
		resetCached();
		return this;
	}

	/**
	 * Round atom counts to nearest integer. This is important for the Mercury algorithm
	 * which cannot take real numbers for atom counts.
	 */
	public void roundAtomCounts() {
		for (final ElementCountPair pair : elements) {
			pair.setCount(Math.round(pair.getCount()));
		}
	}

	private PeriodicTable pt;
	private String name;

	private boolean sorted;
	private List<ElementCountPair> elements = new ArrayList<ElementCountPair>(5);

	private boolean massesCalced;
	private double average;
	private double monoisotopic;
	private double mostAbundant;

	private static final class ElementLess implements Comparator<ElementCountPair>, Serializable {
		private static final long serialVersionUID = 20081212L;

		@Override
		public int compare(final ElementCountPair o1, final ElementCountPair o2) {
			return o1.getElement().getSymbol().compareTo(o2.getElement().getSymbol());
		}
	}
}
