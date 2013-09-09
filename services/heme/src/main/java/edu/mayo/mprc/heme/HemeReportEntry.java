package edu.mayo.mprc.heme;

/**
 * @author Roman Zenka
 */
public final class HemeReportEntry {
	private String proteinAccNum;
	private String proteinDescription;
	private int totalSpectra;
	private double massDelta;

	public HemeReportEntry(String proteinAccNum, String proteinDescription, int totalSpectra, double massDelta) {
		this.proteinAccNum = proteinAccNum;
		this.proteinDescription = proteinDescription;
		this.totalSpectra = totalSpectra;
		this.massDelta = massDelta;
	}

	public void addSpectra(int spectra) {
		this.totalSpectra += spectra;
	}

	public String getProteinAccNum() {
		return proteinAccNum;
	}

	public String getProteinDescription() {
		return proteinDescription;
	}

	public int getTotalSpectra() {
		return totalSpectra;
	}

	public double getMassDelta() {
		return massDelta;
	}
}
