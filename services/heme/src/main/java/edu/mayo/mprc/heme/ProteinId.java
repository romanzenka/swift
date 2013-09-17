package edu.mayo.mprc.heme;

/**
 * Id of a protein - name
 *
 * @author Roman Zenka
 */
public final class ProteinId {
	private final String accNum;
	private final String description;
	private final Double massDelta;

	public ProteinId(String accNum, String description, Double massDelta) {
		this.accNum = accNum;
		this.description = description;
		this.massDelta = massDelta;
	}

	public String getAccNum() {
		return accNum;
	}

	public String getDescription() {
		return description;
	}

	public Double getMassDelta() {
		return massDelta;
	}
}
