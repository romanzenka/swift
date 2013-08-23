package edu.mayo.mprc.chem;

import edu.mayo.mprc.MprcException;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A collection of Elements. This is used all over the place.
 */
public final class PeriodicTable implements Cloneable {

	public PeriodicTable() {
	}

	/**
	 * Shallow copy. The returned PeriodicTable will share Elements with this.
	 */
	@Override
	public PeriodicTable clone() throws CloneNotSupportedException {
		final PeriodicTable copy = (PeriodicTable) super.clone();
		copy.elements = elements;
		copy.protonMass = protonMass;
		copy.electronMass = electronMass;
		return copy;
	}

	/**
	 * Adds an Element to this PeriodicTable.  This PeriodicTable takes ownership of the Element.
	 */
	public void addElement(final Element element) {
		elements.put(element.getSymbol(), element);
	}

	public void removeElement(final Element element) {
		final String symbol = element.getSymbol();
		final Element e = elements.get(symbol);
		if (e == null) {
			throw new MprcException("PeriodicTable does not contain element " + symbol);
		}
		if (elements.remove(symbol) == null) {
			throw new MprcException("PeriodicTable does not contain element " + symbol);
		}
	}

	/**
	 * Replace oldElement with newElement, which must have the same symbol.
	 */
	public void replaceElement(final Element oldElement, final Element newElement) {
		if (!oldElement.getSymbol().equals(newElement.getSymbol())) {
			throw new MprcException(MessageFormat.format("Symbols don''t match: {0} {1}", oldElement.getSymbol(), newElement.getSymbol()));
		}
		if (elements.put(newElement.getSymbol(), newElement) == null) {
			throw new MprcException("PeriodicTable does not contain element " + oldElement.getSymbol());
		}
	}

	public int getNumElements() {
		return elements.size();
	}

	/**
	 * Returns the Element with the given symbol.  If no such element exists, returns NULL.
	 */
	public Element getElementBySymbol(final String symbol) {
		return elements.get(symbol);
	}

	public double getElectronMass() {
		return electronMass;
	}

	public void setElectronMass(final double electronMass) {
		this.electronMass = electronMass;
	}

	public double getProtonMass() {
		return protonMass;
	}

	public void setProtonMass(final double protonMass) {
		this.protonMass = protonMass;
	}

	private Map<String, Element> elements = new LinkedHashMap<String, Element>();
	private double protonMass;
	private double electronMass;
}

